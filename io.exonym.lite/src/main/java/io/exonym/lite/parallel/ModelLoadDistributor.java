package io.exonym.lite.parallel;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;


public class ModelLoadDistributor<T> implements AutoCloseable{
	
	private static final Logger logger = LogManager.getLogger(ModelLoadDistributor.class);


	private final LoadManager loadManager;
	private final ArrayBlockingQueue<Msg> loadManagerPipe;
	private boolean finished = false; 
	
	protected final ConcurrentHashMap<URI, T> resultMap = new ConcurrentHashMap<>();
	
	public ModelLoadDistributor(String taskName, int cylinders) {

		loadManager = new LoadManager(taskName, cylinders);
		loadManagerPipe = loadManager.getPipe();
		
	}
	
	public void go(ResultGenerator<T> generator, boolean first, boolean last) throws InterruptedException{
		if (first == true && last == false){
			this.finished = false;
			
		}
		this.loadManagerPipe.put(new Item(generator, last));
	
	}

	public boolean isFinished() {
		return finished;

	}

	@Override
	public void close() throws Exception {
		loadManager.close();

	}

	private class LoadManager extends ModelCommandProcessor {
		
		private final ArrayList<ResultProcessor<T>> processors = new ArrayList<>();
		private final ArrayList<ResultProcessor<T>> pipes = new ArrayList<>();
		private final ResultCollector collector = new ResultCollector();
		private int lastCylinder = 0;
		private final int cylinders; 

		protected LoadManager(String taskName, int cylinders) {
			super(cylinders, taskName + ":LoadManager", 0);
			this.cylinders = cylinders;
			for (int i = 0; i < cylinders; i++) {
				ResultProcessor<T> p = new ResultProcessor<>(taskName + i, collector.getPipe());
				processors.add(p);
				pipes.add(p);
				
			}
		}


		@Override
		protected void receivedMessage(Msg msg) {
			try {
				pipes.get(lastCylinder).getPipe().put(msg);
				lastCylinder++; 
				lastCylinder = (lastCylinder % cylinders);
					
			} catch (InterruptedException e) {
				logger.error("Error", e);
				
			}
		}
		
		@Override
		protected void periodOfInactivityProcesses() {}

		@Override
		protected void close() throws Exception {

			logger.debug("Closing");

			for (ResultProcessor<T> p : processors){
				p.close();

			}
			this.collector.close();
			super.close();

		}
	}

	private class ResultProcessor<R> extends ModelCommandProcessor {

		private final ArrayBlockingQueue<Msg> pipeToCollector;

		protected ResultProcessor(String taskName, ArrayBlockingQueue<Msg> pipeToCollector) {
			super(3, taskName + ":LoadMngr:Worker", 0);
			this.pipeToCollector=pipeToCollector;

		}

		@Override
		protected void receivedMessage(Msg msg) {
			try {
				Item item = (Item)msg;
				ResultGenerator<R> generator = (ResultGenerator<R>) item.getItem();
				generator.storeResult(generator.execute());
				this.pipeToCollector.put(msg);

			} catch (Exception e) {
				logger.error("Error", e);

			}
		}
		@Override
		protected void periodOfInactivityProcesses() {}

		@Override
		protected void close() throws Exception {
			logger.debug("Closing Processor");
			super.close();
		}
	}
	
	private class ResultCollector extends ModelCommandProcessor {

		protected ResultCollector() {
			super(10, "Result Collector", 0);
			
		}

		@Override
		protected void receivedMessage(Msg msg) {
			Item item = (Item)msg;
			resultMap.put(item.getItem().getCalculationId(), item.getItem().getResult());
			if (item.isLast()){
				synchronized (loadManager) {
					finished=true; 
					loadManager.notifyAll();
					
				}
			}
		}
		
		@Override
		protected void periodOfInactivityProcesses() {}

		@Override
		protected void close() throws Exception {
			logger.debug("Closing");
			super.close();
		}
	}

	private class Item implements Msg{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private final ResultGenerator<T> item;
		private final boolean last;
		public Item(ResultGenerator<T> item, boolean last) {
			this.item=item;
			this.last=last;
		}
		public ResultGenerator<T> getItem() {
			return item;
		}
		public boolean isLast() {
			return last;
		}
	}	
}
