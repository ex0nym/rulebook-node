package io.exonym.actor.actions;

import com.sun.xml.ws.util.ByteArrayBuffer;
import eu.abc4trust.xml.*;
import eu.abc4trust.xml.CredentialInPolicy.IssuerAlternatives.IssuerParametersUID;
import io.exonym.abc.util.FileType;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.storage.*;
import io.exonym.lite.connect.UrlHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.ProgrammingException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.helpers.*;
import io.exonym.lite.pojo.*;
import io.exonym.lite.standard.*;
import io.exonym.lite.time.DateHelper;
import io.exonym.utils.RulebookVerifier;
import io.exonym.utils.node.ProgressReporter;
import io.exonym.utils.storage.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.rmi.UnexpectedException;
import java.util.*;

public class NodeManager {
	
	private static final Logger logger = LogManager.getLogger(NodeManager.class);
	private String leadName;
	private final RulebookNodeProperties props = RulebookNodeProperties.instance();
	private URI trustNetworkUid = null;
	private URI nodeUid = null;
	private TrustNetwork trustNetwork = null;
	private SFTPClient primarySftp = null;
	private String raiHash;
	private String ppHash;

	private String ppB64, ppSigB64;
	private String raiB64, raiSigB64;


	public NodeManager(String leadName) throws UxException {
		validateLeadName(leadName);
		this.leadName =leadName;

	}

	private void validateLeadName(String leadName) throws UxException {
		if (!WhiteList.username(leadName)){
			throw new UxException("The Lead name must be between 3 and 32 " +
					"characters with underscores instead of spaces " + leadName);

		}
	}

	public TrustNetwork setupLead(URL rulebookURL, PassStore store) throws Exception{
		try {
			RulebookVerifier verifier = new RulebookVerifier(rulebookURL);
			Rulebook rulebook = verifier.getRulebook();
			boolean isSybil = Rulebook.isSybil(rulebook.getRulebookId());
			if (isSybil){
				this.leadName = Rulebook.SYBIL_LEAD;

			}
			IdContainerJSON x = establishNewContainer(this.leadName, store);

			try {

				AsymStoreKey key = establishKey(store, x, Const.LEAD);

				// create a globally unique uid
				URI trustNetworkUid = generateTrustNetworkUid(rulebook);

				// create a credential specification
				CredentialSpecification cred = generateCredentialSpecification(verifier);

				// create a presentation policy
				PresentationPolicy policy = generatePolicy(trustNetworkUid, cred, x);

				// define trust network
//				URI primaryStaticUrl = getLeadUrlForThisNode(props.getPrimaryDomain(),
//						props.getPrimaryStaticDataFolder());
//				logger.debug("PRIMARY >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + primaryStaticUrl);

				NodeInformation nodeInfo = new NodeInformation();
				nodeInfo.setLeadUid(trustNetworkUid);
				nodeInfo.setRulebookNodeUrl(
						UIDHelper.ensureTrailingSlash(props.getRulebookNodeURL()));

				nodeInfo.setNodeUid(trustNetworkUid);
				URI staticUrl = computeStaticURL(nodeInfo.getRulebookNodeUrl(), true);

				nodeInfo.setStaticLeadUrl0(staticUrl);
				nodeInfo.setStaticNodeUrl0(staticUrl);
				nodeInfo.setNodeName(leadName);
				nodeInfo.setRegion(props.getIsoCountryCode());

				TrustNetwork network = new TrustNetwork();
				network.setNodeInformation(nodeInfo);
				network.setNodeInformationUid(Const.TRUST_NETWORK_UID);
				network.setLastUpdated(DateHelper.currentIsoUtcDateTime());

				// save all files
				x.saveLocalResource(cred);
				x.saveLocalResource(policy);

				String cs = IdContainerJSON.convertObjectToXml(cred);
				String p = IdContainerJSON.convertObjectToXml(policy);

				String tn = JaxbHelper.serializeToXml(network, TrustNetwork.class);
				logger.info(tn);

				byte[] csSign = NodeVerifier.stripStringToSign(cs).getBytes();
				byte[] pSign = NodeVerifier.stripStringToSign(p).getBytes();
				byte[] tnSign = NodeVerifier.stripStringToSign(tn).getBytes();

				// publish all public files
				byte[] cSpecBytes = cs.getBytes();
				byte[] cPolicy = p.getBytes();
				byte[] trust = tn.getBytes();

				// Sign Public Key
				KeyContainer pub = new KeyContainer();
				KeyContainerWrapper wrapper = new KeyContainerWrapper(pub);
				XKey xk = new XKey();
				xk.setKeyUid(KeyContainerWrapper.TN_ROOT_KEY);
				xk.setPublicKey(key.getPublicKey().getEncoded());
				xk.setSignature(signData(xk.getPublicKey(), key, null));
				wrapper.addKey(xk);

				// Sign all files
				HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();
				toSign.put(cred.getSpecificationUID(), new ByteArrayBuffer(csSign));
				toSign.put(policy.getPolicyUID(), new ByteArrayBuffer(pSign));
				toSign.put(network.getNodeInformationUid(), new ByteArrayBuffer(tnSign));

				updatePpSignatures(key, p, pSign);

				URI url0 = staticUrl;

				signatureUpdateXml(key, toSign, wrapper, url0);
				String xml = JaxbHelper.serializeToXml(wrapper.getKeyContainer(), KeyContainer.class);

				publishOnlyIfNew(url0, xml.getBytes(), Const.SIGNATURES_XML);
				publish(url0, cSpecBytes, IdContainerJSON.uidToXmlFileName(cred.getSpecificationUID()));
				publish(url0, cPolicy, IdContainerJSON.uidToXmlFileName(policy.getPolicyUID()));
				publish(url0, trust, IdContainerJSON.uidToXmlFileName(Const.TRUST_NETWORK_UID));

				try {
					Path rulebookPath = Path.of(staticUrl.getPath()).getParent();

					URI rulebookUrl = nodeInfo.getRulebookNodeUrl().resolve(rulebookPath.toUri());

					String json = JaxbHelper.gson.toJson(rulebook, Rulebook.class);
					writeLocal(rulebookUrl, json.getBytes(StandardCharsets.UTF_8),
							"rulebook.json");

				} catch (Exception e) {
					logger.warn("Failed to write model rulebook.json", e);

				}
				return network;

			} catch (Exception e){
				logger.info("Tidy up IdContainer");
				x.delete();
				throw e;

			}
		} catch (Exception e) {
			throw e;
			
		}
	}

