package io.exonym.utils;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class SequentialThreadedProcess implements Runnable{

	private static final Logger logger = LogManager.getLogger(SequentialThreadedProcess.class);
	private final long pause;
	private Thread thread;
	
	public SequentialThreadedProcess(String name) {
		this.pause=0;
		start(name); 
		
	} 
	
	public SequentialThreadedProcess(String name, long pause) {
		this.pause=pause;
		start(name);
		
	}
	
	private void start(String name) {
		thread = new Thread(this, name);
		thread.start();
		
	}
	
	
	@Override
	public void run() {
		try {
			Thread.sleep(pause);
			process();
			
		} catch (Exception e) {
			logger.error(Thread.currentThread().getName() + " error", e);
			
		}
		logger.debug("Sequential Process Ended");
	}
	
	/**
	 * The super constructor will begin a thread with the name specified and call process.  
	 * Once process has finished the thread will end.
	 * 
	 */
	protected abstract void process();

	public Thread getThread() {
		return thread;
	}
	
}
