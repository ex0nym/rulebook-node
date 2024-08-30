package io.exonym.rulebook.context;

import eu.abc4trust.xml.*;
import io.exonym.actor.actions.*;
import io.exonym.helpers.BuildIssuancePolicy;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.pojo.*;
import io.exonym.lite.standard.Const;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.PassStore;
import io.exonym.utils.RulebookVerifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JoinSupportSingleton {
    private static final Logger logger = LogManager.getLogger(JoinSupportSingleton.class);
    private static JoinSupportSingleton instance;
    private final RulebookNodeProperties props = RulebookNodeProperties.instance();

    private final ConcurrentHashMap<String , ExonymIssuer> requests = new ConcurrentHashMap<>();

    private final NetworkMapWeb networkMap;
    private final PassStore store;
    private final NetworkMapItemModerator myAdvocate;
    private final UIDHelper myAdvocateHelper;
    private final UIDHelper sybilHelper;
    private final Cache cache;
    private final PkiExternalResourceContainer external = PkiExternalResourceContainer.getInstance();



    private RulebookVerifier rulebookVerifier;
    private final ArrayList<String> myRules;

    private JoinSupportSingleton() throws Exception {
        this.networkMap = new NetworkMapWeb();
        this.cache = new Cache();
        this.external.setNetworkMapAndCache(this.networkMap, this.cache);

        this.store = new PassStore(props.getNodeRoot(), false);
        this.myAdvocate = this.networkMap.nmiForMyNodesModerator();
        this.myAdvocateHelper = new UIDHelper(this.myAdvocate.getLastIssuerUID());
        this.sybilHelper = new UIDHelper(this.networkMap.nmiForSybilModTest().getLastIssuerUID());

        this.rulebookVerifier = openRulebookVerifier();
        this.myRules = rulebookVerifier.toRulebookUIDs();

    }

    protected IssuancePolicy buildIssuancePolicy() throws Exception {
        PresentationPolicy pp = JoinHelper.baseJoinPolicy(rulebookVerifier, this.sybilHelper.getIssuerParameters(),
                external, CryptoUtils.generateNonce(32));

        BuildIssuancePolicy bip = new BuildIssuancePolicy(pp,
                myAdvocateHelper.getCredentialSpec(), myAdvocateHelper.getIssuerParameters());

        return bip.getIssuancePolicy();
    }


    protected void loadAdvocateWithSybilCryptoMaterials(ExonymIssuer issuer) throws Exception {
        loadIssuer(issuer, sybilHelper);
        loadIssuer(issuer, myAdvocateHelper);

    }

    protected void loadIssuer(ExonymIssuer myAdvocateIssuer, UIDHelper helper) throws Exception {
        myAdvocateIssuer.openResourceIfNotLoaded(helper.getCredentialSpec());
        myAdvocateIssuer.openResourceIfNotLoaded(helper.getIssuerParameters());
        myAdvocateIssuer.openResourceIfNotLoaded(helper.getRevocationInfoParams());
        myAdvocateIssuer.openResourceIfNotLoaded(helper.getRevocationAuthority());

    }

    protected RulebookVerifier openRulebookVerifier() throws Exception {
        NodeData node = NodeStore.getInstance().openThisAdvocate();
        String target = node.getNodeUrl().toString()
                .replaceAll(Const.MODERATOR, "rulebook.json");
        this.rulebookVerifier = new RulebookVerifier(new URL(target));
        return this.rulebookVerifier;

    }

    protected NetworkMapWeb getNetworkMap() {
        return networkMap;
    }

    protected PassStore getStore() {
        return store;
    }

    protected NetworkMapItemModerator getMyAdvocate() {
        return myAdvocate;
    }

    protected UIDHelper getMyAdvocateHelper() {
        return myAdvocateHelper;
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

    protected ArrayList<String> getMyRules() {
        return myRules;
    }

    static {
        try {
            instance = new JoinSupportSingleton();

        } catch (Exception e) {
            logger.error("Critical Error - This node will not accept subscriptions", e);

        }
    }

    public static JoinSupportSingleton getInstance() {
        return instance;

    }
}
