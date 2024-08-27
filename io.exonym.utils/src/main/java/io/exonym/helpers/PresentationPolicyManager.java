package io.exonym.helpers;

import eu.abc4trust.xml.*;
import eu.abc4trust.xml.CredentialInPolicy.IssuerAlternatives;
import io.exonym.lite.pojo.Rulebook;
import io.exonym.lite.standard.Const;
import io.exonym.uri.NamespaceMngt;
import io.exonym.utils.storage.IdContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PresentationPolicyManager {
	

	private static final Logger logger = LogManager.getLogger(PresentationPolicyManager.class);
	public final static URI ROOT_ALIAS = URI.create("urn:io:exonym");
	private final PresentationPolicyAlternatives ppa;  
	private CredentialInPolicy cip = null;
	private CredentialInPolicy sybilCip = null;
	private final CredentialSpecification cSpec;
	private HashMap<URI, List<IssuerAlternatives.IssuerParametersUID>> issuerUrisByNode = new HashMap<>();
	private HashMap<URI, URI> insUriByNode = new HashMap<>();
	private HashMap<String, PseudonymInPolicy> pips = new HashMap<>();

	private final IssuerAlternatives.IssuerParametersUID sybilIpUid;


	public PresentationPolicyManager(PresentationPolicy pp, CredentialSpecification cSpec,
									 IssuerAlternatives.IssuerParametersUID sybilIpUid) throws Exception {
		if (pp==null){
			throw new NullPointerException();
		}
		PresentationPolicyAlternatives ppa = new PresentationPolicyAlternatives();
		ppa.getPresentationPolicy().add(pp);
		this.cSpec = cSpec;
		this.ppa = ppa;
		this.sybilIpUid = sybilIpUid;
		extract();
		logger.debug("CIP=" + cip +  " SybilCI=" + sybilCip);

	}

	public PresentationPolicyManager(PresentationPolicy pp, CredentialSpecification cSpec) throws Exception {
		if (pp==null){
			throw new NullPointerException();
		}
		PresentationPolicyAlternatives ppa = new PresentationPolicyAlternatives();
		ppa.getPresentationPolicy().add(pp);
		this.cSpec = cSpec;
		this.ppa = ppa;
		this.sybilIpUid = null;
		extract();
		logger.debug("CIP=" + cip +  " SybilCI=" + sybilCip);

	}


	private void extract() throws Exception {
		ArrayList<PresentationPolicy> policies = (ArrayList<PresentationPolicy>) this.ppa.getPresentationPolicy();

		for (PresentationPolicy p : policies) {
			for (PseudonymInPolicy nym : p.getPseudonym()) {
				if (nym!=null && nym.getScope()!=null) {
					pips.put(nym.getScope(), nym);
					
				} else {
					String s = (nym!=null ? nym.getScope() : null);
					logger.warn("DROPPING nym " + s);
					
				}
			}
			if (p.getCredential().size()>2){
				throw new Exception();

			}
			for (CredentialInPolicy c : p.getCredential()) {
				if (Rulebook.isSybil(c.getCredentialSpecAlternatives().getCredentialSpecUID().get(0))){
					this.sybilCip = c;
				} else {
					this.cip = c;

				}
				List<IssuerAlternatives.IssuerParametersUID> params = c.getIssuerAlternatives().getIssuerParametersUID();
				for (IssuerAlternatives.IssuerParametersUID ipuid : params) {
					URI rootUid = createRootUid(ipuid.getValue());
					List<IssuerAlternatives.IssuerParametersUID> issuers = issuerUrisByNode.remove(rootUid);
					if (issuers==null){
						issuers = new ArrayList<>();

					}
					issuers.add(ipuid);
					issuerUrisByNode.put(rootUid, issuers);
					insUriByNode.putIfAbsent(rootUid, URI.create(rootUid.toString() + ":ins"));

				}

			}
			if (this.cip == null && this.sybilCip !=null){
				// This presentation policy is for Sybil.
				this.cip = this.sybilCip;
				this.sybilCip = null;

			}
		}
	}
	
	private URI createRootUid(URI uid) throws Exception {
		logger.info("Creating Root UID=" + uid);
		String[] parts = uid.toString().split(":");
		if (parts.length==6) {
			return uid;

		} else if (parts.length < 6) {
			throw new Exception("The UID was invalid " + uid);
			
		}
		return URI.create(parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3] + ":" + parts[4] + ":" + parts[5]);
		
	}

	public void addNym(ArrayList<String> scopes) {
		addNym(scopes, null);
	}

	public void addNym(ArrayList<String> scopes, URI keyBinding) {
		PseudonymInPolicy template = pips.get(Const.BINDING_ALIAS);
		
		for (String scope : scopes) {
			PseudonymInPolicy pip = new PseudonymInPolicy();
			pip.setExclusive(true);
			keyBinding = (keyBinding==null ? template.getAlias() : keyBinding);
			logger.debug("keyBinding=" + keyBinding);
			pip.setSameKeyBindingAs(keyBinding);
			pip.setScope(scope);
			logger.debug("Adding scope to policy:" + scope);
			pips.put(scope, pip);
			
		}
	}
	
	public void addNym(String scope) {
		ArrayList<String> t = new ArrayList<String>();
		t.add(scope);
		addNym(t);
		
	}

	public void addIssuer(IssuerParameters i, InspectorPublicKey ins) throws Exception {
		logger.info("Adding I + INS to PP: " + i.getParametersUID() + " " + ins.getPublicKeyUID());
		if (cip==null){
			cip = buildCredentialInPolicy(cSpec, i, ins.getPublicKeyUID());


		}
		if (sybilCip==null && sybilIpUid!=null){
			sybilCip = buildSybilCip();

		}
		URI iUid = i.getParametersUID();
		UIDHelper helper = new UIDHelper(iUid);

		IssuerAlternatives.IssuerParametersUID ip = new IssuerAlternatives.IssuerParametersUID();
		ip.setRevocationInformationUID(helper.getRevocationInfoParams());
		ip.setValue(helper.getIssuerParameters());
		ArrayList<IssuerAlternatives.IssuerParametersUID> l = new ArrayList<>();
		l.add(ip);

		URI root = createRootUid(iUid);

		issuerUrisByNode.putIfAbsent(root, l);
		insUriByNode.putIfAbsent(root, ins.getPublicKeyUID());

	}

	private CredentialInPolicy buildSybilCip() throws Exception {
		CredentialInPolicy sybilCip = new CredentialInPolicy();
		CredentialInPolicy.CredentialSpecAlternatives sybilCsa =
				new CredentialInPolicy.CredentialSpecAlternatives();

		sybilCip.setCredentialSpecAlternatives(sybilCsa);
		UIDHelper helper = new UIDHelper(sybilIpUid.getValue());
		sybilCsa.getCredentialSpecUID().add(helper.getCredentialSpec());
		IssuerAlternatives iaSybil = new IssuerAlternatives();
		iaSybil.getIssuerParametersUID().add(sybilIpUid);
		sybilCip.setIssuerAlternatives(iaSybil);
		sybilCip.setSameKeyBindingAs(ROOT_ALIAS);
		return sybilCip;

	}
	
	public void removeIssuer(ArrayList<URI> uids) throws Exception {
		for (URI uid : uids) {
			removeIssuer(uid);
			
		}
	}
	
	public void removeIssuer(URI uid) throws Exception  {
		URI nodeUid = createRootUid(uid);
		issuerUrisByNode.remove(nodeUid);
		insUriByNode.remove(nodeUid);

	}
	
	public void removeNym(ArrayList<String> scopes) {
		for (String scope : scopes) {
			removeNym(scope);
			
		}
	}
	
	public void removeNym(String scope) {
		this.pips.remove(scope);
		
	}
	
	public PresentationPolicy build() {
		PresentationPolicy policy = ppa.getPresentationPolicy().get(0);
		policy.getCredential().clear();
		policy.getPseudonym().clear();
		// Build Credential In Policy
		// Add Single Credential In Policy
		List<AttributeInPolicy> disclosedAttributes = (cip==null ? null : cip.getDisclosedAttribute());
		if (disclosedAttributes!=null && !disclosedAttributes.isEmpty()){
			AttributeInPolicy.InspectorAlternatives ia = disclosedAttributes.get(0).getInspectorAlternatives();
			if (ia!=null){
				ia.getInspectorPublicKeyUID().clear();

			}
		}
		ArrayList<IssuerAlternatives.IssuerParametersUID> newList = new ArrayList<>();

		for (URI root : issuerUrisByNode.keySet()){
			List<IssuerAlternatives.IssuerParametersUID> ipuid = issuerUrisByNode.get(root);
			if (!ipuid.isEmpty()){
				newList.addAll(ipuid);

			}
		}
		if (cip!=null){
			cip.getIssuerAlternatives().getIssuerParametersUID().clear();
			cip.getIssuerAlternatives().getIssuerParametersUID().addAll(newList);
			if (!cip.getIssuerAlternatives().getIssuerParametersUID().isEmpty()){
				policy.getCredential().add(cip);

			}
			policy.getCredential().add(sybilCip);
			List<AttributeInPolicy> aips = cip.getDisclosedAttribute();
			if (!aips.isEmpty()){
				AttributeInPolicy.InspectorAlternatives ias = aips.get(0).getInspectorAlternatives();
				if (ias!=null){
					ias.getInspectorPublicKeyUID().addAll(insUriByNode.values());
				}
			}
		}
		policy.getPseudonym().addAll(pips.values());
		return policy;
		
	}

	private CredentialInPolicy buildCredentialInPolicy(CredentialSpecification cred, IssuerParameters ip, URI inspectorUid) throws Exception {
		String root = NamespaceMngt.URN_PREFIX_COLON + IdContainer.stripUidSuffix(ip.getRevocationParametersUID().toString(), 2);
		URI cUid = cred.getSpecificationUID();

		URI raiUid = URI.create(root + ":rai");

		CredentialInPolicy cip = new CredentialInPolicy();
		CredentialInPolicy.CredentialSpecAlternatives csa = new CredentialInPolicy.CredentialSpecAlternatives();
		cip.setCredentialSpecAlternatives(csa);
		cip.getCredentialSpecAlternatives().getCredentialSpecUID().add(cUid);
		IssuerAlternatives issuerAlternatives = new IssuerAlternatives();
		ArrayList<IssuerAlternatives.IssuerParametersUID> list = BuildPresentationPolicy.startIssuerParams(ip.getParametersUID(), raiUid);
		issuerAlternatives.getIssuerParametersUID().addAll(list);
		cip.setIssuerAlternatives(issuerAlternatives);

		if (Rulebook.isSybil(cUid)){
			AttributeDescription ad = cred.getAttributeDescriptions().getAttributeDescription().get(1);
			AttributeInPolicy aip = new AttributeInPolicy();
			aip.setAttributeType(ad.getType());
			cip.getDisclosedAttribute().add(aip);

		} else {
			AttributeDescription ad = cred.getAttributeDescriptions()
					.getAttributeDescription().get(0);
			AttributeInPolicy aip = new AttributeInPolicy();
			aip.setAttributeType(ad.getType());
			aip.setInspectionGrounds("The value can be inspected on presentation of proof that " +
					"the Producer may have infringed on the interpreted rulebook.");
			AttributeInPolicy.InspectorAlternatives ia = new AttributeInPolicy.InspectorAlternatives();
			ia.getInspectorPublicKeyUID().add(inspectorUid);
			aip.setInspectorAlternatives(ia);
			cip.getDisclosedAttribute().add(aip);

		}
		cip.setSameKeyBindingAs(ROOT_ALIAS);

		return cip;
	}

	public CredentialInPolicy getCredentialInPolicy() {
		return cip;
	}

	public boolean hasIssuer(URI issuerUid) throws Exception {
		IssuerAlternatives ia;
		if (cip==null){
			ia = sybilCip.getIssuerAlternatives();
		} else {
			ia = cip.getIssuerAlternatives();
		}
		for (IssuerAlternatives.IssuerParametersUID ip : ia.getIssuerParametersUID()) {
			if (ip.getValue().equals(issuerUid)) {
				return true;

			}
		}
		return false;
			
	}
	
	public boolean hasScope(String scope) {
		return pips.containsKey(scope);
		
	}

}
