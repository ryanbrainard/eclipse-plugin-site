package utils;

import org.apache.commons.pool.impl.GenericObjectPool.Config;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JedisPoolFactory {

    protected final Pattern HEROKU_REDISTOGO_URL_PATTERN = Pattern.compile("^redis://([^:]*):([^@]*)@([^:]*):([^/]*)(/)?");

    protected static JedisPool pool;

    public JedisPool getPool() {
        if (System.getenv("REDISTOGO_URL") != null && System.getenv("REDISTOGO_URL").length() > 0) {
            if (JedisPoolFactory.pool == null) {
                Config config = new Config();
                config.testOnBorrow = true;
                Matcher matcher = HEROKU_REDISTOGO_URL_PATTERN.matcher(System.getenv("REDISTOGO_URL"));
                matcher.matches();
                JedisPoolFactory.pool = new JedisPool(config, matcher.group(3), Integer.parseInt(matcher.group(4)), Protocol.DEFAULT_TIMEOUT, matcher.group(2));
            }

            return JedisPoolFactory.pool;
        }
        return null;
    }

}