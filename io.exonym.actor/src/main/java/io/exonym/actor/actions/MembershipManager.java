package io.exonym.actor.actions;

import com.sun.xml.ws.util.ByteArrayBuffer;
import eu.abc4trust.xml.*;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.abc.util.UidType;
import io.exonym.actor.VerifiedClaim;
import io.exonym.helpers.BuildIssuancePolicy;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.helpers.BuildCredentialSpecification;
import io.exonym.lite.pojo.Namespace;
import io.exonym.lite.pojo.Rulebook;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.Const;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.PassStore;
import io.exonym.uri.NamespaceMngt;
import io.exonym.utils.storage.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.net.URI;
import java.rmi.server.UID;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// TODO This needs a rewrite specifically as a Sybil Onboarding process.  It's awful.
public class MembershipManager {
	
	private static final Logger logger = LogManager.getLogger(MembershipManager.class);

	private final ConcurrentHashMap<String, SybilIssuanceData> contextToIssuanceData = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, RecipientData> contextToRecipientData = new ConcurrentHashMap<>();
	private final String username;
	private final NodeManager nodeManager;
	private final TrustNetwork trustNetwork;
	private final NodeInformation info;
	private final URI nodeUrl;
	private final URI lastIssuerUid;
	private final IdContainerJSON xIssuer;

	private ExonymIssuer issuer = null;
	private SybilIssuanceData issuanceData = null;

	private CredentialSpecification sybilC = null;

	//
	// This appears to only be used for Sybil onboarding.
	// I'm somewhat confused and it appears to be an artifact
	// left over from when the SSI containers were hosted.
	//
	public MembershipManager(String networkName) throws Exception {
		nodeManager = new NodeManager(networkName);
		trustNetwork = nodeManager.openMyTrustNetwork(false);
		info = trustNetwork.getNodeInformation();
		if (info.getNodeName()==null) {
			throw new NullPointerException("Node Name");
			
		}
		username = info.getNodeName();
		xIssuer = initializeContainer(username);
		nodeUrl = info.getStaticNodeUrl0();
		LinkedList<URI> ll = new LinkedList<>(info.getIssuerParameterUids());
		lastIssuerUid = ll.getLast();

	}
	
	protected IdContainerJSON initializeContainer(String username) throws Exception {
		try {
			return new IdContainerJSON(username);
			
		} catch (Exception e) {
			throw new Exception("The core node container did not exist. " + username);

		}
	}


	public IssuanceMessageAndBoolean ssiIssuerInit(String context, String sybilClass, PassStore store) throws Exception {
		logger.debug("Calling ssiIssuerInit");

		if (issuer == null){
			logger.debug("Calling ssiIssuerInit - it was null");
			issuer = new ExonymIssuer(xIssuer);
			issuer.openContainer(store.getDecipher());
			sybilC = xIssuer.openResource(URI.create(Rulebook.SYBIL_RULEBOOK_UID_TEST + ":c"));

		}
		issuanceData = new SybilIssuanceData(context, sybilClass, sybilC);
		issuanceData.setStore(store);
		this.contextToIssuanceData.put(context, issuanceData);
		return issuer.issueInit(issuanceData.claim, issuanceData.ip,
				store.getEncrypt(), URI.create(context.toString()));

	}

