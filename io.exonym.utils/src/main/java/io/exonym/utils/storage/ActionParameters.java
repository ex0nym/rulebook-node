package io.exonym.utils.storage;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="ActionParameters")
@XmlType(name = "ActionParameters", namespace = Namespace.EX) 
//		propOrder = {"demUid", "actionUid", "assignedDelegatesUid", "state", "votable", 
//				"actionSigKeyUid", "keyDistribution", "discoveryThreshold",
//				"parts", "prime","motion", "assetLibrary", "nextMotionDelegates", 
//				"nextMotionSigKeyUid", "distributionNextMotion", "discoveryThresholdNextMotion", "history"})
public class ActionParameters {
	
	public enum STATE {
		ADD_MOTION, LOCK_MOTION, VOTE_ON_MOTION, 
		VOTE_ON_STRUCTURED_ACTION
	};
	
	private URI demUid;	
	private URI actionUid;
	private STATE state;
	private URI assignedDelegatesUid;
	private URI actionSigKeyUid;
	private byte[] votable;
	private int keyDistribution;
	private int discoveryThreshold;
	private byte[] parts; 
	private BigInteger prime;
	private Motion motion;
	private AssetLibrary assetLibrary;
	private URI nextMotionDelegates;
	private URI nextMotionSigKeyUid;
	private Integer distributionNextMotion;
	private Integer discoveryThresholdNextMotion;
	private final ArrayList<ActionParametersHistoryItem> history = new ArrayList<>();

	@XmlElement(name="DemUid", namespace=Namespace.EX)
	public URI getDemUid() {
		return demUid;
	}

	public void setDemUid(URI demUid) {
		this.demUid = demUid;
	}

	@XmlElement(name="ActionUid", namespace=Namespace.EX)
	public URI getActionUid() {
		return actionUid;
	}

	public void setActionUid(URI actionUid) {
		this.actionUid = actionUid;
	}

	@XmlElement(name="AssignedDelegatesUid", namespace=Namespace.EX)
	public URI getAssignedDelegatesUid() {
		return assignedDelegatesUid;
	}

	public void setAssignedDelegatesUid(URI actionDelegates) {
		this.assignedDelegatesUid = actionDelegates;
	}

	@XmlElement(name="Votable", namespace=Namespace.EX)
	public byte[] getVotable() {
		return votable;
	}

	public void setVotable(byte[] votable) {
		this.votable = votable;
	}

	@XmlElement(name="KeyDistribution", namespace=Namespace.EX)
	public int getKeyDistribution() {
		return keyDistribution;
	}

	public void setKeyDistribution(int keyDistribution) {
		this.keyDistribution = keyDistribution;
	}

	@XmlElement(name="DiscoveryThreshold", namespace=Namespace.EX)
	public int getDiscoveryThreshold() {
		return discoveryThreshold;
	}

	public void setDiscoveryThreshold(int discoveryThreshold) {
		this.discoveryThreshold = discoveryThreshold;
	}

	@XmlElement(name="ActionSigKeyUid", namespace=Namespace.EX)
	public URI getActionSigKeyUid() {
		return actionSigKeyUid;
	}

	public void setActionSigKeyUid(URI actionSigKeyUid) {
		this.actionSigKeyUid = actionSigKeyUid;
	}

	@XmlElement(name="Parts", namespace=Namespace.EX)
	public byte[] getParts() {
		return parts;
	}

	public void setParts(byte[] parts) {
		this.parts = parts;
	}

	@XmlElement(name="Prime", namespace=Namespace.EX)
	public BigInteger getPrime() {
		return prime;
	}

	public void setPrime(BigInteger prime) {
		this.prime = prime;
	}

	@XmlElement(name="Motion", namespace=Namespace.EX)
	public Motion getMotion() {
		return motion;
	}

	public void setMotion(Motion motion) {
		this.state = STATE.LOCK_MOTION;
		this.motion = motion;
	}

	@XmlElement(name="AssetLibrary", namespace=Namespace.EX)
	public AssetLibrary getAssetLibrary() {
		return assetLibrary;
	}

	public void setAssetLibrary(AssetLibrary assetLibrary) {
		this.assetLibrary = assetLibrary;
	}

	@XmlElement(name="NextMotionDelegates", namespace=Namespace.EX)
	public URI getNextMotionDelegates() {
		return nextMotionDelegates;
	}

	public void setNextMotionDelegates(URI nextMotionDelegates) {
		this.nextMotionDelegates = nextMotionDelegates;
	}

	@XmlElement(name="NextMotionSigKeyUid", namespace=Namespace.EX)
	public URI getNextMotionSigKeyUid() {
		return nextMotionSigKeyUid;
	}

	public void setNextMotionSigKeyUid(URI nextMotionSigKeyUid) {
		this.nextMotionSigKeyUid = nextMotionSigKeyUid;
	}

	@XmlElement(name="DistributionNextMotion", namespace=Namespace.EX)
	public int getDistributionNextMotion() {
		return distributionNextMotion;
	}

	public void setDistributionNextMotion(int distributionNextMotion) {
		this.distributionNextMotion = distributionNextMotion;
	}

	@XmlElement(name="DiscoveryThresholdNextMotion", namespace=Namespace.EX)
	public int getDiscoveryThresholdNextMotion() {
		return discoveryThresholdNextMotion;
	}

	public void setDiscoveryThresholdNextMotion(int discoveryThresholdNextMotion) {
		this.discoveryThresholdNextMotion = discoveryThresholdNextMotion;
	}

	@XmlElement(name="State", namespace=Namespace.EX)
	public STATE getState() {
		return state;
	}

	public void setState(STATE state) {
		this.state = state;
	}

	@XmlElement(name="DistributionNextMotion", namespace=Namespace.EX)
	public void setDistributionNextMotion(Integer distributionNextMotion) {
		this.distributionNextMotion = distributionNextMotion;
	}

	public void setDiscoveryThresholdNextMotion(Integer discoveryThresholdNextMotion) {
		this.discoveryThresholdNextMotion = discoveryThresholdNextMotion;
	}

	@XmlElement(name="History", namespace=Namespace.EX)
	public ArrayList<ActionParametersHistoryItem> getHistory() {
		return history;
	}
	
	
}
