package io.exonym.lite.parallel;

import io.exonym.lite.time.Timing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public abstract class ModelSingleAck implements Runnable {
	
	private static final Logger logger = LogManager.getLogger(ModelSingleAck.class);
	private Msg msg = null;
	private final long timeoutMs;
	private boolean timedOut = false; 
	
	protected ModelSingleAck(String name, long timeoutMs) throws Exception {
		Thread thread = new Thread(this, name);
		thread.start();
		if (timeoutMs==0){
			throw new Exception("Zero timeout is not allowed");
			
		}
		this.timeoutMs=timeoutMs;
		
	}

	@Override
	public void run() {
		init();
		try {
			if (msg==null){
				synchronized (this) {
					logger.debug("Waiting for message return");
					long start = Timing.currentTime();
					this.wait(timeoutMs);
					if (Timing.hasBeen(start, timeoutMs)){
						timedOut=true;
						timeOut();
						
					} else {
						message(this.msg);
						
					}
					logger.debug("Received Notify or Timeout");
					
				}
			} else {
				logger.warn("Didn't need to wait for the message.  This is unexpected.  Processing anyway.");
				message(msg);
				
			}
		} catch (Exception e) {
			logger.error("Error waiting ", e);
			
		}
		logger.debug("Single Response Processor Ended. TimedOut(" + timedOut + ")");
		
	}
	
	protected void setMsg(Msg msg) {
		synchronized (this) {
			if (msg==null){
				logger.error("Message was null when set");
				
			}
			this.msg = msg;	
			notify();
			
		}
	}
	
	protected boolean isTimedOut(){
		return timedOut;
		
	}

	/**
	 * Process the message that was received.
	 * 
	 * @param msg
	 */
	protected abstract void message(Msg msg);

	/**
	 * Process time out actions
	 * 
	 */
	protected abstract void timeOut();

	/**
	 * The first tasks that must be done by this processor.
	 * <p>It will wait for the command or Time out  
	 */
	protected abstract void init();

}
