package io.exonym.managers;

import com.google.inject.Inject;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.keyManager.KeyManagerException;
import eu.abc4trust.xml.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.List;

public final class KeyManagerExonym implements KeyManager {


    private static final Logger logger = LogManager.getLogger(KeyManagerExonym.class);

    private static final URI DEFAULT_SYSTEM_PARAMETERS_URI = URI.create("urn:idmx:params:system");

    private KeyManagerSingleton manager = KeyManagerSingleton.getInstance();

    @Inject
    public KeyManagerExonym() {
        logger.info("Using Exonym KeyManager");

    }

    public void clearStale(){
        manager.clearStale();

    }

    @Override
    public boolean storeSystemParameters(SystemParameters systemParameters) throws KeyManagerException {
        return this.storeSystemParameters(systemParameters, DEFAULT_SYSTEM_PARAMETERS_URI);

    }

    @Override
    public SystemParameters getSystemParameters() throws KeyManagerException {
        SystemParameters sp = this.getSystemParameters(DEFAULT_SYSTEM_PARAMETERS_URI);
        if (sp == null) {
            logger.debug("System parameters not found: " + DEFAULT_SYSTEM_PARAMETERS_URI);

        }
        return sp;

    }

    public boolean storeSystemParameters(SystemParameters systemParameters, URI systemParameterUri) {
        return manager.storeSystemParameters(systemParameters, systemParameterUri);

    }

    public SystemParameters getSystemParameters(URI systemParameterUri) {
        return manager.getSystemParameters(systemParameterUri);

    }

    @Override
    public IssuerParameters getIssuerParameters(URI issuid) throws KeyManagerException {
        return manager.getIssuerParameters(issuid);

    }

    @Override
    public boolean storeIssuerParameters(URI issuid, IssuerParameters issuerParameters) throws KeyManagerException {
        return manager.storeIssuerParameters(issuid, issuerParameters);

    }

    @Override
    public RevocationAuthorityParameters getRevocationAuthorityParameters(URI rapuid) throws KeyManagerException {
        return manager.getRevocationAuthorityParameters(rapuid);

    }

    @Override
    public boolean storeRevocationAuthorityParameters(URI issuid, RevocationAuthorityParameters revocationAuthorityParameters) throws KeyManagerException {
        return manager.storeRevocationAuthorityParameters(issuid, revocationAuthorityParameters);

    }

    @Override
    public CredentialSpecification getCredentialSpecification(URI credspec) throws KeyManagerException {
        return manager.getCredentialSpecification(credspec);

    }

    @Override
    public boolean storeCredentialSpecification(URI uid, CredentialSpecification credentialSpecification) throws KeyManagerException {
        return manager.storeCredentialSpecification(uid, credentialSpecification);

    }

    @Override
    public InspectorPublicKey getInspectorPublicKey(URI ipkuid) throws KeyManagerException {
        return manager.getInspectorPublicKey(ipkuid);

    }

    @Override
    public boolean storeInspectorPublicKey(URI ipkuid, InspectorPublicKey inspectorPublicKey) throws KeyManagerException {
        return manager.storeInspectorPublicKey(ipkuid, inspectorPublicKey);

    }

    @Override
    public RevocationInformation getCurrentRevocationInformation(URI rapuid) throws KeyManagerException {
        return this.getRevocationInformation(rapuid, (URI)null);

    }

    @Override
    public RevocationInformation getLatestRevocationInformation(URI rapuid) throws KeyManagerException {
        return this.getRevocationInformation(rapuid, (URI)null);

    }

    @Override
    public RevocationInformation getRevocationInformation(URI rapuid, URI revinfouid) throws KeyManagerException {
        return manager.getRevocationInformation(rapuid, revinfouid);

    }

    @Override
    public boolean hasSystemParameters() throws KeyManagerException {
        return manager.hasSystemParameters();

    }

    @Override
    public void storeRevocationInformation(URI informationUID, RevocationInformation revocationInformation) throws KeyManagerException {
        this.manager.storeRevocationInformation(informationUID, revocationInformation);

    }

    @Override
    public void storeCurrentRevocationInformation(RevocationInformation ri) throws KeyManagerException {
        this.storeRevocationInformation(ri.getRevocationAuthorityParametersUID(), ri);
        this.storeRevocationInformation(ri.getRevocationInformationUID(), ri);

    }

    @Override
    public List<URI> listIssuerParameters() throws KeyManagerException {
        return manager.listIssuerParameters();

    }
}
