package io.exonym.actor.actions;

import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.time.DateHelper;
import io.exonym.utils.storage.NetworkParticipant;
import io.exonym.utils.storage.NodeInformation;
import io.exonym.utils.storage.TrustNetwork;
import io.exonym.lite.pojo.XKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URL;
import java.util.*;

public class TrustNetworkWrapper {
	
	private final TrustNetwork trustNetwork;
	private HashMap<URI, NetworkParticipant> participants = new HashMap<>();
	
	private static final Logger logger = LogManager.getLogger(TrustNetworkWrapper.class);
	
	public TrustNetworkWrapper(TrustNetwork trustNetwork) {
		if (trustNetwork==null) {
			throw new NullPointerException();
			
		}
		this.trustNetwork=trustNetwork;
		
		for (NetworkParticipant n : this.trustNetwork.getParticipants()) {
			participants.put(n.getNodeUid(), n);

		}
		this.trustNetwork.getParticipants().clear();
		
	}

	/**
	 *
	 * @param p
	 * @throws Exception
	 */
	public void addParticipant(NetworkParticipant p) throws Exception {
		if (p!=null && p.getNodeUid()==null) {
			throw new Exception("Node Name was not set " + p);
			
		}
		participants.put(p.getNodeUid(), p);
		
	}

	public Collection<NetworkParticipant> getAllParticipants(){
		if (!this.participants.isEmpty()){
			return this.participants.values();

		} else {
			return Collections.EMPTY_LIST;

		}
	}

	/**
	 *
	 * @param nodeUid
	 * @param nodeUrl
	 * @param publicKey
	 * @return
	 */
	public NetworkParticipant addParticipant(URI nodeUid, URI nodeUrl,
											 URI xNodeUrl, URI multicastUrl,
											 XKey publicKey, String region, URI lastIssuerUID) {
		logger.info("Adding Participant " + nodeUid);
		if (nodeUid== null) {
			throw new NullPointerException("Node UID");

		} if (nodeUrl == null) {
			throw new NullPointerException("Node URL");

		} if (xNodeUrl == null) {
			throw new NullPointerException("XNode URL");

		} if (publicKey == null) {
			throw new NullPointerException("Node Public Key");
			
		} if (region==null){
			// throw new NullPointerException("Region");

		}
		NetworkParticipant p = new NetworkParticipant();
		p.setNodeUid(nodeUid);
		p.setStaticNodeUrl0(nodeUrl);
		p.setRulebookNodeUrl(xNodeUrl);
		p.setBroadcastAddress(multicastUrl);
		p.setPublicKey(publicKey);
		p.setRegion(region);
		p.setLastIssuerUID(lastIssuerUID);
		p.setLastUpdateTime(DateHelper.currentIsoUtcDateTime());
		p.setAvailableOnMostRecentRequest(true);
		this.participants.put(nodeUid, p);
		return p;
		
	}
	
	
	public void removeParticipant(URI p) throws HubException {
		URI participantUid = computeParticipantUid(p);
		logger.info("Attempting to remove Participant " + participantUid);
		NetworkParticipant part = participants.remove(participantUid);
		if (part!=null) {
			logger.info("Successfully removed participant");
			
		} else {
			logger.info("Failed to removed participant");
			
		}
	}
	
	private URI computeParticipantUid(URI p) throws HubException {
		String uid = p.toString();
		String[] parts = uid.split(":");
		if (parts.length==5) {
			return p;
			
		} else if (uid.endsWith(":i")) {
			return URI.create(parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3] + ":" + parts[4]);
			
		} else {
			throw new HubException("Unknown Participant Translation " + p);
			
		}
	}

	public NetworkParticipant getParticipant(URI name) {
		return participants.get(name);
		
	}
	
	public NetworkParticipant getParticipantWithError(URI  name) throws Exception {
		NetworkParticipant p = participants.get(name);
		if (p!=null) {
			return p; 
			
		} else {
			throw new Exception("There was no participant called " + name);
			
		}
	}
	
	public TrustNetwork finalizeTrustNetwork() {
		ArrayList<NetworkParticipant> l = this.trustNetwork.getParticipants();
		for (URI name : participants.keySet()) {
			l.add(participants.get(name));
			
		}
		trustNetwork.setLastUpdated(DateHelper.currentIsoUtcDateTime());
		return trustNetwork;
		
	}
	
	public NodeInformation getNodeInformation() {
		return trustNetwork.getNodeInformation();
		
	}
	
	public URI getMostRecentIssuerParameters() {
		LinkedList<URI> params = this.trustNetwork.getNodeInformation().getIssuerParameterUids();
		if (params.isEmpty()){
			return null;
		} else {
			return params.getLast();
		}
	}
	
	public static TrustNetworkWrapper open(URI url) {
		return null;
		
	}
}