package io.exonym.actor.actions;

import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.sun.xml.ws.util.ByteArrayBuffer;
import eu.abc4trust.xml.*;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.connect.UrlHelper;
import io.exonym.helpers.XmlHelper;
import io.exonym.lite.pojo.NetworkMapItem;
import io.exonym.lite.pojo.Rulebook;
import io.exonym.lite.pojo.XKey;
import io.exonym.lite.standard.Const;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.WhiteList;
import io.exonym.lite.time.Timing;
import io.exonym.uri.NamespaceMngt;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.utils.RulebookVerifier;
import io.exonym.utils.storage.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// TODO Node Transfer Protocol
public class NodeVerifier {
	
	private static final Logger logger = LogManager.getLogger(NodeVerifier.class);
	private  KeyContainer rawKeys;
	private  KeyContainerWrapper keys;
	
	private  ConcurrentHashMap<String, ByteArrayBuffer> byteContent;
	private  ConcurrentHashMap<String, ByteArrayBuffer> signatureBytes;
	private  ConcurrentHashMap<String, Object> contents;
	private boolean openingOwnLead = false;
	private boolean openingOwnMod = false;
	private XKey nodePublicKey = null;
	private String keyCheck = null;
	private PresentationPolicy presentationPolicy = null;
	private CredentialSpecification credentialSpecification = null;
	private InspectorPublicKey inspectorPublicKey = null;
	private Rulebook rulebook = null;
	private final HashMap<String, IssuerParameters> issuerParameterMap = new HashMap<>();
	private final HashMap<String, RevocationAuthorityParameters> revocationAuthorityMap = new HashMap<>();
	private final HashMap<String, RevocationInformation> revocationInformationMap = new HashMap<>();
	private final HashMap<String, PresentationToken> presentationTokenMap = new HashMap<>();

	private MyTrustNetworks myTrustNetworks;

	private TrustNetwork targetTrustNetwork;


	private URI nodeUrl;
	
	private long touched = Timing.currentTime();
	private boolean amILead = false;

	private AsymStoreKey publicKey;

	public NodeVerifier(URI nodeUid) throws Exception {
		myTrustNetworks = new MyTrustNetworks();
		boolean shouldWrite = false;
		if (myTrustNetworks.isMyNode(nodeUid)) {
			logger.info("Detected verification of my own node - getting locally.");

			Path localContent = Path.of(Const.PATH_OF_HTML, Const.STATIC);
			if (WhiteList.isLeadUid(nodeUid)){
				keys = myTrustNetworks.getLead().getKcw();
				localContent = localContent.resolve(Const.LEAD);

			} else {
				keys = myTrustNetworks.getModerator().getKcw();
				localContent = localContent.resolve(Const.MODERATOR);

			}
			byteContent = readLocalFileBytes(localContent, keys);

		} else {
			logger.info("Trying local network.");
			AbstractNetworkMap nm = PkiExternalResourceContainer.getInstance()
					.getNetworkMap();
			NetworkMapItem item = nm.nmiForNode(nodeUid);
			this.nodeUrl = item.getStaticURL0();

			try {
				Path localNetworkPath = computeNetworkPathToNode(nodeUid);
				logger.info("Third-party node path=" + localNetworkPath);
				AsymStoreKey key = AsymStoreKey.blank();
				key.assembleKey(item.getPublicKeyB64());

				byteContent = readLocalNetworkBytes(localNetworkPath);

			} catch (Exception e) {
				logger.info("Local network failed: trying the target node");
//				logger.debug("Caught exception to handle: ", e);
				byteContent = XmlHelper.openXmlBytesAtUrl(this.nodeUrl);
				shouldWrite = true;

			}
		}
		signatureBytes = computeBytesThatWereSigned(byteContent);
		contents = XmlHelper.deserializeOpenXml(byteContent);

		if (keys==null){
			rawKeys = (KeyContainer) contents.get(Const.SIGNATURES_XML);
			keys = new KeyContainerWrapper(rawKeys);

		}
		verification(true);
		logger.info("Finished Verification:" + nodeUid + " saving=" + shouldWrite);

		if (shouldWrite){
			saveToNetwork(this.getTargetTrustNetwork().getNodeInformation());
		}
	}
	public NodeVerifier(URL newNodeUrl) throws Exception {
		logger.warn(">>>>>>>>>>> ----------------- Using Remote ONLY Node Verifier ------------------- <<<<<<<<<<<< ");
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		int i = 0;
		for (StackTraceElement s : stackTrace){
			logger.debug(s);
			i++;
			if (i>3){ break; }

		}
		logger.warn(">>>>>>>>>>> -----------------  END ------------------- <<<<<<<<<<<< ");

		myTrustNetworks = new MyTrustNetworks();
		this.nodeUrl=newNodeUrl.toURI();
		byteContent = XmlHelper.openXmlBytesAtUrl(this.nodeUrl);
		signatureBytes = computeBytesThatWereSigned(byteContent);
		contents = XmlHelper.deserializeOpenXml(byteContent);

		if (keys==null){
			rawKeys = (KeyContainer) contents.get(Const.SIGNATURES_XML);
			keys = new KeyContainerWrapper(rawKeys);

		}
		verification(false);
		saveToNetwork(this.getTargetTrustNetwork().getNodeInformation());

	}

