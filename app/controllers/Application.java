package controllers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import dto.PluginInstallInfo;
import dto.PluginInstallStat;

import play.*;
import play.libs.Json;
import play.mvc.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import utils.JedisPoolFactory;
import views.html.*;
import play.mvc.BodyParser;
public class Application extends Controller {
	
	public static JedisPoolFactory poolFactory = new JedisPoolFactory();
	private static final String FILE_TO_MATCH = System.getenv("FILE_TO_MATCH")==null?"site.xml":System.getenv("FILE_TO_MATCH");
	private static final String PLUGIN_INSTALL_COUNT = "install_count";
	private static final String REQUESTOR = "requestor";
	private static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";
	private static final String USER_AGENT="USER-AGENT";
	private static final SimpleDateFormat MMDDYYYY = new SimpleDateFormat("MMddyyyy");
	
	public static Result index() throws JsonProcessingException, IOException {
		JedisPool pool = poolFactory.getPool();
	    Jedis jedis = pool.getResource();
	    Integer installCount=0;
	    Set<String> requestorIPs = new HashSet<String>();
	    
	    try{
	    	Set<PluginInstallInfo> installs = getInstallsToDate(jedis.hvals(REQUESTOR));
		    for(PluginInstallInfo install:installs ){
		    	
		    	if(!requestorIPs.contains(install.getRequestorIP())){
		    		requestorIPs.add(install.getRequestorIP());
		    	}
		    }
	    	installCount = requestorIPs.size();
		}finally{
			pool.returnResource(jedis);
		}
		return ok(index.render(String.valueOf(installCount)));
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	public static Result dailyStats() throws JsonProcessingException, IOException {
		JedisPool pool = poolFactory.getPool();
	    Jedis jedis = pool.getResource();
	    String dailyStats;
	    
	    try{
	    	Set<PluginInstallInfo> installs = getInstallsToDate(jedis.hvals(REQUESTOR));
		    dailyStats = getDailyInstallStats(installs);
	    }finally{
			pool.returnResource(jedis);
		}
		return ok(dailyStats);
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	public static Result installCount(){
		JedisPool pool = poolFactory.getPool();
	    Jedis jedis = pool.getResource();
	    Integer installCount=0;
	    ObjectNode result = Json.newObject();
	    try{
	    	installCount = Integer.valueOf(jedis.get(PLUGIN_INSTALL_COUNT));
	    	result.put("installCount",installCount);
		}finally{
			pool.returnResource(jedis);
		}
	    return ok(result);
		
	}

	public static Result install(String file) {
		
		JedisPool pool = poolFactory.getPool();
	    Jedis jedis = pool.getResource();
		try{
			if(file.equalsIgnoreCase(FILE_TO_MATCH)){
				jedis.incr(PLUGIN_INSTALL_COUNT);
				PluginInstallInfo installInfo = new PluginInstallInfo(	new Date().getTime(),
																		request().headers().get(X_FORWARDED_FOR)[0],
																		request().headers().get(USER_AGENT)[0]);
				ObjectMapper mapper = new ObjectMapper();
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				try {
					mapper.writeValue(byteOut, installInfo);
					jedis.hset(	REQUESTOR,
								java.util.UUID.randomUUID().toString(), 
								new String(byteOut.toByteArray()));
				} catch (JsonGenerationException e) {
					System.out.println(String.format("install-error %s", e.getMessage()));
					e.printStackTrace();
				} catch (JsonMappingException e) {
					System.out.println(String.format("install-error %s", e.getMessage()));
					e.printStackTrace();
				} catch (IOException e) {
					System.out.println(String.format("install-error %s", e.getMessage()));
					e.printStackTrace();
				}
			}
			String absFilePath = "releases" + File.separatorChar + "v0.1.0-SNAPSHOT" + File.separatorChar + file;
			System.out.println(String.format("install %s", absFilePath));
			File theFile = new File(absFilePath);
			if (theFile.exists()) {
				return status(200, new File("releases" + File.separatorChar
						+ "v0.1.0-SNAPSHOT" + File.separatorChar + file));
			} else {
				return notFound(absFilePath);
			}
		}finally{
			pool.returnResource(jedis);
		}
		
	}
	
	private static Set<PluginInstallInfo> getInstallsToDate(List<String> redisData) throws JsonParseException, JsonMappingException, IOException{
		
		List<PluginInstallInfo> retList = new ArrayList<PluginInstallInfo>();
		TreeSet<PluginInstallInfo> sortestList = new TreeSet<PluginInstallInfo>(new Comparator<PluginInstallInfo>(){

			@Override
			public int compare(PluginInstallInfo o1, PluginInstallInfo o2) {
				if(new Date(o1.getInstallDt()).after(new Date(o2.getInstallDt()))){
					return -1;
				}else if(new Date(o1.getInstallDt()).before(new Date(o2.getInstallDt()))){
					return 1;
				}else{
					return 0;
				}
			}
			
		});
		ObjectMapper mapper = new ObjectMapper();
		for(String installData:redisData){
			sortestList.add(mapper.readValue(installData, PluginInstallInfo.class));
		}
		
		return sortestList;
		
	}
	
	private static String getDailyInstallStats(Set<PluginInstallInfo> installs) throws JsonGenerationException, JsonMappingException, IOException {
		
		Map<String,PluginInstallStat> dailyStats = new HashMap<String,PluginInstallStat>();
		PluginInstallStat stat;
		for(PluginInstallInfo install:installs){
			stat = dailyStats.get(MMDDYYYY.format(new Date(install.getInstallDt())));
			if(stat==null){
				stat = new PluginInstallStat(MMDDYYYY.format(new Date(install.getInstallDt())),1);
				dailyStats.put(MMDDYYYY.format(new Date(install.getInstallDt())),stat);
			}else{
				stat.incrementCount();
			}
		}
		TreeSet<PluginInstallStat> sortedList = new TreeSet<PluginInstallStat>(new Comparator<PluginInstallStat>(){

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