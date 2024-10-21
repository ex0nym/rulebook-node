package io.exonym.actor.actions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.zurich.idmx.jaxb.JaxbHelperClass;
import com.ibm.zurich.idmx.parameters.system.SystemParametersWrapper;
import eu.abc4trust.smartcard.Base64.OutputStream;
import eu.abc4trust.xml.*;
import io.exonym.abc.util.FileType;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.Rulebook;
import io.exonym.utils.adapters.PresentationPolicyAlternativesAdapter;
import io.exonym.utils.storage.AbstractIdContainer;
import io.exonym.utils.storage.KeyContainer;
import io.exonym.utils.storage.IdContainerSchema;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBIntrospector;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

public class IdContainerJSON extends AbstractIdContainer {
	
	private static final Logger logger = LogManager.getLogger(IdContainerJSON.class);
	
	private final IdContainerSchema schema;
	private File testFolder;
	private File file;
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public IdContainerJSON(String username) throws Exception {
		super(username);
		testFolder = new File("resource/local/" + this.getUsername());
		file = new File(testFolder.getAbsolutePath() + "/" + getUsername() + ".json");
		schema = init(false);
		updateLists();

	}
	

	public IdContainerJSON(String username, boolean create) throws Exception {
		super(username);
		testFolder = new File("resource/local/" + this.getUsername());
		file = new File(testFolder.getAbsolutePath() + "/" + getUsername() + ".json");
		schema = init(create);
		updateLists();

	}

