package io.exonym.utils.storage;

import java.io.File;
import java.net.URI;


import eu.abc4trust.xml.CredentialSpecification;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.helpers.BuildCredentialSpecification;
import io.exonym.uri.UriDataType;
import io.exonym.uri.UriEncoding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * todo migrate to jupiter
 */
public class TestXContainer {

	
	private static final Logger logger = LogManager.getLogger(TestXContainer.class);
	public void testCreateAndDeleteEmptyContainer(){
		
		// Open Container that does not exist.
		try {
			new IdContainer(UtilsSuite.XC1);
			assert(false);
			
		} catch (Exception e) {
			logger.info("CREATE_AND_DELETE_CONTAINER: Successfully rejected container name that was not there.");
			assert(true);
			
		}
		
	}
	
	public void testSavePlainResource(){
		try {
			BuildCredentialSpecification bcs = new BuildCredentialSpecification(URI.create("urn:credential:c"), true);
			CredentialSpecification cs = bcs.getCredentialSpecification();
			
			IdContainer xc0 = new IdContainer(UtilsSuite.XC0);
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

	
	public void testSerialization(){
		try {
			AnonCredentialParameters acp = new AnonCredentialParameters();
			acp.setGroupUid(URI.create("urn:one:two:three"));
			String x = JaxbHelper.serializeToJson(acp, AnonCredentialParameters.class);
			logger.info("\\n HELP" + x);
			
		} catch (Exception e) {
			logger.error("Error", e);
			
		}
	}
}
