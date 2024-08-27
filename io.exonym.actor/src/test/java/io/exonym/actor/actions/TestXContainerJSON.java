package io.exonym.actor.actions;

import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.Secret;
import io.exonym.helpers.BuildCredentialSpecification;
import io.exonym.uri.UriDataType;
import io.exonym.uri.UriEncoding;
import io.exonym.lite.standard.PassStore;


public class TestXContainerJSON {

	private static final Logger logger = LogManager.getLogger(TestXContainerJSON.class);

	public void testCreateAndDeleteEmptyContainer(){
		
		// Open Container that does not exist.
		try {
			new IdContainerJSON(ContainerSuite.XC1);
			assert(false);
			
		} catch (Exception e) {
			logger.info("CREATE_AND_DELETE_CONTAINER: Successfully rejected container name that was not there.");
			assert(true);
			
		}
		
		// Create Container
		try {
			logger.info("CREATE_AND_DELETE_CONTAINER: Creating contianer xc1");
			IdContainerJSON xc1 = new IdContainerJSON(ContainerSuite.XC1, true);
			
			logger.info("CREATE_AND_DELETE_CONTAINER: Checking schema container exists");
			assert(xc1.getSchema()!=null);
			
			// Delete Container & Check files are no longer there.
			xc1.delete(); // TODO delete not yet implemented.
			// logger.info("CREATE_AND_DELETE_CONTAINER: Checking folder in the container does not exist after delete " + file.getAbsolutePath());
			// assert(!file.exists());
			
		} catch (Exception e) {
			logger.error("CREATE_AND_DELETE_CONTAINER: Error", e);
			assert(false);
			
		}
	}
	
	public void testSavePlainResource(){
		try {
			BuildCredentialSpecification bcs = new BuildCredentialSpecification(URI.create("urn:credential:c"), true);
			CredentialSpecification cs = bcs.getCredentialSpecification();
			
			IdContainerJSON xc0 = new IdContainerJSON(ContainerSuite.XC0);
			xc0.saveLocalResource(cs);
			logger.info("SAVE_PLAIN_RESOURCE: Saved a credential specification and checking that it's there");
			assert(!xc0.getLocalLedgerList().isEmpty());
			
			URI cuid = cs.getSpecificationUID();
			CredentialSpecification csOpen = xc0.openResource(cuid);
			
			logger.info("SAVE_PLAIN_RESOURCE: Opening credential specification and testing contents");
			assert(csOpen.getSpecificationUID().equals(cuid));
			
			try {
				logger.info("SAVE_PLAIN_RESOURCE: Attempting to overwrite resource without explicit command.");
				xc0.saveLocalResource(csOpen);
				assert(false);
				
			} catch (Exception e) {
				assert(true);
				
			}
			bcs.addAttribute(BuildCredentialSpecification.createAttributeDescription(UriDataType.ANY_URI, UriEncoding.ANY_URI_UTF_8, URI.create("urn:test-name")));
			xc0.saveLocalResource(bcs.getCredentialSpecification(), true);
			CredentialSpecification updated = xc0.openResource(cuid);
			
			logger.info("SAVE_PLAIN_RESOURCE: Explicit overwrite with addition of new attribute.");
			assert(updated.getAttributeDescriptions().getAttributeDescription().size()==2);
			
		} catch (Exception e) {
			logger.error("SAVE_PLAIN_RESOURCE: Error", e);
			assert(false);
			
		}
	}
	
	public void testSaveEncryptedResource(){
		try {
			IdContainerJSON xc0 = new IdContainerJSON(ContainerSuite.XC0);
			ExonymOwner owner = new ExonymOwner(xc0);
			PassStore store = new PassStore("password", false);
			owner.openContainer(store);
			owner.setupContainerSecret(store.getEncrypt(), store.getDecipher());
			
			logger.info("SAVE_ENC_RESOURCE: Opened save container secret");
			Secret s = xc0.openResource("xc0-json-test-container.ss.xml", store.getDecipher());
			assert(s!=null);
			
		} catch (Exception e) {
			logger.error("SAVE_ENC_RESOURCE: Error - you need to run from the Suite", e);
			assert(false);

		}
	}
}
