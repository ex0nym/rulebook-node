package io.exonym.actor.actions;

import io.exonym.abc.util.FileType;
import io.exonym.helpers.BuildCredentialSpecification;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.NetworkMapItem;
import io.exonym.lite.pojo.Rulebook;
import io.exonym.utils.storage.CacheContainer;
import io.exonym.utils.RulebookVerifier;
import io.exonym.utils.storage.ExternalResourceContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URL;

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
		if (fileName == null) {
			throw new NullPointerException("File Name");

		}
		T t = null;
		if (!overrideCache) {
			t = this.getCache().open(fileName);

		}
		if (t == null) {
			if (FileType.isPresentationPolicy(fileName) ||
					FileType.isCredentialSpecification(fileName)) {
				return verifySource(fileName);

			} else if (FileType.isInspectorPublicKey(fileName) ||
					FileType.isRevocationInformation(fileName) ||
					FileType.isRevocationAuthority(fileName) ||
					FileType.isIssuerParameters(fileName)) {
				return verifyAdvocate(fileName);

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

	private <T> T verifySource(String fileName) throws Exception {
		if (FileType.isCredentialSpecification(fileName)){
			if (Rulebook.isSybil(fileName)){
				RulebookVerifier verifier = new RulebookVerifier(new URL("https://trust.exonym.io/sybil-rulebook.json"));
				return (T) BuildCredentialSpecification.buildSybilCredentialSpecification(verifier);

			} else {
				return (T) new BuildCredentialSpecification(
						UIDHelper.fileNameToUid(fileName), true)
						.getCredentialSpecification();
			}
		} else {
			URI sourceUID = UIDHelper.computeSourceUidFromNodeUid(UIDHelper.fileNameToUid(fileName));
			NetworkMapItem nmi = getNetworkMap().nmiForNode(sourceUID);
			NodeVerifier sourceVerifier = NodeVerifier.tryNode(nmi.getStaticURL0(),
					nmi.getRulebookNodeURL().resolve("static"), true, false);
			CacheContainer cache = this.getCache();
			cache.store(sourceVerifier.getPresentationPolicy());
			cache.store(sourceVerifier.getCredentialSpecification());
			cache.store(sourceVerifier.getRulebook());
			if (FileType.isPresentationPolicy(fileName)) {
				return (T) sourceVerifier.getPresentationPolicy();
			} else if (FileType.isRulebook(fileName)){
				return (T) sourceVerifier.getRulebook();
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

	private <T> T verifyAdvocate(String fileName) throws Exception {
		URI searchingFor = UIDHelper.fileNameToUid(fileName);
		URI advocateUID = UIDHelper.computeAdvocateUidFromMaterialUID(searchingFor);
		NetworkMapItem nmi = getNetworkMap().nmiForNode(advocateUID);
		NodeVerifier advocateVerifier = NodeVerifier.tryNode(nmi.getStaticURL0(),
				nmi.getRulebookNodeURL().resolve("static"), false, false);

		CacheContainer cache = this.getCache();
		TrustNetworkWrapper tnw = new TrustNetworkWrapper(advocateVerifier.getTargetTrustNetwork());
		URI issuerUID = tnw.getMostRecentIssuerParameters();
		UIDHelper helper = new UIDHelper(issuerUID);
		cache.store(advocateVerifier.getIssuerParameters(helper.getIssuerParametersFileName()));
		cache.store(advocateVerifier.getRevocationAuthorityParameters(helper.getRevocationAuthorityFileName()));
		cache.store(advocateVerifier.getInspectorPublicKey());
		cache.store(advocateVerifier.getRevocationInformation(helper.getRevocationInformationFileName()));
		return cache.open(fileName);

	}

	public void setNetworkMapAndCache(AbstractNetworkMap nmi, CacheContainer cache){
		this.networkMap = nmi;
		this.cache = cache;

	}

}
