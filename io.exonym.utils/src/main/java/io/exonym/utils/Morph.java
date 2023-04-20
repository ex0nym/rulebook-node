package io.exonym.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;


public class Morph<T> {
	

	private static final Logger logger = LogManager.getLogger(Morph.class);
	public byte[] toByteArray(T t) throws Exception {
		if (t==null){
			throw new Exception("Null object for byte conversion");
			
		}
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		ObjectOutput out = null;
		
		try{ 
			out = new ObjectOutputStream(byteOut);
			out.writeObject(t);
			return byteOut.toByteArray();
			
		} catch (Exception e){
			logger.error("Morph Failure ", e);
			return null;
			
		} finally {
			try {
				if (out!=null) {
					out.close();
					
				}
			} catch (Exception e){
				logger.error("Close Failure ", e);
				
			}
			try {
				if (byteOut!=null) {
					byteOut.close();
					
				}
				
			} catch (Exception e2) {
				logger.error("Close Failure ", e2);
				
			}
		}	
	}

	@SuppressWarnings("unchecked")
	public T construct(byte[] byteArray){
		if (byteArray==null){
			return null;
			
		}
		ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
		ObjectInput in = null;
		
		try {
			in = new ObjectInputStream(bis);
			return (T) in.readObject();

		}catch (Exception e){
			logger.error("Morph Construct Failure", e);
			return null; 
			  
		} finally {
			try {
				if (bis!=null){ bis.close(); }
			} catch (Exception ex) {
				logger.error("Morph Close Input Stream Error", ex);	
				
			}
			
			try {
				if (in!=null){ in.close(); }
			} catch (Exception ex) {
				logger.error("Morph Close Object Input Error", ex);
				
			}
		}
	}

	
}
