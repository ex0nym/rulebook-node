package io.exonym.utils.storage;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import io.exonym.lite.pojo.Namespace;
import io.exonym.utils.adapters.CredentialSpecificationAdapter;
import io.exonym.utils.adapters.IssuancePolicyAdapter;

@XmlRootElement(name="XNodeMsg")
@XmlType(name = "XNodeMsg", namespace = Namespace.EX 
, propOrder={"ack", "command", "message", "peerMessage", "messageFacade", "proofs", "state", "peerId", 
		"username", "connectionUid", "context",
		"commandData", "assetLib", "motions", "peerProposal", "uidList", "credSpec", "issuancePolicy",
		"originalContext", "xmlOrJson", "contextGenId"}) //*/ 
public class XNodeMsg implements Serializable{

	private static final long serialVersionUID = 1L;
	
	// Container Mngt
	public static final String CMD_ADD_CONTAINER = "add-container";
	public static final String CMD_OPEN_CONTAINER = "open-container";
	public static final String CMD_CLOSE_CONTAINER = "close-container";
	public static final String CMD_DELETE_CONTAINER = "delete-container";
	
	// Data Request
	public static final String CMD_GET_CRED_SPEC = "get-cred-spec";
	public static final String CMD_GET_INSPECTOR = "get-inspector";
	public static final String CMD_GET_ASSET_LIB = "get-asset-lib";
	public static final String CMD_GET_PP = "get-presentation-policy";
	
	// Peer Connect
	public static final String CMD_ADD_PEER = "add-peer";
	public static final String CMD_CONFIRM_ADD_PEER = "confirm-add-peer";
	public static final String CMD_ACCEPT_ADD = "accept-add";
	
	// Peer Proofs
	public static final String CMD_MESSAGE_PEER = "message-peer";
	public static final String CMD_PROOF_REQUEST = "proof-request";
	public static final String CMD_PROVE = "prove";
	public static final String CMD_GET_ALL_OPEN_PROOFS = "all-open-proofs";
	
	// Issuance
	public static final String CMD_SETUP_ISSUER= "setup-issuer";
	public static final String CMD_CLAIM= "claim";
	public static final String CMD_ISSUE_INIT = "issue-init";
	public static final String CMD_RECEIVE_CREDENTIAL = "receive-credential";
	public static final String CMD_ISSUE = "issue";
	
	// Prove
	public static final String CMD_VERIFY = "verify";
	
	@XmlElement(name = "ConnectionUID", namespace = Namespace.EX)
	private URI connectionUid;
	
	@XmlElement(name = "Context", namespace = Namespace.EX)
	private String context;
	
	@XmlElement(name = "Ack", namespace = Namespace.EX)
	private String ack; 
	
	@XmlElement(name = "OriginalContext", namespace = Namespace.EX)
	private String originalContext;
	
	@XmlElement(name = "State", namespace = Namespace.EX)
	private String state;
	
	@XmlElement(name = "Username", namespace = Namespace.EX)
	private String username;
	
	@XmlElement(name = "Message", namespace = Namespace.EX)
	private String message;
	
	@XmlElement(name = "PeerMessage", namespace = Namespace.EX)
	private PeerMessage peerMessage;
	
	@XmlElement(name = "Proof", namespace = Namespace.EX)
	private ArrayList<Proof> proofs;
	
	@XmlElement(name = "MessageFacade", namespace = Namespace.EX)
	private NetworkFacade messageFacade;

	@XmlElement(name = "Command", namespace = Namespace.EX)
	private String command; 
	
	@XmlElement(name = "CommandData", namespace = Namespace.EX)
	private ArrayList<String> commandData = new ArrayList<>();

	@XmlElement(name = "PeerID", namespace = Namespace.EX)
	private String peerId; 
	
	@XmlElement(name = "UID", namespace = Namespace.EX)
	private ArrayList<URI> uidList;

	@XmlElement(name = "DeviceAssetLibrary", namespace = Namespace.EX)
	private DeviceAssetLibrary assetLib;

	@XmlElement(name = "Motions", namespace = Namespace.EX)
	private ArrayList<Motion> motions = new ArrayList<>();
	
	@XmlElement(name = "PeerProposal", namespace = Namespace.EX)
	private PeerProposal peerProposal; 
	