	private void updatePpSignatures(AsymStoreKey key, String p, byte[] pSign) throws Exception {
		ppHash = CryptoUtils.computeSha256HashAsHex(p);
		ppB64 = Base64.encodeBase64String(p.getBytes(StandardCharsets.UTF_8));
		ppSigB64 = Base64.encodeBase64String(
				signData(pSign, key, null));

	}

	public static URI computeStaticURL(URI rulebookNodeUrl, boolean forLead){
		rulebookNodeUrl = rulebookNodeUrl.resolve("/static/");
		if (forLead){
			return rulebookNodeUrl.resolve(Const.LEAD + "/");
		} else {
			return rulebookNodeUrl.resolve(Const.MODERATOR + "/");
		}
	}



	public NodeInformation setupModeratorNode(URI leadUrl, String nodeName,
											  PassStore store, ProgressReporter progress) throws Exception{
		ArrayList<String> pending = progress.getPending();
		if (pending.size()!=7){
			throw new ProgrammingException("Expected 7 Updates");

		}

		NodeVerifier verifiedLead = new NodeVerifier(leadUrl.toURL());
		URI leadUid = verifiedLead.getTargetTrustNetwork().getNodeInformation().getNodeUid();
		if (!WhiteList.isLeadUid(leadUid)){
			throw new UxException("URL_IS_MODERATOR_USE_A_LEAD_URL");

		}
		Rulebook rulebook = verifiedLead.getRulebook();
		if (Rulebook.isSybil(rulebook.getRulebookId())){
			if (rulebook.getDescription().isProduction()){
				nodeName = Rulebook.SYBIL_MOD_MAIN;
			} else {
				nodeName = Rulebook.SYBIL_MOD_TEST;
			}
		}
		IdContainerJSON modIdContainer = establishNewContainer(nodeName, store);

		try {
			NodeInformation lead = verifiedLead
					.getTargetTrustNetwork().getNodeInformation();

			// OPENED NODE
			logger.debug("Sending update");
			progress.setComplete(pending.get(0));

			// Establish Identifiers
			CredentialSpecification cs = verifiedLead.getCredentialSpecification();
			String root = computeModeratorUID(lead.getLeadUid(), nodeName);
			String issuerUid = ":" + establishNodeName(null) + ":i";
			logger.info("Established IssuerUID=" + issuerUid);

			UIDHelper helper = new UIDHelper(root + issuerUid);
			helper.out();

			URI iUid = helper.getIssuerParameters();
			URI rapUid = helper.getRevocationAuthority();
			URI raiUid = helper.getRevocationInfoParams();
			URI insUid = helper.getInspectorParams();

//			URI primaryUrl = getModUrlForThisNode(props.getPrimaryDomain(),
//					props.getPrimaryStaticDataFolder());

			URI rulebookNodeUrl = UIDHelper.ensureTrailingSlash(props.getRulebookNodeURL());
			logger.info("NodeURL=" + rulebookNodeUrl);

			NodeInformation nodeInfo = new NodeInformation();
			nodeInfo.setNodeName(nodeName);
			nodeInfo.setRulebookNodeUrl(rulebookNodeUrl);
			nodeInfo.setNodeUid(URI.create(root));

			nodeInfo.setStaticNodeUrl0(computeStaticURL(rulebookNodeUrl, false));

			nodeInfo.setRegion(props.getIsoCountryCode());

			nodeInfo.getIssuerParameterUids().add(iUid);

			nodeInfo.setLeadUid(lead.getLeadUid());
			nodeInfo.setStaticLeadUrl0(leadUrl);

			NetworkParticipant leadParticipant = new NetworkParticipant();
			leadParticipant.setNodeUid(lead.getNodeUid());
			leadParticipant.setLastUpdateTime(DateHelper.currentIsoUtcDateTime());
			leadParticipant.setPublicKey(verifiedLead.getPublicKey());
			leadParticipant.setAvailableOnMostRecentRequest(true);
			leadParticipant.setStaticNodeUrl0(lead.getStaticLeadUrl0());

			logger.info("leadUid " + lead.getNodeUid());
			logger.info("sourceUrl " + lead.getStaticLeadUrl0());

			TrustNetwork network = new TrustNetwork();
			network.setNodeInformation(nodeInfo);
			network.setNodeInformationUid(Const.TRUST_NETWORK_UID);
			network.setLastUpdated(DateHelper.currentIsoUtcDateTime());
			network.getParticipants().add(leadParticipant);

			modIdContainer.saveLocalResource(cs);
			modIdContainer.saveLocalResource(
					verifiedLead.getPresentationPolicy());

			// ESTABLISHED TRUST NETWORK
			progress.setComplete(pending.get(1));

			// Sign Public Key
			AsymStoreKey key = establishKey(store, modIdContainer, Const.MODERATOR);

			KeyContainer pub = new KeyContainer();
			KeyContainerWrapper wrapper = new KeyContainerWrapper(pub);
			XKey xk = new XKey();
			xk.setKeyUid(KeyContainerWrapper.TN_ROOT_KEY);
			xk.setPublicKey(key.getPublicKey().getEncoded());
			xk.setSignature(signData(xk.getPublicKey(), key, null));
			wrapper.addKey(xk);

			// ESTABLISHED ROOT KEY
			progress.setComplete(pending.get(2));

			ExonymOwner owner = new ExonymOwner(modIdContainer);
			owner.openContainer(store);
			owner.setupContainerSecret(store.getEncrypt(), store.getDecipher());

			ExonymIssuer issuer = new ExonymIssuer(modIdContainer);
			issuer.initSystemParameters();
			issuer.setupAsRevocationAuthority(iUid, store.getEncrypt());
			// ESTABLISHED REVOCATION AUTHORITY KEY
			progress.setComplete(pending.get(3));

			issuer.setupAsCredentialIssuer(cs.getSpecificationUID(), iUid, rapUid, store.getEncrypt()); //*/
			// ESTABLISHED CREDENTIAL ISSUER KEY
			progress.setComplete(pending.get(4));

			ExonymInspector ins = new ExonymInspector(modIdContainer);
			ins.generateInspectorMaterials(insUid, null, store);
			// ESTABLISHED INSPECTOR KEY
			progress.setComplete(pending.get(5));

			RevocationAuthorityParameters rap = modIdContainer.openResource(rapUid);
			RevocationInformation ri = modIdContainer.openResource(raiUid);

			ExonymIssuer.removeRevocationHistory(ri);
//			modIdContainer.saveLocalResource(ri, true);

			IssuerParameters i = modIdContainer.openResource(iUid);
			InspectorPublicKey insKey = modIdContainer.openResource(insUid);

			String rapString = IdContainerJSON.convertObjectToXml(rap);
			String raiString = IdContainerJSON.convertObjectToXml(ri);
			this.raiHash = CryptoUtils.computeSha256HashAsHex(raiString);
			String iString = IdContainerJSON.convertObjectToXml(i);
			String insString = IdContainerJSON.convertObjectToXml(insKey);
			String niString = JaxbHelper.serializeToXml(network, TrustNetwork.class);

			String rapSign = NodeVerifier.stripStringToSign(rapString);
			String raiSign = NodeVerifier.stripStringToSign(raiString);
			String iSign = NodeVerifier.stripStringToSign(iString);
			String insSign = NodeVerifier.stripStringToSign(insString);
			String niSign = NodeVerifier.stripStringToSign(niString);

			byte[] rapBytes = rapString.getBytes();
			byte[] raiBytes = raiString.getBytes();
			byte[] iBytes = iString.getBytes();
			byte[] insBytes = insString.getBytes();
			byte[] niBytes = niString.getBytes();

			HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();
			toSign.put(rapUid, new ByteArrayBuffer(rapSign.getBytes()));
			toSign.put(raiUid, new ByteArrayBuffer(raiSign.getBytes()));
			toSign.put(iUid, new ByteArrayBuffer(iSign.getBytes()));
			toSign.put(insUid, new ByteArrayBuffer(insSign.getBytes()));
			toSign.put(network.getNodeInformationUid(), new ByteArrayBuffer(niSign.getBytes()));

			URI primaryUrl = nodeInfo.getStaticNodeUrl0();

			signatureUpdateXml(key, toSign, wrapper, primaryUrl);

			String xml = JaxbHelper.serializeToXml(wrapper.getKeyContainer(), KeyContainer.class);

			publishOnlyIfNew(primaryUrl, xml.getBytes(), "signatures.xml");
			publish(primaryUrl, rapBytes, IdContainerJSON.uidToXmlFileName(rapUid));
			publish(primaryUrl, raiBytes, IdContainerJSON.uidToXmlFileName(raiUid));
			publish(primaryUrl, insBytes, IdContainerJSON.uidToXmlFileName(insUid));
			publish(primaryUrl, iBytes, IdContainerJSON.uidToXmlFileName(iUid));
			publish(primaryUrl, niBytes, IdContainerJSON.uidToXmlFileName(network.getNodeInformationUid()));
			// PUBLISHED DATA
			progress.setComplete(pending.get(6));

			try {
				Path rulebookPath = Path.of(primaryUrl.getPath()).getParent();

				URI rulebookUrl = nodeInfo.getRulebookNodeUrl().resolve(rulebookPath.toUri());

				String xnd = JaxbHelper.gson.toJson(rulebook, Rulebook.class);
				writeLocal(rulebookUrl, xnd.getBytes(StandardCharsets.UTF_8),
						"rulebook.json");

//				String filename = "/rulebook.json";
//				String path = props.getPrimaryStaticDataFolder() + "/" + lead.getNodeName();
//				String xnd = JaxbHelper.serializeToJson(verifiedLead.getRulebook(), Rulebook.class);
//				try {
//					primarySftp.overwrite(path + filename, xnd, false);
//
//				} catch (Exception e) {
//					primarySftp.overwrite(path + filename, xnd, true);
//
//				}
			} catch (Exception e) {
				logger.warn("Failed to write model Rulebook", e);

			}
			return nodeInfo;

		} catch (Exception e) {
			logger.warn("Tidying up Container after failure", e);
			modIdContainer.delete();
			throw e;

		}
	}

