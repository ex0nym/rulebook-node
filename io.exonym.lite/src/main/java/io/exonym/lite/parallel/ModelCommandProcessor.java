package io.exonym.lite.parallel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;



public abstract class ModelCommandProcessor implements Runnable {
	
	private static final Logger logger = LogManager.getLogger(ModelCommandProcessor.class);
	
	private volatile Thread thread;
	private ArrayBlockingQueue<Msg> pipeIn; 
	private long timeoutMs;
	private boolean busy = false;
	protected final String name;

	/**
	 * 
	 * @param queueSize Set to larger values the longer it takes to process the messages. 
	 * @param threadName Name the thread something sensible so that you know where to find it in the code base.
	 * @param timeoutMs use 0 if there are no period jobs
	 */
	protected ModelCommandProcessor(int queueSize, String threadName, long timeoutMs) {
		this.pipeIn = new ArrayBlockingQueue<>(queueSize);
		thread = new Thread(this, threadName);
		this.name = threadName;
		thread.start();
		this.timeoutMs=timeoutMs;
		
	}

	@Override
	public void run() {
		Thread currentThread = Thread.currentThread();
		logger.info("Started " + currentThread.getName());
		
		while(currentThread==thread){
			try {
				Msg msg = null;
				if (timeoutMs==0){
					msg = this.pipeIn.take();
					
				} else {
					msg = this.pipeIn.poll(timeoutMs, TimeUnit.MILLISECONDS);
					
				}
				if (msg!=null){
					setBusy(msg);
					
				} else {
					periodOfInactivityProcesses();
					
				}
			} catch (InterruptedException e) {
				logger.debug("Interrupt");
				
			} catch (Exception e) {
				logger.error("Error at Command Manager", e);
				
			}
		}
		logger.info("Command Processor Ended");
		
	}

	private synchronized void setBusy(Msg msg) {
		this.busy = true; 
		receivedMessage(msg);
		this.busy = false;
		
	}
	
	protected synchronized boolean isBusy(){
		return busy;
		
	}

	protected abstract void periodOfInactivityProcesses(); 

	protected abstract void receivedMessage(Msg msg);
	
	protected ArrayBlockingQueue<Msg> getPipe(){
		return pipeIn;
		
	}

	protected void close() throws Exception {
		if (thread!=null){ 
			this.thread.interrupt();
			
		}
		this.thread=null;
		synchronized (this) {
			notifyAll();
			
		}
	}
	
	protected void extendThreadName(String parenthesis){
		String prefix = this.thread.getName();
		this.thread.setName(prefix + "[" +parenthesis + "]");
		
	}
	
	protected void removeExtension(){
		String name = this.thread.getName();
		int p = name.indexOf('[');
		if (p>0){
			name = name.substring(0, p);
			this.thread.setName(name);
			
		}
	}

	public long getTimeoutMs() {
		return timeoutMs;
	}
}
