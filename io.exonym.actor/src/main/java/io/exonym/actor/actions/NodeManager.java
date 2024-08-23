package io.exonym.actor.actions;

import com.sun.xml.ws.util.ByteArrayBuffer;
import eu.abc4trust.xml.*;
import eu.abc4trust.xml.CredentialInPolicy.IssuerAlternatives.IssuerParametersUID;
import io.exonym.abc.util.FileType;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.storage.*;
import io.exonym.lite.connect.UrlHelper;
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
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.UnexpectedException;
import java.util.*;

public class NodeManager {
	
	private static final Logger logger = LogManager.getLogger(NodeManager.class);
	private final String leadName;
	private final RulebookNodeProperties props = RulebookNodeProperties.instance();
	private URI trustNetworkUid = null;
	private URI nodeUid = null;
	private TrustNetwork trustNetwork = null;
	private SFTPClient primarySftp = null;
	private String raiHash;
	private String ppHash;

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
			XContainerJSON x = establishNewContainer(this.leadName, store);
			try {
				AsymStoreKey key = establishKey(store, x, Const.LEAD);

				// create a globally unique uid
				RulebookVerifier verifier = new RulebookVerifier(rulebookURL);
				Rulebook rulebook = verifier.getRulebook();
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
				nodeInfo.setBroadcastAddress(URI.create(props.getBroadcastUrl()));

				nodeInfo.setNodeUid(trustNetworkUid);
				URI staticUrl = nodeInfo.getRulebookNodeUrl()
						.resolve("/static/")
						.resolve(Const.LEAD + "/");

				nodeInfo.setStaticLeadUrl0(staticUrl);
				nodeInfo.setStaticNodeUrl0(staticUrl);
				nodeInfo.setNodeName(leadName);
				nodeInfo.setRegion(props.getIsoCountryCode());

				TrustNetwork network = new TrustNetwork();
				network.setNodeInformation(nodeInfo);
				network.setNodeInformationUid(Const.TRUST_NETWORK_URN);
				network.setLastUpdated(DateHelper.currentIsoUtcDateTime());

				// save all files
				x.saveLocalResource(cred);
				x.saveLocalResource(policy);

				String cs = XContainerJSON.convertObjectToXml(cred);
				String p = XContainerJSON.convertObjectToXml(policy);
				this.ppHash = CryptoUtils.computeSha256HashAsHex(p);
				String tn = JaxbHelper.serializeToXml(network, TrustNetwork.class);

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

				URI url0 = staticUrl;

				signatureUpdateXml(key, toSign, wrapper, url0);
				String xml = JaxbHelper.serializeToXml(wrapper.getKeyContainer(), KeyContainer.class);

				publishOnlyIfNew(url0, xml.getBytes(), Const.SIGNATURES_XML);
				publish(url0, cSpecBytes, XContainerJSON.uidToXmlFileName(cred.getSpecificationUID()));
				publish(url0, cPolicy, XContainerJSON.uidToXmlFileName(policy.getPolicyUID()));
				publish(url0, trust, XContainerJSON.uidToXmlFileName(Const.TRUST_NETWORK_URN));

				try {
					Path rulebookPath = Path.of(staticUrl.getPath()).getParent();

					URI rulebookUrl = nodeInfo.getRulebookNodeUrl().resolve(rulebookPath.toUri());

					String xnd = JaxbHelper.serializeToJson(rulebook, Rulebook.class);
					writeLocal(rulebookUrl, xnd.getBytes(StandardCharsets.UTF_8),
							"rulebook.json");

//					try {
//						primarySftp.overwrite(path + filename, xnd, false);
//
//					} catch (Exception e) {
//						primarySftp.overwrite(path + filename, xnd, true);
//
//					}
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



	public NodeInformation setupAdvocateNode(URI sourceUrl,  String nodeName,
											 PassStore store, ProgressReporter progress) throws Exception{
		ArrayList<String> pending = progress.getPending();
		if (pending.size()!=7){
			throw new ProgrammingException("Expected 7 Updates");

		}
		XContainerJSON hostXContainer = establishNewContainer(nodeName, store);
		try {
			NodeVerifier verifiedSource = NodeVerifier.openNode(sourceUrl, true, false);
			NodeInformation source = verifiedSource.getTargetTrustNetwork().getNodeInformation();
			// OPENED NODE
			logger.debug("Sending update");
			progress.setComplete(pending.get(0));

			// Establish Identifiers
			CredentialSpecification cs = verifiedSource.getCredentialSpecification();
			String root = computeAdvocateUID(source.getLeadUid(), nodeName);
			String issuerUid = ":" + establishNodeName(null);
			URI iUid = URI.create(root + issuerUid + ":i");
			URI rapUid = URI.create(root + issuerUid + ":ra");
			URI raiUid = URI.create(root + issuerUid + ":rai");
			URI insUid = URI.create(root + ":ins");

			URI primaryUrl = getAdvocateUrlForThisNode(props.getPrimaryDomain(),
					props.getPrimaryStaticDataFolder());

			NodeInformation nodeInfo = new NodeInformation();
			nodeInfo.setNodeName(nodeName);
			nodeInfo.setRulebookNodeUrl(URI.create(props.getRulebookNodeURL()));
			nodeInfo.setBroadcastAddress(URI.create(props.getBroadcastUrl()));
			nodeInfo.setNodeUid(URI.create(root));
			nodeInfo.setStaticNodeUrl0(primaryUrl);
			nodeInfo.setRegion(props.getIsoCountryCode());

			nodeInfo.getIssuerParameterUids().add(iUid);

			nodeInfo.setLeadUid(source.getLeadUid());
			nodeInfo.setStaticLeadUrl0(sourceUrl);

			NetworkParticipant sourceParticipant = new NetworkParticipant();
			sourceParticipant.setNodeUid(source.getNodeUid());
			sourceParticipant.setLastUpdateTime(DateHelper.currentIsoUtcDateTime());
			sourceParticipant.setPublicKey(verifiedSource.getPublicKey());
			sourceParticipant.setBroadcastAddress(nodeInfo.getBroadcastAddress());
			sourceParticipant.setAvailableOnMostRecentRequest(true);
			sourceParticipant.setStaticNodeUrl0(source.getStaticLeadUrl0());
			logger.info("sourceUid " + source.getNodeUid());
			logger.info("sourceUrl " + source.getStaticLeadUrl0());

			TrustNetwork network = new TrustNetwork();
			network.setNodeInformation(nodeInfo);
			network.setNodeInformationUid(URI.create(nodeInfo.getNodeUid() + ":ni"));
			network.setLastUpdated(DateHelper.currentIsoUtcDateTime());
			network.getParticipants().add(sourceParticipant);

			hostXContainer.saveLocalResource(cs);
			hostXContainer.saveLocalResource(verifiedSource.getPresentationPolicy());

			// ESTABLISHED TRUST NETWORK
			progress.setComplete(pending.get(1));

			// Sign Public Key
			AsymStoreKey key = establishKey(store, hostXContainer, Const.MODERATOR);

			KeyContainer pub = new KeyContainer();
			KeyContainerWrapper wrapper = new KeyContainerWrapper(pub);
			XKey xk = new XKey();
			xk.setKeyUid(KeyContainerWrapper.TN_ROOT_KEY);
			xk.setPublicKey(key.getPublicKey().getEncoded());
			xk.setSignature(signData(xk.getPublicKey(), key, null));
			wrapper.addKey(xk);

			// ESTABLISHED ROOT KEY
			progress.setComplete(pending.get(2));

			ExonymOwner owner = new ExonymOwner(hostXContainer);
			owner.openContainer(store);
			owner.setupContainerSecret(store.getEncrypt(), store.getDecipher());

			ExonymIssuer issuer = new ExonymIssuer(hostXContainer);
			issuer.initSystemParameters();
			issuer.setupAsRevocationAuthority(iUid, store.getEncrypt());
			// ESTABLISHED REVOCATION AUTHORITY KEY
			progress.setComplete(pending.get(3));

			issuer.setupAsCredentialIssuer(cs.getSpecificationUID(), iUid, rapUid, store.getEncrypt()); //*/
			// ESTABLISHED CREDENTIAL ISSUER KEY
			progress.setComplete(pending.get(4));

			ExonymInspector ins = new ExonymInspector(hostXContainer);
			ins.generateInspectorMaterials(insUid, null, store);
			// ESTABLISHED INSPECTOR KEY
			progress.setComplete(pending.get(5));

			RevocationAuthorityParameters rap = hostXContainer.openResource(rapUid);
			RevocationInformation ri = hostXContainer.openResource(raiUid);
			IssuerParameters i = hostXContainer.openResource(iUid);
			InspectorPublicKey insKey = hostXContainer.openResource(insUid);

			String rapString = XContainerJSON.convertObjectToXml(rap);
			String raiString = XContainerJSON.convertObjectToXml(ri);
			this.raiHash = CryptoUtils.computeSha256HashAsHex(raiString);
			String iString = XContainerJSON.convertObjectToXml(i);
			String insString = XContainerJSON.convertObjectToXml(insKey);
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

			signatureUpdateXml(key, toSign, wrapper, primaryUrl);

			String xml = JaxbHelper.serializeToXml(wrapper.getKeyContainer(), KeyContainer.class);

			publishOnlyIfNew(primaryUrl, xml.getBytes(), "signatures.xml");
			publish(primaryUrl, rapBytes, XContainerJSON.uidToXmlFileName(rapUid));
			publish(primaryUrl, raiBytes, XContainerJSON.uidToXmlFileName(raiUid));
			publish(primaryUrl, insBytes, XContainerJSON.uidToXmlFileName(insUid));
			publish(primaryUrl, iBytes, XContainerJSON.uidToXmlFileName(iUid));
			publish(primaryUrl, niBytes, XContainerJSON.uidToXmlFileName(network.getNodeInformationUid()));
			// PUBLISHED DATA
			progress.setComplete(pending.get(6));

			try {
				String filename = "/rulebook.json";
				String path = props.getPrimaryStaticDataFolder() + "/" + source.getNodeName();
				String xnd = JaxbHelper.serializeToJson(verifiedSource.getRulebook(), Rulebook.class);
				try {
					primarySftp.overwrite(path + filename, xnd, false);

				} catch (Exception e) {
					primarySftp.overwrite(path + filename, xnd, true);

				}
			} catch (Exception e) {
				logger.warn("Failed to write model Rulebook", e);

			}
			return nodeInfo;

		} catch (Exception e) {
			logger.warn("Tidying up Container after failure", e);
			hostXContainer.delete();
			throw e;

		}
	}

	private String computeAdvocateUID(URI sourceUid, String nodeName) {
		String[] parts = sourceUid.toString().split(":");
		StringBuilder b = new StringBuilder();
		int i = 0;
		for (String part : parts){
			b.append(part);
			if (i==2){
				b.append(":");
				b.append(nodeName);

			} if (i < 3){
				b.append(":");
			}
			i++;

		}
		return b.toString();

	}

	@Deprecated
	// Do not delete until the system has a large number of users.
	public void freshIssuer(PassStore store) throws Exception {
		
		TrustNetworkWrapper tnw = openMyTrustNetwork(false);

		NodeInformation info = tnw.getNodeInformation();
		String username = info.getNodeName().split("-")[0];
		XContainerJSON x = openContainer(leadName, store);
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
		IssuerParameters i = x.openResource(iUid);
		
		tnw.getNodeInformation().getIssuerParameterUids().add(iUid);
		TrustNetwork network = tnw.finalizeTrustNetwork();

		String rapString = XContainerJSON.convertObjectToXml(rap);
		String raiString = XContainerJSON.convertObjectToXml(ri);
		String iString = XContainerJSON.convertObjectToXml(i);
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

		publish(nodeUrl, iBytes, XContainerJSON.uidToXmlFileName(iUid));
		publish(nodeUrl, rapBytes, XContainerJSON.uidToXmlFileName(raUid));
		publish(nodeUrl, raiBytes, XContainerJSON.uidToXmlFileName(raiUid));
		publish(nodeUrl, niBytes, XContainerJSON.uidToXmlFileName(network.getNodeInformationUid()));
		publish(nodeUrl, xml.getBytes(), "signatures.xml");
		
	}

	public void publishTrustNetwork(TrustNetwork tn, KeyContainer publicKeyContainer, PassStore store) throws Exception {
		XContainerJSON x = openContainer(leadName, store);
		KeyContainer kcPrivate= x.openResource("keys.xml", store.getDecipher());
		KeyContainerWrapper kcwPrivate = new KeyContainerWrapper(kcPrivate);
		AsymStoreKey key = openKey(kcwPrivate.getKey(KeyContainerWrapper.TN_ROOT_KEY), store);

		String niString = JaxbHelper.serializeToXml(tn, TrustNetwork.class);
		byte[] niBytes = niString.getBytes();
		String niSign = NodeVerifier.stripStringToSign(niString);
		URI url = getAdvocateUrlForThisNode(props.getPrimaryDomain(), props.getPrimaryStaticDataFolder());

		HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();
		toSign.put(tn.getNodeInformationUid(), new ByteArrayBuffer(niSign.getBytes()));


		signatureUpdateXml(key, toSign, new KeyContainerWrapper(publicKeyContainer), url);
		String xml = JaxbHelper.serializeToXml(publicKeyContainer, KeyContainer.class);

		publish(url, niBytes, XContainerJSON.uidToXmlFileName(tn.getNodeInformationUid()));
		publish(url, xml.getBytes(), "signatures.xml");

	}

	protected XContainerJSON establishNewContainer(String name, PassStore store) throws Exception {
		try {
			if (WhiteList.username(name)){
				return new XContainerJSON(name, true);
				
			} else {
				throw new UxException("A valid name is between 3 and 32 characters with underscores replacing spaces (" +  name + ")");	
				
			}
		} catch (Exception e) {
			throw new UxException("There is either a Network Source or a Node with this name on this hosting (" +  name  + ")" , e);
			
		}
	}
	
	protected XContainerJSON openContainer(String name, PassStore store) throws Exception {
		try {
			return new XContainerJSON(name);
				
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
	
	protected PresentationPolicy generatePolicy(URI networkUid, CredentialSpecification cred, XContainerJSON x) throws Exception {
		// URI ppaUid = URI.create(networkUid.toString() + ":pp");
		URI ppUid = URI.create(networkUid.toString() + ":pp");
		// URI cUid = URI.create(networkUid.toString() + ":c");
		
		PkiExternalResourceContainer external = PkiExternalResourceContainer.getInstance();

		BuildPresentationPolicy bpp = new BuildPresentationPolicy(ppUid, external);
		String rootAlias = "urn:io:exonym";
		bpp.addPseudonym(rootAlias, false, rootAlias);

		return bpp.getPolicy();

	}

	private URI generateTrustNetworkUid(Rulebook rulebook) {
		String rulebookId = rulebook.getRulebookId().split(":")[2];

		String uid = Namespace.URN_PREFIX_COLON
				+ rulebook.getDescription().getName().toLowerCase() + ":"
				+ leadName + ":"
				+ rulebookId;

		return URI.create(uid);
		
	}

	private AsymStoreKey establishKey(PassStore store, XContainerJSON x, String type) throws Exception {
		AsymStoreKey key = new AsymStoreKey();

		XKey xk = new XKey();
		xk.setKeyUid(KeyContainerWrapper.TN_ROOT_KEY);
		xk.setType(type);
		xk.setPublicKey(key.getPublicKey().getEncoded());
		xk.setPrivateKey(key.getEncryptedEncodedForm(store.getEncrypt()));

		KeyContainer container = new KeyContainer();
		KeyContainerWrapper wrapper = new KeyContainerWrapper(container);
		wrapper.addKey(xk);
		if (x!=null){
			x.saveLocalResource(wrapper.getKeyContainer());

		}
		saveKey(xk);
		return key;
		
	}

	protected void saveKey(XKey xk) {
		logger.debug("saveKey must be overridden to perform an operation.");

	}

	/**
	 * 
	 * @param nodeUrl
	 * @param store
	 * @return the UID of the Node Added
	 * @throws Exception
	 */
	public URI addModeratorToLead(URI nodeUrl, PassStore store, AbstractNetworkMap networkMap, boolean testnet) throws Exception{
		NodeVerifier nodeToAdd = NodeVerifier.openNode(nodeUrl, false, true);
		IssuerParametersUID sybilUid = defineIssuerParams(nodeToAdd, networkMap, testnet); // null if we're adding a sybil node
		TrustNetworkWrapper addingNetworkWrapper = new TrustNetworkWrapper(nodeToAdd.getTargetTrustNetwork());
		URI addingIssuerUid = addingNetworkWrapper.getMostRecentIssuerParameters();
		UIDHelper helper = new UIDHelper(addingIssuerUid);
		IssuerParameters addingIParams = nodeToAdd.getIssuerParameters(XContainerJSON.uidToXmlFileName(addingIssuerUid));
		
		InspectorPublicKey addingIns = nodeToAdd.getInspectorPublicKey();

		TrustNetworkWrapper myNetworkWrapper = openMyTrustNetwork(true);

		NodeInformation addingNi = addingNetworkWrapper.getNodeInformation();
		LinkedList<URI> currentIssuerParams = myNetworkWrapper.getNodeInformation().getIssuerParameterUids();
		currentIssuerParams.remove(addingIssuerUid);

		myNetworkWrapper.addParticipant(addingNi.getNodeUid(),
				nodeUrl, addingNi.getRulebookNodeUrl(), addingNi.getBroadcastAddress(),
				nodeToAdd.getPublicKey(), addingNi.getRegion(),
				addingNetworkWrapper.getMostRecentIssuerParameters());

		TrustNetwork myNetwork = myNetworkWrapper.finalizeTrustNetwork();

		// From source
		XContainerJSON x = openContainer(this.leadName, store);
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
		
		URI url = getLeadUrlForThisNode(props.getPrimaryDomain(), props.getPrimaryStaticDataFolder());
		
		KeyContainerWrapper kcw = openSignaturesContainer(url);
		
		KeyContainer secret = x.openResource("keys.xml");
		KeyContainerWrapper secretWrapper = new KeyContainerWrapper(secret);
		AsymStoreKey key = openKey(secretWrapper.getKey(KeyContainerWrapper.TN_ROOT_KEY), store);

		String ppa0String = XContainerJSON.convertObjectToXml(myPresentationPolicy);
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
		
		HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();
		toSign.put(myPresentationPolicy.getPolicyUID(), new ByteArrayBuffer(ppa0Sign.getBytes()));
		toSign.put(myNetwork.getNodeInformationUid(), new ByteArrayBuffer(niSign.getBytes()));
		
		signatureUpdateXml(key, toSign, kcw, url);
		
		String xml = JaxbHelper.serializeToXml(kcw.getKeyContainer(), KeyContainer.class);

		publish(url, xml.getBytes(), "signatures.xml");
		publish(url, ppa0Bytes, XContainerJSON.uidToXmlFileName(myPresentationPolicy.getPolicyUID()));
		publish(url, niBytes, XContainerJSON.uidToXmlFileName(myNetwork.getNodeInformationUid()));
		return addingNi.getNodeUid();
		
	}

	private IssuerParametersUID defineIssuerParams(NodeVerifier nodeToAdd, AbstractNetworkMap networkMap, boolean testnet) throws Exception {
		boolean isSybil = Rulebook.isSybil(nodeToAdd.getRulebook().getRulebookId());
		if (!isSybil){
			if (testnet){
				return sybilIssuerParameters(networkMap.nmiForSybilTestNet());

			} else {
				return sybilIssuerParameters(networkMap.nmiForSybilMainNet());

			}
		} else {
			return null;

		}
	}

	private IssuerParametersUID sybilIssuerParameters(NetworkMapItemAdvocate sybilAdvocate) throws Exception {
		IssuerParametersUID result = new IssuerParametersUID();
		UIDHelper helper = new UIDHelper(sybilAdvocate.getLastIssuerUID());
		result.setRevocationInformationUID(helper.getRevocationInfoParams());
		result.setValue(helper.getIssuerParameters());
		return result;

	}

	public void pollNodeForNewParameters(URI nodeUrl, URI failoverUrl,
										 String lastUpdateTime, PassStore store,
										 IssuerParametersUID sybilUid) throws Exception {
		URI uri = NodeVerifier.ping(nodeUrl, failoverUrl,
				lastUpdateTime,false, true);

		if (uri!=null){
			NodeVerifier v = NodeVerifier.openNode(nodeUrl, false, true);
			NodeInformation targetInfo = v.getTargetTrustNetwork().getNodeInformation();

			if (!isSource(targetInfo)) {
				TrustNetworkWrapper tn = openMyTrustNetwork(true);
				NodeInformation myInfo = tn.getNodeInformation();
				XContainerJSON x = openContainer(myInfo.getNodeName(), store);
				URI ppaUid = URI.create(myInfo.getNodeUid() + ":pp");
				URI cUid = URI.create(myInfo.getNodeUid() + ":c");
				PresentationPolicy pp = x.openResource(ppaUid);
				PresentationPolicyAlternatives ppa = new PresentationPolicyAlternatives();
				ppa.getPresentationPolicy().add(pp);
				PresentationPolicyManager ppm = new PresentationPolicyManager(pp, x.openResource(cUid), sybilUid);

				for (String uid : v.getIssuerParameterFileNames()) {

					if (!ppm.hasIssuer(URI.create(uid))) {
						CredentialInPolicy cip = ppm.getCredentialInPolicy();
						String rootIssuer = "urn:exonym:" + XContainerJSON.stripUidSuffix(uid, 1);
						URI raiUid = URI.create(rootIssuer + ":rai");
						URI iUid = URI.create(rootIssuer + ":i");
						IssuerParametersUID ipuid = new IssuerParametersUID();
						ipuid.setRevocationInformationUID(raiUid);
						ipuid.setValue(iUid);
						cip.getIssuerAlternatives().getIssuerParametersUID().add(ipuid);

					} else {
						logger.info("UID already in list " + uid);

					}
				}
				TrustNetwork tnet = tn.finalizeTrustNetwork();

				pp = ppm.build();

				String ppaString = XContainerJSON.convertObjectToXml(pp);
				String niString = JaxbHelper.serializeToXml(tnet, TrustNetwork.class);

				String ppaSign = NodeVerifier.stripStringToSign(ppaString);
				String niSign = NodeVerifier.stripStringToSign(niString);

				byte[] ppaBytes = ppaString.getBytes();
				byte[] niBytes = niString.getBytes();
				KeyContainer kcPrivate = x.openResource("keys.xml");
				KeyContainerWrapper kcwPrivate = new KeyContainerWrapper(kcPrivate);

				AsymStoreKey key = openKey(kcwPrivate.getKey(KeyContainerWrapper.TN_ROOT_KEY), store);

				HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();
				toSign.put(pp.getPolicyUID(), new ByteArrayBuffer(ppaSign.getBytes()));
				toSign.put(tnet.getNodeInformationUid(), new ByteArrayBuffer(niSign.getBytes()));

				URI url = getLeadUrlForThisNode(props.getPrimaryDomain(),
						props.getPrimaryStaticDataFolder());

				KeyContainerWrapper kcw = openSignaturesContainer(url);

				signatureUpdateXml(key, toSign, kcw, url);


				String xml = JaxbHelper.serializeToXml(kcw.getKeyContainer(), KeyContainer.class);
				publish(url, xml.getBytes(), "signatures.xml");
				publish(url, ppaBytes, XContainerJSON.uidToXmlFileName(pp.getPolicyUID()));
				publish(url, niBytes, XContainerJSON.uidToXmlFileName(tnet.getNodeInformationUid()));

			} else {
				throw new Exception("To poll for new paramaters, select a Rulebook Node");

			}
		} else {
			logger.info("Checked for params at " + nodeUrl + " and there were no updates");

		}
	}
	
	public static boolean isSource(NodeInformation info) {
		if (info==null) {
			logger.warn("Null info at isSource");
			return false; 
			
		}
		return info.getNodeUid().equals(info.getLeadUid());
		
	}

	public URI removeNode(URI nodeUrl, PassStore store) throws Exception {
		NodeVerifier v = NodeVerifier.openNode(nodeUrl, false, true);
		try {
			TrustNetworkWrapper tnw = new TrustNetworkWrapper(v.getTargetTrustNetwork());
			removeNode(tnw.getMostRecentIssuerParameters().toString(), store);
			return v.getTargetTrustNetwork().getNodeInformation().getNodeUid();

		} catch (NoSuchElementException e) {
			return v.getTargetTrustNetwork().getNodeInformation().getNodeUid();

		} catch (Exception e) {
			throw new UxException("The node may be deliberately corrupted.  "
					+ "Remove the Node manually by selecting them from the list.", e);

		}
	}

	public void removeNode(String issuerUid, PassStore store) throws Exception {
		URI iUid = URI.create(issuerUid);
		XContainerJSON x = openContainer(leadName, store);
		KeyContainer kcSecret = x.openResource("keys.xml");
		KeyContainerWrapper kcwSecret = new KeyContainerWrapper(kcSecret);
		XKey xkey = kcwSecret.getKey(KeyContainerWrapper.TN_ROOT_KEY);
		AsymStoreKey key = openKey(xkey, store);

		UIDHelper helper = new UIDHelper(issuerUid);

		PresentationPolicy ppa0 = x.openResource(helper.getPresentationPolicy());
		
		PresentationPolicyManager ppm = new PresentationPolicyManager(ppa0,
				x.openResource(helper.getCredentialSpecFileName()),
				null);
		ppm.removeIssuer(iUid);
		
		ppa0 = ppm.build();
		x.saveLocalResource(ppa0, true);

		TrustNetworkWrapper networkWrapper = openMyTrustNetwork(true);

		networkWrapper.removeParticipant(URI.create(issuerUid));
		TrustNetwork network = networkWrapper.finalizeTrustNetwork();

		String ppString = XContainerJSON.convertObjectToXml(ppa0);
		this.ppHash = CryptoUtils.computeSha256HashAsHex(ppString);
		String niString = JaxbHelper.serializeToXml(network, TrustNetwork.class);

		String ppSign = NodeVerifier.stripStringToSign(ppString);
		String niSign = NodeVerifier.stripStringToSign(niString);
		
		// Sign all files
		byte[] ppBytes = ppString.getBytes();
		byte[] niBytes = niString.getBytes();
		
		HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();
		toSign.put(ppa0.getPolicyUID(), new ByteArrayBuffer(ppSign.getBytes()));
		toSign.put(network.getNodeInformationUid(), new ByteArrayBuffer(niSign.getBytes()));

		URI url = getLeadUrlForThisNode(props.getPrimaryDomain(),
				props.getPrimaryStaticDataFolder());
		
		KeyContainerWrapper kcw = openSignaturesContainer(url);

		signatureUpdateXml(key, toSign, kcw, url);
		String xml = JaxbHelper.serializeToXml(kcw.getKeyContainer(), KeyContainer.class);

		publish(url, xml.getBytes(), "signatures.xml");
		publish(url, ppBytes, XContainerJSON.uidToXmlFileName(ppa0.getPolicyUID()));
		publish(url, niBytes, XContainerJSON.uidToXmlFileName(network.getNodeInformationUid()));
		
	}

	public KeyContainerWrapper openSignaturesContainer(URI url) throws Exception {
		Path path = Path.of(url.getPath());
		if (path.getFileName().equals(Const.SIGNATURES_XML)){
			// do nothing

		} else if (path.getFileName().equals(Const.LEAD) || path.getFileName().equals(Const.MODERATOR)){
			url = url.resolve(Const.SIGNATURES_XML);

		} else {
			throw new UxException("There is no signature file associated with this URL: " + url);

		}
		String sigs = new String(UrlHelper.read(url.toURL()));
		KeyContainer kcPublic = JaxbHelper.xmlToClass(sigs, KeyContainer.class);
		KeyContainerWrapper kcw = new KeyContainerWrapper(kcPublic);
		return kcw;
		
	}
	
	public TrustNetworkWrapper openMyTrustNetwork(boolean amILead) throws Exception {
		MyTrustNetwork mtn = new MyTrustNetwork(amILead);
		return mtn.getTrustNetworkWrapper();

	}

	public void addScope(String scope,  PassStore store) throws Exception {
		ArrayList<String> t = new ArrayList<String>();
		t.add(scope);
		addScope(t, store);
		
	}

	public void addScope(ArrayList<String> scope, PassStore store) throws Exception {
		XContainerJSON x = openContainer(leadName, store);
		KeyContainer kcSecret = x.openResource("keys.xml");
		KeyContainerWrapper kcwSecret = new KeyContainerWrapper(kcSecret);
		XKey xkey = kcwSecret.getKey(KeyContainerWrapper.TN_ROOT_KEY);
		AsymStoreKey key = openKey(xkey, store);

		URI networkUid = discoverNetworkUid();
		URI ppaUid = URI.create(networkUid + ":pp");
		URI cUid = UIDHelper.credentialSpecFromSourceUID(networkUid);

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

		String ppString = XContainerJSON.convertObjectToXml(ppa0);

		String ppSign = NodeVerifier.stripStringToSign(ppString);
		
		byte[] ppBytes = ppString.getBytes();
		
		HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();
		toSign.put(ppa0.getPolicyUID(), new ByteArrayBuffer(ppSign.getBytes()));
		
		signatureUpdateXml(key, toSign, kcw, url);
		String xml = JaxbHelper.serializeToXml(kcw.getKeyContainer(), KeyContainer.class);

		publish(url, xml.getBytes(), "signatures.xml");
		publish(url, ppBytes, XContainerJSON.uidToXmlFileName(ppa0.getPolicyUID()));
		
	}

	public void removeScope(String scope, PassStore store) throws Exception {
		XContainerJSON x = openContainer(leadName, store);
		KeyContainer kcSecret = x.openResource("keys.xml");
		KeyContainerWrapper kcwSecret = new KeyContainerWrapper(kcSecret);
		XKey xkey = kcwSecret.getKey(KeyContainerWrapper.TN_ROOT_KEY);
		AsymStoreKey key = openKey(xkey, store);
		
		URI networkUid = discoverNetworkUid();
		URI ppaUid = URI.create(networkUid + ":pp");
		URI cUid = UIDHelper.credentialSpecFromSourceUID(networkUid);

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

		String ppString = XContainerJSON.convertObjectToXml(ppa0);

		String ppSign = NodeVerifier.stripStringToSign(ppString);
		
		byte[] ppaBytes = ppString.getBytes();
		
		HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();
		toSign.put(ppa0.getPolicyUID(), new ByteArrayBuffer(ppSign.getBytes()));

		signatureUpdateXml(key, toSign, kcw, url);
		String xml = JaxbHelper.serializeToXml(kcw.getKeyContainer(), KeyContainer.class);

		publish(url, xml.getBytes(), "signatures.xml");
		publish(url, ppaBytes, XContainerJSON.uidToXmlFileName(ppa0.getPolicyUID()));

	}

	public void publishNonInteractiveProof(NodeVerifier node, NodeVerifier source,
										   XContainerJSON x, PassStore store) throws Exception {
		URI iuid = node.getTargetTrustNetwork().getNodeInformation()
				.getIssuerParameterUids().getLast();
		String raw = XContainer.stripUidSuffix(iuid, 1);
		URI raUid = URI.create(raw + ":ra");
		URI raiUid = URI.create(raw + ":rai");
		String iFile = XContainer.uidToXmlFileName(iuid);
		String raFile = XContainer.uidToXmlFileName(raUid);
		String raiFile = XContainer.uidToXmlFileName(raiUid);

		ExonymOwner owner = new ExonymOwner(x);
		owner.openContainer(store);
		owner.addCredentialSpecification(source.getCredentialSpecification());
		owner.addIssuerParameters(node.getIssuerParameters(iFile));
		owner.addRevocationAuthorityParameters(node.getRevocationAuthorityParameters(raFile));
		owner.addRevocationInformation(raUid, node.getRevocationInformation(raiFile));
		owner.addInspectorParameters(node.getInspectorPublicKey());

		PresentationPolicy pp = source.getPresentationPolicy();
		URI url = getAdvocateUrlForThisNode(props.getPrimaryDomain(),
				props.getPrimaryStaticDataFolder());
		Message message = new Message();
		message.setNonce(url.toString().getBytes());
		pp.setMessage(message);

		PresentationTokenDescription ptd = owner.canProveClaimFromPolicy(pp);
		PresentationPolicyAlternatives ppa = new PresentationPolicyAlternatives();
		ppa.getPresentationPolicy().add(source.getPresentationPolicy());

		if (ptd!=null){
			PresentationToken token = owner.proveClaim(ptd, ppa);
			String ptXml = XContainer.convertObjectToXml(token);

			KeyContainer kcSecret = x.openResource("keys.xml");
			KeyContainerWrapper kcwSecret = new KeyContainerWrapper(kcSecret);
			XKey xkey = kcwSecret.getKey(KeyContainerWrapper.TN_ROOT_KEY);
			AsymStoreKey key = openKey(xkey, store);

			KeyContainerWrapper kcw = openSignaturesContainer(url);

			String tSign = NodeVerifier.stripStringToSign(ptXml);
			byte[] tBytes = tSign.getBytes();

			HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();
			String tokenUid = source.getTargetTrustNetwork()
					.getNodeInformation().getNodeUid() + ":t";

			toSign.put(URI.create(tokenUid), new ByteArrayBuffer(tBytes));

			signatureUpdateXml(key, toSign, kcw, url);
			String xml = JaxbHelper.serializeToXml(kcw.getKeyContainer(), KeyContainer.class);
			publish(url, xml.getBytes(), "signatures.xml");
			publish(url, ptXml.getBytes(), XContainer.uidToXmlFileName(tokenUid));

		} else {
			throw new HubException("There was more than one possible option to fill the credential");

		}
	}

	public void transferInit(URI transferUrl, PassStore store) throws Exception{
		try {
			try {
				transferUrl = NodeVerifier.trainAtFolder(transferUrl);
				
			} catch (UxException e1) {
				throw e1;
				
			}
			try {
				NodeVerifier v = NodeVerifier.openNode(transferUrl, true, true);
				if (isSource(v.getTargetTrustNetwork().getNodeInformation())){
					copyNode(v, v.getTargetTrustNetwork(), store);
					
				} else {
					throw new UxException("Only Source Nodes can be transferred");
					
				}
			} catch (HubException e2) { // In
				defineTransferXml(transferUrl, store);
				
			}
		} catch (HubException e) {
			throw new UnexpectedException("Node was corrupt!", e);
			
		}
	}	

	private static void copyNode(NodeVerifier v, TrustNetwork trustNetwork, PassStore store) throws Exception {
		TrustNetworkWrapper tnw = new TrustNetworkWrapper(v.getTargetTrustNetwork());
		NodeInformation info = tnw.getNodeInformation();
		logger.info("Copying published data from source " + info.getStaticNodeUrl0());
		String name = info.getNodeName() + "test";
		NodeManager nm = new NodeManager(name);
		RulebookNodeProperties props = RulebookNodeProperties.instance();
		URI sourceUrl = nm.getLeadUrlForThisNode(props.getPrimaryDomain(),
				props.getPrimaryStaticDataFolder());
		logger.info("To " + sourceUrl);
		info.setStaticLeadUrl0(sourceUrl);

		if (!info.getNodeName().equals("adasda")){
			throw new Exception("You need to establish the failover");

		}
		// info.setFailOverSourceUrl(URI.create(failover));

		XContainerJSON x = nm.establishNewContainer(name, store);
		AsymStoreKey key = nm.establishKey(store, x, Const.MODERATOR);

		CredentialSpecification cs = v.getCredentialSpecification();
		PresentationPolicy ppa = v.getPresentationPolicy();

		x.saveLocalResource(cs);
		x.saveLocalResource(ppa);

		KeyContainerWrapper kcw = new KeyContainerWrapper(new KeyContainer());

		XKey xk = new XKey();
		xk.setKeyUid(KeyContainerWrapper.TN_ROOT_KEY);
		xk.setPublicKey(key.getPublicKey().getEncoded());
		xk.setSignature(nm.signData(xk.getPublicKey(), key, null));
		kcw.addKey(xk);

		TrustNetwork tn = tnw.finalizeTrustNetwork();

		String csString = XContainerJSON.convertObjectToXml(cs);
		String ppString = XContainerJSON.convertObjectToXml(ppa);
		String niString = JaxbHelper.serializeToXml(tn, TrustNetwork.class);

		String csSign = NodeVerifier.stripStringToSign(csString);
		String ppSign = NodeVerifier.stripStringToSign(ppString);
		String niSign = NodeVerifier.stripStringToSign(niString);

		byte[] csBytes = csString.getBytes();
		byte[] ppaBytes = ppString.getBytes();
		byte[] niBytes = niString.getBytes();

		// Sign all files
		HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();
		toSign.put(cs.getSpecificationUID(), new ByteArrayBuffer(csSign.getBytes()));
		toSign.put(ppa.getPolicyUID(), new ByteArrayBuffer(ppSign.getBytes()));
		toSign.put(tn.getNodeInformationUid(), new ByteArrayBuffer(niSign.getBytes()));

		nm.signatureUpdateXml(key, toSign, kcw, sourceUrl);

		String xml = JaxbHelper.serializeToXml(kcw.getKeyContainer(), KeyContainer.class);

		nm.publish(sourceUrl, xml.getBytes(), "signatures.xml");
		nm.publish(sourceUrl, csBytes, XContainerJSON.uidToXmlFileName(cs.getSpecificationUID()));
		nm.publish(sourceUrl, ppaBytes, XContainerJSON.uidToXmlFileName(ppa.getPolicyUID()));
		nm.publish(sourceUrl, niBytes, XContainerJSON.uidToXmlFileName(tn.getNodeInformationUid()));

	}

	private void defineTransferXml(URI destinationUrl, PassStore store) throws Exception {
		Transfer t = new Transfer();
		DateTime dt = new DateTime(DateTimeZone.UTC);
		dt = dt.plusDays(1);
		t.setStatus("This Source is Active.  Destination Setup Required Before Transfer.");
		t.setTransferRequestTime(DateHelper.currentIsoUtcDateTime());
		t.setTransferToBeCompletedBy(DateHelper.isoUtcDateTime(dt));
		t.setDestinationUrl(destinationUrl);

		URI sourceUrl = getLeadUrlForThisNode(props.getPrimaryDomain(),
				props.getPrimaryStaticDataFolder());

		KeyContainerWrapper kcwPublic = openSignaturesContainer(sourceUrl);
		XContainerJSON x = openContainer(leadName, store);
		KeyContainer kcPrivate = x.openResource("keys.xml");
		KeyContainerWrapper kcwPrivate = new KeyContainerWrapper(kcPrivate);
		AsymStoreKey key = openKey(kcwPrivate.getKey(KeyContainerWrapper.TN_ROOT_KEY), store);

		String tString = JaxbHelper.serializeToXml(t, Transfer.class);

		String tSign = NodeVerifier.stripStringToSign(tString);
		
		byte[] tBytes = tString.getBytes();

		HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();
		toSign.put(t.getTransferUid(), new ByteArrayBuffer(tSign.getBytes()));

		signatureUpdateXml(key, toSign, kcwPublic, sourceUrl);
		String xml = JaxbHelper.serializeToXml(kcwPublic.getKeyContainer(), KeyContainer.class);

		publish(sourceUrl, xml.getBytes(), "signatures.xml");
		publish(sourceUrl, tBytes, XContainerJSON.uidToXmlFileName(t.getTransferUid()));
		
	}

	public void transferOutComplete(URI transferUrl) throws HubException{
		try {
			// TODO
			//NodeVerifier v = new NodeVerifier(transferUrl);
			// rename all files except signature.xml and transfer.xml with a 'trans-' prefix
			// This needs doing after the proper publishing protocols are in place.
			
		} catch (Exception e) {
			throw new HubException("The node has not yet been copied", e);
			
		}
	}
	
	public ArrayList<String> computeAllNetworks(){
		// TODO 
		return null;
		
	}

	public ArrayList<String> computeAllAcceptedNodes(){
		// TODO
		return null;
		
	}

	public AsymStoreKey openKey(XKey x, PassStore store) throws Exception {
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
			throw new SecurityException("Cannot write this KeyContainer Publicly as it contains a Private Key");
			
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
		ArrayList<URI> tmp = new ArrayList<URI>(kcw.getKeyRingUids());
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

	protected void publish(URI url0, byte[] xml, String xmlFileName) throws Exception{
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
		Path writeLocation = computeWriteLocation(url);
		Files.createDirectories(writeLocation);
		Files.write(writeLocation.resolve(xmlFileName), xml, StandardOpenOption.CREATE);

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

	public URI getLeadUrlForThisNode(String root, String folder) throws UxException, MalformedURLException {
		String r = constructBaseNodeUrl(root, folder) + Const.LEAD;
		return URI.create(r);

	}
	
	public URI getAdvocateUrlForThisNode(String root, String folder) throws UxException, MalformedURLException {
		String baseUrl = constructBaseNodeUrl(root, folder) + Const.MODERATOR;
		logger.info("Built URL=" + baseUrl);
		return URI.create(baseUrl);

	}
	
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
			TrustNetworkWrapper tn = openMyTrustNetwork(false);
			this.nodeUid = tn.getNodeInformation().getNodeUid();
			return nodeUid;
			
		}
	}

	
	public static String computeNetworkName(URI url) throws Exception {
		if (url!=null) {
			String start = url.toString();
			int index = start.lastIndexOf("/x-");
			
			if (index>-1) {
				start = start.substring(0, index);
				String[] parts = start.split("/");
				return parts[parts.length-1];
				
			} else {
				throw new Exception("Invalid URI: Expected 'lead' or 'moderator' in the path: " + url);
				
			}
		} else {
			throw new Exception("Null URI");
			
		}
	}

	public void setTrustNetworkUid(URI trustNetworkUid) {
		this.trustNetworkUid = trustNetworkUid;
		
	}

	public String getRaiHash() {
		return raiHash;
	}

	public void setRaiHash(String raiHash) {
		this.raiHash = raiHash;
	}

	public String getPpHash() {
		return ppHash;
	}

	public void setPpHash(String ppHash) {
		this.ppHash = ppHash;
	}
}