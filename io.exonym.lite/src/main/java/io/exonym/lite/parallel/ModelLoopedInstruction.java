package io.exonym.lite.parallel;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ModelLoopedInstruction implements Runnable {
	

	private static final Logger logger = LogManager.getLogger(ModelLoopedInstruction.class);
	private volatile Thread thread; 
	
	/**
	 * You must call start when it is in an acceptable state. 
	 * 
	 * @param name
	 */
	public ModelLoopedInstruction(String name) {
		thread = new Thread(this, name);
	
	}
	
	protected void start(){
		this.thread.start();
		
	}
	
	@Override
	public void run() {
		Thread thisThread = Thread.currentThread();
		while(thisThread==thread){
			try {
				loop();
				
			} catch (Exception e) {
				logger.error("Loop Threaded Process Error ", e);
				
			}
		}
	}

	protected abstract void loop();


	protected void close() {
		logger.info("Called close on " + this);
		try {
			if (this.thread!=null){
				this.thread.interrupt();
				
			}
			this.thread=null;
			synchronized (this) {
				notifyAll();
				
			}
		} catch (Exception e) {
			logger.info("Closing error ", e);
			
		}
	}
}
