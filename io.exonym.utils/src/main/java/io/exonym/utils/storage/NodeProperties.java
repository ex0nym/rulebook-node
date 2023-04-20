package io.exonym.utils.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;


public class NodeProperties {
	

	protected static final Logger logger = LogManager.getLogger(NodeProperties.class);
	private final String file = "resource/node.properties";
	private Properties properties = null; 
	
	public static final String FAILOVER_SOURCE = "failover-url-source";
	public static final String FAILOVER_SOURCE_USERNAME = "failover-source-username";
	public static final String FAILOVER_SOURCE_PASSWORD = "failover-source-password";
	
	public static final String FAILOVER_NODE = "failover-url-node";
	public static final String FAILOVER_NODE_USERNAME = "failover-node-username";
	public static final String FAILOVER_NODE_PASSWORD = "failover-node-password";
	
	public static final String SYNC_NETWORK = "sync-network";
	public static final String STORE_STATIC_MATERIALS = "store-static-material";
	
	// main url 
	// failover url
	// 
	private void createDefaultProperties()  {
		try (FileOutputStream fos = new FileOutputStream(file)){
			Properties p = createProperties();
			
			p.setProperty(FAILOVER_SOURCE, "https://existence.global/ccc-trust-network/x-source");
			p.setProperty(FAILOVER_SOURCE_USERNAME, "ftp-username");
			p.setProperty(FAILOVER_SOURCE_PASSWORD, "ftp-password");
			p.setProperty(FAILOVER_NODE, "https://existence.global/ccc-trust-network/x-node");
			p.setProperty(FAILOVER_NODE_USERNAME, "ftp-username");
			p.setProperty(FAILOVER_NODE_PASSWORD, "ftp-password");
			p.setProperty(SYNC_NETWORK, "false");
			p.setProperty(STORE_STATIC_MATERIALS, "false");

			p.store(fos, null);
			
			this.properties = p; 
			
		} catch (Exception e) {
			logger.error("Error", e);
			
		}
	}
	
	private Properties createProperties(){
		return new Properties(){
			
			private static final long serialVersionUID = 1L;

			@Override
		    public synchronized Enumeration<Object> keys() {
		        return Collections.enumeration(new TreeSet<Object>(super.keySet()));
		    }
		};
	}
	
	public synchronized Properties openProperties() throws Exception {
		try(FileInputStream fis = new FileInputStream(file)){
			Properties p = createProperties();
			p.load(fis);
			this.properties = p;
			if (this.properties.stringPropertyNames().isEmpty()){
				createDefaultProperties();
				
			}
			return this.properties;
			
		} catch (FileNotFoundException e) {
			createDefaultProperties();
			return this.properties;
			
		} catch (Exception e) {
			throw e;  
			
		}
	}
	
	public void savePropertes(){
		try (FileOutputStream fos = new FileOutputStream(file)){
			properties.store(fos, null);
			
		} catch (Exception e) {
			logger.error("Error", e);
			
		}
	}
	
	public Properties getProperties() {
		return properties;
		
	}
	
	public static void main(String[] args) {
		NodeProperties p = new NodeProperties();
		p.createDefaultProperties();
	}
}