	public void show(URI file, OutputStream out, Cipher dec) throws Exception{
		try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(file)))){
			byte[] in = new byte[bis.available()];
			bis.read(in);
			out.write(in);

		} catch (Exception e) {
			throw e; 
			
		}
	}
	
	public void deleteCredential(String prefix, String suffix) throws Exception {
		HashMap<String, String> map = this.schema.getOwnerSecretStore();
		ArrayList<String> toDelete = new ArrayList<String>();
		for (String k : map.keySet()) {
			if (k.startsWith(prefix) && k.endsWith(suffix)) {
				logger.warn("Deleting " + k);
				toDelete.add(k);
			}
		}
		for (String d : toDelete) {
			map.remove(d);
			
		}
		commitSchema();
	}
	
	
	protected IdContainerSchema init(boolean create) throws Exception {
		
		// Is creating a new container.
		if (create){
			if (!testFolder.exists()){
				testFolder.mkdirs();
				IdContainerSchema t = new IdContainerSchema();
				t.setUsername(getUsername());
				
				try (FileOutputStream fos = new FileOutputStream(file)){
					String json = gson.toJson(t);
					fos.write(json.getBytes());
					return t;
					
				} catch (Exception e) {
					throw new Exception("Create container failure.  Unable to write new file.", e);
					
				}
			} else {
				throw new UxException("The Container already exists " + this.getUsername() + " @ " + testFolder.getAbsolutePath());
				
			}
		} else {
			if (testFolder.exists()){
				Path path = Path.of(file.toURI());
				return JaxbHelper.jsonFileToClass(path, IdContainerSchema.class);
				
			} else {
				throw new UxException("The Container does not exist (For Web Run XNodeContainer)" + this.getUsername());
				
			}
		}
	}
	
	protected void commitSchema() throws Exception {
		try (FileOutputStream fos = new FileOutputStream(file)){
			fos.write(JaxbHelper.serializeToJson(schema, IdContainerSchema.class).getBytes());
			updateLists();
			
		} catch (Exception e) {
			throw e;
			
		}
	}	

	// TODO
	@SuppressWarnings("unchecked")
	public void updateLists() {
		localLedgerList = new ArrayList<>(schema.getLocalLedger().keySet());
		issuerSecretList = new ArrayList<>(schema.getIssuerSecretStore().keySet());
		issuanceParameterList = new ArrayList<>(schema.getIssuanceParameterStore().keySet());
		issuancePolicyList = new ArrayList<>(schema.getIssuancePolicyStore().keySet());
		issuedList = new ArrayList<>(schema.getIssuedStore().keySet());
		ownerSecretList = new ArrayList<>(schema.getOwnerSecretStore().keySet());
		noninteractiveTokenList = new ArrayList<>(schema.getNoninteractiveTokenStore().keySet());
		inspectorList = new ArrayList<>(schema.getInspectorStore().keySet());
		revocationAuthList = new ArrayList<>(schema.getRevocationAuthStore().keySet());  

	}
	
	@Override
	public void saveIssuanceToken(IssuanceToken it, String name, Cipher store) throws Exception {
		throw new Exception();
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T openResource(String fullFileName, Cipher dec) throws Exception {
		if (!(FileType.isXmlDocument(fullFileName) || fullFileName.endsWith(".json"))){
			throw new FileSystemException("Resources are all .xml or .json files: " + fullFileName);

		} else if (FileType.isIssuerSecret(fullFileName)){
			return openEncryptedFile(ISSUER_SECRET_STORE, fullFileName, SecretKey.class, dec);

		} else if (FileType.isInspectorPrivateKey(fullFileName)){
			return openEncryptedFile(ISSUER_SECRET_STORE, fullFileName, SecretKey.class, dec);

		} else if (FileType.isKeys(fullFileName)){
			return openXFile(OWNER_PRIVATE_STORE, fullFileName, KeyContainer.class);

		} else if (FileType.isOwnerSecret(fullFileName)){
			return openEncryptedFile(OWNER_PRIVATE_STORE, fullFileName, Secret.class, dec);

		} else if (FileType.isIssuerParameters(fullFileName)){
			return openFile(ISSUER_PARAMETERS_STORE, fullFileName, IssuerParameters.class);

		} else if (FileType.isIssuanceLog(fullFileName)){
			return openFile(ISSUER_ISSUED, fullFileName, IssuanceLogEntry.class);

		} else if (FileType.isSystemParameters(fullFileName)){
			return (T) openSystemParameters();

		} else if (FileType.isPresentationPolicyAlternatives(fullFileName)){
			return openXFile(LOCAL_LEDGER, fullFileName, PresentationPolicyAlternativesAdapter.class);

		} else if (FileType.isCredentialSpecification(fullFileName)){
			return openFile(LOCAL_LEDGER, fullFileName, CredentialSpecification.class);

		} else if (FileType.isIssuancePolicy(fullFileName)){
			return openFile(ISSUANCE_POLICY_STORE, fullFileName, IssuancePolicy.class);

		} else if (FileType.isProofToken(fullFileName)){
			return openFile(NONINTERACTIVE_TOKENS, fullFileName, PresentationToken.class);

		} else if (FileType.isInspectorPublicKey(fullFileName)){
			return openFile(INSPECTOR_STORE, fullFileName, InspectorPublicKey.class);

		} else if (FileType.isRevocationAuthority(fullFileName)){
			return openFile(REVOCATION_AUTH_STORE, fullFileName, RevocationAuthorityParameters.class);

		} else if (FileType.isRevocationInformation(fullFileName)){
			return openFile(REVOCATION_AUTH_STORE, fullFileName, RevocationInformation.class);

		} else if (FileType.isCredential(fullFileName)){ // Local
			return openEncryptedFile(OWNER_PRIVATE_STORE, fullFileName, Credential.class, dec);

		} else if (FileType.isRevocationAuthorityPrivateKey(fullFileName)){ // Local
			return openEncryptedFile(REVOCATION_AUTH_STORE, fullFileName, PrivateKey.class, dec);

		} else if (FileType.isRevocationHistory(fullFileName)){ // Local
			return openFile(REVOCATION_AUTH_STORE, fullFileName, RevocationHistory.class);

		} else if (FileType.isIssuancePolicy(fullFileName)){ // Local
			return openFile(ISSUANCE_POLICY_STORE, fullFileName, IssuancePolicy.class);

		} else if (FileType.isPresentationPolicy(fullFileName)){ // Local
			return openFile(ISSUANCE_POLICY_STORE, fullFileName, PresentationPolicy.class);

		} else if (FileType.isRulebook(fullFileName)){ // Local
			return openJsonFile(LOCAL_LEDGER, fullFileName);

		} else {
			throw new FileSystemException("File type not recognized " + fullFileName);

		}
	}

	public static SystemParameters openSystemParameters() throws Exception {
		try (InputStream stream = IdContainerJSON.class.getClassLoader().getResourceAsStream("lambda.xml")){
			if (stream!=null){
				byte[] in = new byte[stream.available()];
				stream.read(in);
				String systemParameters = new String(in, StandardCharsets.UTF_8);
				SystemParametersWrapper systemParametersFacade = SystemParametersWrapper.deserialize(systemParameters);
				return systemParametersFacade.getSystemParameters();

			} else {
				throw new FileNotFoundException("lambda.xml");

			}
		}

	}


	@SuppressWarnings("unchecked")
	private <T> T openEncryptedFile(URI location, String fullFileName, Class<?> clazz, Cipher dec) throws Exception {
		try {
			if (dec!=null){
				HashMap<String, String> l = computeLocation(location);
				String encB64 = l.get(fullFileName);
				if (encB64!=null){
					byte[] xml = dec.doFinal(Base64.decodeBase64(encB64.getBytes()));
					ByteArrayInputStream is = new ByteArrayInputStream(xml);
					JAXBElement<?> resourceAsJaxbElement = JaxbHelperClass.deserialize(is, true);
					return (T)JAXBIntrospector.getValue(resourceAsJaxbElement);

				} else {
					throw new Exception("The file does not exist " + fullFileName + " in container " + this.getUsername());

				}
			} else {
				throw new UxException("Expected a decryption cipher for the encrypted file" + fullFileName);

			}
		} catch (BadPaddingException e){
			throw new UxException("The pass store for this container was incorrect");

		}
	}

	private <T> T openJsonFile(URI location, String fullFileName) throws Exception {
		HashMap<String, String> l = computeLocation(location);
		String encB64 = l.get(fullFileName);
		if (encB64!=null){
			byte[] json = Base64.decodeBase64(encB64.getBytes());
			return (T)JaxbHelper.jsonToClass(new String(json, StandardCharsets.UTF_8), Rulebook.class);

		} else {
			throw new FileNotFoundException("The file does not exist " + fullFileName + " in container " + this.getUsername());

		}
	}


	@SuppressWarnings("unchecked")
	private <T> T openFile(URI location, String fullFileName, Class<?> clazz) throws Exception {
		HashMap<String, String> l = computeLocation(location);
		String encB64 = l.get(fullFileName);
		if (encB64!=null){
			byte[] xml = Base64.decodeBase64(encB64.getBytes());
			ByteArrayInputStream is = new ByteArrayInputStream(xml);
			JAXBElement<?> resourceAsJaxbElement = JaxbHelperClass.deserialize(is, true);
			return (T)JAXBIntrospector.getValue(resourceAsJaxbElement);
			
		} else {
			throw new FileNotFoundException("The file does not exist " + fullFileName + " in container " + this.getUsername());
			
		}
	}

	
	@SuppressWarnings("unchecked")
	private <T> T openXFile(URI location, String fullFileName, Class<?> clazz) throws Exception {
		logger.debug("Getting file from schema object: " + schema);
		HashMap<String, String> l = computeLocation(location);
		String encB64 = l.get(fullFileName);
		if (encB64!=null){
			byte[] xml = Base64.decodeBase64(encB64.getBytes());
			return (T)JaxbHelper.xmlToClass(new String(xml), clazz);
			
		} else {
			throw new Exception("The file does not exist " + fullFileName + " in container " + this.getUsername());
			
		}
	}


	@Override
	protected void saveEncrypted(String xml, URI location, String name, boolean overwrite, Cipher store) throws Exception {
		String b64 = Base64.encodeBase64String(store.doFinal(xml.getBytes()));
		put(b64, name, computeLocation(location), overwrite);
		commitSchema();
		
	}
	
	@Override
	protected void save(String xml, URI location, String name, boolean overwrite) throws Exception {
		String b64 = Base64.encodeBase64String(xml.getBytes());
		put(b64, name, computeLocation(location), overwrite);
		commitSchema();
		
	}	
	
	protected HashMap<String, String> computeLocation(URI location) throws Exception {
		if (location.equals(LOCAL_LEDGER)){
			return schema.getLocalLedger();
			
		} else if (location.equals(ISSUER_SECRET_STORE)){
			return schema.getIssuerSecretStore();
			
		} else if (location.equals(ISSUER_PARAMETERS_STORE)){
			return schema.getIssuanceParameterStore();
			
		} else if (location.equals(RUNTIME_KEYS)){
			return schema.getOwnerSecretStore();
			
		} else if (location.equals(ISSUANCE_POLICY_STORE)){
			return schema.getIssuancePolicyStore();
			
		} else if (location.equals(ISSUER_ISSUED)){
			return schema.getIssuedStore();
			
		} else if (location.equals(OWNER_PRIVATE_STORE)){
			return schema.getOwnerSecretStore();
			
		} else if (location.equals(NONINTERACTIVE_TOKENS)){
			return schema.getNoninteractiveTokenStore();
			
		} else if (location.equals(INSPECTOR_STORE)){
			return schema.getInspectorStore();
			
		} else if (location.equals(REVOCATION_AUTH_STORE)){
			return schema.getRevocationAuthStore();

		} else {
			throw new HubException("Programming Error - Case for " + location + " not handled.");
			
		}
	}	

	private void put(String b64, String name, HashMap<String, String> map, boolean overwrite) throws UxException {
		if (overwrite){
			map.put(name, b64);
			
		} else {
			String f = map.putIfAbsent(name, b64);
			if (f !=null && !overwrite){
				throw new UxException("The XML already exists: " + name);
				
			}
		}
	}

	@Override
	public void delete() throws Exception {
		String l = this.testFolder.getAbsoluteFile() + "//" + this.getUsername() + ".json";
		File f = new File(l);
		File g = f.getParentFile();
		f.delete();
		g.delete();
		
	}

	public IdContainerSchema getSchema() {
		return schema;
		
	}
}