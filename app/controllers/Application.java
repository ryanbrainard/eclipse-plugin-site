package controllers;

import dto.PluginInstallInfo;
import dto.PluginInstallStat;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.JedisPool;
import utils.JedisPoolFactory;
import views.html.index;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Application extends Controller {

    public static JedisPoolFactory poolFactory = new JedisPoolFactory();
    private static final String FILE_TO_MATCH = System.getenv("FILE_TO_MATCH") == null ? "site.xml" : System.getenv("FILE_TO_MATCH");
    private static final String PLUGIN_INSTALL_COUNT = "install_count";
    private static final String REQUESTOR = "requestor";
    private static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";
    private static final String USER_AGENT = "USER-AGENT";
    private static final SimpleDateFormat MMDDYYYY = new SimpleDateFormat("MMddyyyy");

    static interface JedisRunnable<V> {
        V without();

        V with(JedisCommands jedis) throws IOException;
    }

    static <V> V withRedis(JedisRunnable<V> runnable) {
        JedisPool pool = poolFactory.getPool();

        if (pool == null) {
            return runnable.without();
        }

        Jedis jedis = pool.getResource();

        if (jedis == null) {
            return runnable.without();
        }

        try {
            return runnable.with(jedis);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            pool.returnResource(jedis);
        }
    }

    public static Result index() throws IOException {
        final Integer installCount = withRedis(new JedisRunnable<Integer>() {
            @Override
            public Integer without() {
                return -1;
            }

            @Override
            public Integer with(JedisCommands jedis) throws IOException {
                Set<String> requestorIPs = new HashSet<String>();
                Set<PluginInstallInfo> installs = getInstallsToDate(jedis.hvals(REQUESTOR));
                for (PluginInstallInfo install : installs) {

                    if (!requestorIPs.contains(install.getRequestorIP())) {
                        requestorIPs.add(install.getRequestorIP());
                    }
                }
                return requestorIPs.size();
            }
        });

        return ok(index.render((String.valueOf(installCount))));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result dailyStats() throws IOException {
        final String dailyStats = withRedis(new JedisRunnable<String>() {
            @Override
            public String without() {
                return "";
            }

            @Override
            public String with(JedisCommands jedis) throws IOException {
                Set<PluginInstallInfo> installs = getInstallsToDate(jedis.hvals(REQUESTOR));
                return getDailyInstallStats(installs);
            }
        });

        return ok(dailyStats);
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result installCount() {
        final Integer installCount = withRedis(new JedisRunnable<Integer>() {
            @Override
            public Integer without() {
                return 0;
            }

            @Override
            public Integer with(JedisCommands jedis) throws IOException {
                return Integer.valueOf(jedis.get(PLUGIN_INSTALL_COUNT));
            }
        });

        ObjectNode result = Json.newObject();
        result.put("installCount", installCount);
        return ok(result);
    }

    public static Result install(final String relFilePath) {
        withRedis(new JedisRunnable<Void>() {
            @Override
            public Void without() {
                return null;
            }

            @Override
            public Void with(JedisCommands jedis) throws IOException {
                if (relFilePath.equalsIgnoreCase(FILE_TO_MATCH)) {
                    jedis.incr(PLUGIN_INSTALL_COUNT);
                    PluginInstallInfo installInfo = new PluginInstallInfo(new Date().getTime(),
                            request().headers().get(X_FORWARDED_FOR)[0],
                            request().headers().get(USER_AGENT)[0]);
                    ObjectMapper mapper = new ObjectMapper();
                    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                    mapper.writeValue(byteOut, installInfo);
                    jedis.hset(REQUESTOR,
                            java.util.UUID.randomUUID().toString(),
                            new String(byteOut.toByteArray()));
                }

                return null;
            }
        });

        String absFilePath = "release" + File.separatorChar + relFilePath;
        System.out.println(String.format("install %s", absFilePath));
        File file = new File(absFilePath);
        if (file.exists()) {
            return status(200, file);
        } else {
            return notFound(absFilePath);
        }
    }

    private static Set<PluginInstallInfo> getInstallsToDate(List<String> redisData) throws JsonParseException, JsonMappingException, IOException {

        List<PluginInstallInfo> retList = new ArrayList<PluginInstallInfo>();
        TreeSet<PluginInstallInfo> sortestList = new TreeSet<PluginInstallInfo>(new Comparator<PluginInstallInfo>() {

            @Override
            public int compare(PluginInstallInfo o1, PluginInstallInfo o2) {
                if (new Date(o1.getInstallDt()).after(new Date(o2.getInstallDt()))) {
                    return -1;
                } else if (new Date(o1.getInstallDt()).before(new Date(o2.getInstallDt()))) {
                    return 1;
                } else {
                    return 0;
                }
            }

        });
        ObjectMapper mapper = new ObjectMapper();
        for (String installData : redisData) {
            sortestList.add(mapper.readValue(installData, PluginInstallInfo.class));
        }

        return sortestList;

    }

    private static String getDailyInstallStats(Set<PluginInstallInfo> installs) throws JsonGenerationException, JsonMappingException, IOException {

        Map<String, PluginInstallStat> dailyStats = new HashMap<String, PluginInstallStat>();
        PluginInstallStat stat;
        for (PluginInstallInfo install : installs) {
            stat = dailyStats.get(MMDDYYYY.format(new Date(install.getInstallDt())));
            if (stat == null) {
                stat = new PluginInstallStat(MMDDYYYY.format(new Date(install.getInstallDt())), 1);
                dailyStats.put(MMDDYYYY.format(new Date(install.getInstallDt())), stat);
            } else {
                stat.incrementCount();
            }
        }
        TreeSet<PluginInstallStat> sortedList = new TreeSet<PluginInstallStat>(new Comparator<PluginInstallStat>() {

            @Override
            public int compare(PluginInstallStat o1, PluginInstallStat o2) {
                return o1.getInstallDt().compareTo(o2.getInstallDt());
            }

        });
        sortedList.addAll(dailyStats.values());
        ObjectMapper mapper = new ObjectMapper();
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        mapper.writeValue(byteOut, sortedList);
        return byteOut.toString();

    }


}