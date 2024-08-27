package io.exonym.actor.actions;

import com.ibm.zurich.idmx.parameters.system.SystemParametersWrapper;
import eu.abc4trust.abce.internal.inspector.credentialManager.CredentialManagerException;
import eu.abc4trust.cryptoEngine.CryptoEngineException;
import eu.abc4trust.keyManager.KeyManagerException;
import eu.abc4trust.xml.*;
import io.exonym.actor.AbstractExonymInspector;
import io.exonym.lite.standard.PassStore;
import io.exonym.utils.storage.AbstractIdContainer;
import io.exonym.utils.storage.ExternalResourceContainer;

import java.net.URI;
import java.util.List;

public final class ExonymInspector extends AbstractExonymInspector {

	protected ExonymInspector(IdContainerJSON container) throws Exception {
		super(container);
		
	}

	@Override
	protected boolean openResourceIfNotLoaded(URI uid) throws Exception {
		return super.openResourceIfNotLoaded(uid);
	}

	@Override
	protected void generateInspectorMaterials(URI uid, List<FriendlyDescription> friendlyDescription, PassStore store) {
		super.generateInspectorMaterials(uid, friendlyDescription, store);
	}

	@Override
	protected List<Attribute> publishInspectorMaterials(PresentationToken presentationToken)
			throws CryptoEngineException {
		return super.publishInspectorMaterials(presentationToken);
	}

	@Override
	protected List<Attribute> inspect(IssuanceToken issuanceToken) throws CryptoEngineException {
		return super.inspect(issuanceToken);
	}

	@Override
	protected void publishInspectorMaterials() {
		super.publishInspectorMaterials();
	}

	@Override
	public SystemParametersWrapper initSystemParameters() throws Exception {
		return super.initSystemParameters();
	}

	@Override
	protected SystemParametersWrapper initSystemParameters(String spFilename) throws Exception {
		return super.initSystemParameters(spFilename);
	}

	@Override
	public VerifierParameters getVerifierParameters() throws Exception {
		return super.getVerifierParameters();
	}

	@Override
	public SystemParameters getSystemParameters() throws KeyManagerException {
		return super.getSystemParameters();
	}

	@Override
	protected void addCredentialSpecification(CredentialSpecification credentialSpecification) {
		super.addCredentialSpecification(credentialSpecification);
	}

	@Override
	protected URI addIssuerParameters(IssuerParameters issuerParams) throws Exception {
		return super.addIssuerParameters(issuerParams);
	}

	@Override
	protected AbstractIdContainer getContainer() {
		return super.getContainer();
	}

	@Override
	protected void addRevocationAuthorityParameters(RevocationAuthorityParameters rap) throws KeyManagerException {
		super.addRevocationAuthorityParameters(rap);
	}

	@Override
	protected void addRevocationInformation(URI rapUid, RevocationInformation ri) throws Exception {
		super.addRevocationInformation(rapUid, ri);
		
	}
	
	@Override
	protected ExternalResourceContainer initialzeExternalResourceContainer() {
		return PkiExternalResourceContainer.getInstance();
	}

	@Override
	protected List<Attribute> inspect(PresentationToken presentationToken) throws CryptoEngineException {
		return super.inspect(presentationToken);
	}
	

	@Override
	protected void addInspectorSecretKey(URI inssUid, SecretKey key) throws CredentialManagerException {
		super.addInspectorSecretKey(inssUid, key);
		
	}

	@Override
	protected void addInspectorParameters(InspectorPublicKey ins) throws Exception {
		super.addInspectorParameters(ins);
	}

	public static void main(String[] args) throws Exception {
		IdContainerJSON x = new IdContainerJSON("noborder");
		String ip = "jti.c586115e-aca8-420d-a3f4-38a22df395f6.noborder.41c19148.ip.xml";
		IssuancePolicy policy = x.openResource(ip);
		
			
	}
}
