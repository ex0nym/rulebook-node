package io.exonym.managers;

import eu.abc4trust.keyManager.KeyManagerException;
import eu.abc4trust.xml.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class KeyManagerSingleton {
    
    private static final Logger logger = LogManager.getLogger(KeyManagerSingleton.class);
    private final static KeyManagerSingleton instance;
    private final ConcurrentHashMap<URI, SystemParameters> systemParametersMap = new ConcurrentHashMap();
    private final ConcurrentHashMap<URI, IssuerParameters> issuerParameterMap = new ConcurrentHashMap();
    private final ConcurrentHashMap<URI, CredentialSpecification> credentialSpecificationMap = new ConcurrentHashMap();
    private final ConcurrentHashMap<URI, RevocationAuthorityParameters> revocationParametersMap = new ConcurrentHashMap();
    private final ConcurrentHashMap<URI, InspectorPublicKey> inspectorKeyMap = new ConcurrentHashMap();
    private final ConcurrentHashMap<URI, RevocationInformation> revocationInfoMap = new ConcurrentHashMap();

    public boolean storeSystemParameters(SystemParameters systemParameters, URI systemParameterUri) {
        this.systemParametersMap.put(systemParameterUri, systemParameters);
        return true;

    }

    public SystemParameters getSystemParameters(URI systemParameterUri) {
        SystemParameters sp = (SystemParameters)this.systemParametersMap.get(systemParameterUri);
        if (sp == null) {
            logger.debug("System parameters not found: " + systemParameterUri);
        }
        return sp;

    }

    public IssuerParameters getIssuerParameters(URI issuid) throws KeyManagerException {
        IssuerParameters ip = this.issuerParameterMap.get(issuid);
        if (ip == null) {
            logger.debug("Issuer parameters not found: " + issuid);

        }
        return ip;

    }

    public boolean storeIssuerParameters(URI issuid, IssuerParameters issuerParameters) throws KeyManagerException {
        this.issuerParameterMap.put(issuid, issuerParameters);
        return true;

    }

    public RevocationAuthorityParameters getRevocationAuthorityParameters(URI rapuid) throws KeyManagerException {
        RevocationAuthorityParameters rap = this.revocationParametersMap.get(rapuid);
        if (rap == null) {
            logger.debug("Revocation authority parameters not found: " + rapuid);
        }
        return rap;

    }

    public boolean storeRevocationAuthorityParameters(URI issuid, RevocationAuthorityParameters revocationAuthorityParameters) throws KeyManagerException {
        this.revocationParametersMap.put(issuid, revocationAuthorityParameters);
        return true;

    }

    public CredentialSpecification getCredentialSpecification(URI credspec) throws KeyManagerException {
        CredentialSpecification cs = this.credentialSpecificationMap.get(credspec);
        if (cs == null) {
            logger.debug("Credential specification not found: " + credspec);

        }
        return cs;

    }

    public boolean storeCredentialSpecification(URI uid, CredentialSpecification credentialSpecification) throws KeyManagerException {
        this.credentialSpecificationMap.put(uid, credentialSpecification);
        return true;

    }

    public InspectorPublicKey getInspectorPublicKey(URI ipkuid) throws KeyManagerException {
        InspectorPublicKey ret = this.inspectorKeyMap.get(ipkuid);
        if (ret == null) {
            logger.debug("Could not find inspector public key: " + ipkuid);
        }
        return ret;

    }

    public boolean storeInspectorPublicKey(URI ipkuid, InspectorPublicKey inspectorPublicKey) throws KeyManagerException {
        this.inspectorKeyMap.put(ipkuid, inspectorPublicKey);
        return true;

    }

    public RevocationInformation getRevocationInformation(URI rapuid, URI revinfouid) throws KeyManagerException {
        logger.debug("REVOCATION INFORMATION REQUEST" + rapuid + " " + revinfouid);
        RevocationInformation ri = this.revocationInfoMap.get(rapuid);
        if (ri == null) {
            logger.debug("Could not get revocation information: " + ri);

        }
        return ri;

    }

    public void storeRevocationInformation(URI informationUID, RevocationInformation revocationInformation) throws KeyManagerException {
        logger.info(" Storing " + informationUID);
        this.revocationInfoMap.put(informationUID, revocationInformation);

    }

    public List<URI> listIssuerParameters() throws KeyManagerException {
        return new ArrayList(this.issuerParameterMap.keySet());

    }




    public boolean hasSystemParameters() throws KeyManagerException {
        return !this.systemParametersMap.isEmpty();

    }





    private KeyManagerSingleton(){
    }
    
    static {
        instance = new KeyManagerSingleton();
    }
    
    public static KeyManagerSingleton getInstance(){
        return instance;
        
    }

    public void clearStale(){
        revocationInfoMap.clear();
        revocationParametersMap.clear();
        issuerParameterMap.clear();
        inspectorKeyMap.clear();
        credentialSpecificationMap.clear();

    }


}
