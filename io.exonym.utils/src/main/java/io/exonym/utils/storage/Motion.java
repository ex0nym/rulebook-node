package io.exonym.utils.storage;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="Motion")
@XmlType(name = "Motion", namespace = Namespace.EX, 
			propOrder = {"motionUid", "debateUid", "actionDelegateUid", "thisActionDistribution", 
					"thisActionThreshold",   "backgroundParagraphs", 
					"problemsParagraphs","motionEvents", "lockToVote"})
public class Motion implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 12L;

	@XmlElement(name = "MotionUID", namespace = Namespace.EX)
	private URI motionUid; 
	
	@XmlElement(name = "DebateUID", namespace = Namespace.EX)
	private URI debateUid;
	
	@XmlElement(name = "ActionDelegateUID", namespace = Namespace.EX)
	private URI actionDelegateUid;
	
	@XmlElement(name = "ResultingActionDistribution", namespace = Namespace.EX)
	private Integer thisActionDistribution;

	@XmlElement(name = "ResultingActionDiscoveryThreshold", namespace = Namespace.EX)
	private Integer thisActionThreshold;
	
	@XmlElement(name = "Background", namespace = Namespace.EX)
	private ArrayList<String> backgroundParagraphs; 
	
	@XmlElement(name = "ProblemsToSolve", namespace = Namespace.EX)
	private ArrayList<String> problemsParagraphs;
	
	@XmlElement(name = "LockedToVote", namespace = Namespace.EX)
	private boolean lockToVote;
	
	@XmlElement(name = "MotionEvents", namespace = Namespace.EX)
	private final ArrayList<MotionEvent> motionEvents = new ArrayList<>();

	public URI getMotionUid() {
		return motionUid;
	}

	public void setMotionUid(URI motionUid) {
		this.motionUid = motionUid;
	}

	public URI getDebateUid() {
		return debateUid;
	}

	public void setDebateUid(URI debateUid) {
		this.debateUid = debateUid;
	}

	public ArrayList<String> getBackgroundParagraphs() {
		return backgroundParagraphs;
	}

	public void setBackgroundParagraphs(ArrayList<String> backgroundParagraphs) {
		this.backgroundParagraphs = backgroundParagraphs;
	}

	public ArrayList<String> getProblemsParagraphs() {
		return problemsParagraphs;
	}

	public void setProblemsParagraphs(ArrayList<String> problemsParagraphs) {
		this.problemsParagraphs = problemsParagraphs;
	}

	public boolean isLockToVote() {
		return lockToVote;
	}

	public void setLockToVote(boolean lockToVote) {
		this.lockToVote = lockToVote;
	}

	public ArrayList<MotionEvent> getMotionEvents() {
		return motionEvents;
	}

	public Integer getThisActionThreshold() {
		return thisActionThreshold;
	}

	public void setThisActionThreshold(Integer thisActionThreshold) {
		this.thisActionThreshold = thisActionThreshold;
	}

	public Integer getThisActionDistribution() {
		return thisActionDistribution;
	}

	public void setThisActionDistribution(Integer thisActionDistribution) {
		this.thisActionDistribution = thisActionDistribution;
	}

	public URI getActionDelegateUid() {
		return actionDelegateUid;
	}

	public void setActionDelegateUid(URI actionDelegateUid) {
		this.actionDelegateUid = actionDelegateUid;
	}
}