	private String computeModeratorUID(URI leadUid, String nodeName) {
		String[] parts = leadUid.toString().split(":");
		StringBuilder b = new StringBuilder();
		int i = 0;
		for (String part : parts){
			b.append(part);
			if (i==3){
				b.append(":");
				b.append(nodeName);

			} if (i < 4){
				b.append(":");
			}
			i++;

		}
		return b.toString();

	}

	@Deprecated
	// Do not delete until the system has a large number of users.
	public void freshIssuer(PassStore store) throws Exception {
		
		TrustNetworkWrapper tnw = new TrustNetworkWrapper(openMyTrustNetwork(false));

		NodeInformation info = tnw.getNodeInformation();
		String username = info.getNodeName().split("-")[0];
		IdContainerJSON x = openContainer(leadName, store);
		String root = info.getNodeUid() + ":" + establishNodeName(null);
		
		KeyContainer kcPrivate= x.openResource("keys.xml", store.getDecipher());
		KeyContainerWrapper kcwPrivate = new KeyContainerWrapper(kcPrivate);
		AsymStoreKey key = openKey(kcwPrivate.getKey(KeyContainerWrapper.TN_ROOT_KEY), store);
		
		URI cUid = URI.create(info.getLeadUid() + ":c");
		URI iUid = URI.create(root + ":i");
		URI raUid = URI.create(root + ":ra");
		URI raiUid = URI.create(root + ":rai");
		
		ExonymIssuer issuer = new ExonymIssuer(x);
		issuer.initSystemParameters();
		issuer.setupAsRevocationAuthority(iUid, store.getEncrypt());
		issuer.setupAsCredentialIssuer(cUid, iUid, raUid, store.getEncrypt()); //*/

		RevocationAuthorityParameters rap = x.openResource(raUid);
		RevocationInformation ri = x.openResource(raiUid);
		ExonymIssuer.removeRevocationHistory(ri);

		IssuerParameters i = x.openResource(iUid);
		
		tnw.getNodeInformation().getIssuerParameterUids().add(iUid);
		TrustNetwork network = tnw.finalizeTrustNetwork();

		String rapString = IdContainerJSON.convertObjectToXml(rap);
		String raiString = IdContainerJSON.convertObjectToXml(ri);
		String iString = IdContainerJSON.convertObjectToXml(i);
		String niString = JaxbHelper.serializeToXml(tnw.finalizeTrustNetwork(), TrustNetwork.class);

		String rapSign = NodeVerifier.stripStringToSign(rapString);
		String raiSign = NodeVerifier.stripStringToSign(raiString);
		String iSign = NodeVerifier.stripStringToSign(iString);
		String niSign = NodeVerifier.stripStringToSign(niString);

		byte[] rapBytes = rapString.getBytes();
		byte[] raiBytes = raiString.getBytes();
		byte[] iBytes = iString.getBytes();
		byte[] niBytes = niString.getBytes();
		
		HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();
		toSign.put(raUid, new ByteArrayBuffer(rapSign.getBytes()));
		toSign.put(raiUid, new ByteArrayBuffer(raiSign.getBytes()));
		toSign.put(iUid, new ByteArrayBuffer(iSign.getBytes()));
		toSign.put(network.getNodeInformationUid(), new ByteArrayBuffer(niSign.getBytes()));

		URI nodeUrl = info.getStaticNodeUrl0();
		KeyContainerWrapper kcPublic = openSignaturesContainer(nodeUrl);
		signatureUpdateXml(key, toSign, kcPublic, nodeUrl);
		String xml = JaxbHelper.serializeToXml(kcPublic.getKeyContainer(), KeyContainer.class);

		publish(nodeUrl, iBytes, IdContainerJSON.uidToXmlFileName(iUid));
		publish(nodeUrl, rapBytes, IdContainerJSON.uidToXmlFileName(raUid));
		publish(nodeUrl, raiBytes, IdContainerJSON.uidToXmlFileName(raiUid));
		publish(nodeUrl, niBytes, IdContainerJSON.uidToXmlFileName(network.getNodeInformationUid()));
		publish(nodeUrl, xml.getBytes(), "signatures.xml");
		
	}