	public ImabAndHandle ssiIssuerStep(String context, IssuanceMessage im) throws Exception {
		if (context==null){
			throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, "No Context defined");
		}
		SybilIssuanceData issuanceData = this.contextToIssuanceData.get(context);
		if (issuanceData!=null){
			return issuer.issueStep(im, issuanceData.getStore().getEncrypt());

		} else {
			throw new UxException(ErrorMessages.CONTEXT_NOT_FOUND);

		}
	}

	public UUID ssiRecipientInit(NodeVerifier v, CredentialSpecification cs, PassStore store) throws Exception {
		ExonymOwner owner = new ExonymOwner(xIssuer);
		owner.openContainer(store);
		URI iuid = v.getTargetTrustNetwork().getNodeInformation()
					.getIssuerParameterUids().getLast();
		String raw = IdContainerJSON.stripUidSuffix(iuid, 1);
		URI rap = URI.create(raw + ":ra");
		URI rai = URI.create(raw + ":rai");
		owner.addRevocationInformation(rap,
				v.getRevocationInformation(IdContainerJSON.uidToXmlFileName(rai)));

		owner.addRevocationAuthorityParameters(v.getRevocationAuthorityParameters(
				IdContainerJSON.uidToXmlFileName(rap)));

		owner.addIssuerParameters(v.getIssuerParameters(IdContainerJSON.uidToXmlFileName(iuid)));

		owner.addCredentialSpecification(cs);
		UUID context = UUID.randomUUID();
		RecipientData recipientData = new RecipientData();
		recipientData.setExonymOwner(owner);
		recipientData.setStore(store);
		this.contextToRecipientData.put(context.toString(), recipientData);
		return context;

	}

	public IssuanceMessage ssiRecipientStep(UUID context, IssuanceMessageAndBoolean imab) throws Exception {
		RecipientData recipientData = this.contextToRecipientData.get(context);
		if (recipientData!=null){
			ExonymOwner owner = recipientData.getExonymOwner();
			IssuanceMessage im = owner.issuanceStep(imab, recipientData.getStore().getEncrypt());
			if (imab.isLastMessage()){
				this.contextToRecipientData.remove(context);

			}
			return im;

		} else {
			throw new UxException("Unable to find issuance protocol with context: " + context);

		}
	}

	private BigInteger issue(IdContainerJSON xUser, PassStore nodePassStore, PassStore userPassStore) throws Exception {
		URI iUid = lastIssuerUid;
		UIDHelper helper = new UIDHelper(iUid);

		CredentialSpecification credSpec = xIssuer.openResource(helper.getCredentialSpecFileName());
		IssuancePolicy ip = xIssuer.openResource(helper.getIssuancePolicyFileName());
		IssuerParameters i = xIssuer.openResource(helper.getIssuerParametersFileName());
		RevocationAuthorityParameters ra = xIssuer.openResource(helper.getRevocationAuthorityFileName());
		RevocationInformation rai = xIssuer.openResource(helper.getRevocationInformationFileName());
		
		logger.info("Defining Main Container");
		ExonymOwner owner = new ExonymOwner(xUser);
		owner.openContainer(userPassStore);
		owner.addCredentialSpecification(credSpec);
		owner.addIssuerParameters(i);
		owner.addRevocationAuthorityParameters(ra);
		owner.addRevocationInformation(helper.getRevocationInfoParams(), rai);
		logger.info("Defined Container");
		
		VerifiedClaim claim = new VerifiedClaim(credSpec);
		
		issuer = new ExonymIssuer(xIssuer);
		issuer.openContainer(nodePassStore.getDecipher());
		
		// ISSUER - INIT (Step 1)
		IssuanceMessageAndBoolean imab = issuer.issueInit(claim, ip, nodePassStore.getEncrypt(), URI.create("ctx"));

		// 			OWNER - Step A
		IssuanceMessage im = owner.issuanceStep(imab, userPassStore.getEncrypt());

		// ISSUER - Step (Step 2)
		ImabAndHandle handle = issuer.issueStep(im, nodePassStore.getEncrypt());

		// 			OWNER - Step B
		owner.issuanceStep(handle.getImab(), userPassStore.getEncrypt());

		if (!handle.getImab().isLastMessage()) {
			throw new Exception();
			
		}
		return handle.getHandle();

	}


	public String discoverRevocationHandle(PresentationToken token, PassStore store) throws Exception {
		ExonymInspector ins = new ExonymInspector(this.xIssuer);
		loadAvailableInspectorParameters(token, this.xIssuer, ins, store);
		
		ArrayList<Attribute> atts = (ArrayList<Attribute>) ins.inspect(token);
		HashMap<URI, Attribute> map = new HashMap<URI, Attribute>();
		logger.info("There were " + atts.size() + " attribute(s) produced as a result of the inspection");
		for (Attribute a : atts) {
			map.put(a.getAttributeDescription().getType(), a);

		}
		Attribute a = map.get(BuildCredentialSpecification.REVOCATION_HANDLE_UID);
		return "" + a.getAttributeValue();

	}

	private void loadAvailableInspectorParameters(PresentationToken token, IdContainerJSON x, ExonymInspector ins, PassStore store) throws Exception {
		if (x==null){
			throw new Exception();

		} if (store == null){
			throw new Exception();

		}
		for (CredentialInToken cit : token.getPresentationTokenDescription().getCredential()) {
			URI cUid = cit.getCredentialSpecUID();
			URI iUid = cit.getIssuerParametersUID();
			URI raUid = URI.create(NamespaceMngt.URN_PREFIX_COLON + IdContainerJSON.stripUidSuffix(iUid.toString(), 1) + ":ra");
			
			ins.openResourceIfNotLoaded(cUid);
			ins.openResourceIfNotLoaded(iUid);
			ins.openResourceIfNotLoaded(raUid);
			
			for (AttributeInToken ait : cit.getDisclosedAttribute()) {
				URI insUid = ait.getInspectorPublicKeyUID();
				URI inssUid = URI.create(insUid.toString() + "s");

				try {
					logger.info("About to open inss resource"  +x + " " + inssUid + " " + store);
					SecretKey sk = x.openResource(inssUid, store.getDecipher());
					ins.addInspectorSecretKey(insUid, sk);
					ins.openResourceIfNotLoaded(insUid);
					
				} catch (Exception e) {
					URI modUid = UIDHelper.computeModUidFromMaterialUID(insUid);
					String modName = UIDHelper.computeModNameFromModUid(modUid);
					String leadName = UIDHelper.computeLeadNameFromModOrLeadUid(modUid);
					throw new UxException("Moderated by: "
							+ leadName.toUpperCase()
							+ "~" + modName.toUpperCase(), e);

				}
			}
		}
	}

	public String revokeMember(PresentationToken token, PassStore store) throws Exception {
		if (token!=null) {
			PresentationTokenDescription ptd = token.getPresentationTokenDescription();
			
			if (ptd!=null) {
				CredentialInToken cit = ptd.getCredential().get(1);
				
				if (cit!=null) {
					String ra = IdContainerJSON.stripUidSuffix(cit.getIssuerParametersUID(), 1);
					URI raUid = URI.create(NamespaceMngt.URN_PREFIX_COLON + ra + ":ra");
					BigInteger handle = new BigInteger(discoverRevocationHandle(token, store));
					issuer = new ExonymIssuer(xIssuer);
					issuer.openContainer(store.getDecipher());
					logger.info("----------- Revocation Request for RA UID");
					logger.info("raUid: "   + raUid);
					logger.info("-----------");
					RevocationInformation ri = issuer.revokeCredential(raUid, handle, store.getDecipher());
					issuer.clearStale();
					return publishedRevocationData(ri, store);
					
				} else {
					throw new UxException("Credential In Token was missing");
					
				}
			} else {
				throw new UxException("Token Description was missing");	
				
			}
		} else {
			throw new UxException("No Token Provided");
			
		}
	}
	
	protected String publishedRevocationData(RevocationInformation ri, PassStore store) throws Exception {
		URI raUid = ri.getRevocationAuthorityParametersUID();
		String root = NamespaceMngt.URN_PREFIX_COLON + IdContainerJSON.stripUidSuffix(raUid, 2);
		URI raiUid = URI.create(root + ":rai");
		
		TrustNetwork tn = nodeManager.openMyTrustNetwork(false);

		String riString = IdContainerJSON.convertObjectToXml(ri);
		String raiHash = CryptoUtils.computeSha256HashAsHex(riString);
		String niString = JaxbHelper.serializeToXml(tn, TrustNetwork.class);

		byte[] riSign = NodeVerifier.stripStringToSign(riString).getBytes();
		byte[] niSign = NodeVerifier.stripStringToSign(niString).getBytes();

		byte[] rai = riString.getBytes();
		byte[] ni = niString.getBytes();

		KeyContainerWrapper kcPublic = nodeManager.openSignaturesContainer(nodeUrl);
		KeyContainerWrapper kcPrivate = new KeyContainerWrapper(
				(KeyContainer) xIssuer.openResource(Const.KEYS));
		AsymStoreKey key = nodeManager.openKey(
				kcPrivate.getKey(KeyContainerWrapper.TN_ROOT_KEY), store);
		
		HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();
		toSign.put(raiUid, new ByteArrayBuffer(riSign));
		toSign.put(tn.getNodeInformationUid(), new ByteArrayBuffer(niSign));
		
		nodeManager.signatureUpdateXml(key, toSign, kcPublic, nodeUrl);
		
		String xml = JaxbHelper.serializeToXml(kcPublic.getKeyContainer(), KeyContainer.class);

		nodeManager.publish(nodeUrl, xml.getBytes(), Const.SIGNATURES_XML);
		nodeManager.publish(nodeUrl, rai, IdContainerJSON.uidToXmlFileName(raiUid));
		nodeManager.publish(nodeUrl, ni, IdContainerJSON.uidToXmlFileName(tn.getNodeInformationUid()));
		return raiHash;
		
	}

	// TODO after web container
	public void updateMember() throws Exception {
		throw new Exception();
		
	}

	private class RecipientData {

		private ExonymOwner exonymOwner = null;
		private PassStore store = null;

		public ExonymOwner getExonymOwner() {
			return exonymOwner;
		}

		public void setExonymOwner(ExonymOwner exonymOwner) {
			this.exonymOwner = exonymOwner;
		}

		public PassStore getStore() {
			return store;
		}

		public void setStore(PassStore store) {
			this.store = store;
		}
	}

	private class SybilIssuanceData {

		private final URI iUid;
		private final IssuancePolicy ip;
		private final VerifiedClaim claim;
		private final String contextId;
		private PassStore store = null;

		public SybilIssuanceData(String contextId, String sybilClass, CredentialSpecification sybilC) throws Exception {
			this.contextId = contextId;
			iUid = lastIssuerUid;
			UIDHelper helper = new UIDHelper(iUid);

			BuildIssuancePolicy bip = new BuildIssuancePolicy(null, helper.getCredentialSpec(), iUid);
			String sybil = Namespace.URN_PREFIX_COLON + "sybil";
			bip.addPseudonym(sybil, true, sybil, "urn:io:exonym");
			ip = bip.getIssuancePolicy();

			claim = new VerifiedClaim(sybilC);
			populateSybilClaim(claim, sybilClass);

		}

		private void populateSybilClaim(VerifiedClaim claim, String sybilClass) {
			if (sybilClass!=null){
				HashMap<URI, Object> map = claim.getLabelValuesMap();
				map.put(URI.create(Rulebook.SYBIL_CLASS_TYPE), sybilClass);

			}
		}

		public PassStore getStore() {
			return store;
		}

		public void setStore(PassStore store) {
			this.store = store;
		}

	}
}
