package io.exonym.actor.actions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ContainerSuite {
	
	private static final Logger logger = LogManager.getLogger(ContainerSuite.class);
	
	public static String XC0 = "xc0-json-test-container";
	public static String XC1 = "xc1-json-test-container";
	

	public ContainerSuite() {
	}
	
	public static void setup(){
		try {
			IdContainerJSON xc1 = new IdContainerJSON(XC1, true);
			xc1.delete();
			
		} catch (Exception e) {
			logger.info("XC1 did not exist");
			
		}

		try {
			IdContainerJSON base = new IdContainerJSON(XC0, true);
			logger.debug("Created container " + base.getUsername());
			
		} catch (Exception e) {
			try {
				IdContainerJSON base = new IdContainerJSON(XC0);
				base.delete();
				base = new IdContainerJSON(XC0, true);
				logger.debug("Created container after emptying an old container " + base.getUsername());
				
			} catch (Exception e1) {
				logger.error("Unexpected Error", e1);
				
			}
		}
	}	
	

}