	public void publishTrustNetwork(TrustNetwork tn, KeyContainer publicKeyContainer, PassStore store) throws Exception {
		IdContainerJSON x = openContainer(leadName, store);
		KeyContainer kcPrivate= x.openResource("keys.xml", store.getDecipher());
		KeyContainerWrapper kcwPrivate = new KeyContainerWrapper(kcPrivate);
		AsymStoreKey key = openKey(kcwPrivate.getKey(KeyContainerWrapper.TN_ROOT_KEY), store);

		String niString = JaxbHelper.serializeToXml(tn, TrustNetwork.class);
		byte[] niBytes = niString.getBytes();
		String niSign = NodeVerifier.stripStringToSign(niString);
		URI url = getModUrlForThisNode(props.getPrimaryDomain(), props.getPrimaryStaticDataFolder());

		HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();
		toSign.put(tn.getNodeInformationUid(), new ByteArrayBuffer(niSign.getBytes()));


		signatureUpdateXml(key, toSign, new KeyContainerWrapper(publicKeyContainer), url);
		String xml = JaxbHelper.serializeToXml(publicKeyContainer, KeyContainer.class);

		publish(url, niBytes, IdContainerJSON.uidToXmlFileName(tn.getNodeInformationUid()));
		publish(url, xml.getBytes(), "signatures.xml");

	}

	protected IdContainerJSON establishNewContainer(String name, PassStore store) throws Exception {
		try {
			if (WhiteList.username(name)){
				return new IdContainerJSON(name, true);
				
			} else {
				throw new UxException("A valid name is between 3 and 32 characters with underscores replacing spaces (" +  name + ")");	
				
			}
		} catch (Exception e) {
			throw new UxException("There is either a Network Source or a Node with this name on this hosting (" +  name  + ")" , e);
			
		}
	}
	
	protected IdContainerJSON openContainer(String name, PassStore store) throws Exception {
		try {
			return new IdContainerJSON(name);
				
		} catch (Exception e) {
			throw new UxException("The container '" + name + "' does not exist", e);
			
		}
	}	
	
	private CredentialSpecification generateCredentialSpecification(RulebookVerifier verifier) {
		if (verifier.isSybil()){
			return BuildCredentialSpecification.buildSybilCredentialSpecification(verifier);

		} else {
			URI cSpecUid = verifier.getRulebook().computeCredentialSpecId();
			BuildCredentialSpecification bcs = new BuildCredentialSpecification(cSpecUid, true);
			return bcs.getCredentialSpecification();

		}
	}
	
	protected PresentationPolicy generatePolicy(URI networkUid, CredentialSpecification cred, IdContainerJSON x) throws Exception {
		URI ppUid = URI.create(networkUid.toString() + ":pp");

		PkiExternalResourceContainer external = PkiExternalResourceContainer.getInstance();

		BuildPresentationPolicy bpp = new BuildPresentationPolicy(ppUid, external);
		String rootAlias = Const.BINDING_ALIAS;
		bpp.addPseudonym(rootAlias, false, rootAlias);

		return bpp.getPolicy();

	}

	public URI generateTrustNetworkUid(Rulebook rulebook) throws UxException {
		String rulebookId = UIDHelper.computeRulebookHashUid(rulebook.getRulebookId());

		String uid = Namespace.URN_PREFIX_COLON
				+ rulebook.getDescription().getName().toLowerCase() + ":"
				+ leadName + ":"
				+ rulebookId;

		return URI.create(uid);
		
	}

	private AsymStoreKey establishKey(PassStore store, IdContainerJSON x, String type) throws Exception {
		AsymStoreKey key = new AsymStoreKey();

		XKey xk = new XKey();
		xk.setKeyUid(KeyContainerWrapper.TN_ROOT_KEY);
		xk.setPublicKey(key.getPublicKey().getEncoded());
		xk.setPrivateKey(key.getEncryptedEncodedForm(store.getEncrypt()));

		KeyContainer container = new KeyContainer();
		KeyContainerWrapper wrapper = new KeyContainerWrapper(container);
		wrapper.addKey(xk);
		if (x!=null){
			x.saveLocalResource(wrapper.getKeyContainer());

		}
		return key;
		
	}

