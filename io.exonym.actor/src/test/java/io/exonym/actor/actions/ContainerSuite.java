package io.exonym.actor.actions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.exonym.actor.actions.XContainerJSON;

public class ContainerSuite {
	
	private static final Logger logger = LogManager.getLogger(ContainerSuite.class);
	
	public static String XC0 = "xc0-json-test-container";
	public static String XC1 = "xc1-json-test-container";
	

	public ContainerSuite() {
	}
	
	public static void setup(){
		try {
			XContainerJSON xc1 = new XContainerJSON(XC1, true);
			xc1.delete();
			
		} catch (Exception e) {
			logger.info("XC1 did not exist");
			
		}

		try {
			XContainerJSON base = new XContainerJSON(XC0, true);
			logger.debug("Created container " + base.getUsername());
			
		} catch (Exception e) {
			try {
				XContainerJSON base = new XContainerJSON(XC0);
				base.delete();
				base = new XContainerJSON(XC0, true);
				logger.debug("Created container after emptying an old container " + base.getUsername());
				
			} catch (Exception e1) {
				logger.error("Unexpected Error", e1);
				
			}
		}
	}	
	

}
