package io.exonym.helpers;

import com.beust.jcommander.internal.Nullable;
import eu.abc4trust.xml.*;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.uri.NamespaceMngt;
import io.exonym.utils.storage.IdContainer;

import java.net.URI;

public class BuildIssuancePolicy {
	
	private static final ObjectFactory of = new ObjectFactory();
	private IssuancePolicy ip = of.createIssuancePolicy();
	private CredentialTemplate ct = of.createCredentialTemplate();

	public static final String BASE_ALIAS = "urn:io:exonym";
	

	public BuildIssuancePolicy(PresentationPolicy preConditions, URI credSpecToIssue, URI issuerParameters) throws Exception {
		if (preConditions==null){
			preConditions = of.createPresentationPolicy();
			Message m = new Message();
			m.setNonce(CryptoUtils.generateNonce(32));;
			preConditions.setMessage(m);

			ip.setPresentationPolicy(preConditions);

		}
		ip.setVersion(NamespaceMngt.VERSION);
		ip.setPresentationPolicy(preConditions);

		ip.setCredentialTemplate(ct);


		ct.setCredentialSpecUID(credSpecToIssue);

		ct.setIssuerParametersUID(issuerParameters);
		definePresentationPolicyUid(issuerParameters);
		addPseudonym(BASE_ALIAS, false, BASE_ALIAS, BASE_ALIAS);


	}

	private void definePresentationPolicyUid(URI issuerParameters) throws Exception {
		String value = IdContainer.stripUidSuffix(issuerParameters, 1);
		URI policyUid = URI.create(NamespaceMngt.URN_PREFIX_COLON+ value + ":pp");
		ip.getPresentationPolicy().setPolicyUID(policyUid);

	}

	/**
	 * Helper method to add a pseudonym to the policy.
	 * 
	 * @param alias
	 * @param sameKeyBinding
	 * @param scope
	 * @param exclusive
	 */
	public void addPseudonym(String scope, boolean exclusive, String alias, @Nullable String sameKeyBinding){
		PseudonymInPolicy pseudonym = of.createPseudonymInPolicy();
		pseudonym.setAlias(URI.create(alias));
		if (sameKeyBinding!=null){
			pseudonym.setSameKeyBindingAs(URI.create(sameKeyBinding));
			
		}
		pseudonym.setScope(scope);
		pseudonym.setExclusive(exclusive);
		this.addPseudonym(pseudonym);
		
	}
	
	/**
	 * Helper method to add a pseudonym to the policy.
	 * 
	 * @param alias
	 * @param sameKeyBinding
	 * @param scope
	 * @param exclusive
	 */
	public static PseudonymInPolicy createPseudonym(String scope, boolean exclusive, String alias, String sameKeyBinding){
		PseudonymInPolicy pseudonym = of.createPseudonymInPolicy();
		pseudonym.setAlias(URI.create(alias));
		if (sameKeyBinding!=null){
			pseudonym.setSameKeyBindingAs(URI.create(sameKeyBinding));
			
		}
		pseudonym.setScope(scope);
		pseudonym.setExclusive(exclusive);
		return pseudonym;
		
	}	
	
	public void addPseudonym(PseudonymInPolicy pseudonym){
		ip.getPresentationPolicy().getPseudonym().add(pseudonym);
		ip.getCredentialTemplate().setSameKeyBindingAs(pseudonym.getAlias());
		
	}
	
	public IssuancePolicy getIssuancePolicy() {
		return ip;
	}
	
	public PresentationPolicy getPresentaionPolicy(){
		return ip.getPresentationPolicy();
		
	}
	
}