	/**
	 * 
	 * @param nodeUrl
	 * @param store
	 * @return the UID of the Node Added
	 * @throws Exception
	 */
	public URI addModeratorToLead(URI nodeUrl, PassStore store, AbstractNetworkMap networkMap) throws Exception{
		NodeVerifier nodeToAdd = new NodeVerifier(nodeUrl.toURL());
		boolean isTestNet = !nodeToAdd.getRulebook().getDescription().isProduction();

		IssuerParametersUID sybilUid = defineIssuerParams(
				nodeToAdd, networkMap, isTestNet); // null if we're adding a sybil node

		TrustNetworkWrapper addingModeratorTnw = new TrustNetworkWrapper(
				nodeToAdd.getTargetTrustNetwork());

		URI addingIssuerUid = addingModeratorTnw.getMostRecentIssuerParameters();
		logger.info("------------ >> Issuer UID to Add=" + addingIssuerUid + " sybil=" + sybilUid + " (should be null if Sybil)");

		UIDHelper helper = new UIDHelper(addingIssuerUid);
		helper.out();

		IssuerParameters addingIParams = nodeToAdd.getIssuerParameters(
				IdContainerJSON.uidToXmlFileName(addingIssuerUid));
		
		InspectorPublicKey addingIns = nodeToAdd.getInspectorPublicKey();

		TrustNetworkWrapper myNetworkWrapper = new TrustNetworkWrapper(openMyTrustNetwork(true));

		NodeInformation addingNi = addingModeratorTnw.getNodeInformation();
		LinkedList<URI> currentIssuerParams = myNetworkWrapper.getNodeInformation().getIssuerParameterUids();
		currentIssuerParams.remove(addingIssuerUid);

		myNetworkWrapper.addParticipant(addingNi.getNodeUid(),
				nodeUrl, addingNi.getRulebookNodeUrl(),
				nodeToAdd.getPublicKey(), addingNi.getRegion(),
				addingModeratorTnw.getMostRecentIssuerParameters());

		TrustNetwork myNetwork = myNetworkWrapper.finalizeTrustNetwork();

		// From source
		IdContainerJSON x = openContainer(this.leadName, store);
		CredentialSpecification cred = x.openResource(helper.getCredentialSpecFileName());
		PresentationPolicy myPresentationPolicy = x.openResource(helper.getPresentationPolicyFileName());
		if (myPresentationPolicy==null) {
			myPresentationPolicy = new PresentationPolicy();

		}

		PresentationPolicyManager myPresentationPolicyManager =
				new PresentationPolicyManager(myPresentationPolicy, cred, sybilUid);

		myPresentationPolicyManager.addIssuer(addingIParams, addingIns);
		myPresentationPolicy = myPresentationPolicyManager.build();

		x.saveLocalResource(myPresentationPolicy, true);
		
		MyTrustNetworkAndKeys mtn = new MyTrustNetworkAndKeys(true);
		URI url = mtn.getTrustNetwork()
				.getNodeInformation().getStaticNodeUrl0();
		logger.info("Attempting to publish to URL=" + url);

		KeyContainerWrapper kcw = openSignaturesContainer(
				mtn.getTrustNetwork()
						.getNodeInformation().getStaticNodeUrl0());
		
		KeyContainer secret = x.openResource("keys.xml");
		KeyContainerWrapper secretWrapper = new KeyContainerWrapper(secret);
		AsymStoreKey key = openKey(secretWrapper.getKey(
				KeyContainerWrapper.TN_ROOT_KEY), store);

		String ppa0String = IdContainerJSON.convertObjectToXml(myPresentationPolicy);
		this.ppHash = CryptoUtils.computeSha256HashAsHex(ppa0String);
		String niString = JaxbHelper.serializeToXml(myNetwork, TrustNetwork.class);

		String ppa0Sign = NodeVerifier.stripStringToSign(ppa0String);
		String niSign = NodeVerifier.stripStringToSign(niString);

		logger.debug(ppa0String);
		logger.debug("");
		logger.debug(niString);

		// Sign all files
		byte[] ppa0Bytes = ppa0String.getBytes();
		byte[] niBytes = niString.getBytes();

		updatePpSignatures(key, ppa0String, ppa0Sign.getBytes());

		HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();
		toSign.put(myPresentationPolicy.getPolicyUID(), new ByteArrayBuffer(ppa0Sign.getBytes()));
		toSign.put(myNetwork.getNodeInformationUid(), new ByteArrayBuffer(niSign.getBytes()));

		signatureUpdateXml(key, toSign, kcw, url);

		String xml = JaxbHelper.serializeToXml(kcw.getKeyContainer(), KeyContainer.class);

		publish(url, xml.getBytes(), "signatures.xml");
		publish(url, ppa0Bytes, IdContainerJSON.uidToXmlFileName(myPresentationPolicy.getPolicyUID()));
		publish(url, niBytes, IdContainerJSON.uidToXmlFileName(myNetwork.getNodeInformationUid()));
		return addingNi.getNodeUid();
		// after this the UI runs; `cmd: getNetworks` `cmd: fullNetworkDataRequest`
		
	}

	private IssuerParametersUID defineIssuerParams(NodeVerifier nodeToAdd, AbstractNetworkMap networkMap, boolean testnet) throws Exception {
		Rulebook rulebook = nodeToAdd.getRulebook();
		boolean isSybil = Rulebook.isSybil(rulebook.getRulebookId());
		if (!isSybil){
			if (rulebook.getDescription().isProduction()){
				return sybilIssuerParameters(networkMap.nmiForSybilMainNet());

			} else {
				return sybilIssuerParameters(networkMap.nmiForSybilModTest());

			}
		} else {
			return null;

		}
	}

	private IssuerParametersUID sybilIssuerParameters(NetworkMapItemModerator sybilAdvocate) throws Exception {
		IssuerParametersUID result = new IssuerParametersUID();
		UIDHelper helper = new UIDHelper(sybilAdvocate.getLastIssuerUID());
		result.setRevocationInformationUID(helper.getRevocationInfoParams());
		result.setValue(helper.getIssuerParameters());
		return result;

	}

