package io.exonym.utils.storage;

import java.net.URI;
import java.nio.file.FileSystemException;


import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.InspectorPublicKey;
import eu.abc4trust.xml.IssuancePolicy;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.PresentationToken;
import eu.abc4trust.xml.RevocationAuthorityParameters;
import eu.abc4trust.xml.RevocationInformation;
import eu.abc4trust.xml.SystemParameters;
import io.exonym.abc.util.FileType;
import io.exonym.abc.util.IoMngt;
import io.exonym.uri.NamespaceMngt;
import io.exonym.utils.IsoCountryCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ExternalResourceContainer {

	private static final Logger logger = LogManager.getLogger(ExternalResourceContainer.class);
	
	protected final URI ledger = NamespaceMngt.LEDGER;
	
	protected final IsoCountryCode iso = IsoCountryCode.getInstance();
	
	protected ExternalResourceContainer(){}

	public synchronized <T> T openResource(URI uid) throws Exception{
		return openResource(IdContainer.uidToXmlFileName(uid));
	}

	@SuppressWarnings("unchecked")
	public synchronized <T> T openResource(String fileName) throws Exception{
		logger.info("Searching externally for " + fileName);
		
		if (FileType.isIssuerParameters(fileName)){
			return (T) IoMngt.getResource(ledger.resolve(fileName).getPath(), IssuerParameters.class);
			
		} else if (FileType.isSystemParameters(fileName)){
			return (T) IoMngt.getResource(ledger.resolve(fileName).getPath(), SystemParameters.class);

		} else if (FileType.isRegistrationParams(fileName)){
			throw new Exception();
			// return (T) JaxbHelper.xmlFileToClass(new File(ledger.resolve(fileName).getPath()), RegistrationParameters.class);

		} else if (FileType.isPresentationPolicyAlternatives(fileName)){
			throw new Exception();
			// return (T) JaxbHelper.xmlFileToClass(new File(ledger.resolve(fileName).getPath()), PresentationPolicyAlternativesAdapter.class);

		} else if (FileType.isCredentialSpecification(fileName)){
			return (T) IoMngt.getResource(ledger.resolve(fileName).getPath(), CredentialSpecification.class);

		} else if (FileType.isIssuancePolicy(fileName)){
			return (T) IoMngt.getResource(ledger.resolve(fileName).getPath(), IssuancePolicy.class);
			
		} else if (FileType.isProofToken(fileName)){ // Local
			return (T) IoMngt.getResource(ledger.resolve(fileName).getPath(), PresentationToken.class);
			
		} else if (FileType.isInspectorPublicKey(fileName)){
			return (T) IoMngt.getResource(ledger.resolve(fileName).getPath(), InspectorPublicKey.class);
			
		} else if (FileType.isRevocationAuthority(fileName)){
			return (T) IoMngt.getResource(ledger.resolve(fileName).getPath(), RevocationAuthorityParameters.class);

		} else if (FileType.isRevocationInformation(fileName)){
			return (T) IoMngt.getResource(ledger.resolve(fileName).getPath(), RevocationInformation.class);

		} else {
			throw new FileSystemException("File type not recognized " + fileName);
			
		}		
	}
	
	/**
	 * This is a stub that will eventually take, the XML, the UID and 
	 * the publication location written on the ledger to publish the materials.
	 * 
	 * @param xml
	 * @param fileName
	 * @throws Exception
	 * 
	 */
	public synchronized void publish(String xml, String fileName) throws Exception {
		IoMngt.saveToFile(xml, ledger.toString() + fileName, true);
		
	}
	
	protected String fileNameFromUid(URI groupUid) throws Exception {
		return IdContainer.uidToFileName(groupUid) + ".gp.xml";
		
	}
	
}