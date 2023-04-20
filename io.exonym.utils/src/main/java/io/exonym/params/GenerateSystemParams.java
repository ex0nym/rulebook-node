package io.exonym.params;

import java.io.File;
import java.io.IOException;

import io.exonym.idmx.dagger.DaggerExonymComponent;
import io.exonym.idmx.dagger.ExonymComponent;
import com.google.inject.Inject;
import com.ibm.zurich.idmx.buildingBlock.signature.cl.ClSignatureBuildingBlock;
import com.ibm.zurich.idmx.buildingBlock.systemParameters.EcryptSystemParametersTemplateWrapper;
import com.ibm.zurich.idmx.configuration.Configuration;
import com.ibm.zurich.idmx.exception.ConfigurationException;
import com.ibm.zurich.idmx.exception.SerializationException;
import com.ibm.zurich.idmx.interfaces.cryptoEngine.CryptoEngineIssuer;
import com.ibm.zurich.idmx.interfaces.proofEngine.ZkDirector;
import com.ibm.zurich.idmx.interfaces.util.BigIntFactory;
import com.ibm.zurich.idmx.interfaces.util.RandomGeneration;
import com.ibm.zurich.idmx.interfaces.util.group.GroupFactory;
import com.ibm.zurich.idmx.parameters.system.SystemParametersWrapper;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.xml.SystemParameters;
import eu.abc4trust.xml.SystemParametersTemplate;
import io.exonym.abc.util.IoMngt;
import io.exonym.uri.NamespaceMngt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GenerateSystemParams {

	private static final Logger logger = LogManager.getLogger(GenerateSystemParams.class);

	@Inject
	protected BigIntFactory bigIntFactory;
	protected GroupFactory groupFactory;
	protected RandomGeneration randomGeneration;
	
	protected KeyManager keyManager;
	protected ClSignatureBuildingBlock clBuildingBlock;
	protected ZkDirector zkDirector;
	
	protected CryptoEngineIssuer cryptoEngineIssuer;

	protected static ExonymComponent INJECTOR = DaggerExonymComponent.create();

	/**
	 *
	 * These params must be shared throughout Existence to allow for one pass proof of membership.
	 * 
	 * @throws IOException
	 * @throws SerializationException
	 * @throws ConfigurationException
	 * 
	 */
	public GenerateSystemParams() throws IOException, SerializationException, ConfigurationException {

		// Utils
		bigIntFactory = INJECTOR.provideBigIntFactory();
		groupFactory = INJECTOR.provideGroupFactory();
		randomGeneration = INJECTOR.provideRandomGeneration();
		
		// General
		keyManager = INJECTOR.providesKeyManager();
		clBuildingBlock = INJECTOR.provideBuildingBlockFactory()
				.getBuildingBlockByClass(ClSignatureBuildingBlock.class);

		zkDirector = INJECTOR.providesZkDirector();
		
		cryptoEngineIssuer = INJECTOR.providesCryptoEngineIssuer();
		
	}
	
	public void systemParametersGeneration() throws Exception {

		String spFilename = NamespaceMngt.DEFAULT_SYSTEM_PARAMETERS_FILENAME;
		
		// Create a template
		EcryptSystemParametersTemplateWrapper spt = initSystemParametersTemplate();
		
		// Only re-create system parameters if they are not already existing
		if (!new File(spFilename).exists()) {
		    SystemParameters systemParameters = cryptoEngineIssuer.setupSystemParameters(spt.getSystemParametersTemplate());
		    SystemParametersWrapper systemParametersFacade = new SystemParametersWrapper(systemParameters);
		    IoMngt.saveToFile(systemParametersFacade.serialize(), spFilename, false);
		    logger.info("Saved new Params File");
		    
		} else {
			logger.info("Params File Already Existed");
			
		}
		IoMngt.print(spFilename);

	}	
	
	private EcryptSystemParametersTemplateWrapper initSystemParametersTemplate() throws ConfigurationException {
		SystemParametersTemplate template = cryptoEngineIssuer.createSystemParametersTemplate();
		EcryptSystemParametersTemplateWrapper spt = new EcryptSystemParametersTemplateWrapper(template);
		spt.setSecurityLevel(Configuration.defaultSecurityLevel());
		return spt;
	}

	public static void main(String[] args){
		try {
			GenerateSystemParams params = new GenerateSystemParams();
			params.systemParametersGeneration();
			
		} catch (Exception e) {
			logger.error("Error", e);
			
		}
	}
	
}
