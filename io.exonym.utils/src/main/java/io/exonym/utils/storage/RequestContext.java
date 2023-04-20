package io.exonym.utils.storage;

import java.util.ArrayDeque;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="RequestContext")
@XmlType(name = "RequestContext", namespace = Namespace.EX
, propOrder={"command", "state", "ackTarget", "ack", "context", "originalContext", "msgs", "history", "xmlOrJson"})
public class RequestContext {

	/**
	 * Device XML to keep track of requests made to the node
	 * Each context string has a state that allows the 
	 * subsequent actions to be processed when the user connect.
	 * 
	 */
	public RequestContext() {}
	
	@XmlElement(name = "Context", namespace = Namespace.EX)
	private String context; 
	
	@XmlElement(name = "Command", namespace = Namespace.EX)
	private String command; 
	
	@XmlElement(name = "OriginalContext", namespace = Namespace.EX)
	private String originalContext; 
	
	@XmlElement(name = "XNodeMessage", namespace = Namespace.EX)
	private ArrayDeque<XNodeMsg> msgs = new ArrayDeque<>(); 

	@XmlElement(name = "State", namespace = Namespace.EX)
	private String state;
		
	@XmlElement(name = "Target", namespace = Namespace.EX)
	private String ackTarget;
	
	@XmlElement(name = "Ack", namespace = Namespace.EX)
	private String ack;
	
	@XmlElement(name = "HistoryItem", namespace = Namespace.EX)
	private ArrayDeque<HistoryItem> history = new ArrayDeque<>();
	
	@XmlElement(name = "XmlOrJson", namespace = Namespace.EX)
	private String xmlOrJson;
	
	public String getContext() {
		return context;
	}
	public void setContext(String context) {
		this.context = context;
	}

	public ArrayDeque<XNodeMsg> getMsgs() {
		return msgs;
	}
	public void setMsgs(ArrayDeque<XNodeMsg> msgs) {
		this.msgs = msgs;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public ArrayDeque<HistoryItem> getHistory() {
		return history;
	}
	public void addHistoryItem(HistoryItem history) {
		this.history.push(history);
	}

	public String getXmlOrJson() {
		return xmlOrJson;
	}
	public void setXmlOrJson(String xmlOrJson) {
		this.xmlOrJson = xmlOrJson;
	}
	public boolean isTargetClient() {
		if (this.ackTarget==null){
			return false; 
			
		} else {
			return (this.ackTarget.equals("client"));
			
		}
	}
	public void setTargetToClient(boolean client) {
		this.ackTarget = (client ? "client" : "server");
		
	}
	public void setAck(boolean ack) {
		this.ack = (ack ? "true" : "false");
		
	}
	public boolean hasAck() {
		return (ack==null ? false : ack.equals("true"));
		
	}
	public String getOriginalContext() {
		return originalContext;
	}
	public void setOriginalContext(String originalContext) {
		this.originalContext = originalContext;
	}
	
	
	/*
	 * Replacements for these above and these should not be used.
	 *  
	 * @return
	 */
	protected String getAckTarget() {
		return ackTarget;
	}
	protected  void setAckTarget(String ackTarget) {
		this.ackTarget = ackTarget;
	}
	protected  String getAck() {
		return ack;
	}
	protected  void setAck(String ack) {
		this.ack = ack;
	}
	public void setHistory(ArrayDeque<HistoryItem> history) {
		this.history = history;
	}
	
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	@Override
	public String toString() {
		return context + " " + ackTarget + " ackd(" + ack + ") state=" + state +  " msgSize(" + msgs.size() + ")\n\t\t\t" + "oc=" + originalContext;
		
	}
}
