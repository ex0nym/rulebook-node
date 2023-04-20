package io.exonym.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;


public abstract class AbstractXNodeProperties {

	private static final Logger logger = LogManager.getLogger(AbstractXNodeProperties.class);

	// protected final String file = "resource/xnode.properties";
	protected final String file = "resource/cloudant.properties";
	protected Properties properties = null; 
	
	protected abstract void addDefaultProperties(Properties p);

	protected Properties createProperties(){
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
				addDefaultProperties(this.properties);
				saveProperties();
				
			}
			return this.properties;
			
		} catch (FileNotFoundException e) {
			createNewProperties();

			return this.properties;
			
		} catch (Exception e) {
			throw e;  
			
		}
	}
	
	private void createNewProperties() {
		try (FileOutputStream fos = new FileOutputStream(file)){
			this.properties = new Properties();
			addDefaultProperties(properties);
			this.properties.store(fos, null);
			
		} catch (Exception e) {
			logger.error("Error ", e);
			
		}
	}

	public void saveProperties(){
		try (FileOutputStream fos = new FileOutputStream(file)){
			properties.store(fos, null);
			
		} catch (Exception e) {
			logger.error("Error", e);
			
		}
	}
	
	public Properties getProperties() {
		return properties;
		
	}	
}
