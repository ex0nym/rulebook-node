package io.exonym.utils.storage;

import java.io.File;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;

import com.ibm.zurich.idmx.interfaces.util.BigInt;

import io.exonym.abc.util.IoMngt;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.lite.exceptions.UxException;
import io.exonym.helpers.XmlHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LedgerContainer extends ExternalResourceContainer {
	
	private static final Logger logger = LogManager.getLogger(LedgerContainer.class);
	
	private static LedgerContainer instance = null;
	
	public LedgerContainer() {
		super();
		
	}

	static {
		if (instance==null){
			instance = new LedgerContainer();
			logger.info("Instantiated the Ledger Container");
			
		}
	}
	
	public synchronized static LedgerContainer getInstance(){
		return instance;
		
	}
	
	public synchronized void broadcastNewGroup(RegistrationParameters params) throws Exception{
		LocalLedgerGroup group = new LocalLedgerGroup();
		group.setAccumulator(params.getAccumulatorBase());
		group.setGroupUid(params.getGroupUid());
		String groupContainer = JaxbHelper.serializeToXml(group, LocalLedgerGroup.class);
		String registrationParams = JaxbHelper.serializeToXml(params, RegistrationParameters.class);
		String groupFn = fileNameFromUid(params.getGroupUid());
		String paramFn = IdContainer.uidToFileName(params.getGroupUid()) + ".rp.xml";
		IoMngt.saveToFile(groupContainer, ledger.toString() + groupFn, false);
		IoMngt.saveToFile(registrationParams, ledger.toString() + paramFn, false);
		
	}

	private String filterForCountry(String isoCountryCode) {
		try {
			isoCountryCode = isoCountryCode.toLowerCase();
			File folder = new File(ledger.getPath());
			String[] files = folder.list();
			ArrayList<String> primer = new ArrayList<>();
			ArrayList<String> result = new ArrayList<>();
			int longestString = 0; 
			if (files!=null){
				for (int i = 0; i < files.length; i++) {
					if (XmlHelper.isRegistrationParameter(files[i])){
						primer.add(files[i]);
						if (files[i].length() > longestString){
							longestString = files[i].length();
							
						}
					}
				}
				for (String f : primer){
					if (f.length()==longestString){
						result.add(f);
						
					}
				}
				if (result.isEmpty()){
					return null;
					
				} else {
					return result.get(result.size()-1);
					
				}
			} else {
				return null; 
				
			}
		} catch (Exception e) {
			throw e; 
			
		}
	}


	private LocalLedgerGroup openGroup(URI uid) throws Exception {
		String fileName = fileNameFromUid(uid);
		try {
			File file = new File(ledger.resolve(fileName).getPath());
			if (file.exists()){
				return JaxbHelper.xmlFileToClass(
						JaxbHelper.fileToPath(file),
						LocalLedgerGroup.class);
				
			} else {
				throw new Exception();
				
			}
		} catch (Exception e) {
			UxException e0 = new UxException(String.format("The group %s, does not exist at %s.", uid, fileName));
			e0.initCause(e);
			throw e0;
			
				
		}
	}

	public synchronized BigInt discoverGroupForCountry(String isoCountryCode) throws Exception {
		if (!iso.isCountryCode(isoCountryCode)){
			throw new UxException(String.format("%s is not a valid ISO Country Code.", isoCountryCode));
			
		}
		throw new RuntimeException("discoverGroupForCountry() Not implemented");
		
	}
	
	public synchronized ArrayList<BigInteger> assembleAllCredentialsWithinGroup(URI groupUid) throws Exception{
		LocalLedgerGroup group = openGroup(groupUid);
		return group.getCredentials(); 
		
	}

	public synchronized BigInteger discoverAccumulatorValueForGroup(URI groupUid) throws Exception{
		LocalLedgerGroup group = openGroup(groupUid);
		return group.getAccumulator(); 
		
	}

}
