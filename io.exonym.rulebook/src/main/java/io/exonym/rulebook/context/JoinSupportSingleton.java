package io.exonym.rulebook.context;

import eu.abc4trust.xml.*;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.actions.*;
import io.exonym.helpers.BuildIssuancePolicy;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.*;
import io.exonym.lite.standard.Const;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.PassStore;
import io.exonym.rulebook.schema.IdContainer;
import io.exonym.utils.RulebookVerifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class JoinSupportSingleton {
    private static final Logger logger = LogManager.getLogger(JoinSupportSingleton.class);
    private static JoinSupportSingleton instance;
    private final RulebookNodeProperties props = RulebookNodeProperties.instance();

    private final NetworkMapWeb networkMap;
    private final PassStore store;
    private final NetworkMapItemModerator myModerator;
    private final NetworkMapItemLead myLead;
    private final UIDHelper myModeratorHelper;
    private final UIDHelper sybilHelper;
    private final Cache cache;

    private final ExonymMatrixManagerLocal exonymMatrixManagerLocal;

    private ExonymIssuer myModIssuer;

    private ExonymInspector myModInspector = null;
    private IdContainer myModContainer;

    private final PkiExternalResourceContainer external = PkiExternalResourceContainer.getInstance();

    private RulebookVerifier rulebookVerifier;
    private final ArrayList<URI> myRules;

    private final RulebookGovernor governor;

    private final CredentialSpecification sybilCs;
    private final CredentialSpecification modCs;

    private JoinSupportSingleton() throws Exception {

        MyTrustNetworks mtn = new MyTrustNetworks();
        boolean prod = mtn.getRulebook().getDescription().isProduction();
        this.networkMap = new NetworkMapWeb();
        this.cache = new Cache();
        this.external.setNetworkMapAndCache(this.networkMap, this.cache);

        this.store = new PassStore(props.getNodeRoot(), false);

        this.myModerator = this.networkMap.nmiForMyNodesModerator();
        this.myLead = this.networkMap.nmiForMyModeratorsLead();

        this.myModeratorHelper = new UIDHelper(this.myModerator.getLastIssuerUID());

        NetworkMapItemModerator nmiSybilMod = (prod ?
                this.networkMap.nmiForSybilMainNet() :
                this.networkMap.nmiForSybilModTest());

        this.sybilHelper = new UIDHelper(nmiSybilMod.getLastIssuerUID());

        this.myModContainer = new IdContainer(myModerator.getModeratorName());

        this.modCs = myModContainer.openResource(
                myModeratorHelper.getCredentialSpecFileName());

        this.sybilCs = external.openResource(
                sybilHelper.getCredentialSpecFileName());

        this.rulebookVerifier = openRulebookVerifier();
        this.myRules = rulebookVerifier.toRulebookUIDs();

        Rulebook leadRulebook = openLeadRulebook();

        this.governor = new RulebookGovernor(leadRulebook);
        this.governor.addRules(this.rulebookVerifier
                .getRulebook()
                .getRuleExtensions());
        this.governor.out();

        myModIssuer = new ExonymIssuer(myModContainer);
        myModIssuer.openContainer(store.getDecipher());

        loadModeratorWithSybilCryptoMaterials(myModIssuer);

        this.exonymMatrixManagerLocal = new ExonymMatrixManagerLocal
                (myModContainer, myRules, myModerator, props.getNodeRoot());


    }

    private Rulebook openLeadRulebook() {
        URI node = myLead.getRulebookNodeURL();

        Path pathToRb = Path.of("/", Const.STATIC, Const.RULEBOOK_JSON);
        String target = node + pathToRb.toString();
        logger.info(pathToRb);
        logger.info(target);

        try {
            return new RulebookVerifier(new URL(target)).getRulebook();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void reopen() throws Exception {
        myModIssuer.openContainer(store.getDecipher());
        loadModeratorWithSybilCryptoMaterials(myModIssuer);

    }

    public ExonymInspector getMyModInspector(PresentationToken token, PassStore store) throws Exception {
        if (myModInspector==null){
            myModInspector = new ExonymInspector(myModContainer);
            loadAvailableInspectorParameters(token, store);

        }
        return myModInspector;
    }

    private void loadAvailableInspectorParameters(PresentationToken sampleToken,
                                                  PassStore store) throws Exception {
        if (store == null){
            throw new Exception();

        }
        for (CredentialInToken cit : sampleToken.getPresentationTokenDescription().getCredential()) {
            URI cUid = cit.getCredentialSpecUID();
            URI iUid = cit.getIssuerParametersUID();
            UIDHelper helper = new UIDHelper(iUid);
            helper.out();

            URI raUid = helper.getRevocationAuthority();

            myModInspector.openResourceIfNotLoaded(cUid);
            myModInspector.openResourceIfNotLoaded(iUid);
            myModInspector.openResourceIfNotLoaded(raUid);

            for (AttributeInToken ait : cit.getDisclosedAttribute()) {
                URI insUid = ait.getInspectorPublicKeyUID();
                URI inssUid = URI.create(insUid.toString() + "s");

                try {
                    logger.info("About to open inss resource `"  +
                            myModContainer.getUsername() + "` " + inssUid + " " + store);

                    SecretKey sk = myModContainer.openResource(inssUid, store.getDecipher());
                    logger.info("Inspector key opened (should not be null): " + sk);
                    myModInspector.addInspectorSecretKey(insUid, sk);
                    myModInspector.openResourceIfNotLoaded(insUid);

                } catch (Exception e) {
                    URI modUid = UIDHelper.computeModUidFromMaterialUID(insUid);
                    String modName = UIDHelper.computeModNameFromModUid(modUid);
                    String leadName = UIDHelper.computeLeadNameFromModOrLeadUid(modUid);
                    throw new UxException("Moderated by: "
                            + leadName.toUpperCase()
                            + "~" + modName.toUpperCase(), e);

                }
            }
        }
    }


    protected IssuancePolicy buildIssuancePolicy(boolean appeal) throws Exception {
        ArrayList<CredentialSpecification> cspecs = new ArrayList<>();
        cspecs.add(sybilCs);
        byte[] nonce = null;

        if (appeal){
            nonce = CryptoUtils.generateNonce(16);

        } else {
            nonce = CryptoUtils.generateNonce(32);

        }
        PresentationPolicy pp = JoinHelper.baseJoinPolicy(
                rulebookVerifier,
                this.sybilHelper.getIssuerParameters(),
                external, cspecs, nonce);

        BuildIssuancePolicy bip = new BuildIssuancePolicy(pp,
                myModeratorHelper.getCredentialSpec(),
                myModeratorHelper.getIssuerParameters());

        return bip.getIssuancePolicy();
    }


    protected void loadModeratorWithSybilCryptoMaterials(ExonymIssuer issuer) throws Exception {
        loadIssuer(issuer, sybilHelper);
        loadIssuer(issuer, myModeratorHelper);

    }

    protected void loadIssuer(ExonymIssuer myModIssuer, UIDHelper helper) throws Exception {
        myModIssuer.openResourceIfNotLoaded(helper.getCredentialSpec());
        myModIssuer.openResourceIfNotLoaded(helper.getIssuerParameters());
        myModIssuer.openResourceIfNotLoaded(helper.getRevocationInfoParams());
        myModIssuer.openResourceIfNotLoaded(helper.getRevocationAuthority());
        if (!Rulebook.isSybil(helper.getRulebookUID())){
            myModIssuer.openResourceIfNotLoaded(helper.getInspectorParams());
        }
    }

    protected RulebookVerifier openRulebookVerifier() throws Exception {
        Path r = Path.of(Const.PATH_OF_STATIC, Const.RULEBOOK_JSON);
        logger.debug("Path=" + r);
        String rb = new String(Files.readAllBytes(r), StandardCharsets.UTF_8);
        Rulebook rulebook = JaxbHelper.jsonToClass(rb, Rulebook.class);

//        NodeData node = NodeStore.getInstance().openThisModerator();
//        String target = node.getNodeUrl().toString()
//                .replaceAll(Const.MODERATOR + "/", "rulebook.json");
        this.rulebookVerifier = new RulebookVerifier(rulebook);
        return this.rulebookVerifier;

    }

    protected NetworkMapWeb getNetworkMap() {
        return networkMap;
    }

    protected PassStore getStore() {
        return store;
    }

    protected NetworkMapItemModerator getMyModerator() {
        return myModerator;
    }

    protected UIDHelper getMyModeratorHelper() {
        return myModeratorHelper;
    }

    protected UIDHelper getSybilHelper() {
        return sybilHelper;
    }

    protected Cache getCache() {
        return cache;
    }

    protected PkiExternalResourceContainer getExternal() {
        return external;
    }

    protected RulebookVerifier getRulebookVerifier() {
        return rulebookVerifier;
    }

    protected ArrayList<URI> getMyRules() {
        return myRules;
    }

    protected CredentialSpecification getMyModCs() {
        return modCs;
    }

    protected ExonymIssuer getMyModIssuer() {
        return myModIssuer;
    }

    protected IdContainer getMyModContainer() {
        return myModContainer;
    }

    public NetworkMapItemLead getMyLead() {
        return myLead;
    }

    public RulebookGovernor getGovernor() {
        return governor;
    }

    public CredentialSpecification getSybilCs() {
        return sybilCs;
    }

    public CredentialSpecification getModCs() {
        return modCs;
    }

    static {
        try {
            instance = new JoinSupportSingleton();

        } catch (Exception e) {
            logger.error("Critical Error - This node will not accept subscriptions", e);

        }
    }

    public ExonymMatrixManagerLocal getExonymMatrixManagerLocal() {
        return exonymMatrixManagerLocal;
    }

    public static JoinSupportSingleton getInstance() {
        return instance;

    }

}
