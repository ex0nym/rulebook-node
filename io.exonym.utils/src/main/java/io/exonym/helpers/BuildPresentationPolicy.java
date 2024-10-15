package io.exonym.helpers;

import eu.abc4trust.smartcard.Base64;
import eu.abc4trust.xml.*;
import eu.abc4trust.xml.AttributeInPolicy.InspectorAlternatives;
import eu.abc4trust.xml.CredentialInPolicy.CredentialSpecAlternatives;
import eu.abc4trust.xml.CredentialInPolicy.IssuerAlternatives;
import eu.abc4trust.xml.CredentialInPolicy.IssuerAlternatives.IssuerParametersUID;
import io.exonym.abc.attributeType.EnumAllowedValues;
import io.exonym.abc.attributeType.MyAttributeValue;
import io.exonym.actor.VerifiedClaim;
import io.exonym.lite.exceptions.UxException;
import io.exonym.uri.UriEncoding;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.utils.storage.ExternalResourceContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BuildPresentationPolicy {
	
	private static final Logger logger = LogManager.getLogger(BuildPresentationPolicy.class);
	
	private static final ObjectFactory of = new ObjectFactory();

	private final PresentationPolicy policy;
	
	private final HashMap<URI, ArrayList<AttributeDescription>> credentialToAcceptableAttributesMap = new HashMap<>();
	private final HashMap<URI, URI> credentialToRootCredentialMap = new HashMap<>();
	private final HashMap<URI, URI> aliasToRootCredentialMap = new HashMap<>();
	private final HashMap<URI, CredentialInPolicy> credentialToCredentialInPolicyMap = new HashMap<>();
	private final ExternalResourceContainer external;
	
	public BuildPresentationPolicy(URI uid, ExternalResourceContainer container) {
		policy = of.createPresentationPolicy();
		this.external = container;
		policy.setPolicyUID(uid);
		
	}

	public void makeInteractive(){
		Message m = of.createMessage();
		m.setNonce(CryptoUtils.generateNonce(8));
		policy.setMessage(m);

	}

	public void makeInteractive(String base64String) throws IOException {
		Message m = of.createMessage();
		m.setNonce(Base64.decode(base64String));
		policy.setMessage(m);

	}

	public void makeNonInteractive(String content){
		Message m = of.createMessage();
		m.setNonce(content.getBytes());
		policy.setMessage(m);

	}

	public void makeNonInteractiveB64(String b64EncodedString){
		Message m = of.createMessage();
		m.setNonce(org.apache.commons.codec.binary.Base64.decodeBase64(b64EncodedString.getBytes(StandardCharsets.UTF_8)));
		policy.setMessage(m);

	}


	public void addAcceptableCredential(CredentialInPolicy cip){
		policy.getCredential().add(cip);

	}
	
	public void addPseudonym(String scope, boolean exclusive){
		addPseudonym(scope, exclusive, null, null);
		
	}

	public void addPseudonym(String scope, boolean exclusive, String alias){
		addPseudonym(scope, exclusive, alias, null);
		
	}
	
	public void addPseudonym(String scope, boolean exclusive, String alias, URI sameBindingAs){
		PseudonymInPolicy nym = new PseudonymInPolicy();
		nym.setExclusive(exclusive);
		if (alias!=null){
			nym.setAlias(URI.create(alias));
			
		}
		nym.setScope(scope);
		nym.setSameKeyBindingAs(sameBindingAs);
		addPseudonym(nym);
		
	}

	public void addPseudonym(PseudonymInPolicy nym){
		this.policy.getPseudonym().add(nym);
		
	}
	
	/**
	 * 
	 * @param issuerUids
	 * @param alias
	 * @param sameKeyBindingAs
	 * @return a list of credential specs that are acceptable.
	 * 
	 * @throws UxException
	 */
	public List<URI> addCredentialInPolicy(List<CredentialSpecification> cSpecs, List<IssuerParametersUID> issuerUids,
										String alias, /*nullable*/ URI sameKeyBindingAs) throws UxException{
		
		if (cSpecs==null || cSpecs.isEmpty()){
			throw new UxException("Credential Specifications are not defined");
			
		} else if (issuerUids == null || issuerUids.isEmpty()){
			throw new UxException("Issuers of credential " + cSpecs.get(0) + " are not defined.");
			
		} if (alias == null){
			throw new RuntimeException("You must set an alias for a set of Credential Specs and Issuers"); 
			
		}
		ArrayList<CredentialSpecification> specs = new ArrayList<>();
		ArrayList<URI> foundUids = new ArrayList<>();
		String notFound = "";

		for (CredentialSpecification spec : cSpecs){
			specs.add(spec);
			foundUids.add(spec.getSpecificationUID());

		}
		if (foundUids.isEmpty()){
			throw new UxException("The credential specifications do not exist.  Create them first. [" + notFound.trim() + "]");

		}
		URI root = foundUids.get(0);
		for (URI uid : foundUids){
			this.credentialToRootCredentialMap.put(uid, root);
			
		}
		CredentialInPolicy cip = of.createCredentialInPolicy();
		CredentialSpecAlternatives csa = of.createCredentialInPolicyCredentialSpecAlternatives();
		IssuerAlternatives ia = of.createCredentialInPolicyIssuerAlternatives();
		
		cip.setCredentialSpecAlternatives(csa);
		cip.setIssuerAlternatives(ia);
		cip.setSameKeyBindingAs(sameKeyBindingAs);
		cip.setAlias(URI.create(alias));
		
		policy.getCredential().add(cip);
		
		ia.getIssuerParametersUID().addAll(issuerUids);
		csa.getCredentialSpecUID().addAll(foundUids);

		List<AttributeDescription> inAll = computeInAll(specs);
		
		this.credentialToAcceptableAttributesMap.put(root, (ArrayList<AttributeDescription>) inAll);
		this.credentialToCredentialInPolicyMap.put(root, cip);
		this.aliasToRootCredentialMap.put(URI.create(alias), root);
		return foundUids;
		
	}
	
	private List<AttributeDescription> computeInAll(List<CredentialSpecification> specs) {
		List<AttributeDescription> inAll = new ArrayList<>();
		List<List<AttributeDescription>> all = new ArrayList<>();
		
		int max = -1, index = 0, maxList = -1;
		for (CredentialSpecification spec : specs){
			List<AttributeDescription> ads = spec.getAttributeDescriptions().getAttributeDescription();
			all.add(ads);
			if (ads.size() > max){
				max = ads.size();
				maxList = index; 
				
			}
			index++;
		}
		List<AttributeDescription> largestList = specs.get(maxList).getAttributeDescriptions().getAttributeDescription();
		for (AttributeDescription ad0 : largestList){
			URI testValue = ad0.getType();
			boolean memberOfAll = true;
			
			for (List<AttributeDescription> l1 : all){
				boolean inList = false;
				
				for (AttributeDescription ad1 : l1){
					 if (ad1.getType().equals(testValue)){
						 inList=true; 
						 break;
						 
					 }
				}
				if (!inList){
					memberOfAll=false;
					break;
					
				}
			}
			if (memberOfAll){
				inAll.add(ad0);
				
			}
		}
		return inAll;
		
	}

	public void addDisclosableAttributeForCredential(URI credential, AttributeDescription ad) throws UxException{
		addDisclosableAttributeForCredential(credential, ad, null, null);
		
	}

	public void addDisclosableAttributeForCredential(URI credential, AttributeDescription ad, 
			/*Nullable*/ InspectorAlternatives inspectorAlternatives, /*Nullable*/ String inspectionGrounds) throws UxException{

		HashMap<URI, AttributeDescription> ads = getDisclosableAttributeDescriptionsForCredential(credential);
		boolean acceptable = false; 
		for (AttributeDescription ad0 : ads.values()){
			if (ad0.getType().equals(ad.getType())){
				acceptable = true; 
				break; 
				
			}
		}
		if (!acceptable){
			throw new UxException("The attribute requested to disclose is not acceptable " + ad.getType());
			
		}
		CredentialInPolicy cip = getCredentialInPolicyFor(credential);
		if (cip==null){
			throw new RuntimeException("Credential not found " + credential);
			
		}
		AttributeInPolicy aip = of.createAttributeInPolicy();
		aip.setAttributeType(ad.getType());
		aip.setInspectorAlternatives(inspectorAlternatives);
		aip.setInspectionGrounds(inspectionGrounds);
		cip.getDisclosedAttribute().add(aip);
		
	}
	
	public void addAttributePredicateForCredential(URI alias, URI attributeTypeUri, 
									URI uriFunction, URI dataHandling, Object value) throws Exception {
		
		HashMap<URI, AttributeDescription> acceptableAttributes = getDisclosableAttributeDescriptionsForCredential(alias);
		if (acceptableAttributes==null || acceptableAttributes.isEmpty()){
			throw new UxException("There are no acceptable attributes to disclose for the credential with alias " + alias);
			
		}
		AttributeDescription ad = acceptableAttributes.get(attributeTypeUri);
		if (ad==null){
			throw new UxException("There is no attribute in the credential with alias of " + alias + ", "+ attributeTypeUri);
			
		}
		URI encoding = ad.getEncoding();
		UriEncoding.isValid(encoding, uriFunction);
		
		EnumAllowedValues allowed = new EnumAllowedValues(ad);
		MyAttributeValue att = VerifiedClaim.computeMyAttributeValue(encoding, value, allowed);
		
		AttributePredicate.Attribute a = of.createAttributePredicateAttribute();
		a.setAttributeType(ad.getType());
		a.setCredentialAlias(alias);
		a.setDataHandlingPolicy(dataHandling);
		
		AttributePredicate ap = of.createAttributePredicate();
		ap.setFunction(uriFunction);  
		ap.getAttributeOrConstantValue().add(a);
		ap.getAttributeOrConstantValue().add(att.getIntegerValueUnderEncoding(encoding));
		this.policy.getAttributePredicate().add(ap);

	}
	
	public HashMap<URI, AttributeDescription> getDisclosableAttributeDescriptionsForCredential(URI credentialUri) throws UxException{
		List<AttributeDescription> ads = this.credentialToAcceptableAttributesMap.get(
				credentialToRootCredentialMap.get(credentialUri));
		
		if (ads==null){
			ads = this.credentialToAcceptableAttributesMap.get(aliasToRootCredentialMap.get(credentialUri));
			
		}
		if (ads==null){
			throw new UxException("CredentialSpecUid or Alias not found: " + credentialUri);
			
		}
		HashMap<URI, AttributeDescription> result = new HashMap<>();
		
		for (AttributeDescription ad : ads){
			result.put(ad.getType(), ad);

		}
		return result;

	}
	
	public CredentialInPolicy getCredentialInPolicyFor(URI credentialSpecUid){
		return this.credentialToCredentialInPolicyMap.get(credentialToRootCredentialMap.get(credentialSpecUid));
		
	}
	
	public static ArrayList<URI> startCredentialList(URI credentialUid){
		ArrayList<URI> credentials = new ArrayList<>();
		credentials.add(credentialUid);
		return credentials;
		
	}
	
	public static ArrayList<IssuerParametersUID> startIssuerParams(List<URI> issuerParamsUid){
		ArrayList<IssuerParametersUID> result = new ArrayList<>();
		for (URI uid : issuerParamsUid){
			IssuerParametersUID uid0 = new IssuerParametersUID();
			uid0.setValue(uid);
			result.add(uid0);
			
		}
		return result;
		
	}
	
	public static PresentationPolicyAlternatives wrapPolicy(PresentationPolicy pp){
		PresentationPolicyAlternatives ppa = of.createPresentationPolicyAlternatives();
		ppa.getPresentationPolicy().add(pp);
		return ppa; 
		
	}
	
	
	public static ArrayList<IssuerParametersUID> startIssuerParams(URI issuerParamsUid){
		return startIssuerParams(issuerParamsUid, null);
		
	}
	
	
	public static ArrayList<IssuerParametersUID> startIssuerParams(URI issuerParamsUid, URI revocationInfoUid){
		ArrayList<IssuerParametersUID> issuerParams = new ArrayList<>();
		IssuerParametersUID i = new IssuerParametersUID();
		i.setRevocationInformationUID(revocationInfoUid);
		i.setValue(issuerParamsUid);
		issuerParams.add(i);
		return issuerParams;
		
	}	
	
	public PresentationPolicy getPolicy() {
		return policy;
		
	}
	
	public PresentationPolicyAlternatives getPolicyAlternatives() {
		return getPolicyAlternatives(null);
	}
		
		
	public PresentationPolicyAlternatives getPolicyAlternatives(VerifierParameters vp) {
		PresentationPolicyAlternatives ppa = wrapPolicy(policy);
		ppa.setVerifierParameters(vp);
		return ppa;
		
	}
}