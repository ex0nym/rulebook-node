package io.exonym.utils.storage;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * todo Must migrate to Jupiter
 */
public class UtilsSuite {
	
	private static final Logger logger = LogManager.getLogger(UtilsSuite.class);
	public static String XC0 = "xc0-test-container";
	public static String XC1 = "xc1-test-container";

	public UtilsSuite() {
		
	}
	
	public static void setup(){
		try {
			XContainer xc1 = new XContainer(XC1, true);
			xc1.delete();
			
		} catch (Exception e) {
			logger.info("XC1 did not exist");
			
		}

		try {
			XContainer base = new XContainer(XC0, true);
			logger.debug("Created container " + base.getUsername());
			
		} catch (Exception e) {
			try {
				XContainer base = new XContainer(XC0);
				base.delete();
				base = new XContainer(XC0, true);
				logger.debug("Created container after emptying an old container " + base.getUsername());
				
			} catch (Exception e1) {
				logger.error("Unexpected Error", e1);
				
			}
		}
	}	
}
