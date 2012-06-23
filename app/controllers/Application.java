package controllers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import dto.PluginInstallInfo;

import play.*;
import play.mvc.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import utils.JedisPoolFactory;
import views.html.*;

public class Application extends Controller {
	
	public static JedisPoolFactory poolFactory = new JedisPoolFactory();
	private static final String FILE_TO_MATCH = System.getenv("FILE_TO_MATCH")==null?"site.xml":System.getenv("FILE_TO_MATCH");
	private static final String PLUGIN_INSTALL_COUNT = "install_count";
	public static Result index() {
		return ok(index.render("Your new application is ready."));
	}

	public static Result install(String file) {
		
		JedisPool pool = poolFactory.getPool();
	    Jedis jedis = pool.getResource();
		try{
			if(file.equalsIgnoreCase(FILE_TO_MATCH)){
				jedis.incr(PLUGIN_INSTALL_COUNT);
				PluginInstallInfo installInfo = new PluginInstallInfo(new Date().getTime(),request().headers().get("HOST")[0]);
				ObjectMapper mapper = new ObjectMapper();
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				try {
					mapper.writeValue(byteOut, installInfo);
					jedis.set(	java.util.UUID.randomUUID().toString(), 
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

}