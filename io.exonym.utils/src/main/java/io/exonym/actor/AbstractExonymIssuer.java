/*
 * Copyright (c) 2023. All Rights Reserved. Exonym GmbH
 */

package io.exonym.actor;

import com.ibm.zurich.idmix.abc4trust.facades.IssuanceMessageFacade;
import com.ibm.zurich.idmix.abc4trust.facades.RevocationAuthorityParametersFacade;
import com.ibm.zurich.idmix.abc4trust.facades.RevocationInformationFacade;
import com.ibm.zurich.idmix.abc4trust.facades.SecretKeyFacade;
import com.ibm.zurich.idmx.interfaces.cryptoEngine.CryptoEngineRevocationAuthority;
import com.ibm.zurich.idmx.interfaces.util.BigInt;
import com.ibm.zurich.idmx.jaxb.JaxbHelperClass;
import com.ibm.zurich.idmx.keypair.ra.RevocationAuthorityKeyPairWrapper;
import com.ibm.zurich.idmx.parameters.ra.RevocationAuthorityPublicKeyTemplateWrapper;
import com.ibm.zurich.idmx.util.bigInt.BigIntFactoryImpl;
import com.sun.xml.bind.JAXBObject;
import eu.abc4trust.abce.internal.user.credentialManager.CredentialManagerException;
import eu.abc4trust.cryptoEngine.issuer.CryptoEngineIssuer;
import eu.abc4trust.returnTypes.IssuerParametersAndSecretKey;
import eu.abc4trust.xml.*;
import io.exonym.abc.util.FileType;
import io.exonym.abc.util.UidType;
import io.exonym.exceptions.ClaimNotCompleteException;
import io.exonym.helpers.BuildIssuancePolicy;
import io.exonym.helpers.UIDHelper;
import io.exonym.idmx.managers.KeyManagerExonym;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.Namespace;
import io.exonym.lite.time.DateHelper;
import io.exonym.utils.ExtractObject;
import io.exonym.utils.storage.AbstractIdContainer;
import io.exonym.utils.storage.ImabAndHandle;
import io.exonym.utils.storage.IdContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.net.URI;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractExonymIssuer extends AbstractBaseActor {


	private static final Logger logger = LogManager.getLogger(AbstractExonymIssuer.class);

	private final ConcurrentHashMap<URI, URI> contextToRaUid = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<URI, URI> contextToDatastoreUid = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<URI, IssuancePolicy> contextToIssuancePolicy = new ConcurrentHashMap<>();

	private final eu.abc4trust.abce.internal.revocation.credentialManager.CredentialManager credentialManagerRa;
	private final eu.abc4trust.abce.internal.issuer.credentialManager.CredentialManager credentialManagerIssuer;
	
	private final CryptoEngineIssuer cryptoEngineIssuer;
	private final CryptoEngineRevocationAuthority cryptoEngineRaIdmx;

	private BigInteger revocationHandle = null;

	private boolean open = false;	
	
	protected AbstractExonymIssuer(AbstractIdContainer container) throws Exception {
		super(container);


		cryptoEngineIssuer = INJECTOR.providesCryptoEngineIssuerAbc();

		credentialManagerIssuer = INJECTOR.providesCredentialManagerIssuer();

		credentialManagerRa = INJECTOR.providesCredentialManagerRevocation();
		cryptoEngineRaIdmx = INJECTOR.providesCryptoEngineRevocationAuthority();
		cryptoEngineVerifier= INJECTOR.providesCryptoEngineVerifierAbc();
		initSystemParameters();
		
	}
	
	protected boolean openResourceIfNotLoaded(URI uid) throws Exception {
		if (!super.openResourceIfNotLoaded(uid)) {
			if (UidType.isRevocationHistory(uid)) {
				loadRevocationHistoryIf(uid);

			} else {
				logger.warn("No UID found in Actor superclass " + uid + " searching lower levels");
				
			}
		}
		return true; 
		
	}
	
	private void loadRevocationHistoryIf(URI uid) throws Exception {
		if (this.credentialManagerRa.getRevocationHistory(uid)==null){
			RevocationHistory rh = container.openResource(uid);
			addRevocationHistory(rh);
			
		}
	}

	public static void removeRevocationHistory(RevocationInformation ri){
		RevocationInformationFacade rif = new RevocationInformationFacade(ri);
		RevocationHistory rh = rif.getRevocationHistory();
		rh.getRevocationLogEntry().clear();

	}

	/**
	 * Call to start any type of credential issuance
	 * 
	 * @param claim The claim to verify
	 * @param policy the issuance policy enforced on the recipient
	 * @return
	 * @throws Exception
	 */
	protected IssuanceMessageAndBoolean issueInit(VerifiedClaim claim, 
			IssuancePolicy policy, Cipher enc, URI context) throws Exception {
		return issueInit(claim, policy, null, enc, context);
		
	}
	
	/**
	 * Call to start any type of credential issuance
	 * 
	 * @param claim The claim to verify
	 * @param policy The issuance policy enforced on the recipient
	 * @param internalDataStoreUid The uid to refer to issuance log data, not setting it is equivalent to requesting no log 
	 * @return
	 * @throws Exception
	 */
	protected IssuanceMessageAndBoolean issueInit(VerifiedClaim claim, IssuancePolicy policy,  
			URI internalDataStoreUid, Cipher enc, URI context) throws Exception {
		try {
			if (!claim.isComplete()){
				throw new ClaimNotCompleteException();
				
			} if (policy.getCredentialTemplate()==null){
				throw new HubException("No credential template in policy");
				
			}
			this.addCredentialSpecification(claim.getCredSpec());
			URI issuerUID = policy.getCredentialTemplate().getIssuerParametersUID();
			IssuerParameters params = keyManager.getIssuerParameters(issuerUID);
			if (params==null){
				params = this.externalResource.openResource(issuerUID);
				if (params!=null){
					UIDHelper helper = new UIDHelper(issuerUID);
					RevocationAuthorityParameters rap = this.externalResource.openResource(
							helper.getRevocationAuthorityFileName());

					RevocationInformation rai = this.externalResource.openResource(
							helper.getRevocationInformationFileName());

					this.addRevocationInformation(rai.getRevocationAuthorityParametersUID(), rai);
					this.addRevocationAuthorityParameters(rap);
					this.addIssuerParameters(params);
					this.addCredentialSpecification(
							this.externalResource.openResource(helper.getCredentialSpecFileName()));

					this.addInspectorParameters(
							this.externalResource.openResource(helper.getInspectorParamsFileName()));

				} else {
					throw new UxException(ErrorMessages.INCORRECT_PARAMETERS,
							"Unable to find parameters even as external resource.", issuerUID.toString());
				}
			}
			URI raUid = setUpRevocableCredentialParams(params.getRevocationParametersUID());
			IssuanceMessageAndBoolean result = cryptoEngineIssuer.initIssuanceProtocol(policy, claim.commitToDefinedAttributes(), context);
			
			if (result.isLastMessage() && internalDataStoreUid!=null){
				AttributeList al = ExtractObject.extract(result.getIssuanceMessage().getContent(), AttributeList.class);
				createSimpleIssuanceToken(policy, internalDataStoreUid, al, enc);
				
			} else {
				mapContextForSubsequentSteps(result.getIssuanceMessage().getContext(), 
						raUid, internalDataStoreUid, policy);
				
			}
			return result;
			
		} catch (NullPointerException e) {
			throw new HubException("Have you called openContainer() on the ExonymActor?", e);
			
		} catch (Exception e) {
			throw e;
			
		}
	}

	private void createSimpleIssuanceToken(IssuancePolicy policy, URI internalDataStoreUid, AttributeList al, Cipher enc) throws Exception {
		IssuanceToken it = of.createIssuanceToken();
		IssuanceTokenDescription itd = of.createIssuanceTokenDescription();
		PresentationTokenDescription ptd = of.createPresentationTokenDescription();
		CryptoParams params = of.createCryptoParams();

		it.setIssuanceTokenDescription(itd);
		it.setCryptoEvidence(params);
		
		it.getIssuanceTokenDescription().setCredentialTemplate(policy.getCredentialTemplate());
		itd.setPresentationTokenDescription(ptd);
		
		ptd.setMessage(policy.getPresentationPolicy().getMessage());
		ptd.setPolicyUID(policy.getPresentationPolicy().getPolicyUID());
		
		itd.setPresentationTokenDescription(ptd);
		
		params.getContent().add(of.createAttributeList(al));
		this.container.saveIssuanceToken(it, IdContainer.uidToXmlFileName(internalDataStoreUid), enc);
		
	}

	/*
	 * 
	 */
	private URI setUpRevocableCredentialParams(URI revocationParametersUID) throws Exception {
		if (revocationParametersUID!=null){
			openResourceIfNotLoaded(revocationParametersUID);
			RevocationInformation ri = cryptoEngineRaIdmx.updateRevocationInformation(revocationParametersUID, null, null);
			this.keyManager.storeRevocationInformation(revocationParametersUID, ri);

			// This doesn't appear to do anything; but it really does.
			// Issuance pukes without it.  Needs further investigation.
			URI rhUid = Namespace.extendUid(revocationParametersUID, "history");
			String rhFileName = IdContainer.stripUidSuffix(revocationParametersUID, 2) + ":rh";
			try {
				if (this.credentialManagerRa.getRevocationHistory(rhUid)==null){
					RevocationHistory rh = container.openResource(
							IdContainer.uidToXmlFileName(URI.create(rhFileName)));

					this.credentialManagerRa.storeRevocationHistory(rh.getRevocationHistoryUID(), rh);

				}
			} catch (Exception e) {
				logger.info("Creating new revocation history. (Handled Exception):" + e.getMessage());
				RevocationHistory rh = of.createRevocationHistory();
				rh.setRevocationAuthorityParametersUID(revocationParametersUID);
				rh.setRevocationHistoryUID(rhUid);
				this.credentialManagerRa.storeRevocationHistory(rhUid, rh);

			}
		}
		return revocationParametersUID;
		
	}

	/*
	 * 
	 */
	private void mapContextForSubsequentSteps(URI context, URI raUid, 
			URI storeDatabaseUid, IssuancePolicy pp){
		if (raUid!=null){ 
			this.contextToRaUid.put(context, raUid);
			
		}
		if (storeDatabaseUid!=null){
			this.contextToDatastoreUid.put(context, storeDatabaseUid);
			
		}
		logger.debug("Mapping Context=" + context + " " + this);
		this.contextToIssuancePolicy.put(context, pp);
		
	}

	/**
	 * Called after the credential issuance has been started in a complex issuance process.
	 * 
	 * @param im
	 * @return
	 * @throws Exception
	 */
	public ImabAndHandle issueStep(IssuanceMessage im, Cipher enc) throws Exception{
		if (im!=null){
			IssuanceMessageFacade imf = new IssuanceMessageFacade(im, this.bigIntFactory);
			IssuanceToken token = imf.getIssuanceToken();
			URI issuerUid = token.getIssuanceTokenDescription()
					.getCredentialTemplate().getIssuerParametersUID();

			IssuanceTokenAndIssuancePolicy itap = cryptoEngineIssuer.extractIssuanceTokenAndPolicy(im);
			checkTokenMatchesPolicy(im.getContext(), itap);

			IssuanceMessageAndBoolean imab = cryptoEngineIssuer.issuanceProtocolStep(im);

			AttributeList al = ExtractObject.extract(imab.getIssuanceMessage().getContent(), AttributeList.class);
			List<Attribute> atts = al.getAttributes();
			this.revocationHandle = extractHandle(atts);

			logger.debug("Extracted UID " + atts.get(0).getAttributeDescription().getType());
			for (Attribute att : atts){
				logger.debug(att.getAttributeDescription().getType());
				logger.debug(att.getAttributeValue());
				logger.debug(att.getAttributeUID());

			}
			organizeLocalData(im.getContext(), itap, al, enc);
			ImabAndHandle result = new ImabAndHandle();
			result.setHandle(revocationHandle);
			result.setImab(imab);
			result.setIssuerUID(issuerUid);
			return result;
			
		} else {
			throw new Exception("Issuance Message was null");
			
		}
	}

	public List<Attribute> extractAttributes(IssuanceMessageAndBoolean imab){
		AttributeList al = ExtractObject.extract(imab.getIssuanceMessage().getContent(), AttributeList.class);
		return al.getAttributes();

	}

	public BigInteger extractHandle(IssuanceMessageAndBoolean imab){
		return extractHandle(extractAttributes(imab));
	}

	public BigInteger extractHandle(List<Attribute> attList){
		return (BigInteger) attList.get(0).getAttributeValue();

	}

	private void checkTokenMatchesPolicy(URI context, IssuanceTokenAndIssuancePolicy itap) throws Exception {
		logger.debug(">>> Context=" + context + " " + contextToIssuancePolicy.size() + " " + this);
		checkIssuancePolicy(itap.getIssuancePolicy(), contextToIssuancePolicy.get(context));
		itap.getIssuancePolicy().getPresentationPolicy();
		itap.getIssuanceToken().getIssuanceTokenDescription();
		contextToIssuancePolicy.remove(context);
		RequestFulfilled.issuancePolicySatisfied(itap);
		
	}

	private void checkIssuancePolicy(IssuancePolicy policyRequired, IssuancePolicy policyProvided) throws Exception {
		if (policyProvided!=null){
			String received = JaxbHelperClass.serialize(new ObjectFactory().createIssuancePolicy(policyRequired));
			String expected = JaxbHelperClass.serialize(new ObjectFactory().createIssuancePolicy(policyProvided));
			
			if (!received.equals(expected)){
				throw new UxException(ErrorMessages.PROOF_IS_OUT_OF_SCOPE + ":The Policy fulfilled does not match the provided Issuance Policy.");
				
			}
		} else {
			throw new RuntimeException("Could not find policy from context.");
			
		}
	}

	private void organizeLocalData(URI context, 
			IssuanceTokenAndIssuancePolicy itap, 
			AttributeList attList, 
			Cipher enc) throws Exception {
		
		URI storeUid = contextToDatastoreUid.get(context);
		contextToDatastoreUid.remove(context);
		contextToRaUid.remove(context);

		URI raUid = contextToRaUid.get(context);
		URI historyUid = null; RevocationHistory rh = null; RevocationLogEntry rle = null;
		
		if (raUid!=null){
			historyUid = Namespace.extendUid(raUid, "history");
			rh = this.credentialManagerRa.getRevocationHistory(historyUid);
			rle = processRevocationHistory(rh);

		}
		if (itap!=null && itap.getIssuanceToken() !=null){
			IssuanceToken token = itap.getIssuanceToken();
			List<Object> tokenCryptoParams = token.getCryptoEvidence().getContent();
			tokenCryptoParams.add(of.createAttributeList(attList));
			
			if (rle!=null){
				RevocationHistory historyLogParent = of.createRevocationHistory();
				historyLogParent.setRevocationHistoryUID(historyUid);
				historyLogParent.setRevocationAuthorityParametersUID(raUid);
				historyLogParent.getRevocationLogEntry().add(rle);
				tokenCryptoParams.add(of.createRevocationHistory(historyLogParent));
				
			}
			if (storeUid!=null){
				this.container.saveIssuanceToken(token, IdContainer.uidToFileName(storeUid), enc);
				
			}
		} else {
			throw new RuntimeException("Unexpected case:  No issuance token on Complex Issuance, step 2.");
			
		}
	}

	private RevocationLogEntry processRevocationHistory(RevocationHistory rh) throws Exception {
		RevocationLogEntry rle = null; 
		if (rh != null){
			rle = rh.getRevocationLogEntry().get(0);
			GregorianCalendar gc = DateHelper.getCurrentUtcTime();
			rle.setDateCreated(gc);
			this.container.saveLocalResource(rh, true);
			RevocationLogEntry rle0 = of.createRevocationLogEntry();
			rle0.setDateCreated(gc);
			rle0.setRevocationLogEntryUID(rle.getRevocationLogEntryUID());
			rle0.getRevocableAttribute().addAll(rle.getRevocableAttribute());
			rle0.setRevoked(false);
			rle = rle0;

		} 
		return rle;
		
	}

	/**
	 * Define the necessary Cryptomaterials to issue Issuer Driven Credentials.
	 * 
	 * @param credential
	 * @throws Exception
	 */
	protected void setupAsCredentialIssuer(URI credential, URI issuer, Cipher enc) throws Exception { 
		setupAsCredentialIssuer(credential, issuer, null, enc); 
	
	}

	/**
	 * Define the necessary Cryptomaterials to issue Issuer Driven Credentials.
	 * 
	 * @param credential
	 * @param revocationAuthorityUid
	 * @throws Exception
	 */
	protected void setupAsCredentialIssuer(URI credential, URI issuerParamsUid, /*Nullable*/ URI revocationAuthorityUid, Cipher enc) throws Exception { 
		try {
			openResourceIfNotLoaded(credential);
			CredentialSpecification spec = this.keyManager.getCredentialSpecification(credential);
			if (spec.isRevocable() && revocationAuthorityUid==null){
				throw new UxException("A Revocable Credential must specify a Revocation Authority");
				
			}
			validateIssuerParams(credential, issuerParamsUid);
			int attributes = spec.getAttributeDescriptions().getAttributeDescription().size();
			
			ArrayList<FriendlyDescription> fd = new ArrayList<>();
			IssuerParametersAndSecretKey ipsk = 
					cryptoEngineIssuer.setupIssuerParameters(keyManager.getSystemParameters(), 
														attributes, URI.create("cl"), 
														issuerParamsUid, revocationAuthorityUid, fd);

			this.keyManager.storeIssuerParameters(
					ipsk.issuerParameters.getParametersUID(), ipsk.issuerParameters);
			this.addIssuerSecretKey(ipsk.issuerSecretKey);
			
			BuildIssuancePolicy bip = new BuildIssuancePolicy(null, credential, issuerParamsUid);
			if (revocationAuthorityUid!=null){
				bip.addPseudonym(IdContainer.stripUidSuffix(revocationAuthorityUid, 2),
										false, "ra", null);
				
				if (spec.isKeyBinding()){
					bip.getIssuancePolicy().getCredentialTemplate()
						.setSameKeyBindingAs(URI.create("ra"));
					
				}
			} else if (spec.isKeyBinding()){
				bip.addPseudonym(credential.toString(), false, "nym", null);
				bip.getIssuancePolicy().getCredentialTemplate()
					.setSameKeyBindingAs(URI.create("nym"));
				
			}
			container.saveLocalResource(ipsk.issuerParameters);
			container.saveLocalResource(ipsk.issuerSecretKey, enc);
			
			try {
				container.saveLocalResource(bip.getIssuancePolicy());
				
			} catch (Exception e) {
				logger.debug("File already existed " + e.getMessage());

			}
			try {
				container.saveLocalResource(spec);
				
			} catch (Exception e) {
				logger.debug("File already existed " + e.getMessage());

			}
		} catch (Exception e) {
			throw e;
		
		}
	}

	private void validateIssuerParams(URI credential, URI issuerParamsUid) throws Exception {
		String px = UIDHelper.computeRulebookHashUid(credential);

		HubException h = new HubException("The issuer parameters (" + issuerParamsUid + ") must contain the rulebookID "
				+ "parameters URN without the :c and suffixed instead with a public Issuer ID and :i"
				+ " So, urn:source:advocate:rulebookID:issuer:i");

		try {
			if (!issuerParamsUid.toString().contains(px)){
				throw h;
				
			} else if (!FileType.isIssuerParameters(IdContainer.uidToXmlFileName(issuerParamsUid))){
				throw h; 
				
			}
		} catch (HubException e) {
			throw e; 
			
		} catch (Exception e) {
			h.initCause(e);
			throw h;
			
		}
	}

	/**
	 * Define the necessary Cryptomaterials to define this issuer as a revocation authority.
	 * 
	 * @param issuerParametersUid
	 * @throws Exception
	 */
	protected URI setupAsRevocationAuthority(URI issuerParametersUid, Cipher enc) throws Exception{
	    try {
	    	if (!FileType.isIssuerParameters(IdContainer.uidToXmlFileName(issuerParametersUid))){
	    		throw new UxException("Expected an Issuer Parameter UID " + issuerParametersUid);
	    		
	    	}
	    	String uid = issuerParametersUid.toString();
	    	uid = uid.substring(0, uid.length()-2) + ":ra";
	    	URI revocationUid = URI.create(uid);
	    	
	    	KeyPair raKeyPair = generateRevocationAuthorityKeyPair(revocationUid);

	    	RevocationAuthorityKeyPairWrapper raKeyPairWrapper = new RevocationAuthorityKeyPairWrapper(raKeyPair);
	        
	        RevocationAuthorityParametersFacade raParametersFacade = 
	        		RevocationAuthorityParametersFacade.initRevocationAuthorityParameters(
	        															raKeyPairWrapper.getKeyPair().getPublicKey());
	        
	        RevocationAuthorityParameters rap = raParametersFacade.getRevocationAuthorityParameters();

	        PrivateKey key = raKeyPair.getPrivateKey();
	        addRevocationAuthorityParameters(rap);
	        addRevocationAuthorityKey(rap.getParametersUID(), key);
	        
			RevocationInformation ri = cryptoEngineRaIdmx.updateRevocationInformation(revocationUid, null, null);
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.systemDefault()));
			cal.add(2, 6);
			ri.setExpires(cal);
	        
	        container.saveLocalResource(rap);
	        container.saveLocalResource(key, enc);
	        container.saveLocalResource(ri);
	        return rap.getParametersUID();
	        
	    } catch (Exception e){
	    	throw e;
	    	
	    }
	}

	protected RevocationInformation revocationBulkValidHandles(URI raUid, ArrayList<BigInteger> handles, Cipher dec) throws Exception{
		URI revocationInfo = null;

		for (BigInteger handle: handles){
			BigInt handle0 = bigIntFactory.valueOf(handle);
			revocationInfo = this.cryptoEngineRaIdmx.revoke(raUid, handle0);

		}
		logger.info(revocationInfo);
		RevocationInformation ri = this.keyManager.getRevocationInformation(raUid, revocationInfo);

		logger.info("Revoked to new " + revocationInfo);
		return ri;

	}
	
	/**
	 * Revoke an issued credential.
	 * 
	 * @param raUid
	 * @param handle
	 * @throws Exception
	 */
	protected RevocationInformation revokeCredential(URI raUid, BigInteger handle, Cipher dec) throws Exception{
		// Note: the cipher is not passed to anywhere.  It is assumed that the ras was open
		// with the container
		
		BigInt handle0 = bigIntFactory.valueOf(handle);
		URI revocationInfo = this.cryptoEngineRaIdmx.revoke(raUid, handle0);

		RevocationInformation ri = this.keyManager.getRevocationInformation(raUid, revocationInfo);
		container.saveLocalResource(ri, true);
		logger.info(revocationInfo);
		return ri;
		
	}


	public void clearStale() throws Exception {
		if (this.keyManager instanceof KeyManagerExonym){
			KeyManagerExonym k = (KeyManagerExonym)this.keyManager;
			k.clearStale();
			this.open=false;
			logger.info("Cleared Revocation Information");

		} else {
			throw new Exception("The key manager was not an acceptable class " + this.keyManager);

		}
	}
	
	/**
	 * This looks through the local resources available to the issuer
	 * with this container name and loads all resources.
	 * 
	 * Credential Specifications must be added separately
	 * 
	 */
	protected void openContainer(Cipher dec) throws Exception {
		if (!open){
			try {
				container.updateLists();
				ArrayList<String> issuerParams = container.getIssuerParameterList();
				for (String resource: issuerParams){
					IssuerParameters ip = container.openResource(resource);
					this.addIssuerParameters(ip);
					
				}
				ArrayList<String> secretKeys = container.getIssuerSecretList();
				for (String resource: secretKeys){
					SecretKey sk = container.openResource(resource, dec);
					this.addIssuerSecretKey(sk);
					
				}
				ArrayList<String> revocation = container.getRevocationList();
				Collections.sort(revocation);
				
				URI raUid = null; 
				for (String rp : revocation){
					try {
						Object o = container.openResource(rp);
						if (o instanceof RevocationAuthorityParameters){
							RevocationAuthorityParameters rap = (RevocationAuthorityParameters)o;
							raUid = rap.getParametersUID();
							addRevocationAuthorityParameters(rap);
							
						} else if (o instanceof RevocationInformation){
							RevocationInformation ri = (RevocationInformation)o;
							addRevocationInformation(ri.getRevocationAuthorityParametersUID(), ri);
							
						} else if (o instanceof RevocationHistory){
							addRevocationHistory((RevocationHistory)o);
							
						} else {
							logger.warn("Unexpected file " + rp);
							
						}
					} catch (Exception e) {
						Object o = container.openResource(rp, dec);
						if (o instanceof PrivateKey){
							addRevocationAuthorityKey(raUid, (PrivateKey)o);
							
						} else {
							logger.warn("Unexpected file " + rp);
							
						}
					}
				}
			} catch (Exception e) {
				throw e;
				
			}
			open = true;
		}
	}		
	
	protected void addRevocationHistory(RevocationHistory rh) throws CredentialManagerException {
		if (this.credentialManagerRa.getRevocationHistory(rh.getRevocationHistoryUID())==null){
			this.credentialManagerRa.storeRevocationHistory(rh.getRevocationHistoryUID(), rh);
			// 2017 TODO add revocation information 
			// 201909 - this may already be done in the super class.
			logger.info("Added revocation history " + rh.getRevocationHistoryUID());
			
		}
	}
	
	protected void addRevocationAuthorityKey(URI raUid, PrivateKey sk) throws Exception {
		if (!FileType.isRevocationAuthority(IdContainer.uidToXmlFileName(raUid))){
			throw new RuntimeException("Requires the Revocation Authority Uid");
			
		}
		if (this.credentialManagerRa.getSecretKey(sk.getPublicKeyId())==null){
	        SecretKeyFacade skf = SecretKeyFacade.initSecretKey(sk.getPublicKeyId(), sk);
			this.credentialManagerRa.storeSecretKey(raUid, skf.getSecretKey());
			logger.info("Added key " + sk.getPublicKeyId());
			
		}
	}	
	
	protected void addIssuerSecretKey(SecretKey secretKey){
		try {
			if (credentialManagerIssuer.getIssuerSecretKey(secretKey.getSecretKeyUID())==null){
			    credentialManagerIssuer.storeIssuerSecretKey(secretKey.getSecretKeyUID(), secretKey);
			    logger.info("Add Secret Key - " + secretKey.getSecretKeyUID() + " " + this.credentialManagerIssuer);
				
			}
		} catch (Exception e) {
			logger.error("Error", e);
			
		}
	}
	
	protected KeyPair generateRevocationAuthorityKeyPair(URI revocationUid) throws Exception{
		RevocationAuthorityPublicKeyTemplateWrapper templateWrapper =
			    new RevocationAuthorityPublicKeyTemplateWrapper(
			        cryptoEngineRaIdmx.createRevocationAuthorityPublicKeyTemplate());

	        templateWrapper.setModulusLength(1024);
	        templateWrapper.setPublicKeyPrefix(revocationUid);
	        templateWrapper.setNonRevocationEvidenceReference(URI.create("non:revocation:evidence:reference"));
	        templateWrapper.setNonRevocationEvidenceUpdateReference(URI.create("non:revocation:evidence:update:reference"));
	        templateWrapper.setRevocationInformationReference(URI.create("revocation:information:reference"));  
	    	
	        return cryptoEngineRaIdmx.setupRevocationAuthorityKeyPair(this.getSystemParameters(), 
	        					templateWrapper.getRevocationAuthorityPublicKeyTemplate());//*/
	        
	}

	/**
	 * Only contains a value, after issueStep() has been called.
	 *
	 * @return
	 */
	public BigInteger getRevocationHandle() {
		return revocationHandle;
	}
}