	public static boolean isLead(NodeInformation info) {
		if (info==null) {
			logger.warn("Null info at isSource");
			return false; 
			
		}
		return info.getNodeUid().equals(info.getLeadUid());
		
	}

	public URI removeModeratorFromLead(URI nodeUrl, PassStore store) throws Exception {
		NodeVerifier v = new NodeVerifier(nodeUrl.toURL());
		try {
			TrustNetworkWrapper tnw = new TrustNetworkWrapper(v.getTargetTrustNetwork());
			removeModeratorFromLead(tnw.getMostRecentIssuerParameters().toString(), store);
			return v.getTargetTrustNetwork().getNodeInformation().getNodeUid();

		} catch (NoSuchElementException e) {
			return v.getTargetTrustNetwork().getNodeInformation().getNodeUid();

		} catch (Exception e) {
			throw new UxException("The node may be deliberately corrupted.  "
					+ "Remove the Node manually by selecting them from the list.", e);

		}
	}

	public void removeModeratorFromLead(String moderatorUID, PassStore store) throws Exception {
		URI iUid = URI.create(moderatorUID);
		IdContainerJSON x = openContainer(leadName, store);
		KeyContainer kcSecret = x.openResource("keys.xml");
		KeyContainerWrapper kcwSecret = new KeyContainerWrapper(kcSecret);
		XKey xkey = kcwSecret.getKey(KeyContainerWrapper.TN_ROOT_KEY);
		AsymStoreKey key = openKey(xkey, store);

		UIDHelper helper = new UIDHelper(moderatorUID);
		helper.out();

		PresentationPolicy ppa0 = x.openResource(helper.getPresentationPolicy());
		
		PresentationPolicyManager ppm = new PresentationPolicyManager(ppa0,
				x.openResource(helper.getCredentialSpecFileName()),
				null);
		ppm.removeIssuer(iUid);
		
		ppa0 = ppm.build();
		x.saveLocalResource(ppa0, true);

		TrustNetworkWrapper networkWrapper = new TrustNetworkWrapper(openMyTrustNetwork(true));

		networkWrapper.removeParticipant(URI.create(moderatorUID));
		TrustNetwork network = networkWrapper.finalizeTrustNetwork();

		String ppString = IdContainerJSON.convertObjectToXml(ppa0);


		String niString = JaxbHelper.serializeToXml(network, TrustNetwork.class);

		logger.info(ppString);
		logger.info(niString);

		String ppSign = NodeVerifier.stripStringToSign(ppString);
		String niSign = NodeVerifier.stripStringToSign(niString);

		updatePpSignatures(key, ppString, ppSign.getBytes(StandardCharsets.UTF_8));
		
		// Sign all files
		byte[] ppBytes = ppString.getBytes();
		byte[] niBytes = niString.getBytes();


		HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();
		toSign.put(ppa0.getPolicyUID(), new ByteArrayBuffer(ppSign.getBytes()));
		toSign.put(network.getNodeInformationUid(), new ByteArrayBuffer(niSign.getBytes()));

//		URI url = getLeadUrlForThisNode(props.getPrimaryDomain(),
//				props.getPrimaryStaticDataFolder());

		URI url = networkWrapper.getNodeInformation().getStaticNodeUrl0();
		
		KeyContainerWrapper kcw = openSignaturesContainer(url);

		signatureUpdateXml(key, toSign, kcw, url);
		String xml = JaxbHelper.serializeToXml(kcw.getKeyContainer(), KeyContainer.class);

		publish(url, xml.getBytes(), "signatures.xml");
		publish(url, ppBytes, IdContainerJSON.uidToXmlFileName(ppa0.getPolicyUID()));
		publish(url, niBytes, IdContainerJSON.uidToXmlFileName(network.getNodeInformationUid()));
		
	}

	public KeyContainerWrapper openSignaturesContainer(URI url) throws Exception {
		Path path = Path.of(url.getPath());
		logger.info("URL Provided=" + url + " path=" + path + " file=" + path.getFileName());
		if (path.getFileName().equals(Const.SIGNATURES_XML)){
			// do nothing

		} else if (path.toString().endsWith(Const.LEAD) ||
				path.toString().endsWith(Const.MODERATOR)){
			url = url.resolve(Const.SIGNATURES_XML);
			logger.info("Resulting URL=" + url);

		} else {
			throw new UxException("There is no signature file associated with this URL: " + url);

		}
		String sigs = new String(UrlHelper.read(url.toURL()));
		KeyContainer kcPublic = JaxbHelper.xmlToClass(sigs, KeyContainer.class);
		KeyContainerWrapper kcw = new KeyContainerWrapper(kcPublic);
		return kcw;
		
	}
	
	public TrustNetwork openMyTrustNetwork(boolean amILead) throws Exception {
		MyTrustNetworkAndKeys mtn = new MyTrustNetworkAndKeys(amILead);
		return mtn.getTrustNetwork();

	}

	public void addScope(String scope,  PassStore store) throws Exception {
		ArrayList<String> t = new ArrayList<String>();
		t.add(scope);
		addScope(t, store);
		
	}

	public void addScope(ArrayList<String> scope, PassStore store) throws Exception {
		IdContainerJSON x = openContainer(leadName, store);
		KeyContainer kcSecret = x.openResource("keys.xml");
		KeyContainerWrapper kcwSecret = new KeyContainerWrapper(kcSecret);
		XKey xkey = kcwSecret.getKey(KeyContainerWrapper.TN_ROOT_KEY);
		AsymStoreKey key = openKey(xkey, store);

		URI networkUid = discoverNetworkUid();
		URI ppaUid = URI.create(networkUid + ":pp");
		URI cUid = UIDHelper.credentialSpecFromLeadUID(networkUid);

		PresentationPolicy ppa0 = x.openResource(ppaUid);
		PresentationPolicyManager ppm = new PresentationPolicyManager(ppa0,
				x.openResource(cUid),
				null); //*/
		ppm.addNym(scope);
		ppa0 = ppm.build();
		
		URI url = getLeadUrlForThisNode(props.getPrimaryDomain(),
				props.getPrimaryStaticDataFolder());

		KeyContainerWrapper kcw = openSignaturesContainer(url);
		x.saveLocalResource(ppa0, true);

		String ppString = IdContainerJSON.convertObjectToXml(ppa0);

		String ppSign = NodeVerifier.stripStringToSign(ppString);
		
		byte[] ppBytes = ppString.getBytes();
		
		HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();
		toSign.put(ppa0.getPolicyUID(), new ByteArrayBuffer(ppSign.getBytes()));
		
		signatureUpdateXml(key, toSign, kcw, url);
		String xml = JaxbHelper.serializeToXml(kcw.getKeyContainer(), KeyContainer.class);

		publish(url, xml.getBytes(), "signatures.xml");
		publish(url, ppBytes, IdContainerJSON.uidToXmlFileName(ppa0.getPolicyUID()));
		
	}