	public static Path computeNetworkPathToNode(URI nodeUid) {
		boolean isTargetLead = WhiteList.isLeadUid(nodeUid);
		String leadOrMod = isTargetLead ? Const.LEAD : Const.MODERATOR;
		String toNode = CryptoUtils.computeSha256HashAsHex(nodeUid.toString());
		return Path.of(Const.PATH_OF_NETWORK, toNode, leadOrMod);

	}


	private void saveToNetwork(NodeInformation nodeInformation) {
		URI nodeUid = nodeInformation.getNodeUid();
		Path localNetworkPath = computeNetworkPathToNode(nodeUid);
		try {
			Files.createDirectories(localNetworkPath);
			ByteArrayBuffer rulebook = byteContent.remove("description");

			for (String fileName : byteContent.keySet()){
				try {
					Path p = localNetworkPath.resolve(fileName);
					Files.write(p, byteContent.get(fileName).getRawData(),
							StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

				} catch (IOException e) {
					logger.warn("Could not write file: " + fileName);

				}
			}
			Files.write(localNetworkPath.getParent().resolve(Const.RULEBOOK_JSON),
					rulebook.getRawData(),
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		} catch (IOException e) {
			logger.warn("Could not write file: rulebook.json");

		}
	}


	public static ConcurrentHashMap<String, ByteArrayBuffer> computeBytesThatWereSigned(
			ConcurrentHashMap<String, ByteArrayBuffer> byteContent) throws UnsupportedEncodingException {
		ConcurrentHashMap<String, ByteArrayBuffer> result = new ConcurrentHashMap<>();
		for (String key : byteContent.keySet()){
			String s = new String(byteContent.get(key).getRawData(), "UTF8");
			String t = NodeVerifier.stripStringToSign(s);
			result.put(key, new ByteArrayBuffer(t.getBytes()));

		}
		return result;

	}

	private ConcurrentHashMap<String, ByteArrayBuffer> readLocalFileBytes(Path root, KeyContainerWrapper kcw) throws Exception {
		ConcurrentHashMap<String, ByteArrayBuffer> result = new ConcurrentHashMap<>();
		ArrayList<XKey> keys = kcw.getKeyContainer().getKeyPairs();
		for (XKey key : keys){
			if (key.getKeyUid().toString().startsWith(NamespaceMngt.URN_PREFIX_COLON)){
				String fn = IdContainerJSON.uidToXmlFileName(key.getKeyUid());
				byte[] b = Files.readAllBytes(root.resolve(fn));
				result.put(fn, new ByteArrayBuffer(b));

			} else {
				logger.info("Opening materials and ignoring " + key.getKeyUid());

			}
		}
		String fn = "rulebook.json";
		Path rbpath = root.getParent().resolve(fn);
		byte[] b = Files.readAllBytes(rbpath);
		result.put(fn, new ByteArrayBuffer(b));
		String kc = JaxbHelper.serializeToXml(kcw.getKeyContainer(), KeyContainer.class);
		result.put(Const.SIGNATURES_XML, new ByteArrayBuffer(kc.getBytes(StandardCharsets.UTF_8)));
		return result;

	}

	private ConcurrentHashMap<String, ByteArrayBuffer> readLocalNetworkBytes(Path root) throws Exception {
		ConcurrentHashMap<String, ByteArrayBuffer> result = new ConcurrentHashMap<>();
		Path pathToSig = root.resolve(Const.SIGNATURES_XML);
		logger.info("Path to Signature file: " + pathToSig + " " + Files.exists(pathToSig));
		String kc = Files.readString(pathToSig);
		KeyContainerWrapper kcw = new KeyContainerWrapper(JaxbHelper.xmlToClass(kc, KeyContainer.class));

		ArrayList<XKey> keys = kcw.getKeyContainer().getKeyPairs();
		for (XKey key : keys){
			if (key.getKeyUid().toString().startsWith(NamespaceMngt.URN_PREFIX_COLON)){
				String fn = IdContainerJSON.uidToXmlFileName(key.getKeyUid());
				byte[] b = Files.readAllBytes(root.resolve(fn));
				result.put(fn, new ByteArrayBuffer(b));

			} else {
				logger.info("Opening materials and ignoring " + key.getKeyUid());

			}
		}
		result.put(Const.SIGNATURES_XML, new ByteArrayBuffer(kc.getBytes(StandardCharsets.UTF_8)));
		return result;

	}
	//		String fn = "rulebook.json";
	//		Path rbpath = root.getParent().resolve(fn);
	//		byte[] b = Files.readAllBytes(rbpath);
	//		result.put(fn, new ByteArrayBuffer(b));


	private ConcurrentHashMap<String, ByteArrayBuffer> readUrlBytes(URL url, KeyContainer keys) throws Exception {
		ConcurrentHashMap<String, ByteArrayBuffer> result = new ConcurrentHashMap<>();
		String sigXml = JaxbHelper.serializeToXml(keys, KeyContainer.class);
		result.put(Const.SIGNATURES_XML, new ByteArrayBuffer(sigXml.getBytes()));

		for (XKey key : keys.getKeyPairs()){
			if (key.getKeyUid().toString().startsWith(NamespaceMngt.URN_PREFIX_COLON)){
				String fn = IdContainerJSON.uidToXmlFileName(key.getKeyUid());
				byte[] b = UrlHelper.read(new URL(url.toString() + "/" + fn));
				result.put(fn, new ByteArrayBuffer(b));

			} else {
				logger.debug("Opening materials and ignoring " + key.getKeyUid());

			}
		}
		return result;

	}

	public UIDHelper getUidHelperForMostRecentIssuerParameters() throws Exception {
		TrustNetworkWrapper tnw = new TrustNetworkWrapper(this.getTargetTrustNetwork());
		return new UIDHelper(tnw.getMostRecentIssuerParameters());

	}

	private void verification(boolean insistKeyKnown) throws Exception {
		try {
			logger.info("Found keys and verifying PublicKey signature.");
			
			XKey x = keys.getKey(KeyContainerWrapper.TN_ROOT_KEY);

			this.keyCheck = Base64.encodeBase64String(x.getPublicKey());

			this.nodePublicKey = x;
			
			publicKey = AsymStoreKey.blank();
			publicKey.assembleKey(x.getPublicKey());

			// Verify Signature on Public Key
			verifySignature(x.getPublicKey(), publicKey, x.getSignature());
			verifyMaterialSignatures(keys.getKeyRingUids());
			
			updateObjects();
			verifyChecksum(keys.getKeyRingUids());
			TrustNetwork tn = this.getTargetTrustNetwork();
			verifyPublicKey(tn.getNodeInformation().getNodeUid(), insistKeyKnown);
			contents.clear();
			
		} catch (Exception e) {
			throw e; 
			
		}
	}

	private boolean verifyPublicKey(URI nodeUid, boolean insistKeyKnown) throws Exception {
		PkiExternalResourceContainer extenal = PkiExternalResourceContainer.getInstance();
		AbstractNetworkMap networkMap = extenal.getNetworkMap();
		try {
			NetworkMapItem nmi = networkMap.nmiForNode(nodeUid);
			String k = new String(nmi.getPublicKeyB64(), StandardCharsets.UTF_8);
			return k.equals(keyCheck);

		} catch (NoDocumentException e) {
			if (insistKeyKnown){
				throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, "unknown key");

			} else {
				logger.warn("Participant is unknown - this should only be seen during the addition of Nodes to the Network");
				return false;

			}
		} catch (UxException e) {
			if (insistKeyKnown){
				throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, "unknown key");

			} else {
				logger.warn("Participant is unknown - this should only be seen during the addition of Nodes to the Network");
				return false;

			}
		}
	}


