package io.exonym.lite.parallel;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ModelSingleSequence implements Runnable {
	
	private static final Logger logger = LogManager.getLogger(ModelSingleSequence.class);
	
	private final long pause;
	private Thread thread;
	private final String name;
	private boolean busy = false; 
	
	/**
	 * Does not start the thread.  You must call start.
	 * @param name
	 * @throws Exception
	 */
	public ModelSingleSequence(String name) throws Exception {
		this.pause=0;
		this.name=name;
		
	} 
	
	public ModelSingleSequence(String name, long pause) throws Exception {
		this.pause=pause;
		this.name=name;
	}
	
	public void start() {
		thread = new Thread(this, name);
		thread.start();
		
	}

	public boolean isBusy() {
		return busy;
	}

	@Override
	public void run() {
		logger.debug("Sequential Process Started");
		busy = true;
		try {
			Thread.sleep(pause);
			process();
			synchronized (this) {
				busy = false;
				this.notifyAll();
				
			}
		} catch (Exception e) {
			logger.error(Thread.currentThread().getName() + " error", e);
			
		}
		logger.debug("Sequential Process Ended");
	}
	
	/**
	 * The super constructor will begin a thread with the name specified and call process.  
	 * Once process has finished the thread will end.
	 * 
	 * It is advised to set the implemeting class visibility to package only.
	 *  
	 */
	protected abstract void process();

	protected Thread getThread() {
		return thread;
	}
	
}