	public void removeScope(String scope, PassStore store) throws Exception {
		IdContainerJSON x = openContainer(leadName, store);
		KeyContainer kcSecret = x.openResource("keys.xml");
		KeyContainerWrapper kcwSecret = new KeyContainerWrapper(kcSecret);
		XKey xkey = kcwSecret.getKey(KeyContainerWrapper.TN_ROOT_KEY);
		AsymStoreKey key = openKey(xkey, store);
		
		URI networkUid = discoverNetworkUid();
		URI ppaUid = URI.create(networkUid + ":pp");
		URI cUid = UIDHelper.credentialSpecFromLeadUID(networkUid);

		PresentationPolicy ppa0 = x.openResource(ppaUid);
		PresentationPolicyManager ppm = new PresentationPolicyManager(ppa0,
				x.openResource(cUid),
				null); //*/
		ppm.removeNym(scope);
		ppa0 = ppm.build();
		x.saveLocalResource(ppa0, true);
		
		URI url = getLeadUrlForThisNode(props.getPrimaryDomain(),
				props.getPrimaryStaticDataFolder());
		KeyContainerWrapper kcw = openSignaturesContainer(url);

		String ppString = IdContainerJSON.convertObjectToXml(ppa0);

		String ppSign = NodeVerifier.stripStringToSign(ppString);
		
		byte[] ppaBytes = ppString.getBytes();
		
		HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();
		toSign.put(ppa0.getPolicyUID(), new ByteArrayBuffer(ppSign.getBytes()));

		signatureUpdateXml(key, toSign, kcw, url);
		String xml = JaxbHelper.serializeToXml(kcw.getKeyContainer(), KeyContainer.class);

		publish(url, xml.getBytes(), "signatures.xml");
		publish(url, ppaBytes, IdContainerJSON.uidToXmlFileName(ppa0.getPolicyUID()));

	}

	public static AsymStoreKey openKey(XKey x, PassStore store) throws Exception {
		if (x.getPrivateKey()==null){ 
			throw new HubException("You must open the key from an XContainer, not from signatures.xml");
			
		}
		AsymStoreKey key = AsymStoreKey.blank();
		key.assembleKey(x.getPublicKey(), x.getPrivateKey(), store.getDecipher());
		return key; 
		
	}

	public static void signatureUpdateXml(AsymStoreKey key, HashMap<URI, ByteArrayBuffer> toSign,
										  KeyContainerWrapper kcw, URI nodeUrl) throws Exception{
		if (kcw.getKey(KeyContainerWrapper.TN_ROOT_KEY).getPrivateKey()!=null) {
			throw new SecurityException(
					"Cannot write this KeyContainer Publicly as it contains a Private Key");
			
		}
		for (URI uid : toSign.keySet()){
			byte[] ba = toSign.get(uid).getRawData();
			byte[] sig = signData(ba, key, null);
			XKey x = new XKey();
			x.setKeyUid(uid);
			x.setSignature(sig);
			kcw.updateKey(x);
			
			
		}
		String check = nodeUrl.toString();
		ArrayList<URI> tmp = new ArrayList<>(kcw.getKeyRingUids());
		tmp.remove(KeyContainerWrapper.TN_ROOT_KEY);
		tmp.remove(KeyContainerWrapper.SIG_CHECKSUM);
		Collections.sort(tmp);
		for (URI uid : tmp) {
			check += uid;	
			
		}
		check = check.replaceAll("/", "");
		logger.debug("CHECK(PRODUCE)\n\t\t" + check);
		byte[] sig = signData(check.getBytes(), key, null);
		XKey x = new XKey();
		x.setKeyUid(URI.create("urn:sig-checksum"));
		x.setSignature(sig);
		kcw.updateKey(x);
		
	}

	public static byte[] signData(byte[] data, AsymStoreKey key, OutputStream out) throws Exception{
		byte[] sig = key.sign(data);
		if (out != null) {
			out.write(Base64.encodeBase64(sig));
			
		}
		return sig;
		
	}

	public void publish(URI url0, byte[] xml, String xmlFileName) throws Exception{
		if (UrlHelper.isXml(xml)){
			if (FileType.isXmlDocument(xmlFileName)){
				primaryPut(url0, xml, xmlFileName);

			} else {
				throw new Exception("The file name was not an xml file name " + xmlFileName);
				
			}
		} else {
			throw new SecurityException("The value passed was not XML");
			
		}
	}

	protected void publishOnlyIfNew(URI url0, byte[] xml, String xmlFileName) throws Exception{
		if (UrlHelper.isXml(xml)){
			if (FileType.isXmlDocument(xmlFileName)){
				primaryPutNew(url0, xml, xmlFileName);

			} else {
				throw new Exception("The file name was not an xml file name " + xmlFileName);
				
			}
		} else {
			throw new SecurityException("The value passed was not XML ");
			
		}
	}

	private String establishNodeName(String nodeName) throws UxException {
		String rnd = UUID.randomUUID().toString();
		String[] parts = rnd.split("-");
		if (nodeName==null || nodeName == ""){
			return parts[0];

		}
		if (WhiteList.username(nodeName)){
			return nodeName + "-" +  parts[1];

		} else {
			throw new UxException("Node name is invalid " + nodeName);

		}
	}


