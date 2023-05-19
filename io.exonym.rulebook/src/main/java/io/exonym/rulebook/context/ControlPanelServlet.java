package io.exonym.rulebook.context;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.google.gson.JsonObject;
import com.ibm.zurich.idmx.exception.SerializationException;
import com.ibm.zurich.idmx.jaxb.JaxbHelperClass;
import eu.abc4trust.xml.PresentationToken;
import io.exonym.actor.actions.MembershipManager;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.couchdb.QueryOrGate;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.ProgrammingException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.*;
import io.exonym.lite.standard.WhiteList;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.PassStore;
import io.exonym.lite.time.DateHelper;
import io.exonym.utils.node.ProgressReporter;
import io.exonym.utils.storage.NodeInformation;
import io.exonym.utils.storage.TrustNetwork;
import io.exonym.rulebook.exceptions.ItemNotFoundException;
import io.exonym.rulebook.schema.Administrators;
import io.exonym.lite.connect.WebUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.sasl.AuthenticationException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@WebServlet("/cp")
public final class ControlPanelServlet extends HttpServlet {
	
	private static final Logger logger = LogManager.getLogger(ControlPanelServlet.class);
	
	private final static String NAME_INVALID = "Network Name should be a minimum of three characters and can only contains hyphens";
	private final static String NO_PASS_STORE= "This administrator was not currently active - Exonym Support";
	private final static String URL_INVALID = "Please provide a valid URL";
	private final static String IS_NOT_VALID_USERNAME = " is not a valid username - Between 3 &amp 32 standard ASCII characters";

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private IAuthenticator a = IAuthenticator.getInstance();
	private LocalNodeInformation localNodeInfo = null;
	private ArrayList<NodeData> localNodes = new ArrayList<>();
	private ConcurrentHashMap<String, PresentationToken> sidToPresentationTokenMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, ProgressReporter> progressReporters = new ConcurrentHashMap<>();

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			a.getPassStore(req);
			logger.info("ContentType=" + req.getContentType());
			if (req.getContentType().startsWith("multipart/form-data")){
				registerTokenWithSession(req, resp);

			} else {
				standardRequest(req, resp);

			}
		} catch (ProgrammingException e) {
			logger.error("Programming Error", e);
			WebUtils.processError(new Exception("Server Error - check logs", e), resp);

		} catch (HubException e) {
			logger.info(e.getMessage());

		} catch (UxException e) {
			WebUtils.processError(e, resp);

		} catch (AuthenticationException e) {
			WebUtils.processError(new UxException(ErrorMessages.FAILED_TO_AUTHORIZE), resp);

		} catch (Exception e) {
			WebUtils.processError(new Exception("Server Error - check logs", e), resp);
			
		}		
	}

	private void registerTokenWithSession(HttpServletRequest req, HttpServletResponse resp) throws IOException, SerializationException {
		BufferedReader reader = req.getReader();
		String endSearch = "PresentationToken>";
		String xml  = reader.lines().collect(Collectors.joining(System.lineSeparator()));
		xml = xml.substring(xml.indexOf("<?xml version"), xml.lastIndexOf(endSearch) + endSearch.length());
		PresentationToken token = (PresentationToken) JaxbHelperClass.deserialize(xml, true).getValue();
		this.sidToPresentationTokenMap.put(req.getSession().getId(), token); //*/
		logger.info(this.sidToPresentationTokenMap.size());
		WebUtils.success(resp);

	}

	private void standardRequest(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		String sid = req.getSession().getId();
		HashMap<String, String> dataReq = WebUtils.buildParams(req, resp);
		if (a.isAdministrator(sid)) {
			adminRequests(dataReq, req, resp);

		} else if (a.isPrimaryAdministrator(sid)) {
			primaryAdminRequests(dataReq, req, resp);

		} else {
			throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE);

		}
	}

	/*
	 * 
	 * 		ADMIN REQUESTS
	 * 
	 */
	private void adminRequests(HashMap<String, String> dataReq,
							   HttpServletRequest req, HttpServletResponse resp) throws Exception {
		try {
			logger.debug("Administrator Commands");
			String cmd = dataReq.get("command");

			if (cmd!=null) {
				if (cmd.equals("progress")){
					progressMonitor(req, resp);

				} else if (cmd.equals("getNetworks")) {
					getNetworks(dataReq, resp);

				} else if (cmd.equals("sourceNewNetworkCreate")) {
					sourceNewNetworkCreate(dataReq, req, resp);
					refreshNetworkMap();

				} else if (cmd.equals("sourceNodeManagementAdd")) {
					sourceNodeManagementAdd(dataReq, resp);
					refreshNetworkMap();

				} else if (cmd.equals("sourceNodeManagementRemove")) {
					sourceNodeManagementRemove(dataReq, resp);
					refreshNetworkMap();

				} else if (cmd.equals("nodeSourceAttachmentAttach")) {
					nodeSourceAttachmentAttach(dataReq, req, resp);

				} else if (cmd.equals("nodeMemberRevoke")) {
					nodeMemberRevoke(dataReq, req, resp);

				} else if (cmd.equals("fullNetworkDataRequest")) {
					fullNetworkRequest(req, resp);

				// Pretty sure this doesn't get called and it's a duplicate of sourceNodeManagementRemove
				} else if (cmd.equals("sourceNodeManagementAuthorizedNodesDelete")) {
					sourceNodeManagementAuthorizedNodesDelete(dataReq, req, resp);
					refreshNetworkMap();
					throw new RuntimeException("It does get called");

				} else {
					throw new Exception("Unknown Administrator Request - " + cmd);
					
				}
			} else {
				throw new ProgrammingException("Client Side Error - no 'command' attribute provided");
				
			}
		} catch (Exception e) {
			throw e; 
			
		}
	}

	private void refreshNetworkMap() throws Exception {
		NetworkMapWeb map = new NetworkMapWeb();
		map.refresh();

	}

	private void progressMonitor(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		logger.info("Publisher Test");
		String sid = req.getSession().getId();
		ProgressReporter reporter = this.progressReporters.get(sid);
		JsonObject o = new JsonObject();

		if (reporter!=null){
			if (reporter.getException()!=null){
				logger.debug("CHECK FOR EXCEPTION");
				endReport(reporter, sid, o);

			} else if (reporter.isFinished()){
				logger.debug("CHECK FOR FINISHED");
				o.addProperty("complete", reporter.getWrapMessage());
				this.progressReporters.remove(sid);

			} else {
				String m = reporter.takeComplete();

				if (m==null){
					synchronized (resp){
						m = reporter.setNextResponse(resp);
						logger.debug("Got notify or timeout");

						if (reporter.getException()!=null){
							logger.debug("-CHECK FOR EXCEPTION");
							endReport(reporter, sid, o);

						} else if (reporter.isFinished()){
							logger.debug("-CHECK FOR FINISHED");
							o.addProperty("complete", reporter.getWrapMessage());
							this.progressReporters.remove(sid);

						} else if (m==null){
							o.addProperty("update", "Nearly there - Key Computation shouldn't take much longer");

						} else {
							o.addProperty("update", m);

						}
					}
				}
			}
			WebUtils.respond(resp, o);

		} else {
			throw new UxException("No current jobs");

		}
	}

	private void endReport(ProgressReporter reporter, String sid, JsonObject o) {
		reporter.close();
		o.addProperty("error", reporter.getException().getMessage());
		logger.error("Error", reporter.getException());
		this.progressReporters.remove(sid);

	}

	private void nodeMemberRevoke(HashMap<String, String> dataReq,
								  HttpServletRequest req, HttpServletResponse resp) throws Exception {
		try {
			String rawB64Token = dataReq.get("token");
			PresentationToken token = this.sidToPresentationTokenMap.get(req.getSession().getId());
			if (token==null){
				if (rawB64Token!=null){
					byte[] rawToken = Base64.decodeBase64(rawB64Token);
					token = (PresentationToken) JaxbHelperClass.deserialize(
							new String(rawToken, "UTF8"), true).getValue();

				} else {
					throw new UxException("Please provide a valid token");

				}
			}
			MembershipManager mm = new MembershipManagerWeb(a.getNetworkNameForNode());
			PassStore store = new PassStore(RulebookNodeProperties.instance().getNodeRoot(), false);
			String raiHash = mm.revokeMember(token, store);
			NodeStore ns = NodeStore.getInstance();
			NodeData node = ns.openThisAdvocate();
			node.setLastRAIHash(raiHash);
			ns.updateNodeDataItem(node);
			WebUtils.success(resp);

		} catch (Exception e){
			throw new UxException("The Token was Invalid", e);

		}
	}

	private void nodeSourceAttachmentAttach(HashMap<String, String> dataReq,
											HttpServletRequest req, HttpServletResponse resp) throws Exception{
		try {
			String name = dataReq.get("publicName");
			URL sourceUrl = new URL(dataReq.get("sourceUrl"));
			
			if (name!=null) {
				if (WhiteList.isMinLettersAllowsNumbersAndHyphens(name, 3)) {
					
					if (WhiteList.url(sourceUrl.toString())) {
						PassStore p = new PassStore(RulebookNodeProperties.instance().getNodeRoot(), false);
						
						if (p!=null) {
							String networkName = NodeManagerWeb.computeNetworkName(sourceUrl.toURI());
							NodeManagerWeb n = new NodeManagerWeb(networkName);
							ProgressReporter reporter = new ProgressReporter(new String[] {
									"Defining Trust Network",
									"Computing Root Key",
									"Computing Revocation Authority Key - Please be patient...",
									"Computing Credential Issuer Key - Please be patient...",
									"Computing Inspector Key - Last key before publishing...",
									"Publishing Network to Replication URLs",
									"Almost done!"

							});
							String sid = req.getSession().getId();
							this.progressReporters.put(sid, reporter);
							// String fullPath = WebUtils.getFullPath(req);

							new Thread(() -> {
								try {
									logger.info("Starting Source Attachment");
									NodeInformation info = n.setupAdvocateNode(sourceUrl, name, p, reporter);
									NodeData d = new NodeData();
									d.setNetworkName(networkName);
									d.setName(name);
									d.setType(NodeData.TYPE_NODE);
									d.setNodeUrl(info.getStaticNodeUrl0());
									d.setNodeUid(info.getNodeUid());
									d.setSourceUid(info.getSourceUid());
									d.setFailOverUrl(info.getStaticNodeUrl1());
									d.setLastRAIHash(n.getRaiHash());
									NodeStore.getInstance().add(d);
									localNodeInfo.clear();
									progressReporters.remove(sid).wrap(info.getStaticNodeUrl0().toString());
									refreshNetworkMap();
//										RulebookManager.getInstance().reset();

								} catch (Exception e) {
									logger.error("Error", e);
									progressReporters.get(sid).setException(e);

								}
							}).start();

							JsonObject r = new JsonObject();
							r.addProperty("update", "Defining X-Node");
							WebUtils.respond(resp, r);
							
						} else {
							throw new UxException(NO_PASS_STORE);
							
						}
					} else {
						throw new UxException(URL_INVALID);
						
					}
				} else {
					throw new UxException(NAME_INVALID);
					
				}
			} else {
				throw new UxException(NAME_INVALID + " - No Name");
				
			}
		} catch (Exception e) {
			throw e; 
			
		}
	}

	/*
	private void nodeSecondaryNetworksAdd(HashMap<String, String> dataReq, UserAuthData user,
			HttpServletRequest req, HttpServletResponse resp) throws Exception {
		Gson gson = new Gson();
		URI uid = auth.getNodeUid();
		UUID uuid = auth.prepareIssuance();
		JsonObject o = new JsonObject();
		o.addProperty("command", "accept");
		o.addProperty("iuuid", uuid.toString());
		o.addProperty("nodeUid", uid.toString());
		NetworkMap networkMap = NetworkMap.getInstance();
		String url = dataReq.get("url");
		URI nodeUid = networkMap.getNodeUid(url);
		if (nodeUid!=null){
			NetworkMapDataItem target = networkMap.getNode(nodeUid);
			String endPoint = target.getxNodeUrl().toString() + "/own";
			String response = UrlHelper.post(endPoint, o);
			Ack ack = gson.fromJson(response, Ack.class);
			if (ack.isAck()){
				Utils.success(resp);

			} else {
				logger.info(response);
				throw new UxException("Response from Counterpart: " + ack.getMessage());

			}
		} else {
			throw new UxException("The URL was invalid " + url);

		}
	} //*/

	/*
	private void nodeSecondaryNetworksJoin(HashMap<String, String> dataReq,
			HttpServletRequest req, HttpServletResponse resp) throws Exception{
		try {
			NodeStore nodeStore = NodeStore.getInstance();
			ArrayList<NodeData> recipients = null;
			try {
				recipients = nodeStore.findType(NodeData.TYPE_RECIPIENT);

			} catch (ItemNotFoundException e) {
				logger.info("No Receipts Expected");


			}
			NetworkMap map = NetworkMap.getInstance();
			URI nodeUid = map.getNodeUid(dataReq.get("url"));
			if (nodeUid==null){
				throw new UxException("The URL provided was invalid: " + dataReq.get("url"));

			}
			if (recipients!=null){
				for (NodeData d : recipients){
					if (d.getNodeUid().equals(nodeUid)){
						throw new UxException("Request for Issuance already made");

					}
				}
			}
			NodeData nd = new NodeData();
			NetworkMapDataItem nmi = map.getNode(nodeUid);
			NodeVerifier verifier = NodeVerifier.tryNode(nmi.getUrl0().toString(), nmi.getUrl1().toString(),
					false, false);

			TrustNetwork tn = verifier.getTargetTrustNetwork();

			nd.setNodeUrl(nmi.getUrl0().toURI());

			nd.setNodeUid(tn.getNodeInformation()
					.getNodeRootUid());

			nd.setType(NodeData.TYPE_RECIPIENT);
			nd.setNetworkName(tn.getNodeInformation().getSourceUid().toString());
			nd.setName(tn.getNodeInformation().getNodeName());
			nd.setPublishAfterReceipt(true);
			nodeStore.add(nd);
			Utils.success(resp);

		} catch (Exception e) {
			throw new UxException(e.getMessage(), e);

		}
	} //*/

	/*
	protected void nodeMembershipAdd(HashMap<String, String> dataReq,
			HttpServletRequest req, HttpServletResponse resp) throws Exception{
		try {
			String sid = req.getSession().getId();
			String username = dataReq.get("username");
			String email = dataReq.get("email");
			String tel = dataReq.get("tel");
			PassStore p = auth.getP();
			if (WhiteList.email(email)) {
				
				if (WhiteList.username(username)) {
					try {
						new XNodeContainer(username);
						throw new UxException("There is already a user on this node with the name " + username);
						
					} catch (Exception e) {
						// Crypto Onboarding
						MembershipManagerWeb mm = new MembershipManagerWeb(auth.getNetworkNameForNode());
						Member member = mm.addMember(username, tel, email, p);
						
						NodeData nodeData = new NodeData();
						nodeData.setName(username);
						nodeData.setNetworkName(auth.getNetworkNameForNode());
						nodeData.setType(NodeData.TYPE_MEMBER);
						nodeData.setNodeUrl(URI.create(email));
						nodeData.setHandle(
								CryptoUtils.computeSha256HashAsHex(
										member.getHandle().toString()));

						if (this.localNodeInfo!=null) {
							localNodeInfo.clear();

						}
						onboardMemberToNode(email, nodeData, req, resp);

					}
				} else {
					throw new UxException(username + IS_NOT_VALID_USERNAME);
					
				}
			} else {
				throw new UxException(email + IS_NOT_VALID_EMAIL);
				
			}
		} catch (Exception e) {
			throw e; 
			
		}
	} //*/

	/*
	private void onboardMemberToNode(String email, NodeData nodeData,
									 HttpServletRequest req, HttpServletResponse resp) throws Exception {
		JsonObject json = new JsonObject();
		try {
			// This can throw a UserAlready...
			// Administrators cannot give themselves privilege.
			String id = register.addNewMemberAccount(Utils.getFullPath(req), nodeData);

			// Add a temporary item to the list and allow deletion
			json.addProperty("message", "Registration Email Sent to " + email);
			json.addProperty("email", email);
			json.addProperty("session", id);
			Utils.respond(resp, json);
			
		} catch (UserAlreadyExistsException e) {
			throw new UxException("An administrator cannot add themselves to the network");
			
		}
	} //*/

	/*

	private void nodeMembershipEdit(HashMap<String, String> dataReq, UserAuthData user, 
			HttpServletRequest req, HttpServletResponse resp) throws Exception{
		throw new Exception("Code Not Implemented");
		
	}

	private void sourceSecondaryNetworkGetNetwork(HashMap<String, String> dataReq, UserAuthData user, 
			HttpServletRequest req, HttpServletResponse resp) throws Exception{
		throw new Exception("Code Not Implemented");
		
	}

	private void sourceSecondaryNetworksEnforce(HashMap<String, String> dataReq, UserAuthData user, 
			HttpServletRequest req, HttpServletResponse resp) throws Exception{
		throw new Exception("Code Not Implemented");
		
	} //*/

	private void sourceNewNetworkCreate(HashMap<String, String> dataReq,
			HttpServletRequest req, HttpServletResponse resp) throws Exception{

		String rulebookUrl = dataReq.get("rulebookUrl");
		try {
			String orgName = dataReq.get("networkName");

			if (WhiteList.isMinLettersAllowsNumbersAndHyphens(orgName, 3)) {
				PassStore store = new PassStore(RulebookNodeProperties.instance().getNodeRoot(), false);
				orgName = orgName.toLowerCase();
				NodeManagerWeb n = new NodeManagerWeb(orgName);
				TrustNetwork t = n.setupNetworkSource(new URL(rulebookUrl), store); //, WebUtils.getFullPath(req)
				NodeInformation info = t.getNodeInformation();
				URL url = info.getStaticSourceUrl0();
				NodeData nd = new NodeData();
				nd.setName(orgName);
				nd.setNetworkName(orgName);
				nd.setType(NodeData.TYPE_SOURCE);
				nd.setNodeUrl(url);
				nd.setNodeUid(info.getSourceUid());
				nd.setSourceUid(info.getSourceUid());
				nd.setLastPPHash(n.getPpHash());

				NodeStore node = NodeStore.getInstance();
				node.add(nd);
				// RulebookManager.getInstance().reset(); // commented out before IUser
				JsonObject r = new JsonObject();
				r.addProperty("networkName", info.getNodeName());

				if (this.localNodeInfo!=null) {
					localNodeInfo.clear();

				}
				this.localNodes.clear();
				WebUtils.respond(resp, r);

			} else {
				throw new UxException(NAME_INVALID);
				
			}
		} catch (FileNotFoundException e) {
			throw new UxException(ErrorMessages.RULEBOOK_FAILED_TO_VERIFY_OR_NOT_FOUND, e);

		} catch (MalformedURLException e) {
			throw new UxException(ErrorMessages.URL_INVALID, e, rulebookUrl);

		} catch (Exception e) {
			throw e;
			
		}
	}
	
	private void sourceNodeManagementAdd(HashMap<String, String> dataReq, HttpServletResponse resp) throws Exception{
		String networkName = dataReq.get("networkName");
		try {

			String nodeUrl = dataReq.get("nodeUrl");
			String internalName = dataReq.get("internalName");
			boolean testNet = Boolean.parseBoolean(dataReq.getOrDefault("testNet", "true"));
			PassStore passStore = new PassStore(RulebookNodeProperties.instance().getNodeRoot(), false);
			passStore.setUsername(networkName);
			
			if (WhiteList.url(nodeUrl)) {
				NodeManagerWeb n = new NodeManagerWeb(networkName);
				NetworkMapWeb networkMap = new NetworkMapWeb();

				URI nodeUid = n.addNodeToSource(new URL(nodeUrl), passStore, networkMap, testNet);
				broadcast();
				NodeData d = new NodeData();
				d.setNodeUrl(new URL(nodeUrl));
				d.setNodeUid(nodeUid);
				d.setType(NodeData.TYPE_NETWORK_NODE);
				d.setName(internalName);
				d.setSourceUid(UIDHelper.computeSourceUidFromNodeUid(nodeUid));
				d.setNetworkName(networkName);
				d.setLastPPHash(n.getPpHash());
				NodeStore.getInstance().add(d);
				localNodeInfo.clear();
				localNodeInfo.clear();
				WebUtils.success(resp);
				
			} else {
				throw new UxException(nodeUrl + " is not a valid URL");
				
			}
		} catch (FileNotFoundException e) {
			logger.info("File Not Found " + e.getMessage());
			throw new UxException("Node is not trying to attach to the source - " + networkName, e);
			
		} catch (Exception e) {
			throw e;
			
		}
	}

	private void broadcast() throws Exception {
		ExoNotify notify = new ExoNotify();
		notify.setT(DateHelper.currentIsoUtcDateTime());
		URI nodeUuid = IAuthenticator.getInstance().getNodeUid();
		URI sourceUuid = UIDHelper.computeSourceUidFromNodeUid(nodeUuid);
		notify.setAdvocateUID(sourceUuid);
		notify.setType(ExoNotify.TYPE_SOURCE);
		byte[] toSign = ExoNotify.signatureOnAckAndOrigin(notify);
		Signer signer = Signer.getInstance();
		String pwd = RulebookNodeProperties.instance().getNodeRoot();
		PassStore store = new PassStore(pwd, false);
		byte[] sig = signer.sign(toSign, sourceUuid.toString(), store);
		notify.setSigB64(Base64.encodeBase64String(sig));
		Broadcaster broadcaster = new Broadcaster(notify,
				CouchDbHelper.repoNetworkMapItem());
		broadcaster.execute();
		broadcaster.close();

	}

	/*
	 * Remove by URL
	 */
	private void sourceNodeManagementRemove(HashMap<String, String> dataReq, HttpServletResponse resp) throws Exception{
		try {
			String networkName = dataReq.get("networkName");
			URL nodeUrl = new URL(dataReq.get("nodeUrl"));
			PassStore p = new PassStore(RulebookNodeProperties.instance().getNodeRoot(), false);
			
			if (WhiteList.url(nodeUrl.toString())) {
				NodeManagerWeb n = new NodeManagerWeb(networkName);
				URI nodeUid = n.removeNode(nodeUrl, p);
				broadcast();
				NodeStore store = NodeStore.getInstance();
				NodeData d = store.findNetworkNodeDataItem(nodeUid.toString());
				d.setLastPPHash(n.getPpHash());
				store.delete(d);
				
				localNodeInfo.clear();
				localNodes.clear();
				WebUtils.success(resp);
			
			} else {
				throw new UxException(nodeUrl + " is not a valid URL");
				
			}
		} catch (Exception e) {
			throw e;
			
		}
	}

	private void sourceNodeManagementAuthorizedNodesDelete(HashMap<String, String> dataReq,
														   HttpServletRequest req, HttpServletResponse resp) throws Exception {
		try {
			String nodeUid = dataReq.get("selectedId");
			String networkName = dataReq.get("selectedNetwork");
			RulebookNodeProperties props = RulebookNodeProperties.instance();
			PassStore p = new PassStore(props.getNodeRoot(), false);
			NodeManagerWeb n = new NodeManagerWeb(networkName);
			n.removeNode(nodeUid, p);
			broadcast();
			NodeStore store = NodeStore.getInstance();
			NodeData d = store.findNetworkNodeDataItem(nodeUid);
			store.delete(d);

			localNodeInfo.clear();
			localNodes.clear();
			WebUtils.success(resp);

		} catch (Exception e) {
			throw e;

		}
	}	//*/

	private void initNode(HttpServletResponse resp) throws Exception {
		if (localNodeInfo==null) {
			localNodeInfo = new LocalNodeInformation();

		}
		if (localNodes.isEmpty()) {
			NodeStore node = NodeStore.getInstance();
			try {
				logger.info("Finding all local sources and nodes");
				localNodes = node.findAllLocalNodes();

			} catch (ItemNotFoundException e) {
				throw e;

			}
		}
	}

	private void getNetworks(HashMap<String, String> dataReq, HttpServletResponse resp) throws Exception{
		try {
			initNode(resp);
			String name = dataReq.get("networkName");
			JsonObject json = localNodeInfo.discover(localNodes);
			if (name != null) {
				json.addProperty("selectedNetwork", name);

			}
			WebUtils.respond(resp, json);

		} catch (ItemNotFoundException e){
			JsonObject j = new JsonObject();
			j.addProperty("noSources", true);
			WebUtils.respond(resp, j);
			throw new HubException("There are no sources - expected error");

		} catch (Exception e) {
			throw e; 
			
		}
	}

	private void fullNetworkRequest(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		try {
			if (localNodeInfo!=null) {
				JsonObject json = localNodeInfo.waitForFullList();
				WebUtils.respond(resp, json);
				
			} else {
				throw new HubException("Unexpected failure - Node Reader null");
				
			}
		} catch (Exception e) {
			throw e; 
			
		}
	}
	
	/*
	private void sourceNodeInformationAddDefaultPlatform(HashMap<String, String> dataReq, 
			HttpServletRequest req, HttpServletResponse resp) throws Exception {
		try {
			String network = dataReq.get("networkName");
			String item = dataReq.get("defaultPlatform");
			String sid = req.getSession().getId();
			
			if (localNodeInfo!=null) {
				PresentationPolicy pp = localNodeInfo.fetchPresentationPolicy(network);
				
				if (pp!=null) {
					List<PseudonymInPolicy> nyms = pp.getPseudonym();
					for (PseudonymInPolicy nym : nyms) {
						if (nym.getScope().equals(item)) {
							throw new UxException("The item " + nym.getScope() + " already exists");
							
						}
					}
					NodeManagerWeb m = new NodeManagerWeb(network);
					NodeInformation ni = localNodeInfo.fetchNodeInformation(network);
					m.setNetworkUid(ni.getSourceUid());
					m.addScope(item, auth.getPassStore(sid));
					localNodeInfo.clear();
					Utils.success(resp);

				} else {
					throw new HubException("Presentation Policy was unexpectedly null");
					
				}
			} else {
				throw new HubException("NodeInfo Manager was unexpectedly null");
				
			}
		} catch (Exception e) {
			throw e;
			
		}
	} //*/
	
	/*
	private void sourceNodeInformationDefaultPlatformDelete(HashMap<String, String> dataReq, 
			HttpServletRequest req, HttpServletResponse resp) throws Exception {
		try {
			String network = dataReq.get("selectedNetwork");
			String item = dataReq.get("selectedValue");
			
			if (!item.equals("urn:io:exonym")) {
				String sid = req.getSession().getId();
				NodeManagerWeb m = new NodeManagerWeb(network);
				NodeInformation ni = localNodeInfo.fetchNodeInformation(network);
				m.setNetworkUid(ni.getSourceUid());
				m.removeScope(item, auth.getPassStore(sid));
				if (localNodeInfo!=null) {
					localNodeInfo.clear();
					
				}
				Utils.success(resp);
				
			} else {
				throw new UxException("io:exonym is the base pseudonym and is required - Cannot Delete");
				
			}
		} catch (Exception e) {
			throw e; 
			
		}
	} //*/

	/*
	 * 
	 * 
	 * 
	 * 		PRIMARY ADMIN REQUESTS
	 * 
	 * 
	 * 
	 */
	// {"selectedId":"b802c1912e2b4afabf8aa417cb86f0a4Btn",
	// "multiId":"adminAccessAdministratorAccounts","selectedValue":"mike@exonym.io","command":"delete"}
	private void primaryAdminRequests(HashMap<String, String> dataReq,
									  HttpServletRequest req, HttpServletResponse resp) throws Exception {
		try {
			logger.debug("Primary Administrator Commands");
			String cmd = dataReq.get("command");
			if (cmd!=null) {
				if (cmd.equals("adminAccessAdministratorAccountsAdd")) {
					addAdministrator(dataReq, req, resp);

				} else if (cmd.equals("addApiKey")) {
					addApiKey(resp);

				} else if (cmd.equals("adminAccessAdministratorAccountsDelete")) {
					deleteAdministrator(dataReq, req, resp);
					
				} else if (cmd.equals("adminAccessAdministratorAccountsEdit")) {
					adminAccessAdministratorAccountsEdit(dataReq, req, resp);
					
				} else if (cmd.equals("hostTransferSource")) {
					throw new UxException("Removed Functionality");
					// hostTransferSource(dataReq, req, resp);
				
				} else if (cmd.equals("getNetworks")) {
					primaryAdminNetworks(dataReq, resp);

				} else if (cmd.equals("getAdministrators")) {
					administratorListRequest(req, resp);

				} else if (cmd.equals("setupRecovery0")) {
					throw new UxException("Removed Functionality");
					// setupRecoverySaltRequest(req, resp, user);

				} else if (cmd.equals("setupRecovery1")) {
					throw new UxException("Removed Functionality");
					// setupRecoveryProduceHtml(dataReq, req, resp, user);

				} else {
					throw new Exception("Unknown Primary Administrator Request");
					
				}
			} else {
				throw new ProgrammingException("Client Side Error - no 'command' attribute provided");
				
			}
		} catch (Exception e) {
			throw e; 
			
		}
	}

	private void primaryAdminNetworks(HashMap<String, String> dataReq, HttpServletResponse resp) throws Exception {
		try {
			initNode(resp);
			getNetworks(dataReq, resp);

		} catch (ItemNotFoundException e) {
			WebUtils.success(resp);

		} catch (Exception e) {
			throw e;

		}
	}

	private void addApiKey(HttpServletResponse resp) throws Exception {
		try {
			UUID keyId = UUID.randomUUID();
			String key = CryptoUtils.computeSha256HashAsHex(
					CryptoUtils.generateNonce(12));

			String storeKey = CryptoUtils.computeSha256HashAsHex(key);
			IApiKey api = new IApiKey();
			api.setType(IUser.I_USER_API_KEY);
			api.setKey(storeKey);
			api.setUuid(keyId.toString());
			CloudantClient client = CouchDbClient.instance();
			Database db = client.database(CouchDbHelper.getDbUsers(), true);
			CouchRepository<IApiKey> repo = new CouchRepository<>(db, IApiKey.class);
			repo.create(api);

			JsonObject o = new JsonObject();
			o.addProperty("keyId", keyId.toString());
			o.addProperty("apiKey", key);
			WebUtils.respond(resp, o);

		} catch (Exception e) {
			throw e;

		}
	}

	/*
	private void setupRecoverySaltRequest(HttpServletRequest req, HttpServletResponse resp, UserAuthData user) {
		JsonObject o = new JsonObject();
		o.addProperty("salt", user.getS());
		Utils.respond(resp, o);

	}

	private void setupRecoveryProduceHtml(HashMap<String, String> dataReq, HttpServletRequest req,
										  HttpServletResponse response, UserAuthData user) throws Exception {
		try {
			String u = dataReq.get("password");
			String v = UserAuthData.computeV(u, user);
			logger.info(u);
			logger.info(v);
			logger.info(user.getV());

			if (v.equals(user.getV())){
				PassStore accessK = new PassStore(u, false);
				String k = new String(accessK.decipher(Base64.decodeBase64(user.getW())), "UTF8");
				logger.info("Recovery k=" + k);
				String token = CryptoUtils.computeSha256HashAsHex(CryptoUtils.generateCode(12));
				logger.info("Recovery t + s =" + token + user.getS());
				String uR = CryptoUtils.computeSha256HashAsHex(token + user.getS());

				String version = CryptoUtils.generateCode(10);
				logger.info("TODO Remove t=" + token + " v=" + version + " uR=" + uR);
				byte[] qr = QrCode.computeQrCodeAsPng(token, 300);
				String qrB64 = Base64.encodeBase64String(qr);
				String html = RecoveryHtml.CONTENT.replace("--version--", version);
				html = html.replace("--qr-base64--", qrB64);
				html = html.replace("--date-time--", DateHelper.currentIsoUtcDateTime());

				PassStore p = new PassStore(uR, false);
				String r = new String(Base64.encodeBase64(p.encrypt(k.getBytes())), "UTF8");
				user.setR(r);
				user.setVersion(version);
				user.setC(CryptoUtils.computeSha256HashAsHex(r + version));

				user = users.updateUser(user);
				auth.addUser(req.getSession().getId(), user);
				response.getWriter().write(html);

			} else {
				throw new UxException("The Primary Administrator Password was incorrect");

			}
		} catch (Exception e) {
			throw e;

		}
	} //*/

	/*
	private void hostTransferSource(HashMap<String, String> dataReq, HttpServletRequest req, HttpServletResponse resp) throws Exception {
		throw new Exception("Code Not Implemented");		
		
	} //*/

	private void addAdministrator(HashMap<String, String> dataReq, HttpServletRequest req, HttpServletResponse resp) throws Exception {
		String username = dataReq.get("username");
		if (WhiteList.username(username)) {
			CloudantClient client = CouchDbClient.instance();
			Database db = client.database(CouchDbHelper.getDbUsers(), true);
			CouchRepository<IUser> repo = new CouchRepository<>(db, IUser.class);
			QueryBasic q = new QueryBasic();
			q.getSelector().put("username", username);
			try {
				repo.read(q);
				throw new UxException("A user with that name already exists");

			} catch (NoDocumentException e) {
				IUser user = new IUser();
				user.setRequiresPassChange(true);
				user.setType(IUser.I_USER_ADMIN);
				user.setUsername(username);
				String pwd = Base64.encodeBase64String(CryptoUtils.generateNonce(6));
				user.setV(CryptoUtils.computeSha256HashAsHex(
						CryptoUtils.computeSha256HashAsHex(pwd)));
				repo.create(user);

				JsonObject json = new JsonObject();
				json.addProperty("username", username);
				json.addProperty("password", pwd);
				WebUtils.respond(resp, json);

			}
		} else {
			throw new UxException(username + IS_NOT_VALID_USERNAME);
			
		}
	}
	
	private void adminAccessAdministratorAccountsEdit(HashMap<String, String> dataReq, HttpServletRequest req, HttpServletResponse resp) throws Exception {
		throw new Exception("Code Not Implemented");
		
	}

	private void deleteAdministrator(HashMap<String, String> dataReq, HttpServletRequest req, HttpServletResponse resp) throws Exception {
		try {
			try {
				JsonObject json = new JsonObject();
				String id = dataReq.get("selectedId");
				UUID apiKeyId = UUID.fromString(id);
				logger.info("API KEY DELETE REQUEST " + apiKeyId);
				CloudantClient client = CouchDbClient.instance();
				Database db = client.database(CouchDbHelper.getDbUsers(), true);
				CouchRepository<IApiKey> repo = new CouchRepository<>(db, IApiKey.class);
				QueryBasic q = new QueryBasic();
				q.getSelector().put(IApiKey.FIELD_API_KEY_UUID, apiKeyId.toString());
				try {
					IApiKey key = repo.read(q).get(0);
					repo.delete(key);
					json.addProperty("message", "API Key Deleted");
					json.addProperty("refresh", "administrators");
					WebUtils.respond(resp, json);

				} catch (NoDocumentException e) {
					throw new UxException("There was no key with ID " + apiKeyId);

				}
			} catch (IllegalArgumentException e) {
				deleteStandardAdministrator(dataReq, resp);

			}
		} catch (Exception e) {
			throw e; 
			
		}
	}

	private void deleteStandardAdministrator(HashMap<String, String> dataReq, HttpServletResponse resp) throws Exception {
		String username = dataReq.get("selectedValue");
		String id = dataReq.get("selectedId");
		try {
			CouchRepository<IUser> repo = openUserRepo();
			IUser user = repo.read(id);
			if (user.getType().equals(IUser.I_USER_PRIMARY_ADMIN)){
				throw new UxException("Cannot delete primary administrator");

			} else if (user.getType().equals(IUser.I_USER_ADMIN)){
				repo.delete(user);
				JsonObject json = new JsonObject();
				json.addProperty("message", "Administrator Deleted");
				json.addProperty("refresh", "administrators");
				WebUtils.respond(resp, json);

			} else {
				throw new UxException(ErrorMessages.SERVER_SIDE_PROGRAMMING_ERROR,
						"Tried to delete a non-administratator");

			}
		} catch (NoDocumentException e) {
			throw new UxException("The administrator account " + username + " does not exist" );

		}
	}

	private void administratorListRequest(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		logger.debug("Administrator List Request");
		try {
			initNode(resp);

		} catch (ItemNotFoundException e){
			logger.info("No sources here: " + e.getMessage());

		}
		ArrayList<String> arrayList = new ArrayList<>();
		arrayList.add(IUser.I_USER_ADMIN);
		arrayList.add(IUser.I_USER_PRIMARY_ADMIN);
		QueryOrGate q = new QueryOrGate("type", arrayList);

		List<IUser> admins = openUserRepo().read(q);
		List<IApiKey> keys;
		try {
			QueryBasic q0 = QueryBasic.selectType(IUser.I_USER_API_KEY);
			keys = openApiKeyRepo().read(q0);

		} catch (NoDocumentException e) {
			keys = new ArrayList<>();

		}
		JsonObject administrators = Administrators.create(admins, keys);
		WebUtils.respond(resp, administrators);
		
	}

	private CouchRepository<IApiKey> openApiKeyRepo() throws Exception {
		CloudantClient client = CouchDbClient.instance();
		Database db = client.database(CouchDbHelper.getDbUsers(), true);
		return new CouchRepository<>(db, IApiKey.class);
	}

	private CouchRepository<IUser> openUserRepo() throws Exception {
		CloudantClient client = CouchDbClient.instance();
		Database db = client.database(CouchDbHelper.getDbUsers(), true);
		return new CouchRepository<>(db, IUser.class);

	}

	/*
	 * 
	 * 
	 * 
	 * 		COMMON PROCESSING
	 * 
	 * 
	 * 
	 */	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		logger.error("Clearing Local Node Information");
		this.localNodeInfo.clear();

	}
}
