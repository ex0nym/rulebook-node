package io.exonym.helpers;

import com.ibm.zurich.idmx.jaxb.JaxbHelperClass;
import com.sun.xml.ws.util.ByteArrayBuffer;
import eu.abc4trust.xml.*;
import io.exonym.abc.util.FileType;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.lite.connect.UrlHelper;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.Rulebook;
import io.exonym.lite.pojo.XKey;
import io.exonym.utils.storage.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBElement;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class XmlHelper {
	
	

	private static final Logger logger = LogManager.getLogger(XmlHelper.class);
	
	public static boolean isRegistrationParameter(String fileName){
		return fileName.matches("[\\w]{2}\\.[\\d]*\\.rp\\.xml");
		
	}
	
	public static XmlObjectType computeClass(byte[] xml) throws Exception{
		XmlObjectType result = new XmlObjectType();
		if (!UrlHelper.isXml(xml)){
			String json = new String(xml, StandardCharsets.UTF_8);
			if (json.startsWith("{\n")){
				result.setClazz(Rulebook.class);
				result.setExonym(true);
				return result;

			} else {
				return null;

			}
		} else {
			String xml0 = new String(xml);
			String[] xmlLn = xml0.split("\n");

			if (xmlLn.length < 2){
				return null;
				
			}
			String xml1 = xmlLn[1];
			
			/*
			 * IDMX Objects
			 */
			if (xml1.contains("CredentialSpecification")){
				result.setClazz(CredentialSpecification.class);
				
			} else if (xml1.contains("IssuerParameters")){
				result.setClazz(IssuerParameters.class);
				
			} else if (xml1.contains("RevocationAuthorityParameters")){
				result.setClazz(RevocationAuthorityParameters.class);
						
			} else if (xml1.contains("PresentationPolicy")){
				result.setClazz(PresentationPolicy.class);
						
			} else if (xml1.contains("SystemParameters")){
				result.setClazz(SystemParameters.class);
						
			} else if (xml1.contains("RevocationInformation")){
				result.setClazz(RevocationInformation.class);

			} else if (xml1.contains("RevocationHistory")){
				result.setClazz(RevocationHistory.class);

			} else if (xml1.contains("InspectorPublicKey")){
				result.setClazz(InspectorPublicKey.class);

			} else if (xml1.contains("IssuancePolicy")){
				result.setClazz(IssuancePolicy.class);

			} else if (xml1.contains("PresentationToken")){
				result.setClazz(PresentationToken.class);

			}
			if (result.getClazz()!=null){
				result.setExonym(false);
				return result; 
				
			}
			/*
			 * Existence Objects
			 */
			if (xml1.contains("ActionParameters")){
				result.setClazz(ActionParameters.class);
				
			} else if (xml1.contains("ActionHistory")){
				result.setClazz(ActionParametersHistoryItem.class);
				
			} else if (xml1.contains("TrustNetwork")){
				result.setClazz(TrustNetwork.class);
				
			} else if (xml1.contains("AssetLibrary")){
				result.setClazz(AssetLibrary.class);
				
			} else if (xml1.contains("SelectedDelegates")){
				result.setClazz(SelectedDelegates.class);
				
			} else if (xml1.contains("DemParameters")){
				result.setClazz(DemParameters.class);
				
			} else if (xml1.contains("InteractiveActionData")){
				result.setClazz(InteractiveActionData.class);
				
			} else if (xml1.contains("KeyContainer")){
				result.setClazz(KeyContainer.class);
				
			} else if (xml1.contains("Motion")){
				result.setClazz(Motion.class);
				
			} else if (xml1.contains("AnonCredentialParameters")){
				result.setClazz(AnonCredentialParameters.class);
				
			} else if (xml1.contains("AnonPresentationToken")){
				result.setClazz(AnonPresentationToken.class);
				
			} else if (xml1.contains("Challenge")){
				result.setClazz(Challenge.class);
				
			} else if (xml1.contains("MintedAnonCredential")){
				result.setClazz(MintedAnonCredential.class);
				
			} else if (xml1.contains("RegistrationParameters")) {
				result.setClazz(RegistrationParameters.class);


			} else {
				throw new Exception("Unhandled XML indentifier: " + xml1);
				
			}
			result.setExonym(true);
			return result;
			
		}
	}

	public static ConcurrentHashMap<String, Object> deserializeOpenXml(ConcurrentHashMap<String, ByteArrayBuffer> xmlBytes){
		ConcurrentHashMap<String, Object> result = new ConcurrentHashMap<String, Object>();
		if (xmlBytes!=null){
			for (String f : xmlBytes.keySet()){
				try {
					byte[] in = xmlBytes.get(f).getRawData();
					XmlObjectType o = XmlHelper.computeClass(in);
					logger.info("Reading file " + f);
					Object obj = null;
					String inString = new String(in, StandardCharsets.UTF_8);
					
					if (o.isExonym()){
						if (inString.startsWith("{")){
							obj = JaxbHelper.jsonToClass(inString, o.getClazz());

						} else {
							obj = JaxbHelper.xmlToClass(inString, o.getClazz());

						}
					} else {
						JAXBElement<?> el = JaxbHelperClass.deserialize(inString);
						obj = el.getValue();
						
					}
					result.put(f, obj);
					
				} catch (Exception e) {
					logger.error("Ignoring an unrecognized object ", e);
					
				}
			}
		}
		return result;
	} 
	
	// TODO Tidy up
	public static ConcurrentHashMap<String, ByteArrayBuffer> openXmlBytesAtUrl(URL nodeUrl) throws Exception {
		String root = nodeUrl.toString();
		String descUrl = null;
		String filename = "/rulebook.json";
		if (root.endsWith("/")){
			if (root.contains("x-source")){
				descUrl = root.replaceAll("/x-source/", filename);

			} else if (root.contains("x-node")){
				descUrl = root.replaceAll("/x-node/", filename);

			}
		} else {
			if (root.contains("x-source")){
				descUrl = root.replaceAll("/x-source", filename);

			} else if (root.contains("x-node")){
				descUrl = root.replaceAll("/x-node", filename);

			}
		}
		byte[] desc = UrlHelper.read(new URL(descUrl));

		URL url = new URL(root + "/signatures.xml");
		
		ConcurrentHashMap<String, ByteArrayBuffer> result = new ConcurrentHashMap<>();
		result.put("description", new ByteArrayBuffer(desc));
		KeyContainerWrapper kcPublic = null; 
		try {
			byte[] signaturesXml = UrlHelper.read(url);
			String xmlString = new String(signaturesXml, "UTF8");
			kcPublic = new KeyContainerWrapper(JaxbHelper.xmlToClass(xmlString, KeyContainer.class));
			result.put("signatures.xml", new ByteArrayBuffer(xmlString.getBytes()));
			
		} catch (Exception e1) {
			throw new HubException("There was no signatures file at the URL " + nodeUrl);
			
		}
		
		ArrayList<XKey> signatures = kcPublic.getKeyContainer().getKeyPairs();
		HashMap<URL, String> urlToFileName = new HashMap<URL, String>();


		for (XKey sig : signatures) {
			if (!sig.getKeyUid().equals(KeyContainerWrapper.TN_ROOT_KEY) && 
					!sig.getKeyUid().equals(KeyContainerWrapper.SIG_CHECKSUM)) {

				String fileName = XContainer.uidToXmlFileName(sig.getKeyUid());
				urlToFileName.put(new URL(root + "/" + fileName), fileName);
				
			}
		}
		for (URL xmlUrl : urlToFileName.keySet()) {
			byte[] content = null;
			boolean isTransfer= false; 
			try {
				content = UrlHelper.read(xmlUrl);
				
			} catch (Exception e) {
				content = fileDidNotExist(nodeUrl, xmlUrl, urlToFileName);
				isTransfer=true;
				
			}
			if (UrlHelper.isXml(content)) {
				String xmlString = new String(content, "UTF8");
				result.put(urlToFileName.get(xmlUrl), new ByteArrayBuffer(xmlString.getBytes()));
				if (isTransfer) {
					break;
					
				}
			} else {
				throw new SecurityException("The file " + urlToFileName.get(xmlUrl) + " was malformed");
				
			}
		}
		return result;
	}
	
	
	private static byte[] fileDidNotExist(URL sourceUrl, URL xmlUrl, HashMap<URL, String> urlToFileName) throws MalformedURLException {
		URL transferUrl = new URL(sourceUrl.toString() + "/transfer.xml");
		try {
			return UrlHelper.read(transferUrl);
			
		} catch (Exception e) {
			throw new SecurityException("The file " + urlToFileName.get(xmlUrl) + " was deleted without authorization - Node Invalid");
			
		}
	}

	public static ConcurrentHashMap<String, Object> openXmlFilesAtUrl(URI sourceUrl) throws UxException {
		File folder = new File(sourceUrl); 
		File[] files = folder.listFiles();
		ConcurrentHashMap<String, Object> result = new ConcurrentHashMap<>();
		
		if (files != null){
			for (File f : files){
				if (FileType.isXmlDocument(f.getName())){
					try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(f))){
						
						byte[] in = new byte[fis.available()];
						fis.read(in);
						
						if (UrlHelper.isXml(in)){
							try {
								XmlObjectType o = XmlHelper.computeClass(in);
								logger.info("Reading file " + f);
								Object obj = null; 
								
								if (o.isExonym()){
									obj = JaxbHelper.xmlToClass(new String(in), o.getClazz());
									
								} else {
									JAXBElement<?> el = JaxbHelperClass.deserialize(new String(in));
									obj = el.getValue();
									
								}
								result.put(f.getName(), obj);
								
							} catch (Exception e) {
								logger.info("Ignoring an unrecognized object at URL " + sourceUrl + f.getName());
								
							}
						} else {
							logger.info("The File " + f.getPath() + " was masquerading as an XML file");		
							
						}
					} catch (Exception e) {
						logger.error("Error", e);
						
					}
				} else {
					logger.info("The File " + f.getPath() + " was not an XML file");
					
				}
			}
		} else {
			throw new UxException(sourceUrl + " was empty");
			
		}
		return result;
	}
}
