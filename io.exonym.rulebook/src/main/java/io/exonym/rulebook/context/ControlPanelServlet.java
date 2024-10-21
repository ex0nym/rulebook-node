package io.exonym.rulebook.context;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ibm.zurich.idmx.exception.SerializationException;
import com.ibm.zurich.idmx.jaxb.JaxbHelperClass;
import eu.abc4trust.xml.PresentationToken;
import io.exonym.actor.actions.MembershipManager;
import io.exonym.actor.actions.MyTrustNetworks;
import io.exonym.actor.actions.PkiExternalResourceContainer;
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
import io.exonym.rulebook.schema.CacheNodeContainer;
import io.exonym.utils.node.ProgressReporter;
import io.exonym.utils.storage.NetworkParticipant;
import io.exonym.utils.storage.NodeInformation;
import io.exonym.utils.storage.TrustNetwork;
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

	private MyTrustNetworks myTrustNetworks;

	/**
	 * 
	 */
	private IAuthenticator authenticator;
//	private LocalNodeInformation localNodeInfo = null;
//	private ArrayList<NodeData> localNodes = new ArrayList<>();
	private ConcurrentHashMap<String, PresentationToken> sidToPresentationTokenMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, ProgressReporter> progressReporters = new ConcurrentHashMap<>();

	@Override
	public void init() throws ServletException {
		this.authenticator = IAuthenticator.getInstance();
		this.myTrustNetworks = new MyTrustNetworks();

	}

	private void refreshTrustNetworks(){
		this.myTrustNetworks = new MyTrustNetworks();

	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			authenticator.getPassStore(req);
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
		if (authenticator.isAdministrator(sid)) {
			adminRequests(dataReq, req, resp);

		} else if (authenticator.isPrimaryAdministrator(sid)) {
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
					fullNetworkRequest(resp); // see fullNetworkDataRequest below.

				} else if (cmd.equals("sourceNewNetworkCreate")) {
					leadCreateNewTrustNetwork(dataReq, req, resp);
					refreshNetworkMap();

				} else if (cmd.equals("sourceNodeManagementAdd")) {
					leadModAdd(dataReq, resp);
					refreshNetworkMap();

				} else if (cmd.equals("sourceNodeManagementRemove")) {
					leadModRemoveByUrl(dataReq, resp);
					refreshNetworkMap();

				} else if (cmd.equals("nodeSourceAttachmentAttach")) {
					nodeLeadAttachmentAttach(dataReq, req, resp);

				} else if (cmd.equals("nodeMemberRevoke")) {
					nodeMemberRevoke(dataReq, req, resp);

				} else if (cmd.equals("fullNetworkDataRequest")) {
					fullNetworkRequest(resp);

				// Pretty sure this doesn't get called and it's a duplicate of sourceNodeManagementRemove
				} else if (cmd.equals("sourceNodeManagementAuthorizedNodesDelete")) {
					leadModRemove(dataReq, req, resp);
					refreshNetworkMap();

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
			throw new UxException("Please refresh your page.");

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
		// TODO This is broken by c30 changes; the tokens are now compressed.
		// it also needs to broadcast an update to the revocation data.

		try {
			if (myTrustNetworks.isModerator()){
				String rawB64Token = dataReq.get("token");
				PresentationToken token = this.sidToPresentationTokenMap
						.get(req.getSession().getId());

				if (token==null){
					if (rawB64Token!=null){
						byte[] rawToken = Base64.decodeBase64(rawB64Token);
						token = (PresentationToken) JaxbHelperClass.deserialize(
								new String(rawToken, "UTF8"), true).getValue();

					} else {
						throw new UxException("Please provide a valid token");

					}
				}
				TrustNetwork modTn = myTrustNetworks.getModerator().getTrustNetwork();
				MembershipManager mm = new MembershipManagerWeb(
						modTn.getNodeInformation().getNodeName());

				PassStore store = new PassStore(RulebookNodeProperties.instance().getNodeRoot(), false);
				mm.revokeMember(token, store);
				WebUtils.success(resp);

			} else {
				throw new UxException(ErrorMessages.INSUFFICIENT_PRIVILEGES, "This node is not a moderator");

			}
		} catch (UxException e){
			throw e;

		} catch (Exception e){
			throw new UxException("The Token was Invalid", e);

		}
	}

	private void nodeLeadAttachmentAttach(HashMap<String, String> dataReq,
										  HttpServletRequest req, HttpServletResponse resp) throws Exception{
		try {
			String name = dataReq.get("publicName");
			URI sourceUrl = URI.create(dataReq.get("sourceUrl"));
			
			if (name!=null) {
				if (WhiteList.isMinLettersAllowsNumbersAndHyphens(name, 3)) {
					
					if (WhiteList.url(sourceUrl.toString())) {
						PassStore p = new PassStore(RulebookNodeProperties.instance().getNodeRoot(), false);
						
						if (p!=null) {
							String networkName = NodeManagerWeb.computeLeadNameUncheckedSignature(sourceUrl);
							NodeManagerWeb n = new NodeManagerWeb(networkName);
							ProgressReporter reporter = new ProgressReporter(new String[] {
									"Defining Trust Network",
									"Computing Root Key",
									"Computing Revocation Authority Key - Please be patient...",
									"Computing Credential Issuer Key - Please be patient...",
									"Computing Inspector Key - Last key before publishing...",
									"Publishing static data",
									"Almost done!"

							});
							String sid = req.getSession().getId();
							this.progressReporters.put(sid, reporter);
							// String fullPath = WebUtils.getFullPath(req);

							new Thread(() -> {
								try {
									logger.info("Starting Lead Attachment");
									NodeInformation info = n.setupModeratorNode(
											sourceUrl, name, p, reporter);
									refreshTrustNetworks();
									progressReporters.remove(sid).wrap(info.getStaticNodeUrl0().toString());

								} catch (Exception e) {
									logger.error("Error", e);
									progressReporters.get(sid).setException(e);

								}
							}).start();

							JsonObject r = new JsonObject();
							r.addProperty("update", "Defining Rulebook Node");
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


	private void leadCreateNewTrustNetwork(HashMap<String, String> dataReq,
										   HttpServletRequest req, HttpServletResponse resp) throws Exception{

		String rulebookUrl = dataReq.get("rulebookUrl");
		try {
			String orgName = dataReq.get("networkName");

			if (WhiteList.isMinLettersAllowsNumbersAndHyphens(orgName, 3)) {
				PassStore store = new PassStore(RulebookNodeProperties.instance()
						.getNodeRoot(), false);
				orgName = orgName.toLowerCase();

				NodeManagerWeb nodeManager = new NodeManagerWeb(orgName);
				TrustNetwork t = nodeManager.setupLead(new URL(rulebookUrl), store); //, WebUtils.getFullPath(req)
				NodeInformation info = t.getNodeInformation();
				JsonObject r = new JsonObject();
				r.addProperty("networkName", info.getNodeName());
				refreshTrustNetworks();
				if (myTrustNetworks.isLeader()){
					NetworkMapWeb map = new NetworkMapWeb();
					map.spawn();

					PkiExternalResourceContainer.getInstance()
							.setNetworkMapAndCache(map,
									CacheNodeContainer.getInstance());

				}
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
	
	private void leadModAdd(HashMap<String, String> dataReq, HttpServletResponse resp) throws Exception{
		String networkName = dataReq.get("networkName");
		try {
			String nodeUrl = dataReq.get("nodeUrl");

			PassStore passStore = new PassStore(
					RulebookNodeProperties.instance().getNodeRoot(), false);
			passStore.setUsername(networkName);
			
			if (WhiteList.url(nodeUrl)) {
				NodeManagerWeb n = new NodeManagerWeb(networkName);
				NetworkMapWeb networkMap = new NetworkMapWeb();
				URI nurl = URI.create(nodeUrl);
				n.addModeratorToLead(nurl, passStore, networkMap);
				broadcastLeadUpdate(n.getPpB64(), n.getPpSigB64());
				refreshTrustNetworks();
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

	private void broadcastLeadUpdate(String ppB64, String ppSigB64) throws Exception {
		if (myTrustNetworks.isLeader()){
			ExoNotify notify = new ExoNotify();
			notify.setT(DateHelper.currentIsoUtcDateTime());

			URI leadUid = myTrustNetworks.getLead()
					.getTrustNetwork().getNodeInformation().getNodeUid();

			notify.setNodeUid(leadUid);
			notify.setType(ExoNotify.TYPE_LEAD);
			notify.setPpB64(ppB64);
			notify.setPpSigB64(ppSigB64);
			NotificationPublisher.getInstance()
					.getPipe().put(notify);

		}
	}

	/*
	 * Remove by URL
	 */
	private void leadModRemoveByUrl(HashMap<String, String> dataReq, HttpServletResponse resp) throws Exception{
		try {
			String networkName = dataReq.get("networkName");
			String nodeUrl = dataReq.get("nodeUrl");
			PassStore p = new PassStore(RulebookNodeProperties.instance().getNodeRoot(), false);
			
			if (WhiteList.url(nodeUrl)) {
				NodeManagerWeb n = new NodeManagerWeb(networkName);
				n.removeModeratorFromLead(URI.create(nodeUrl), p);
				broadcastLeadUpdate(n.getPpB64(), n.getPpSigB64());

				refreshTrustNetworks();
				
				WebUtils.success(resp);
			
			} else {
				throw new UxException(nodeUrl + " is not a valid URL");
				
			}
		} catch (Exception e) {
			throw e;
			
		}
	}

	private void leadModRemove(HashMap<String, String> dataReq,
							   HttpServletRequest req, HttpServletResponse resp) throws Exception {
		try {
			String nodeUid = dataReq.get("selectedId");
			String networkName = dataReq.get("selectedNetwork");
			RulebookNodeProperties props = RulebookNodeProperties.instance();
			PassStore p = new PassStore(props.getNodeRoot(), false);
			NodeManagerWeb n = new NodeManagerWeb(networkName);
			n.removeModeratorFromLead(nodeUid, p);
			broadcastLeadUpdate(n.getPpB64(), n.getPpSigB64());
			refreshTrustNetworks();
			WebUtils.success(resp);

		} catch (Exception e) {
			throw e;

		}
	}	//*/

	private void fullNetworkRequest(HttpServletResponse resp) throws Exception {

		JsonObject result = new JsonObject();

		JsonArray networkList = new JsonArray();
		JsonObject networkData = new JsonObject();
		result.add("networkList", networkList);
		result.add("networkData", networkData);
		result.add("members", new JsonArray());


		if (myTrustNetworks.isLeader()){
			TrustNetwork tn = myTrustNetworks.getLead().getTrustNetwork();
			NodeInformation lead = tn.getNodeInformation();
			String networkName = lead.getNodeName();
			JsonObject networkItem = new JsonObject();
			networkItem.addProperty("networkName", networkName);
			networkItem.addProperty("sourceUrl", lead.getStaticNodeUrl0().toString());
			networkData.add(lead.getNodeName(), networkItem);
			result.addProperty("selectedNetwork", networkName);
			networkList.add(networkName);
			JsonArray authorizedNodes = new JsonArray();
			ArrayList<NetworkParticipant> ps = tn.getParticipants();

			for (NetworkParticipant p : ps) {
				String uid = p.getNodeUid().toString();
				logger.debug("Issuer UID= " + uid);
				JsonObject authNode0 = new JsonObject();
				authNode0.addProperty("nodeUid", uid);
				authNode0.addProperty("internalName",
						UIDHelper.computeModNameFromModUid(p.getNodeUid()));
				authorizedNodes.add(authNode0);

			}
			networkItem.add("authorizedNodes", authorizedNodes);

		} else {
			result.addProperty("noSources", true);

		}
		if (myTrustNetworks.isModerator()){
			TrustNetwork tn = myTrustNetworks.getModerator().getTrustNetwork();
			NodeInformation mod = tn.getNodeInformation();
			result.addProperty("attached", mod.getStaticNodeUrl0().toString());

		}
		WebUtils.respond(resp, result);

	}

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
				CouchRepository<IApiKey> repo = CouchDbHelper.repoApiKey();
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
			CouchRepository<IUser> repo = CouchDbHelper.repoUsersAndAdmins();
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
		ArrayList<String> arrayList = new ArrayList<>();
		arrayList.add(IUser.I_USER_ADMIN);
		arrayList.add(IUser.I_USER_PRIMARY_ADMIN);
		QueryOrGate q = new QueryOrGate("type", arrayList);

		List<IUser> admins = CouchDbHelper.repoUsersAndAdmins().read(q);
		List<IApiKey> keys;
		try {
			QueryBasic q0 = QueryBasic.selectType(IUser.I_USER_API_KEY);
			keys = CouchDbHelper.repoApiKey().read(q0);

		} catch (NoDocumentException e) {
			keys = new ArrayList<>();

		}
		JsonObject administrators = Administrators.create(admins, keys);
		WebUtils.respond(resp, administrators);
		
	}


}
