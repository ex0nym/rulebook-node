package io.exonym.actor.actions;

import com.ibm.zurich.idmx.jaxb.JaxbHelperClass;
import eu.abc4trust.xml.*;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.helpers.Parser;
import io.exonym.lite.connect.Http;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.helpers.PresentationPolicyManager;
import io.exonym.lite.pojo.*;
import io.exonym.lite.standard.Const;
import io.exonym.lite.time.Timing;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.PassStore;
import io.exonym.utils.RulebookVerifier;
import io.exonym.utils.node.ProgressReporter;
import io.exonym.utils.storage.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestNodeManager {
	
	private static final Logger logger = LogManager.getLogger(TestNodeManager.class);
	private static String usernameOwner = "owner";
	private static String mjh = "mjh";
	private static String password = "password";
	private String sourceName = "ccc-test";
	private String advocateName = "waterford-times";
	private static PassStore store;
	private static String sybilServiceUrl = "http://exonym-x-03:8080/";
	private static String sybilServiceRegisterUrl = sybilServiceUrl + "register";


	public static void setup() throws Exception {
		store = new PassStore(password, false);
		blankContainer(usernameOwner);
//		blankContainer(mjh);
		addRootKey();

	}

	public void testAll(){
		try {
//			testPPM();
//			testEstablishNewSource();
//			testEstablishNewNode();
//			testAddNodeToSource();
//			testAddRemoveScope();
//			testRemoveNodeFromSource();
//			testAddNodeToSource(); //*/
			testOnboardToRulebook();

		} catch (Exception e) {
			logger.error("Fail ", e);
			assert(false);
			
		}
	}

	private void testPPM() {
		try {
			NodeVerifier verifier = NodeVerifier.openNode(
					URI.create("https://trust.exonym.io/ccc-test/lead"),
					true, false);
			PresentationPolicyManager ppm = new PresentationPolicyManager(verifier.getPresentationPolicy(),
					verifier.getCredentialSpecification(), null);
			ppm.removeNym("Helllo");
			PresentationPolicy po = ppm.build();
			String xml = IdContainerJSON.convertObjectToXml(po);
			logger.debug(xml);




		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	// @Test
	public void testEstablishNewSource(){
		try {
			resetSource(sourceName);
			LocalNetworkMap networkMap = new LocalNetworkMap();
			if (!networkMap.networkMapExists()){
				networkMap.spawn();
			}
			NodeManager network = new NodeManager(sourceName);
			network.setupLead(new URL("https://trust.exonym.io/source-rulebook.json"), store);
			RulebookNodeProperties props = RulebookNodeProperties.instance();
			URI url = network.getLeadUrlForThisNode(props.getPrimaryDomain(),
					props.getPrimaryStaticDataFolder());

			NodeVerifier nv = NodeVerifier.openNode(url, true, true);
			// Confirm Credential Specification
			CredentialSpecification cs = nv.getCredentialSpecification();
			assert(cs.isRevocable());
			assert(cs.isKeyBinding());
			assert(cs.getAttributeDescriptions().getAttributeDescription().size()>0);
			
			// Confirm Presentation Policy
			PresentationPolicy pp = nv.getPresentationPolicy();
			assert(pp.getCredential().isEmpty());
			assert(pp.getMessage()==null);
			assert(pp.getPseudonym().size()==1);
			
			// Confirm Trust Network
			TrustNetwork tn = nv.getTargetTrustNetwork();
			assert(tn.getLastUpdated()!=null);
			assert(tn.getParticipants().isEmpty());
			assert(tn.getNodeInformationUid()!=null);
			
			// Confirm Node Information
			NodeInformation ni = tn.getNodeInformation();
			assert(ni.getStaticNodeUrl0()!=null);
			assert(ni.getNodeName()!=null);
			assert(ni.getIssuerParameterUids().isEmpty());
			assert(ni.getNodeUid().equals(ni.getLeadUid()));
			assert(ni.getStaticLeadUrl0()!=null);
			assert(ni.getStaticNodeUrl0()!=null);
			
			IdContainerJSON x = new IdContainerJSON(sourceName);
			KeyContainer keyPrivate = x.openResource("keys.xml");
			KeyContainerWrapper kcw = new KeyContainerWrapper(keyPrivate);
			assert(kcw.getKey(KeyContainerWrapper.TN_ROOT_KEY)!=null);

		} catch (Exception e) {
			logger.error("Error", e);
			assert(false);
			
		}
	}

	// @Test
	public void testEstablishNewNode(){
		try {
//			resetNode("noborder", orgName);
			NodeManager network = new NodeManager(sourceName);
			URI sourceUrl = URI.create("https://trust.exonym.io/ccc-test/lead");
			ProgressReporter r = new ProgressReporter(new String[] {"0", "1", "2", "3", "4", "5", "6"});
			network.setupModeratorNode(sourceUrl, advocateName, store, r);

			RulebookNodeProperties props = RulebookNodeProperties.instance();
			URI url = network.getModUrlForThisNode(props.getPrimaryDomain(),
					props.getPrimaryStaticDataFolder());

			NodeVerifier v = NodeVerifier.openNode(url, false, false);

			TrustNetwork tn = v.getTargetTrustNetwork();
			NodeInformation ni = tn.getNodeInformation();
			assert(ni.getLeadUid()!=null);
			assert(!ni.getLeadUid().equals(ni.getNodeUid()));
			assert(ni.getNodeName()!=null);
			assert(!ni.getIssuerParameterUids().isEmpty());
			assert(ni.getStaticNodeUrl0()!=null);
			assert(ni.getStaticLeadUrl0()!=null);
			
		} catch (Exception e) {
			logger.error("Error", e);
			assert(false);
			
		}
	}
	
	// @Test
	public void testAddNodeToSource(){
		try {
			LocalNetworkMap networkMap = new LocalNetworkMap();
			if (!networkMap.networkMapExists()){
				networkMap.spawn();
			}
			NodeManager network = new NodeManager(sourceName);
			URI nodeUrl = URI.create("https://trust.exonym.io/ccc-test/").resolve(Const.MODERATOR);
			network.addModeratorToLead(nodeUrl, store, networkMap, true);

			RulebookNodeProperties props = RulebookNodeProperties.instance();
			URI url = network.getLeadUrlForThisNode(props.getPrimaryDomain(),
					props.getPrimaryStaticDataFolder());

			NodeVerifier n = NodeVerifier.openNode(url, true, false);
			PresentationPolicy pp = n.getPresentationPolicy();
			
			PresentationPolicyManager ppm = new PresentationPolicyManager(pp, n.getCredentialSpecification(), null);
			URI issuerUid = null; // n.getOwnTrustNetwork().getMostRecentIssuerParameters();
			// TODO - this was removed when the Trust Network migrated to the network map in the NodeVerifier
			logger.info("Checking update to public information");
			assert(ppm.hasIssuer(issuerUid));
			
			IdContainerJSON x  = new IdContainerJSON("ccc-test");
			URI sourceUid = network.discoverNetworkUid();
			PresentationPolicy ppaax = x.openResource(URI.create(sourceUid + ":pp"));
			ppm = new PresentationPolicyManager(ppaax, n.getCredentialSpecification(), null);
			logger.info("Checking update to local information");
			assert(ppm.hasIssuer(issuerUid));
			
		} catch (Exception e) {
			logger.error("Error", e);
			assert(false);
			
		}
	}	//*/
	
	// @Test
	public void testRemoveNodeFromSource(){
		try {
			NodeManager network = new NodeManager(sourceName);

			RulebookNodeProperties props = RulebookNodeProperties.instance();
			URI nodeUrl = network.getLeadUrlForThisNode(props.getPrimaryDomain(),
					props.getPrimaryStaticDataFolder());

			network.removeModeratorFromLead(nodeUrl, store);

			NodeVerifier n = NodeVerifier.openNode(nodeUrl, true, true);
			PresentationPolicy ppaa = n.getPresentationPolicy();
			
			PresentationPolicyManager ppm = new PresentationPolicyManager(ppaa, n.getCredentialSpecification());
			URI nodeUid = network.discoverNodeUid();
			URI issuerUid = URI.create(nodeUid + ":i");
			logger.info("Checking update to public information");
			assert(!ppm.hasIssuer(issuerUid));
			
			IdContainerJSON x  = new IdContainerJSON(sourceName);
			URI sourceUid = network.discoverNetworkUid();
			PresentationPolicy ppaax = x.openResource(URI.create(sourceUid + ":pp"));
			ppm = new PresentationPolicyManager(ppaax, null);
			logger.info("Checking update to local information");
			assert(!ppm.hasIssuer(issuerUid));
			
		} catch (Exception e) {
			logger.error("Error", e);
			assert(false);
			
		}
	}	
	
	// @Test
	public void testUidDiscovery() throws UxException, HubException{
		try {
			NodeManager m = new NodeManager(sourceName);
			logger.info(m.discoverNetworkUid());
			
		} catch (Exception e) {
			logger.info("Error", e);
			assert(false);
			
		}
	}	
	
	//@Test
	public void testAddRemoveScope() throws UxException, HubException{
		try {
			String s = "www.we.com";
			NodeManager m = new NodeManager(sourceName);
			m.addScope(s, store);

			RulebookNodeProperties props = RulebookNodeProperties.instance();
			URI url = m.getLeadUrlForThisNode(props.getPrimaryDomain(),
					props.getPrimaryStaticDataFolder());

			NodeVerifier v = NodeVerifier.openNode(url, true, true);
			
			IdContainerJSON x = new IdContainerJSON(sourceName);
			
			URI ppaUid = URI.create(m.discoverNetworkUid() + ":pp");
			
			PresentationPolicy pp = x.openResource(ppaUid);

			assert(hasScopeString(pp, s));
			assert(hasScopeString(v.getPresentationPolicy(), s));
			
			m.removeScope(s, store);
			URI url0 = m.getLeadUrlForThisNode(props.getPrimaryDomain(),
					props.getPrimaryStaticDataFolder());

			v = NodeVerifier.openNode(url0, true, true);
			PresentationPolicyManager ppm = new PresentationPolicyManager(v.getPresentationPolicy(),
					v.getCredentialSpecification());
			x = new IdContainerJSON(sourceName);
			pp = x.openResource(ppaUid);
			assert(!ppm.hasScope(s));
			
			ppm = new PresentationPolicyManager(pp, v.getCredentialSpecification());
			assert(!ppm.hasScope(s));
			
		} catch (Exception e) {
			logger.info("Error", e);
			assert(false);
			
		}
	}

	//@Test
	public void testVerifySignature(){
		try {
			long t = Timing.currentTime();
			KeyContainer kc = openXml(new File("resource//trust-networks//publisher//lead//signatures.xml"), KeyContainer.class);
			KeyContainerWrapper kcw = new KeyContainerWrapper(kc);
			XKey x = kcw.getKey(KeyContainerWrapper.TN_ROOT_KEY);
			
			AsymStoreKey key = AsymStoreKey.blank();
			key.assembleKey(x.getPublicKey());

			assert(NodeVerifier.verifySignature(x.getPublicKey(), key, x.getSignature()));
			logger.info("Checking signature of Public Key took (ms) " + Timing.hasBeenMs(t));
		
		} catch (Exception e) {
			logger.error("Error", e);
			assert(false);
			
		}
	} //*/ 

	private static void addRootKey() throws Exception {
		AsymStoreKey key = new AsymStoreKey();
		XKey xk = new XKey();
		xk.setKeyUid(KeyContainerWrapper.TN_ROOT_KEY);
		xk.setPublicKey(key.getPublicKey().getEncoded());
		xk.setPrivateKey(key.getEncryptedEncodedForm(store.getEncrypt()));
		NodeManager p = new NodeManager(usernameOwner);
		
		KeyContainer container = new KeyContainer();
		KeyContainerWrapper wrapper = new KeyContainerWrapper(container);
		wrapper.addKey(xk);

		IdContainerJSON x = new IdContainerJSON(usernameOwner);
		x.saveLocalResource(container);
		
		KeyContainer pub = new KeyContainer();
		xk.setPrivateKey(null);
		wrapper = new KeyContainerWrapper(pub);
		wrapper.addKey(xk);
		xk.setSignature(p.signData(xk.getPublicKey(), key, null));
		String xml = JaxbHelper.serializeToXml(pub, KeyContainer.class);
		RulebookNodeProperties props = RulebookNodeProperties.instance();
		URI url = p.getLeadUrlForThisNode(props.getPrimaryDomain(),
				props.getPrimaryStaticDataFolder());

		p.publish(url, xml.getBytes(), "signatures.xml");

	}

	// Requires the presence of a Sybil Node
	private void testOnboardToRulebook() {
		try {
			URI myAdvocate = URI.create("urn:rulebook:exosources:baseline:69bb840695e4fd79a00577de5f0071b311bbd8600430f6d0da8f865c5c459d44");
			URI mySource = URI.create("urn:rulebook:exosources:69bb840695e4fd79a00577de5f0071b311bbd8600430f6d0da8f865c5c459d44");

			LocalNetworkMap mapTmp = new LocalNetworkMap();
			mapTmp.spawn();
			NetworkMapTest map  = new NetworkMapTest(
					(NetworkMapItemLead) mapTmp.nmiForNode(mySource),
					(NetworkMapItemModerator) mapTmp.nmiForNode(myAdvocate));

			PkiExternalResourceContainer external = PkiExternalResourceContainer.getInstance();
			external.setNetworkMapAndCache(map, new Cache());

			Http client = new Http();

			resetOwner(mjh);
			IdContainerJSON x = new IdContainerJSON(mjh, true);
			ExonymOwner owner = new ExonymOwner(x);
			owner.openContainer(store);
			owner.setupContainerSecret(store.getEncrypt(), store.getDecipher());

			IssuanceSigma sigma = new IssuanceSigma();
			sigma.setHello(UUID.randomUUID().toString());
			sigma.setTestNet(true);
			sigma.setSybilClass(Rulebook.SYBIL_CLASS_ENTITY);

			String r0 = client.basicPost(sybilServiceRegisterUrl, JaxbHelper.serializeToJson(sigma, IssuanceSigma.class));
			IssuanceSigma response0 = JaxbHelper.jsonToClass(r0, IssuanceSigma.class);
			IssuanceMessageAndBoolean imab = Parser.parseIssuanceMessageAndBoolean(response0.getImab());

			IssuanceMessage im = owner.issuanceStep(imab, store.getEncrypt());
			sigma.setIm(Parser.parseIssuanceMessage(im));
			String r1 = client.basicPost(sybilServiceRegisterUrl,
					JaxbHelper.serializeToJson(sigma, IssuanceSigma.class));

			IssuanceSigma response1 = JaxbHelper.jsonToClass(r1, IssuanceSigma.class);
			imab = Parser.parseIssuanceMessageAndBoolean(response1.getImab());
			owner.issuanceStep(imab, store.getEncrypt());
			logger.debug(response1.getH());
			logger.debug(response1.getIssuerUid());

//			XContainerJSON x = new XContainerJSON(mjh);
//			ExonymOwner owner = new ExonymOwner(x);
//			owner.openContainer(store);

			RulebookVerifier rulebookVerifier = new RulebookVerifier(new URL("https://trust.exonym.io/source-rulebook.json"));
			List<String> sources = map.getSourceFilenamesForRulebook(
					rulebookVerifier.getRulebook().getRulebookId());

			NetworkMapItemLead nmis = (NetworkMapItemLead) map.nmiForNode(
					map.fromNmiFilename(sources.get(1)));

			ArrayList<URI> mods = new ArrayList<>(nmis.getModeratorsForLead());

			NetworkMapItemModerator nmia = (NetworkMapItemModerator)
					map.nmiForNode(mods.get(0));

			URI s0 = nmis.getStaticURL0();
			URI a0 = nmia.getStaticURL0();

			String ax0 = nmia.getRulebookNodeURL() + "/subscribe";
			String r = client.basicGet(ax0);
			Rulebook rulebook = RulebookVerifier.fromString(r);

			rulebookVerifier = new RulebookVerifier(rulebook);

			URI sybilIssuerUID = map.nmiForSybilModTest().getLastIssuerUID();
			IssuanceMessageAndBoolean imabRulebook  = Parser.parseIssuanceMessageAndBoolean(
					rulebookVerifier.getRulebook().getChallengeB64());

			IssuanceMessage imRulebook = owner.issuanceStep(imabRulebook, store.getEncrypt());
			String xml = IdContainer.convertObjectToXml(imRulebook);
			String finalMessage = client.basicPost(ax0, xml);
			if (!finalMessage.contains("error")){
				imabRulebook = (IssuanceMessageAndBoolean) JaxbHelperClass.deserialize(finalMessage).getValue();
				owner.issuanceStep(imabRulebook, store.getEncrypt());

			} else {
				logger.debug(finalMessage);

			}


//			PresentationPolicyAlternatives ppa = JoinHelper.baseJoinPolicy(rulebookVerifier,
//					sybilIssuerUID,
//					external,
//					rulebookVerifier.getRulebook().getChallengeB64()
//			);
//
//			UIDHelper helper = new UIDHelper(sybilIssuerUID);
//			RevocationInformation info = external.openResource(helper.getRevocationInformationFileName());
//			owner.addRevocationInformation(info.getRevocationAuthorityParametersUID(), info);
//
//			PresentationTokenDescription ptd = owner.canProveClaimFromPolicy(ppa);
//			PresentationToken presentationToken = owner.proveClaim(ptd, ppa);
//
//			String p = Parser.parsePresentationPolicyAlt(ppa);
//			String t = Parser.parsePresentationToken(presentationToken);
//
//
//			IssuanceSigma prove = new IssuanceSigma();
//			prove.setPresentationPolicy(p);
//			prove.setPresentationToken(t);
//
//			String response = client.basicPost(ax0, JaxbHelper.serializeToJson(prove, IssuanceSigma.class));
//
//			logger.debug(response);


		} catch (Exception e) {
			throw new RuntimeException(e);

		}
	}


	private static void blankContainer(String name) {
		try {
			IdContainerJSON x = new IdContainerJSON(name);
			x.delete();
			new IdContainerJSON(name, true);
			
		} catch (Exception e) {
			try {
				new IdContainerJSON(name, true);
				
			} catch (Exception e1) {
				logger.error("Error", e);
				
			}
		}		
	}	
	
	private static void resetSource(String name) {
		try {
			try {
				IdContainerJSON x = new IdContainerJSON(name);
				x.delete();
				
			} catch (Exception e) {
				logger.info("There was no container to delete.");
				
				
			}
			File s = new File("resource//trust-networks//" + name + "//lead");
			File n = new File("resource//trust-networks//" + name + "//moderator");
			if (s.exists()){
				deleteContentsOfFolder(s);
				
			}
			if (n.exists()){
				deleteContentsOfFolder(n);
				
			}
			File r = s.getParentFile();
			s.delete();
			n.delete();
			r.delete();
			
		} catch (Exception e) {
			logger.info(e.getMessage());
			
		}		
	}

	private static void resetNode(String name, String sourceName) {
		try {
			try {
				IdContainerJSON x = new IdContainerJSON(name);
				x.delete();
				
			} catch (Exception e) {
				logger.info("There was no container to delete.");
				
				
			}
			File n = new File("resource//trust-networks//" + sourceName + "//" + Const.MODERATOR);
			if (n.exists()){
				deleteContentsOfFolder(n);
				
			}
			File r = n.getParentFile();
			n.delete();
			r.delete();
			
		} catch (Exception e) {
			logger.info(e.getMessage());
			
		}		
	}
	
	private static void resetOwner(String name) {
		try {
			try {
				IdContainerJSON x = new IdContainerJSON(name);
				x.delete();
				
			} catch (Exception e) {
				logger.info("There was no container to delete.");
				
				
			}
		} catch (Exception e) {
			logger.info(e.getMessage());
			
		}		
	}	

	private static void deleteContentsOfFolder(File f) throws UxException {
		File[] files = f.listFiles();
		for (File file : files){
			if (!file.isDirectory()){
				file.delete();
				
			} else {
				throw new UxException("There are directories in this folder");
				
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T openXml(File f, Class<?> clazz) throws Exception {
		return (T) JaxbHelper.xmlFileToClass(
				JaxbHelper.fileToPath(f), clazz);
		
	}
	
	private boolean hasScopeString(PresentationPolicy p, String s) throws Exception {
		for (PseudonymInPolicy nym : p.getPseudonym()) {
			if (nym.getScope().equals(s)) {
				return true;
				
			}
		}
		return false; 
	}	
}
