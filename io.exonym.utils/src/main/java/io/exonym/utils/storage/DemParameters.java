package io.exonym.utils.storage;

import java.net.URI;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;

@XmlRootElement(name="DemParameters")
@XmlType(name = "DemParameters", namespace = Namespace.EX)
public class DemParameters {
	
	@XmlElement(name = "DemUID", namespace = Namespace.EX)
	private URI demUid;
	
	@XmlElement(name = "ContributorMemberCriteriaUID", namespace = Namespace.EX)
	private URI contributorMemershipCriteriaUid;
	
	@XmlElement(name = "DelegateMemberCriteriaUID", namespace = Namespace.EX)
	private URI delegateMemershipCriteriaUid;
	
	@XmlElement(name = "DaysToMotionTimeOut", namespace = Namespace.EX)
	private int motionTimeOut;
	
	@XmlElement(name = "DaysToActionTimeOut", namespace = Namespace.EX)
	private int actionTimeOut;
	
	@XmlElement(name = "MaxConsecutiveMotions", namespace = Namespace.EX)
	private int maxConsecutiveMotions;
	
	@XmlElement(name = "MinActionDelegateCount", namespace = Namespace.EX)
	private int minActionDelegateCount;
	
	@XmlElement(name = "MinThresholdToAction", namespace = Namespace.EX)
	private int minThresholdToAction;
	
	@XmlElement(name = "MinMotionDelegateCount", namespace = Namespace.EX)
	private int minDelegateCountToMotion;
	
	@XmlElement(name = "MinThresholdToMotion", namespace = Namespace.EX)
	private int minThresholdToMotion;
	
	@XmlElement(name = "OverrideActionDelegateCount", namespace = Namespace.EX)
	private int overrideActionDelegateCount;
	
	@XmlElement(name = "OverrideThresholdToAction", namespace = Namespace.EX)
	private int overrideThresholdToAction;
	
	@XmlElement(name = "OverrideMotionDelegateCount", namespace = Namespace.EX)
	private int overrideMotionDelegateCount;
	
	@XmlElement(name = "OverrideThresholdToMotion", namespace = Namespace.EX)
	private int overrideThresholdToMotion;

	public URI getDemUid() {
		return demUid;
	}

	public void setDemUid(URI demUid) {
		this.demUid = demUid;
	}

	public URI getContributorMemershipCriteriaUid() {
		return contributorMemershipCriteriaUid;
	}

	public void setContributorMemershipCriteriaUid(URI contributorMemershipCriteriaUid) {
		this.contributorMemershipCriteriaUid = contributorMemershipCriteriaUid;
	}

	public URI getDelegateMemershipCriteriaUid() {
		return delegateMemershipCriteriaUid;
	}

	public void setDelegateMemershipCriteriaUid(URI delegateMemershipCriteriaUid) {
		this.delegateMemershipCriteriaUid = delegateMemershipCriteriaUid;
	}

	public int getMotionTimeOut() {
		return motionTimeOut;
	}

	public void setMotionTimeOut(int motionTimeOut) {
		this.motionTimeOut = motionTimeOut;
	}

	public int getActionTimeOut() {
		return actionTimeOut;
	}

	public void setActionTimeOut(int actionTimeOut) {
		this.actionTimeOut = actionTimeOut;
	}

	public int getMaxConsecutiveMotions() {
		return maxConsecutiveMotions;
	}

	public void setMaxConsecutiveMotions(int maxConsecutiveMotions) {
		this.maxConsecutiveMotions = maxConsecutiveMotions;
	}

	public int getMinActionDelegateCount() {
		return minActionDelegateCount;
	}

	public void setMinActionDelegateCount(int minActionDelegateCount) {
		this.minActionDelegateCount = minActionDelegateCount;
	}

	public int getMinThresholdToAction() {
		return minThresholdToAction;
	}

	public void setMinThresholdToAction(int minThresholdToAction) {
		this.minThresholdToAction = minThresholdToAction;
	}

	public int getMinDelegateCountToMotion() {
		return minDelegateCountToMotion;
	}

	public void setMinDelegateCountToMotion(int minDelegateCountToMotion) {
		this.minDelegateCountToMotion = minDelegateCountToMotion;
	}

	public int getMinThresholdToMotion() {
		return minThresholdToMotion;
	}

	public void setMinThresholdToMotion(int minThresholdToMotion) {
		this.minThresholdToMotion = minThresholdToMotion;
	}

	public int getOverrideActionDelegateCount() {
		return overrideActionDelegateCount;
	}

	public void setOverrideActionDelegateCount(int overrideActionDelegateCount) {
		this.overrideActionDelegateCount = overrideActionDelegateCount;
	}

	public int getOverrideThresholdToAction() {
		return overrideThresholdToAction;
	}

	public void setOverrideThresholdToAction(int overrideThresholdToAction) {
		this.overrideThresholdToAction = overrideThresholdToAction;
	}

	public int getOverrideMotionDelegateCount() {
		return overrideMotionDelegateCount;
	}

	public void setOverrideMotionDelegateCount(int overrideMotionDelegateCount) {
		this.overrideMotionDelegateCount = overrideMotionDelegateCount;
	}

	public int getOverrideThresholdToMotion() {
		return overrideThresholdToMotion;
	}

	public void setOverrideThresholdToMotion(int overrideThresholdToMotion) {
		this.overrideThresholdToMotion = overrideThresholdToMotion;
	}

}
