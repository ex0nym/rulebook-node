package io.exonym.utils.storage;

import java.io.File;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.HashSet;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


import io.exonym.lite.pojo.Namespace;
import io.exonym.lite.time.DateHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@XmlRootElement(name="ContextTracker")
@XmlType(name = "ContextTracker", namespace = Namespace.EX 
	, propOrder={"contextTrackerUid", "dateUtc", "deviceUid", "username", "active", "complete"})
public class ContextTracker {
	
	
	private static final Logger logger = LogManager.getLogger(ContextTracker.class);
	public static final String START_STATE = "New";
	public static final String END_STATE = "Complete";
	public static final String startState = START_STATE.toLowerCase();
	public static final String endState = END_STATE.toLowerCase();
	
	@XmlElement(name = "ActiveRequest", namespace = Namespace.EX)
	private ArrayDeque<RequestContext> active = new ArrayDeque<>();
	
	@XmlElement(name = "CompleteRequest", namespace = Namespace.EX)
	private ArrayDeque<RequestContext> complete = new ArrayDeque<>();
	
	@XmlElement(name = "DateUTC", namespace = Namespace.EX)
	private String dateUtc;
	
	@XmlElement(name = "DeviceUID", namespace = Namespace.EX)
	private URI deviceUid;
	
	@XmlElement(name = "ContextTrackerUID", namespace = Namespace.EX)
	private URI contextTrackerUid;
	
	@XmlElement(name = "Username", namespace = Namespace.EX)
	private String username;
	
	public ContextTracker() {
	}

	public synchronized ArrayDeque<RequestContext> getComplete() {
		return complete;
	}

	public synchronized void setComplete(ArrayDeque<RequestContext> complete) {
		this.complete = complete;
	}

	public synchronized ArrayDeque<RequestContext> getActive() {
		return active;
	}

	public synchronized void setActive(ArrayDeque<RequestContext> active) {
		this.active = active;
	}

	public String getDateUtc() {
		return dateUtc;
	}

	public void setDateUtc(String dateUtc) {
		this.dateUtc = dateUtc;
	}

	public URI getDeviceUid() {
		return deviceUid;
	}

	public void setDeviceUid(URI deviceUid) {
		this.deviceUid = deviceUid;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getFileName(){
		return getFileName(dateUtc);
		
	}
	
	public URI getFileUid(){
		return generateContextTrackerUid(dateUtc);
		
	}
	
	
	public URI generateContextTrackerUid(){
		return generateContextTrackerUid(dateUtc);
	}

	public URI generateContextTrackerUid(String dateUtc){
		return URI.create(dateUtc.replaceAll("-", "") + "-" + username + "-" + deviceUid);
		
	}

	public String getFileName(String dateUtc){
		return generateContextTrackerUid(dateUtc).toString().replaceAll(":", "-") + ".xml";
		
	}

	public URI getContextTrackerUid() {
		return contextTrackerUid;
	}

	public void setContextTrackerUid(URI contextTrackerUid) {
		this.contextTrackerUid = contextTrackerUid;
	}
	
	public static File computeFileFromFolder(File folder, URI uid, String username) {
		int latest = computeLatestContext(folder, uid, username);
		if (latest==0){
			latest = Integer.parseInt(DateHelper.currentBareIsoUtcDate());
			
		}
		File contextFile = new File(folder.getAbsolutePath() + "/" 
							+ ContextTracker.getFileName(""+ latest, uid, username));
		return contextFile;
		
	}
	
	public static HashSet<URI> computeFilesFromFolder(String username) {
		File folder = new File("resource//local//context");
		if (folder.exists()){
			File[] files = folder.listFiles();
			HashSet<URI> result = new HashSet<>();
			
			for (int i = 0; i < files.length; i++) {
				
				if (files[i].getName().contains(username)){
					String deviceUid = files[i].getName();
					String[] s = deviceUid.split("\\.");
					deviceUid = s[0].substring(s[0].length()-36);
					result.add(URI.create(deviceUid));
					
				}
			}
			return result;
			
		} else {
			folder.mkdirs();
			return null; 
			
		}
	}	

	public static String getFileName(String dateUtc, URI deviceUid, String username){
		return dateUtc + "-" + username + "-" + deviceUid + ".xml";
		
	}

	private static int computeLatestContext(File folder, URI uid, String username) {
		int latest = 0; 
		if (folder.isDirectory()){
			File[] files = folder.listFiles();
			String target = username + "-" + uid.toString();
			
			for (int i = 0; i < files.length; i++) {
				String name = files[i].getName();
				
				if (name.contains(target)){
					String[] parts = name.split("-");
					try {
						int yyyymmdd = Integer.parseInt(parts[0]);
						if (yyyymmdd > latest){
							latest = yyyymmdd;
							
						}
					} catch (Exception e) {
						logger.error("Bad file name - someone has been messing around", e);
						
					}
				}
			}
		} 
		return latest;
		
	}
}