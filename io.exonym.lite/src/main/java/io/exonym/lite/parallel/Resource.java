package io.exonym.lite.parallel;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Resource<T> extends ModelLoopedInstruction {

	private static final Logger logger = LogManager.getLogger(Resource.class);
	private final ArrayList<T> resource = new ArrayList<>();
	private final int resourceSize;
	private final ResourceGenerator<T> generator;

	public Resource(String name, ResourceGenerator<T> generator, int resourceSize) throws Exception {
		super(name);
		if (resourceSize > 20){
			throw new Exception("Max resource size is 20");
			
		}
		this.resourceSize = (resourceSize--);
		this.generator = generator;
		this.start();
		
	}
	
	public T getResource(){
		try {
			T t = null;
			if (!resource.isEmpty()){
				logger.info("Resource was not empty: " + generator);
				t = resource.get(0);
				
			}
			if (t==null){
				logger.info("Resource size: " + resource.size());
				
				while(resource.isEmpty()){
					synchronized (resource) { // Notify me when resource is available.
						logger.info("[#ref0134] Waiting on resource " + generator);
						resource.wait(); 
						t = resource.get(0);
						if (t!=null){
							logger.info("[#ref0134] Got resource!!!!");
							
						}
					}
				} 
			}
			resource.remove(t);
			
			synchronized (this) { // Notify that the list is depleated.
				notifyAll();
				
			}
			return t;
				
		} catch (InterruptedException e) {
			logger.info("Interrupt");
			return null;
			
		}
	}
	
	private void addResource(T t){
		synchronized (resource) {
			logger.info("Adding resource " + t);
			resource.add(t);
			resource.notifyAll();
			
		}
	}

	@Override
	protected void loop() {
		try {
			while(resource.size() < resourceSize){
				T t = generator.generateResource();
				addResource(t);
				
			}
			synchronized (this) {
				try {
					wait();
					
				} catch (InterruptedException e) {
					logger.info("Interrupt");
					
				}
			}
		} catch (Exception e) {
			logger.info("Generator caught an exception. Programming error", e);
			shutdown();
			
		}
	}

	public void shutdown() {
		try {
			this.close();
			
		} catch (Exception e) {
			logger.info("Closing error", e);	
			
		}
	}

	public ResourceGenerator<T> getGenerator() {
		return generator;
		
	}
}
