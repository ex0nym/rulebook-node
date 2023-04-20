package io.exonym.lite.parallel;

public interface ResourceGenerator<T> {
	
	public T generateResource() throws Exception;
	
}
