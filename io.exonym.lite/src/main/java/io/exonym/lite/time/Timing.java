package io.exonym.lite.time;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Timing {
	

	private static final Logger logger = LogManager.getLogger(Timing.class);
	public static long currentTime(){
		return System.currentTimeMillis();
		
	} 
	
	public static boolean hasBeen(long start, long ms){
		long error = (long) (ms * 0.002);
		if ((System.currentTimeMillis() - (start-error)) >= ms){
			return true; 
			
		} else {
			return false;
			
		}
	}
	public static long hasBeenMs(long start){
		return (System.currentTimeMillis() - start); 
		
	}
}