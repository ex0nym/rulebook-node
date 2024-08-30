package io.exonym.actor.actions;

import com.sun.xml.ws.util.ByteArrayBuffer;
import eu.abc4trust.xml.*;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.connect.UrlHelper;
import io.exonym.helpers.XmlHelper;
import io.exonym.lite.pojo.Rulebook;
import io.exonym.lite.pojo.XKey;
import io.exonym.lite.standard.Const;
import io.exonym.lite.time.Timing;
import io.exonym.uri.NamespaceMngt;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.utils.RulebookVerifier;
import io.exonym.utils.storage.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
	
	private TrustNetwork targetTrustNetwork = null;

	private TrustNetwork ownTrustNetwork = null;

	private boolean openingOwnLead = false;
	private boolean openingOwnMod = false;
	private MyTrustNetworkAndKeys ownLeadTnw;
	private MyTrustNetworkAndKeys ownModTnw;
	private XKey nodePublicKey = null;
	private String keyCheck = null;
	private PresentationPolicy presentationPolicy = null;
	private CredentialSpecification credentialSpecification = null;
	private InspectorPublicKey inspectorPublicKey = null;
	private Rulebook rulebook = null;

	private final HashMap<String, IssuerParameters> issuerParameterMap = new HashMap<String, IssuerParameters>();
	private final HashMap<String, RevocationAuthorityParameters> revocationAuthorityMap = new HashMap<String, RevocationAuthorityParameters>();
	private final HashMap<String, RevocationInformation> revocationInformationMap = new HashMap<String, RevocationInformation>();
	private final HashMap<String, PresentationToken> presentationTokenMap = new HashMap<String, PresentationToken>();


	private URI nodeUrl;
	
	private long touched = Timing.currentTime();
	private final boolean amILead;
	
	private AsymStoreKey publicKey;

	/**
	 * Verify Node Regardless of whether Local Data is Up to Date, or not
	 *
	 * @param primary
	 * @param secondary
	 * @param isTargetSource
	 * @param amISource
	 * @return
	 * @throws Exception
	 */
	public static NodeVerifier tryNode(URI primary, URI secondary,
									   boolean isTargetSource, boolean amISource) throws Exception {
		try {
			tryUrl(secondary, isTargetSource, amISource);
			return new NodeVerifier(primary, isTargetSource, amISource);

		} catch (FileNotFoundException e){
			tryUrl(primary, isTargetSource, amISource);
			return new NodeVerifier(secondary, isTargetSource, amISource);

		} catch (UnknownHostException e){
			tryUrl(primary, isTargetSource, amISource);
			return new NodeVerifier(secondary, isTargetSource, amISource);

		} catch (Exception e){
			throw e;

		}
	}

	/**
	 * Verify Node Regardless of whether Local Data is Up to Data, or not
	 *
	 * @param primary
	 * @param secondary
	 * @return
	 * @throws Exception
	 */
	public static void confrimPrimaryAndSecondary(URI primary, URI secondary) throws Exception {
		try {
			tryUrl(primary, false, false);
			tryUrl(secondary, false, false);

		} catch (Exception e){
			throw new UxException("One or more of the publish locations is unavailable.  Check for errors and try again");

		}
	}



	/**
	 * To be used in conjunction with ping(), so the URL has already been established
	 *
	 * @param known
	 * @param isTargetLead
	 * @param amILead
	 * @return
	 * @throws Exception
	 */
	public static NodeVerifier openNode(URI known, boolean isTargetLead, boolean amILead) throws Exception {
		return new NodeVerifier(known, isTargetLead, amILead);

	}

	/**
	 * Establish which URL works
	 * @param primary
	 * @param secondary
	 * @param lastUpdatedTime
	 * @param isTargetSource
	 * @param amISource
	 * @return the URL that worked, or NULL if the lastUpdateTime matched that in the signatures.xml file at the URL
	 *
	 * @throws Exception
	 */
	public static URI ping(URI primary, URI secondary, String lastUpdatedTime,
									   boolean isTargetSource, boolean amISource) throws Exception {
		try {
			String t = tryUrl(primary, isTargetSource, amISource);
			if (t.equals(lastUpdatedTime)){
				return null;

			} else {
				return primary;

			}
		} catch (FileNotFoundException e){
			String t = tryUrl(secondary, isTargetSource, amISource);
			if (t.equals(lastUpdatedTime)){
				return null;

			} else {
				return secondary;

			}
		} catch (UnknownHostException e){
			String t = tryUrl(secondary, isTargetSource, amISource);
			if (t.equals(lastUpdatedTime)){
				return null;

			} else {
				return secondary;

			}
		} catch (Exception e){
			throw e;

		}
	}

	public static NodeVerifier openLocal(URI url, KeyContainer localSourceSig, boolean amISource) throws Exception {
		return new NodeVerifier(url, localSourceSig, amISource);


	}

	private static String tryUrl(URI url, boolean isTargetSource, boolean amISource) throws Exception {
		try {
			String xml = new String(UrlHelper.readXml(
					url.resolve("signatures.xml").toURL()), "UTF8");
			KeyContainer kc = JaxbHelper.xmlToClass(xml, KeyContainer.class);
			return kc.getLastUpdateTime();

		} catch (Exception e){
			throw e;

		}
	}

	private NodeVerifier(URI node, boolean isTargetLead, boolean amILead) throws Exception {
		long t0 = Timing.currentTime();
		if (node==null){
			throw new HubException(ErrorMessages.SERVER_SIDE_PROGRAMMING_ERROR);

		}
		this.amILead = amILead;
		this.nodeUrl = trainAtFolder(node);
		openMyTrustNetworks(this.nodeUrl);

		if (isTargetLead){
			if (!node.toString().contains(Const.LEAD)){
				throw new UxException("URL must be a Lead-URL " + node);

			}
		}
		if (openingOwnMod || openingOwnLead){
			Path localContent = Path.of(Const.PATH_OF_HTML, this.nodeUrl.getPath());
			keys = openingOwnMod ? ownModTnw.getKcw() : ownLeadTnw.getKcw();
			byteContent = readLocalBytes(localContent, keys);

		} else {
			try {
				logger.info("Trying URL: " + this.nodeUrl);
				byteContent = XmlHelper.openXmlBytesAtUrl(this.nodeUrl);

			} catch (Exception e) {
				throw new UxException(ErrorMessages.STATIC_DATA_UNAVAILABLE);

			}
		}
		signatureBytes = computeBytesThatWereSigned(byteContent);
		contents = XmlHelper.deserializeOpenXml(byteContent);
		if (keys==null){
			rawKeys = (KeyContainer) contents.get("signatures.xml");
			keys = new KeyContainerWrapper(rawKeys);

		}
		// Note that changing to network map will not function properly when
		// establishing the node.
		verification();
		logger.info("Opened node static data and verified " + node + " : " + Timing.hasBeenMs(t0));
		
	}

	private NodeVerifier(URI url, KeyContainer keys, boolean amILead) throws Exception {
		try {
			openMyTrustNetworks(url);
			this.amILead = amILead;

			if (openingOwnMod || openingOwnLead){
				Path localContent = Path.of(Const.PATH_OF_STATIC, this.nodeUrl.getPath());
				KeyContainerWrapper kcw = openingOwnMod ? ownModTnw.getKcw() : ownLeadTnw.getKcw();
				byteContent = readLocalBytes(localContent, kcw);

			} else {
				byteContent = readLocalBytes(url.toURL(), keys);

			}
			signatureBytes = computeBytesThatWereSigned(byteContent);
			contents = XmlHelper.deserializeOpenXml(byteContent);
			verification();

		} catch (FileNotFoundException e){
			throw new HubException("The URL was likely incorrect " + url, e);

		} catch (Exception e){
			throw e;

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

	private ConcurrentHashMap<String, ByteArrayBuffer> readLocalBytes(Path root, KeyContainerWrapper kcw) throws Exception {
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


	private ConcurrentHashMap<String, ByteArrayBuffer> readLocalBytes(URL url, KeyContainer keys) throws Exception {
		ConcurrentHashMap<String, ByteArrayBuffer> result = new ConcurrentHashMap<>();
		String sigXml = JaxbHelper.serializeToXml(keys, KeyContainer.class);
		result.put("signatures.xml", new ByteArrayBuffer(sigXml.getBytes()));

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


	private void openMyTrustNetworks(URI node) throws Exception {
		try {
			this.ownLeadTnw = new MyTrustNetworkAndKeys(true);

		} catch (Exception e) {
			logger.info("Did not open Own Lead Trust Network");

		}
		try {
			this.ownModTnw = new MyTrustNetworkAndKeys(false);

		} catch (Exception e) {
			logger.info("Did not open Own Moderator Trust Network");

		}
		openingOwnLead = ownLeadTnw!=null && node.toString().equals(
				ownLeadTnw.getTrustNetwork()
						.getNodeInformation().getStaticNodeUrl0().toString());

		openingOwnMod = ownModTnw!=null && node.toString().equals(
				ownModTnw.getTrustNetwork().getNodeInformation()
						.getStaticNodeUrl0().toString());

		if (amILead){
			if (ownLeadTnw!=null){
				ownTrustNetwork = ownLeadTnw.getTrustNetwork();
			}
		} else {
			if (ownModTnw!=null){
				ownTrustNetwork = ownModTnw.getTrustNetwork();
			}
		}
		logger.info("Opening Own Lead / Mod=" +openingOwnLead + "/" + openingOwnMod);

	}

	public static TrustNetwork openMyHostTrustNetwork(String name) throws Exception {
		MyTrustNetworkAndKeys mtn = new MyTrustNetworkAndKeys(false);
		return mtn.getTrustNetwork();
	}

	private void verification() throws Exception {
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
			
			verifyPublicKey(this.targetTrustNetwork.getNodeInformation().getNodeUid());
			contents.clear();
			
		} catch (Exception e) {
			throw e; 
			
		}
	}

	private boolean verifyPublicKey(URI nodeUid) throws Exception {
		// if network map item exists, use that -- 9/3/23
		// otherwise try the source, if and only if the NMIS exists
		// otherwise build NMI

		if (this.ownTrustNetwork !=null) {
			NetworkParticipant ownRecord = new TrustNetworkWrapper(ownTrustNetwork)
					.getParticipant(nodeUid);

			if (ownRecord!=null) {
				String k = Base64.encodeBase64String(ownRecord.getPublicKey().getPublicKey());
				if (k.equals(keyCheck)) {
					logger.info("Verified there was no change in Public Key");
					return true;
					
				} else {
					throw new SecurityException("Rulebook Node invalid - Public Key has changed");
					
				}
			} else {
				logger.warn("Participant is unknown - this should only be seen during the addition of Nodes to the Network");
				return false;
				
			}
		} else {
			logger.info("Node is being established - no public keys are known");
			return false; 
			
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

	public static void main(String[] args) throws Exception {

		String uid = "urn:rulebook:sybil:the-cyber:thirty-test:2c859cff31d5889ab75027713926056323e6aeebe0fbee6bd126aae12713257c:61c3045c:i";
		new UIDHelper(uid).out();




	}

}
