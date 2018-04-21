package tuc.sk;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
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
	private static Properties serviceConfig = null;
	private ZooKeeper zk = null;
	final CountDownLatch connectedSignal = new CountDownLatch(1);
	private static Zooconf zooConfInstance = null;
	
	public static Zooconf getInstance() {
		if (zooConfInstance == null) {
			zooConfInstance = new Zooconf();
		}
		return zooConfInstance;
	}
	
	public Properties getServiceConfig() {
		return serviceConfig;
	}
	
    public void contextInitialized(ServletContextEvent sce) {
        System.err.println("Fileservice Context start initialization");
		InputStream input = null;
		try {
			input = sce.getServletContext().getResourceAsStream("/WEB-INF/config.properties");
			if (input != null) {
				this.serviceConfig = new Properties();
				this.serviceConfig.load(input);
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
		try {
			this.zk = this.zooConnect();
			this.publishService();
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
		try {
			if (zk != null) {
				zk.close();
			}
		}
		catch ( InterruptedException ex) {
			System.err.println("destroy InterruptedException");
		}
    }
   	private void publishService() {
		// create ephemeral node to make the availability of this file service public
		ACL acl = null;
		try {
			String base64EncodedSHA1Digest = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA1").digest((this.serviceConfig.getProperty("ZOOKEEPER_USER")+":"+this.serviceConfig.getProperty("ZOOKEEPER_PASSWORD")).getBytes()));
			acl = new ACL(ZooDefs.Perms.ALL, new Id("digest",this.serviceConfig.getProperty("ZOOKEEPER_USER")+":" + base64EncodedSHA1Digest));
		}
		catch (NoSuchAlgorithmException ex) {
			System.err.println("destroy NoSuchAlgorithmException");
		}
		try {
			JSONObject data = new JSONObject();
			data.put("SERVERHOSTNAME", serviceConfig.getProperty("SERVERHOSTNAME"));
			data.put("SERVER_PORT", serviceConfig.getProperty("SERVER_PORT"));
			data.put("SERVER_SCHEME", serviceConfig.getProperty("SERVER_SCHEME"));
			data.put("HMACKEY", serviceConfig.getProperty("HMACKEY"));
			data.put("SERVERHOSTNAME", serviceConfig.getProperty("SERVERHOSTNAME"));
			data.put("CONTEXT", serviceConfig.getProperty("CONTEXT"));
			
//			zk.create("/FS/xxxxx"+this.serviceConfig.getProperty("ID"), ("dataxxxxxx").getBytes("UTF-8"), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);	
			zk.create("/FS/xxxxx"+this.serviceConfig.getProperty("ID"), data.toString().getBytes("UTF-8"), Arrays.asList(acl), CreateMode.EPHEMERAL);	
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
	
	private ZooKeeper zooConnect() throws IOException,InterruptedException {
		System.err.println("start zooConnect");
		ZooKeeper zk = new ZooKeeper(this.serviceConfig.getProperty("ZOOKEEPER_HOST"), 3000, new Watcher() {
			@Override
			public void process(WatchedEvent we) {
				if (we.getState() == KeeperState.SyncConnected) {
					connectedSignal.countDown();
				}
			}
		});
		connectedSignal.await();
		
		zk.addAuthInfo("digest", new String(this.serviceConfig.getProperty("ZOOKEEPER_USER")+":"+this.serviceConfig.getProperty("ZOOKEEPER_PASSWORD")).getBytes()); 
		
		System.err.println("finished zooConnect");

		return zk;
	}
}
