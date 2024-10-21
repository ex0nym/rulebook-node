package io.exonym.actor.actions;

import eu.abc4trust.xml.CredentialSpecification;
import io.exonym.abc.util.FileType;
import io.exonym.abc.util.UidType;
import io.exonym.helpers.BuildCredentialSpecification;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.NetworkMapItem;
import io.exonym.lite.pojo.Rulebook;
import io.exonym.lite.standard.WhiteList;
import io.exonym.utils.storage.CacheContainer;
import io.exonym.utils.RulebookVerifier;
import io.exonym.utils.storage.ExternalResourceContainer;
import io.exonym.utils.storage.IdContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URL;
import java.rmi.server.UID;

public final class PkiExternalResourceContainer extends ExternalResourceContainer {
	
	private static final Logger logger = LogManager.getLogger(PkiExternalResourceContainer.class);
	private static PkiExternalResourceContainer instance = null;
	private CacheContainer cache = null;
	private AbstractNetworkMap networkMap;


	static {
		if (instance==null){
			instance = new PkiExternalResourceContainer();
			logger.info("Instantiated the Ledger Container");
			
		}
	}
	
	public synchronized static PkiExternalResourceContainer getInstance(){
		return instance;
		
	}

	public void close() throws Exception {
		instance = null;
		
	}
	public synchronized <T> T openResource(String fileName) throws Exception {
		return openResource(fileName, false);
	}
	
	public synchronized <T> T openResource(String fileName, boolean overrideCache) throws Exception {
		logger.info("Opening External Resource: Overriding Cache ("+overrideCache+")" + fileName);

		T t = null;
		if (!overrideCache) {
			t = this.getCache().open(fileName);
			logger.debug("Tried cache and it returned=" +t);

		}
		if (t == null) {
			boolean isCredSpec = FileType.isCredentialSpecification(fileName);

			if (FileType.isPresentationPolicy(fileName) || isCredSpec) {
				t = verifyLead(fileName);

				if (isCredSpec){
					this.cache.store(t);

				} else {
					logger.debug("Not storing credential in cache: isCredSpec=" + isCredSpec + " " + fileName);

				}
				return t;

			} else if (FileType.isInspectorPublicKey(fileName) ||
					FileType.isRevocationInformation(fileName) ||
					FileType.isRevocationAuthority(fileName) ||
					FileType.isIssuerParameters(fileName)) {
				return verifyModerator(fileName);

			} else if (FileType.isRulebook(fileName)){
				throw new UxException("CANNOT_OPEN_RULEBOOK_EXTERNALLY");

			} else {
				throw new UxException(ErrorMessages.INCORRECT_PARAMETERS,
						"No such file",
						fileName
						);
			}
		}
		return t;
	}

	private void tryLocal(String fileName) throws Exception {
		URI target = URI.create(IdContainer.fileNameToUid(fileName));
		UIDHelper helper = new UIDHelper(target);
		helper.out();


	}

	private <T> T verifyLead(String fileName) throws Exception {
		if (FileType.isCredentialSpecification(fileName)){
			if (Rulebook.isSybil(fileName)){
				if (Rulebook.isSybilMain(fileName)){
					RulebookVerifier verifier = new RulebookVerifier(new URL(Rulebook.SYBIL_URL_MAIN));
					return (T) BuildCredentialSpecification.buildSybilCredentialSpecification(verifier);

				} else {
					RulebookVerifier verifier = new RulebookVerifier(new URL(Rulebook.SYBIL_URL_TEST));
					return (T) BuildCredentialSpecification.buildSybilCredentialSpecification(verifier);

				}
			} else {
				return (T) new BuildCredentialSpecification(
						UIDHelper.fileNameToUid(fileName), true)
						.getCredentialSpecification();
			}
		} else {
			URI modUid = UIDHelper.computeModUidFromMaterialUID(UIDHelper.fileNameToUid(fileName));
			URI leadUid = UIDHelper.computeLeadUidFromModUid(modUid);
			NetworkMapItem nmi = getNetworkMap().nmiForNode(leadUid);

			NodeVerifier leadVerifier = new NodeVerifier(nmi.getNodeUID());

			CacheContainer cache = this.getCache();
			cache.store(leadVerifier.getPresentationPolicy());
			cache.store(leadVerifier.getCredentialSpecification());
			cache.store(leadVerifier.getRulebook());
			if (FileType.isPresentationPolicy(fileName)) {
				return (T) leadVerifier.getPresentationPolicy();
			} else if (FileType.isRulebook(fileName)){
				return (T) leadVerifier.getRulebook();
			} else {
				throw new UxException(ErrorMessages.FILE_NOT_FOUND, fileName);

			}
		}
	}

	public CacheContainer getCache() {
		if (cache==null){
			throw new RuntimeException("You should set the cache and the network map on start of the node");

		}
		return cache;
	}

	public AbstractNetworkMap getNetworkMap() {
		if (networkMap==null){
			throw new RuntimeException("You should set the cache and the network map on start of the node");

		}
		return networkMap;
	}

	private <T> T verifyModerator(String fileName) throws Exception {
		URI searchingFor = UIDHelper.fileNameToUid(fileName);
		URI modUID = UIDHelper.computeModUidFromMaterialUID(searchingFor);
		NodeVerifier modVerifier = new NodeVerifier(modUID);

		CacheContainer cache = this.getCache();
		TrustNetworkWrapper tnw = new TrustNetworkWrapper(modVerifier.getTargetTrustNetwork());
		URI issuerUID = tnw.getMostRecentIssuerParameters();
		UIDHelper helper = new UIDHelper(issuerUID);
		cache.store(modVerifier.getIssuerParameters(helper.getIssuerParametersFileName()));
		cache.store(modVerifier.getRevocationAuthorityParameters(helper.getRevocationAuthorityFileName()));
		cache.store(modVerifier.getInspectorPublicKey());
		cache.store(modVerifier.getRevocationInformation(helper.getRevocationInformationFileName()));
		return cache.open(fileName);

	}

	public void setNetworkMapAndCache(AbstractNetworkMap nmi, CacheContainer cache){
		this.networkMap = nmi;
		this.cache = cache;

	}

}
