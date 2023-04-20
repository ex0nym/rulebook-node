package io.exonym.lite.exceptions;

import com.google.gson.JsonArray;
import io.exonym.lite.time.DateHelper;

import java.util.ArrayList;

public class UxException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JsonArray info = null;

	public UxException(String msg) {
		super(msg);
	}

	public UxException(String msg, String... required) {
		super(msg);
		this.info = new JsonArray();
		for (String r : required){
			this.info.add(r);

		}
	}

	public UxException(String msg, Throwable e, String... required) {
		super(msg, e);
		this.info = new JsonArray();
		for (String r : required){
			this.info.add(r);

		}
	}
	
	public UxException(String msg, Throwable e){
		super(msg, e);
		
	}

	public boolean hasInfo(){
		return info != null;

	}

	public JsonArray getInfo() {
		return info;
	}

	public boolean hasCause(){
		return this.getCause()!=null;
		
	}

	@Override
	public synchronized Throwable initCause(Throwable arg0) {
		return super.initCause(arg0);
	}
	
	public static ArrayList<String> getStackAsString(Exception e) {
		ArrayList<String> result = new ArrayList<>();
		result.add(DateHelper.currentIsoUtcDateTime());
		result.add(e.getMessage());
		StackTraceElement[] stes = e.getStackTrace();
		for (StackTraceElement ste : stes){
			result.add(ste.toString());

		}
		if (e.getCause()!=null){
			Throwable t = addCause(e.getCause(), result);
			if (t!=null){
				addCause(t, result);

			}
		}
		return result;

	}

	private static Throwable addCause(Throwable t, ArrayList<String> result) {
		result.add(t.getMessage());
		StackTraceElement[] stes = t.getStackTrace();
		int i=0;
		for (StackTraceElement ste : stes){
			result.add(ste.toString());
			i++;
			if (i>4){
				break;

			}
		}
		return t.getCause();

	}
}
