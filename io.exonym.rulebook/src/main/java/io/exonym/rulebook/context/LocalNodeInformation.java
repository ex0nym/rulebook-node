package io.exonym.rulebook.context;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.abc4trust.xml.PresentationPolicy;
import eu.abc4trust.xml.PseudonymInPolicy;
import io.exonym.actor.actions.MyStaticData;
import io.exonym.actor.actions.NodeVerifier;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.ProgrammingException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.parallel.ModelSingleSequence;
import io.exonym.lite.time.Timing;
import io.exonym.utils.storage.NetworkParticipant;
import io.exonym.utils.storage.NodeInformation;
import io.exonym.utils.storage.TrustNetwork;
import io.exonym.rulebook.exceptions.ItemNotFoundException;
import io.exonym.lite.pojo.IUser;
import io.exonym.lite.pojo.NodeData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class LocalNodeInformation {

	private static final Logger logger = LogManager.getLogger(LocalNodeInformation.class);
	private static LocalNodeInformation instance = null;

	private final ArrayList<TrustNetwork> leadInfoList = new ArrayList<TrustNetwork>();
	private final ArrayList<TrustNetwork> nodeInformationList = new ArrayList<TrustNetwork>();

	private final ConcurrentHashMap<String, PresentationPolicy> networkNameToPresentationPolicy = new ConcurrentHashMap<String, PresentationPolicy>();
	private final ConcurrentHashMap<String, IUser> idToUserAuthData = new ConcurrentHashMap<>();
	
	private final HashMap<String, HashMap<URI, String>> networkSourceToNetworkNodesThenNodeUidToInternalName = new HashMap<String, HashMap<URI,String>>();
	private final HashMap<String, HashMap<URI, String>> networkNodes = new HashMap<String, HashMap<URI,String>>();
	
	private JsonObject complete = null;
	private ArrayList<NodeData> networkNodeData = null;
	private ArrayList<NodeData> nodeMemberData = null;
	private URI nodeAttached = null; 
		
	private NodeReader nodeReader = null;

	public JsonObject discover(ArrayList<NodeData> sources) throws Exception {
		if (leadInfoList.isEmpty()) {
			JsonObject parent = generateQuickList(sources);
			computeNodeInformation(sources);
			return parent;
			
		} else {
			return generateCompleteList();
		
		} 
	}

	public PresentationPolicy fetchPresentationPolicy(String networkName) {
		return this.networkNameToPresentationPolicy.get(networkName);
		
	}
	
	public NodeInformation fetchNodeInformation(String networkName) throws HubException {
		for (TrustNetwork t : leadInfoList) {
			NodeInformation i = t.getNodeInformation();
			if (i.getNodeName().contentEquals(networkName)) {
				return i; 
				
			}
		}
		throw new HubException("Node Information not found"); 
		

	}
	
	public JsonObject waitForFullList() throws Exception {
		if (this.nodeReader!=null) {
			if (this.nodeReader.isBusy()) {
				synchronized (nodeReader) {
					long time = Timing.currentTime();
					long timeout = 10000;
					logger.info("Waiting for Node Reader " + time);
					nodeReader.wait(timeout);
					
					if (Timing.hasBeen(time, timeout)) {
						throw new UxException("Timeout waiting waiting for node to be read.");
						
					} else {
						return generateCompleteList();
						
					}
				}
			} else if (this.nodeReader.isError()) {
				if (this.nodeReader.getException()!=null) {
					throw this.nodeReader.getException();
					
				} else {
					throw new UxException("General Node Reader Error - Check Logs");
					
				}
			} else {
				return generateCompleteList();
				
			}
		} else {
			throw new ProgrammingException("Node reader was null on second request - fail");
			
		}
	}
	
	private void computeNodeInformation(ArrayList<NodeData> localNodes) throws Exception {
		try {
			if (nodeReader!=null) {
				if (nodeReader.isBusy()) {
					logger.warn("Node Reader was busy - this request is unusual.");
					
				} else if (nodeReader.isError()) {
					logger.error("Node Reader was had an error", nodeReader.getException());
					nodeReader = new NodeReader(localNodes);
					
				} else {
					nodeReader = new NodeReader(localNodes);
					
				}
			} else {
				nodeReader = new NodeReader(localNodes);
				
			}
		} catch (Exception e) {
			throw e; 
			
		}
	}

	private JsonObject generateQuickList(ArrayList<NodeData> sources) throws Exception {
		JsonObject result = new JsonObject();
		JsonObject networkData = new JsonObject();
		JsonArray networkList = new JsonArray();
		result.add("networkList", networkList);
		result.add("networkData", networkData);
		result.addProperty("quickList", true);
		int node = 0; 
		
		for (NodeData d : sources) {
			if (d.getType().equals(NodeData.TYPE_LEAD)) {
				networkList.add(d.getNetworkName());
				JsonObject networkItem = new JsonObject();
				networkItem.addProperty("networkName", d.getName());
				networkItem.addProperty("sourceUrl", d.getNodeUrl().toString());
				networkData.add(d.getName(), networkItem);
				
			} else if (d.getType().equals(NodeData.TYPE_MODERATOR)) {
				if (node==0) {
					node++;
					nodeAttached=d.getNodeUrl();
					result.addProperty("attached", nodeAttached.toString());

				} else {
					throw new UxException("Node Corrupt - Two Source Attachments");
					
				}
			}
		}
		return result;
		
	}

	private JsonObject generateCompleteList() throws Exception {
		if (this.complete!=null) {
			return complete;
			
		} else if (!this.leadInfoList.isEmpty() || !this.nodeInformationList.isEmpty()) {
			return buildJsonObject();
			
		} else {
			throw new HubException("No Node Information Found - yet Client still called for more network info.");

		}
	}

	private JsonObject buildJsonObject() throws Exception {
		JsonObject complete = new JsonObject();
		buildNodeData(complete);
		buildMemberData(complete);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		logger.debug(gson.toJson(complete));
		return complete;
		
	}

	private void buildMemberData(JsonObject complete) throws HubException {
		if (this.nodeMemberData!=null && !this.nodeMemberData.isEmpty()){
			JsonArray members = new JsonArray();
			logger.info("Build Member Data - There are " + this.nodeMemberData.size() + " members");

			for (NodeData n : this.nodeMemberData){
				JsonObject o = new JsonObject();
				o.addProperty("id", n.getFkUser());
				o.addProperty("username", n.getName());
				IUser uad = idToUserAuthData.get(n.getFkUser());
				if (uad!=null){
					o.addProperty("email", "no personal data stored here");

				} else {
					logger.info("Dumping out map before exception.");
					for (String i : idToUserAuthData.keySet()){
						logger.info("Found Key on MAP= " + i);

					}
					throw new HubException("Failed to find auth data for " + n.getFkUser() + " " + n.getName());

				}
				members.add(o);

			}
			complete.add("members", members);
		} else {
			logger.info("Node Member Data is Empty");

		}
	}

	private void buildNodeData(JsonObject complete) throws HubException {
		if (this.nodeAttached!=null) {
			complete.addProperty("attached", this.nodeAttached.toString());

		}
		JsonArray networkList = new JsonArray();
		JsonArray secondaryNetworkList = new JsonArray();
		complete.add("networkList", networkList);

		complete.add("secondaryNetworkList", secondaryNetworkList);
		JsonObject networkData = new JsonObject();
		complete.add("networkData", networkData);

		for (TrustNetwork t : this.leadInfoList) {
			NodeInformation i = t.getNodeInformation();
			String name = i.getNodeName();
			networkList.add(name);

			JsonObject networkItem = new JsonObject();
			networkData.add(name, networkItem);
			networkItem.addProperty("networkName", name);
			networkItem.addProperty("sourceUrl", i.getStaticNodeUrl0().toString());

			// Pseudonym Scopes
			PresentationPolicy pp = networkNameToPresentationPolicy.get(name);
			if (pp!=null) {
				List<PseudonymInPolicy> nyms = pp.getPseudonym();
				JsonArray defaultPlatforms = new JsonArray();
				networkItem.add("defaultPlatforms", defaultPlatforms);

				for (PseudonymInPolicy nym : nyms) {
					String scope = nym.getScope();
					defaultPlatforms.add(scope);

				}
			} else {
				throw new HubException("Failed to find presentation policy for " + i.getNodeName());

			}
			// Presentation Policy - Issuer List
			JsonArray authorizedNodes = new JsonArray();
			HashMap<URI, String> network = networkSourceToNetworkNodesThenNodeUidToInternalName.getOrDefault(name, new HashMap<URI, String>());
			logger.info("Generating Authorized Node List " + network.size());
			ArrayList<NetworkParticipant> ps = t.getParticipants();
			for (NetworkParticipant p : ps) {
				String uid = p.getNodeUid().toString();
				logger.info("Issuer UID= " + uid);
				JsonObject authNode0 = new JsonObject();
				authNode0.addProperty("nodeUid", uid);
				authNode0.addProperty("internalName", network.get(URI.create(uid)));
				authorizedNodes.add(authNode0);

			}
			networkItem.add("authorizedNodes", authorizedNodes);

			JsonObject secondaryNetworks = new JsonObject();
			networkItem.add("secondaryList", secondaryNetworks);

			// Secondary Networks

		}
	}

	public synchronized void clear() {
		logger.debug("Calling CLEAR!!!!! << ------ ");
		leadInfoList.clear();
		networkNameToPresentationPolicy.clear();
		this.nodeAttached=null;
		this.networkNodeData = null;
		this.nodeMemberData = null;
		this.idToUserAuthData.clear();
		this.complete=null;
		
	}

	public ArrayList<TrustNetwork> getLeadInfoList() {
		return leadInfoList;
	}

	public ArrayList<TrustNetwork> getNodeInformationList() {
		return nodeInformationList;
	}

	private class NodeReader extends ModelSingleSequence {
		
		private final ArrayList<NodeData> localNodes;
		private boolean error = false;
		private Exception exception = null;
		
		
		public NodeReader(ArrayList<NodeData> sources) throws Exception {
			super("NodeReader");
			this.localNodes = sources;
			this.start();
			
		}

		@Override
		protected void process() {
			leadInfoList.clear();
			try {
				fetchNetworkNodes();
				fetchNetworkMembers();

				for (NodeData n: localNodes) {
					boolean isLead = n.getType().equals(NodeData.TYPE_LEAD);
					NodeVerifier v = NodeVerifier.openNode(n.getNodeUrl(), isLead, isLead);
					
					if (isLead) {
						leadInfoList.add(v.getTargetTrustNetwork());
						networkNameToPresentationPolicy.put(n.getNetworkName(), v.getPresentationPolicy());
						
					} else {
						nodeInformationList.add(v.getTargetTrustNetwork());

					}
				}
				localNodes.clear();

			} catch (UxException e) {
				this.error = true;
				this.exception = e;
				logger.error("Error", e);
				
			} catch (SecurityException e) {
				this.error = true;
				this.exception = e;
				logger.error("Error", e);
				
			} catch (Exception e) {
				this.error = true;
				logger.error("Error", e);
				
			}
		}

		private void fetchNetworkMembers() throws Exception {
			try {
				// if (nodeMemberData==null) {
					logger.info("Searching for Network Member Data");
					nodeMemberData = NodeStore.getInstance().findType(NodeData.TYPE_MEMBER);

					if (!nodeMemberData.isEmpty()){ // for legibility - cannot happen
						fetchUserAuthDataForNode();

						for (NodeData n : nodeMemberData) {
							IUser u = idToUserAuthData.get(n.getFkUser());
							if (u==null){
								logger.debug("Failed to find User Auth Data - " + n.getFkUser() + " auth data " + u);

							}
						}
					}
				/* } else {
					logger.debug("Node Member Data was not Populated when requested - already populated");

				} //*/
			} catch (ItemNotFoundException e) {
				logger.info("No Node Members on this Node");

			} catch (Exception e) {
				throw e;
				
			}
		}

		private void fetchUserAuthDataForNode() {
			try {
				if (idToUserAuthData.isEmpty()){
					CloudantClient client = CouchDbClient.instance();
					Database db = client.database(CouchDbHelper.getDbUsers(), true);
					CouchRepository<IUser> repo = new CouchRepository<>(db, IUser.class);
					QueryBasic q = QueryBasic.selectType(IUser.I_USER_MEMBER);
					List<IUser> users = repo.read(q);
					logger.debug("Found " + users.size() + " non administrators");
					for (IUser user : users){
						logger.debug("Adding To Map " + user.get_id() + "=" + user.getUsername());
						idToUserAuthData.put(user.get_id(), user);

					}
				} else {
					logger.debug("UserAuthData was not cleared and so not refreshed.");

				}
			} catch (NoDocumentException e){


			} catch (Exception e){
				logger.error("Error fetching member data", e);
			}
		}

		private void fetchNetworkNodes() throws Exception {
			try {
				if (networkNodeData==null) {
					logger.info("Searching for Network Node Data");

					networkNodeData = NodeStore.getInstance().findAllNetworkNodes();

					for (NodeData n : networkNodeData) {
						logger.info("network-node=" + n.getName());
						
						HashMap<URI, String> network = networkSourceToNetworkNodesThenNodeUidToInternalName.get(n.getNetworkName());
						if (network==null) {
							network = new HashMap<URI, String>();
							networkSourceToNetworkNodesThenNodeUidToInternalName.put(n.getNetworkName(), network);
							
						}
						logger.info("network-node=" + n.getName() + " UID=" + n.getNodeUrl() + " networkName=" + n.getNetworkName());
						// For a network-node type the URL is the UID for that node. 
						network.put(n.getNodeUrl(), n.getName());
						
					}
				}
			} catch (ItemNotFoundException e) {
				logger.info("No Network Nodes on this Node");
				
			} catch (Exception e) {
				throw e; 
				
			}
		}

		public Exception getException() {
			return exception;
		}

		public boolean isError() {
			return error;
			
		}
	}
}