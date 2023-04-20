package io.exonym.actor;

import eu.abc4trust.abce.internal.inspector.credentialManager.CredentialManager;
import eu.abc4trust.abce.internal.inspector.credentialManager.CredentialManagerException;
import eu.abc4trust.cryptoEngine.CryptoEngineException;
import eu.abc4trust.cryptoEngine.inspector.CryptoEngineInspector;
import eu.abc4trust.returnTypes.InspectorPublicAndSecretKey;
import eu.abc4trust.xml.*;
import io.exonym.lite.standard.PassStore;
import io.exonym.utils.storage.AbstractXContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.List;

public abstract class AbstractExonymInspector extends AbstractBaseActor {
	
	
	private static final Logger logger = LogManager.getLogger(AbstractExonymInspector.class);
	
	private CryptoEngineInspector ceInspector;
	private CredentialManager credentialManagerInspector;
	
	
	protected AbstractExonymInspector(AbstractXContainer container) throws Exception {
		super(container);
		initSystemParameters();
		ceInspector = INJECTOR.providesCryptoEngineInspectorAbc();
		credentialManagerInspector = INJECTOR.providesCredentialManagerInspector();
		
		
	}
	
	protected void generateInspectorMaterials(URI uid, List<FriendlyDescription> friendlyDescription, PassStore store){
		try {
		    InspectorPublicAndSecretKey keyPair = ceInspector.setupInspectorPublicKey(this.getSystemParameters(), 
		    											URI.create("idemix"), uid, friendlyDescription);
		    InspectorPublicKey pk = keyPair.publicKey;
		    SecretKey sk = keyPair.secretKey;
		    container.saveLocalResource(pk);
		    container.saveLocalResource(sk, store.getEncrypt());
		    
		} catch (Exception e) {
			logger.error("Error", e);
			
		}
	}
	
	protected List<Attribute> publishInspectorMaterials(PresentationToken presentationToken) throws CryptoEngineException{
		return ceInspector.inspect(presentationToken);
	}
	
	protected List<Attribute> inspect(IssuanceToken issuanceToken) throws CryptoEngineException{
		return ceInspector.inspect(issuanceToken);
	}
	
	protected List<Attribute> inspect(PresentationToken presentationToken) throws CryptoEngineException{
		return ceInspector.inspect(presentationToken);
	}
	
	protected void addInspectorSecretKey(URI inssUid, SecretKey key) throws CredentialManagerException {
		this.credentialManagerInspector.storeInspectorSecretKey(inssUid, key);
		
	}
	
	protected void publishInspectorMaterials() {
	
		
	}
}