	@XmlElement(name = "CredentialSpecification", namespace = Namespace.EX)
	private CredentialSpecificationAdapter credSpec;
	
	@XmlElement(name = "IssuancePolicy", namespace = Namespace.EX)
	private IssuancePolicyAdapter issuancePolicy;
	
	@XmlElement(name = "XmlOrJson", namespace = Namespace.EX)
	private String xmlOrJson; 
	
	@XmlElement(name = "ContextGenId", namespace = Namespace.EX)
	private String contextGenId; 

	public String getContext() {
		return context;
		
	}
	public void setContext(String context) {
		this.context = context;
		
	}
	public URI getConnectionUid() {
		return connectionUid;
		
	}
	public void setConnectionUid(URI connectionUid) {
		this.connectionUid = connectionUid;
		
	}

	public ArrayList<Motion> getMotions() {
		return motions;
	}
	public void addMotions(Motion motions) {
		this.motions.add(motions);
		
	}
	public String getXmlOrJson() {
		return xmlOrJson;
	}
	public void setXmlOrJson(String xmlOrJson) {
		this.xmlOrJson = xmlOrJson;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
		
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public ArrayList<String> getCommandData() {
		return commandData;
	}
	public void addCommandData(String commandDataEquality) {
		this.commandData.add(commandDataEquality);
	}
	public void setMotions(ArrayList<Motion> motions) {
		this.motions = motions;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getOriginalContext() {
		return originalContext;
	}
	public void setOriginalContext(String originalContext) {
		this.originalContext = originalContext;
	}
	
	public CredentialSpecificationAdapter getCredSpec() {
		return credSpec;
	}
	public void setCredSpec(CredentialSpecificationAdapter credSpec) {
		this.credSpec = credSpec;
	}
	public IssuancePolicyAdapter getIssuancePolicy() {
		return issuancePolicy;
	}
	public void setIssuancePolicy(IssuancePolicyAdapter ipa) {
		this.issuancePolicy = ipa;
	}
	public ArrayList<URI> getUidList() {
		return uidList;
	}
	public void setUidList(ArrayList<URI> uidList) {
		this.uidList = uidList;
	}
	public PeerProposal getPeerProposal() {
		return peerProposal;
	}
	public void setPeerProposal(PeerProposal peerProposal) {
		this.peerProposal = peerProposal;
	}
	public DeviceAssetLibrary getAssetLib() {
		return assetLib;
	}
	public void setAssetLib(DeviceAssetLibrary assetLib) {
		this.assetLib = assetLib;
	}
	public String getPeerId() {
		return peerId;
	}
	public void setPeerId(String peerId) {
		this.peerId = peerId;
	}
	
	public String getContextGenId() {
		return contextGenId;
	}
	public void setContextGenId(String contextGenId) {
		this.contextGenId = contextGenId;
	}
	public boolean isAck() {
		if (ack==null){
			return false;
			
		} else {
			return ack.equals("true");
			
		}
	}

	public ArrayList<Proof> getProofs() {
		if (proofs==null){
			proofs = new ArrayList<>();
		}
		return proofs;
	}
	public void setProofs(ArrayList<Proof> proofs) {
		this.proofs = proofs;
	}
	public NetworkFacade getMessageFacade() {
		return messageFacade;
	}
	public void setMessageFacade(NetworkFacade messageFacade) {
		this.messageFacade = messageFacade;
	}
	public PeerMessage getPeerMessage() {
		return peerMessage;
	}
	public void setPeerMessage(PeerMessage peerMessage) {
		this.peerMessage = peerMessage;
	}
	public void setAck(boolean ack) {
		this.ack = (ack ? "true" : "false");
	}
	
	public String toString(){
		return this.context + " " + this.command + " isAck " + this.isAck();
		
	}
	
	public void setCommandData(ArrayList<String> commandData) {
		this.commandData = commandData;
	}
	
	public static XNodeMsg closeWithMessage(XNodeMsg msgIn, String messageToUser){
		XNodeMsg msg = new XNodeMsg();
		msg.setContext(msgIn.getContext());
		msg.setOriginalContext(msgIn.getOriginalContext());
		msg.setState(ContextTracker.END_STATE);
		msg.setMessage(messageToUser);
		msg.setXmlOrJson(msgIn.getXmlOrJson());
		return msg; 
		
	}
}