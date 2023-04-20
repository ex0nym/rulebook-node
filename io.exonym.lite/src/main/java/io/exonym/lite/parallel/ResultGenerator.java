package io.exonym.lite.parallel;

import java.net.URI;

public interface ResultGenerator<T> {
	
	/**
	 * Produce the results required.
	 * 
	 * @return
	 * @throws Exception
	 */
	public T execute() throws Exception;
	
	public void storeResult(T result);
	
	public T getResult();
	
	public URI getCalculationId();

}
