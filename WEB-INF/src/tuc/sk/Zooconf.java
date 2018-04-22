package tuc.sk;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContext;
import javax.servlet.annotation.WebListener;

import java.io.*;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.*;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import java.util.concurrent.CountDownLatch;
import java.util.*;
import org.json.*;
import java.security.*;

@WebListener
public class Zooconf implements ServletContextListener {
	private Properties serviceConfig = null;
	private ZooKeeper zk = null;
	final CountDownLatch connectedSignal = new CountDownLatch(1);
	private static Zooconf zooConfInstance = null;
	
	
    public void contextInitialized(ServletContextEvent sce) {
        System.err.println("Fileservice Context start initialization");
        initConfProperties(sce.getServletContext());
		Zooconf instance = getInstance();
		try {
			instance.zk = instance.zooConnect();
			instance.publishService();
		}
		catch (InterruptedException ex) {
			System.err.println("init InterruptedException");
		}
		catch (IOException ex) {
			System.err.println("init IOException");
		}
    }

    public void contextDestroyed(ServletContextEvent sce) {
        System.err.println("Fileservice Context destroyed");
        Zooconf instance = getInstance();
		try {
			if (instance.zk != null) {
				instance.zk.close();
			}
		}
		catch ( InterruptedException ex) {
			System.err.println("destroy InterruptedException");
		}
    }
    
	public static Zooconf getInstance() {
		if (zooConfInstance == null) {
			zooConfInstance = new Zooconf();
		}
		return zooConfInstance;
	}
	
	public static ZooKeeper getZooConnection() {
		Zooconf instance = getInstance();
		return instance.zk;
	}
	public static Properties getServiceConfig() {
		Zooconf instance = getInstance();
		return instance.serviceConfig;
	}
   	private void publishService() {
		// create ephemeral node to make the availability of this file service public
		Zooconf instance = getInstance();
		ACL acl = null;
		try {
			String base64EncodedSHA1Digest = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA1").digest((instance.serviceConfig.getProperty("ZOOKEEPER_USER")+":"+instance.serviceConfig.getProperty("ZOOKEEPER_PASSWORD")).getBytes()));
			acl = new ACL(ZooDefs.Perms.ALL, new Id("digest",instance.serviceConfig.getProperty("ZOOKEEPER_USER")+":" + base64EncodedSHA1Digest));
		}
		catch (NoSuchAlgorithmException ex) {
			System.err.println("destroy NoSuchAlgorithmException");
		}
		try {
			JSONObject data = new JSONObject();
			data.put("SERVERHOSTNAME", instance.serviceConfig.getProperty("SERVERHOSTNAME"));
			data.put("SERVER_PORT", instance.serviceConfig.getProperty("SERVER_PORT"));
			data.put("SERVER_SCHEME", instance.serviceConfig.getProperty("SERVER_SCHEME"));
			data.put("HMACKEY", instance.serviceConfig.getProperty("HMACKEY"));
			data.put("CONTEXT", instance.serviceConfig.getProperty("CONTEXT"));
			
//			zk.create("/FS/xxxxx"+this.serviceConfig.getProperty("ID"), ("dataxxxxxx").getBytes("UTF-8"), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);	
			instance.zk.create("/plh414/fileservices/"+instance.serviceConfig.getProperty("ID"), data.toString().getBytes("UTF-8"), Arrays.asList(acl), CreateMode.EPHEMERAL);	
		}	
		catch (KeeperException ex) {
			System.err.println("create destroy KeeperException");
		}
		catch (InterruptedException ex) {
			System.err.println("create destroy InterruptedException");
		}
		catch (UnsupportedEncodingException ex) {
			System.err.println("create destroy UnsupportedEncodingException");
		}
	}
	private void initConfProperties(ServletContext servletContext) {
		Zooconf instance = getInstance();
		InputStream input = null;
		try {
			input = servletContext.getResourceAsStream("/WEB-INF/config.properties");
			if (input != null) {
				instance.serviceConfig = new Properties();
				instance.serviceConfig.load(input);
				System.err.println("Loaded configuration");
			}
			else {
		        System.err.println("Fileservice configuration unavailable Error");
			}
			
			// TODO: check that all necessary parameters have been defined
		}
		catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	private ZooKeeper zooConnect() throws IOException,InterruptedException {
		System.err.println("start zooConnect");
		
		Properties config = getServiceConfig();
		ZooKeeper zk = new ZooKeeper(config.getProperty("ZOOKEEPER_HOST"), 3000, new Watcher() {
			@Override
			public void process(WatchedEvent we) {
				if (we.getState() == KeeperState.SyncConnected) {
					connectedSignal.countDown();
				}
			}
		});
		connectedSignal.await();
		
		zk.addAuthInfo("digest", new String(config.getProperty("ZOOKEEPER_USER")+":"+config.getProperty("ZOOKEEPER_PASSWORD")).getBytes()); 
		
		System.err.println("finished zooConnect");

		return zk;
	}
}