	private boolean verifyChecksum(Set<URI> keyRingUids) throws Exception {
		String check = this.nodeUrl.toString();
		ArrayList<URI> tmp = new ArrayList<URI>(keyRingUids);
		Collections.sort(tmp);
		tmp.remove(KeyContainerWrapper.TN_ROOT_KEY);
		tmp.remove(KeyContainerWrapper.SIG_CHECKSUM);
		for (URI uid : tmp) {
			check += uid;
			
		}
		check = check.replaceAll("/", "");
		logger.debug("CHECK(VERIFY)\n\t\t " + check);
		XKey signature = this.keys.getKey(KeyContainerWrapper.SIG_CHECKSUM);
		if (publicKey.verifySignature(check.getBytes(), signature.getSignature())) {
			logger.info("Checksum Verified");
			return true; 
			
		} else {
			throw new UxException("The XNode is invalid - These files do not belong on this domain, or a file was added or removed");
			
		}
	}

	private void updateObjects() throws Exception {
		for (String f : contents.keySet()){
			Object o = contents.get(f);
			if (o instanceof PresentationPolicy){
				presentationPolicy = ((PresentationPolicy) o);
				
			} else if (o instanceof CredentialSpecification){
				credentialSpecification = (CredentialSpecification) o;
				
			} else if (o instanceof InspectorPublicKey){
				inspectorPublicKey = (InspectorPublicKey) o;
				
			} else if (o instanceof IssuerParameters){
				this.issuerParameterMap.put(f, (IssuerParameters) o);

			} else if (o instanceof RevocationAuthorityParameters){
				this.revocationAuthorityMap.put(f, (RevocationAuthorityParameters) o);
				
			} else if (o instanceof RevocationInformation){
				this.revocationInformationMap.put(f, (RevocationInformation) o);
				
			} else if (o instanceof TrustNetwork){
				targetTrustNetwork = (TrustNetwork) o;
				this.nodeUrl = targetTrustNetwork.getNodeInformation().getStaticNodeUrl0();

			} else if (o instanceof PresentationToken){
				this.presentationTokenMap.put(f, (PresentationToken) o);

			} else if (o instanceof Rulebook){
				this.rulebook = new RulebookVerifier((Rulebook) o).getRulebook();

			} else if (o instanceof KeyContainer){
				// do nothing
				
			} else {
				throw new Exception("Unimplemented object type " + o);
				
			}
		}
	}