	private void primaryPutNew(URI url, byte[] xml, String xmlFileName) throws Exception {
		writeNewLocal(url, xml, xmlFileName);
//		try {
//			if (primarySftp ==null || !primarySftp.isActive()) {
//				primarySftp = new SFTPClient(props.getPrimarySftpCredentials());
//				primarySftp.connect();
//
//			}
//			logger.debug("url >>>>>>>>>>>>>>>>>>>>>>>>>" + url);
//			String path = url.toString().replace(
//					root(props.getPrimaryDomain()), "/");
//			logger.debug("PATH0 >>>>>>>>>>>>>>>>>>>>>>>>>" + path);
//			String xml0 = new String(xml);
//			try {
//				primarySftp.put(path + "//" + xmlFileName, xml0, false);
//
//			} catch (Exception e) {
//				primarySftp.put(path + "//" + xmlFileName, xml0, true);
//
//			}
//		} catch (ProgrammingException e) {
//			throw new UxException("Files have already been published to this space - Please delete them and try again", e);
//
//		}
	}

	private void primaryPut(URI url, byte[] xml, String xmlFileName) throws Exception {
		writeLocal(url, xml, xmlFileName);
//		if (primarySftp ==null || !primarySftp.isActive()) {
//			primarySftp = new SFTPClient(props.getPrimarySftpCredentials());
//			primarySftp.connect();
//
//		}
//		String path = url.toString().replace(
//				root(props.getPrimaryDomain()), "/");
//		String xml0 = new String(xml);
//		try {
//			primarySftp.overwrite(path + "//" + xmlFileName, xml0, false);
//
//		} catch (Exception e) {
//			primarySftp.overwrite(path + "//" + xmlFileName, xml0, true);
//
//		}
	}


	private void writeNewLocal(URI url, byte[] xml, String xmlFileName) throws Exception {
		try {
			Path writeLocation = computeWriteLocation(url);
			Files.createDirectories(writeLocation);
			Files.write(writeLocation.resolve(xmlFileName), xml, StandardOpenOption.CREATE_NEW);

		} catch (IOException e) {
			throw new UxException("Files have already been published to this space - Please delete them and try again", e);

		}
	}

	private void writeLocal(URI url, byte[] xml, String xmlFileName) throws Exception {
		logger.info("Writing to Location=" + url);
		Path writeLocation = computeWriteLocation(url);
		Files.createDirectories(writeLocation);
		Files.write(writeLocation.resolve(xmlFileName), xml,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);

	}


	private Path computeWriteLocation(URI url) {
		String ofUrl = url.getPath();
		Path writeLocation = Path.of(Const.PATH_OF_HTML, ofUrl);

		logger.info("Path of URL: " + url.getPath() + " write: " + writeLocation.toAbsolutePath());
		return writeLocation;

	}

	private String root(String rootProp){
		return (rootProp.endsWith("/") ? rootProp : rootProp + "/");

	}

	public NodeInformation getNodeInformation() {
		if (this.trustNetwork!=null) {
			return this.trustNetwork.getNodeInformation();

		} else {
			return null;

		}
	}

	@Deprecated
	// Possibly
	public URI getLeadUrlForThisNode(String root, String folder) throws UxException, MalformedURLException {
		String r = constructBaseNodeUrl(root, folder) + Const.LEAD;
		return URI.create(r);

	}

	@Deprecated
	// Possibly
	public URI getModUrlForThisNode(String root, String folder) throws UxException, MalformedURLException {
		String baseUrl = constructBaseNodeUrl(root, folder) + Const.MODERATOR;
		logger.info("Built URL=" + baseUrl);
		return URI.create(baseUrl);

	}

	@Deprecated
	// Possibly
	private String constructBaseNodeUrl(String root, String networkFolder) throws UxException {
		if (!WhiteList.url(root)){
			logger.info("constructBaseNodeUrl() received a root of " + root);
			throw new UxException("Node Setup Error - " + root + " is an invalid URL", root);
			
		} else {
			if (!root.endsWith("/")) {
				root += "/";
				
			}
		}
		if (networkFolder!=null && networkFolder.length() > 0) {
			if (WhiteList.isMinLettersAllowsNumbersAndHyphens(networkFolder, 1)) {
				networkFolder += "/";
				
			} else if (!networkFolder.endsWith("/")){
				throw new UxException("Node Setup Error (Primary or Secondary folder error)" +
						": See exonym.io/help for more information");

			}
		} else {
			networkFolder = "";
			
		}
		logger.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> URL: " + root + networkFolder + this.leadName);
		return root + networkFolder + "/" + this.leadName;
		
	}


	public URI discoverNetworkUid() throws Exception {
		if (trustNetworkUid !=null) {
			return trustNetworkUid;
			
		} else {
			this.openMyTrustNetwork(false);
			return trustNetworkUid;
			
		}
	}

	public URI discoverNodeUid() throws Exception {
		if (nodeUid!=null) {
			return nodeUid;
			
		} else {
			TrustNetwork tn = openMyTrustNetwork(false);
			this.nodeUid = tn.getNodeInformation().getNodeUid();
			return nodeUid;
			
		}
	}

	
	public static String computeLeadNameUncheckedSignature(URI url) throws Exception {
		if (url!=null) {
			byte[] bytes = UrlHelper.read(url.resolve(
					IdContainer.uidToXmlFileName(Const.TRUST_NETWORK_UID))
					.toURL());
			TrustNetwork trustNetwork = JaxbHelper.xmlToClass(bytes, TrustNetwork.class);
			return trustNetwork.getNodeInformation().getNodeName();

		} else {
			throw new Exception("Null URL");
			
		}
	}

	public void setTrustNetworkUid(URI trustNetworkUid) {
		this.trustNetworkUid = trustNetworkUid;
		
	}


	public void setRaiHash(String raiHash) {
		this.raiHash = raiHash;
	}

	public String getPpHash() {
		return ppHash;
	}

	public String getPpB64() {
		return ppB64;
	}

	public String getPpSigB64() {
		return ppSigB64;
	}

	public String getRaiB64() {
		return raiB64;
	}

	public String getRaiSigB64() {
		return raiSigB64;
	}

	public String getRaiHash() {
		return raiHash;
	}
}