	private void verifyMaterialSignatures(Set<URI> keyRingUids) throws Exception {
		HashMap<XKey, ByteArrayBuffer> signatures = new HashMap<>();
		
		for (URI uid : keyRingUids){
			if (!uid.equals(KeyContainerWrapper.TN_ROOT_KEY) && 
					!uid.equals(KeyContainerWrapper.SIG_CHECKSUM)) {
				XKey sig = keys.getKey(uid);
				String fn = IdContainer.uidToXmlFileName(uid);
				ByteArrayBuffer b = signatureBytes.get(fn);
				signatures.put(sig, b);
				
			}
		}
		checkSignatures(publicKey, signatures);
		
	}

	public static void checkSignatures(AsymStoreKey key, HashMap<XKey, ByteArrayBuffer> signatures) throws Exception{
		if (signatures==null || key==null){
			throw new Exception("A required attribute was null key " + key + " signatures " + signatures);
			
		}
		for (XKey sig : signatures.keySet()){
			URI uid = sig.getKeyUid();
			logger.info("Verifying Signature of Resource " + uid);
			
			if (uid==null){
				throw new Exception("KeyUID was null");
				
			} if (sig.getSignature()==null){
				throw new Exception("Signature was null for KeyUid " + uid);
				
			} 
			ByteArrayBuffer baf = signatures.get(sig);
			if (baf==null || baf.getRawData()==null){
				throw new Exception("Null raw data for file " + uid);
				
			}
			if (!verifySignature(baf.getRawData(), key, sig.getSignature())){
				throw new Exception("Signature was invalid for UID " + uid);
				
			} else {
				logger.info("Signature Verified for " + uid);
				
			}
		}
	}

	public static String stripStringToSign(String xml){
		return xml.replaceAll("\t", "")
				.replaceAll("\n", "")
				.replaceAll(" ", "")
				.replaceAll("\"", "");
	}

	public static boolean verifySignature(byte[] data, AsymStoreKey key, byte[] signature) throws Exception {
		if (data==null){
			throw new UxException("Data was null");
			
		} if (key==null){
			throw new UxException("Public key was null");
			
		} if (signature==null){
			throw new UxException("Signature was null");
			
		}
		if (key.verifySignature(data, signature)){
			logger.info("PublicKey Signature Verified");
			return true; 
			
		} else {
			throw new UxException("Public Key Signature Verification Failed - the XNode is invalid");	
			
		}
	}
	
	public static URI trainAtFolder(URI node) throws Exception {
		String f = node.toString();
		if (f.endsWith(Const.MODERATOR + "/") || f.endsWith(Const.LEAD + "/")){
			return node;
			
		} else if (f.endsWith(Const.MODERATOR) || f.endsWith(Const.LEAD)){
			return new URL(node + "/").toURI();
			
		} else if (f.contains(Const.MODERATOR)){
			return new URL(f.substring(0, f.indexOf(Const.MODERATOR)) + Const.MODERATOR + "/").toURI();
			
		} else if (f.contains(Const.LEAD)){
			return new URL(f.substring(0, f.indexOf(Const.LEAD)) + Const.LEAD + "/").toURI();
			
		} else {
			throw new UxException("Node Verification Error.  An inspectable url ends with either `lead` or `moderator` (" + node + ")");
			
		}
	}

	@Deprecated
	public PresentationPolicyAlternatives getPresentationPolicyAlternatives() throws HubException {
		throw new HubException("You should not be using Presentation Policy Alternatives.  Deprecated.  Moved to Presentation Policy :pp suffix files - check source materials");

	}

	public PresentationPolicy getPresentationPolicy() {
		return presentationPolicy;
	}


	public CredentialSpecification getCredentialSpecification() throws HubException {
		if (credentialSpecification==null){
			throw new HubException("Credential Specification null");
			
		}
		return credentialSpecification;
		
	}

	public InspectorPublicKey getInspectorPublicKey() throws HubException {
		if (inspectorPublicKey==null){
			throw new HubException("InspectorPublicKey null");
			
		}
		return inspectorPublicKey;
		
	}

	public IssuerParameters getIssuerParameters(String fileName) throws HubException {
		return issuerParameterMap.get(fileName);
		
	}


	public Set<String> getIssuerParameterFileNames(){
		return issuerParameterMap.keySet();
		
	}


	public KeyContainer getRawKeys() {
		return rawKeys;
	}

	public RevocationAuthorityParameters getRevocationAuthorityParameters(String fileName) throws HubException {
		return revocationAuthorityMap.get(fileName);
		
	}

	public ArrayList<PresentationToken> getPresentationTokens() {
		return (ArrayList<PresentationToken>) this.presentationTokenMap.values();

	}

	public Set<String> getAllRevocationAuthorityFileNames(){
		return revocationAuthorityMap.keySet();

	}

	public RevocationInformation getRevocationInformation(String fileName) throws HubException {
		return revocationInformationMap.get(fileName);
	}

	public Set<String> getAllRevocationInformationFileNames(){
		return revocationInformationMap.keySet();

	}

	public void loadTokenVerifierFromNodeVerifier(TokenVerifier tokenVerifier, UIDHelper uids) throws Exception {

		tokenVerifier.loadRevocationInformation(
				this.getRevocationInformation(uids.getRevocationInformationFileName()));

		tokenVerifier.loadIssuerParameters(this.getIssuerParameters(uids.getIssuerParametersFileName()));
		tokenVerifier.loadInspectorParams(this.getInspectorPublicKey());
		tokenVerifier.loadRevocationAuthorityParameters(
				this.getRevocationAuthorityParameters(uids.getRevocationAuthorityFileName()));

	}

	public TrustNetwork getTargetTrustNetwork() throws HubException {
		if (targetTrustNetwork ==null){
			throw new HubException("TrustNetwork is null");
			
		}
		return targetTrustNetwork;
		
	}
	
	public XKey getPublicKey() throws HubException {
		if (nodePublicKey==null){
			throw new HubException("PublicKey is null");
			
		}
		return nodePublicKey;
		
	}	

	public long getTouched() {
		return touched;
	}

	public void setTouched(long touched) {
		this.touched = touched;
		
	}

	public Rulebook getRulebook() {
		return rulebook;
	}

	public ConcurrentHashMap<String, ByteArrayBuffer> getByteContent() {
		return byteContent;
	}

}
