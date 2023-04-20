package io.exonym.x0basic;

import io.exonym.lite.authenticators.RootPropertyFeatures;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.NetworkMapItem;
import io.exonym.lite.standard.AsymStoreKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class X0Properties extends RootPropertyFeatures {
    
    private static final Logger logger = LogManager.getLogger(X0Properties.class);

    private static X0Properties instance;

    private final String xNodeUrl;
    private final String exonymApiKey;
    private final String exonymApiKid;
    private final int udpPort;
    private final String outputPath;

    // Set on start-up  - Not env-var
    private URI advocateUID;
    private AsymStoreKey hostPublicKey;
    private NetworkMapItem ownNetworkMapItem = null;

    static{
        try {
            instance = new X0Properties();

        } catch (Exception e) {
            logger.error("Critical Error", e);

        }
    }

    protected X0Properties() throws UxException, MalformedURLException {
        super();
        exonymApiKey = System.getenv("EXONYM_API_KEY");
        exonymApiKid = System.getenv("EXONYM_API_KID");
        xNodeUrl = System.getenv("RULEBOOK_NODE_URL");
        outputPath = setDefault(System.getenv("BLOB_PATH"), "");
        String port = optional("UDP_PORT", "9090");
        udpPort = Integer.parseInt(port);

    }

    private String setDefault(String env, String def) {
        return (env == null ? def : env);
    }

    protected String getExonymApiKey() {
        return exonymApiKey;
    }

    protected String getExonymApiKid() {
        return exonymApiKid;
    }

    protected int getUdpPort() {
        return udpPort;
    }

    protected static X0Properties getInstance() {
        return instance;
    }

    protected String getOutputPath() {
        return outputPath;
    }

    protected String getxNodeUrl() {
        return xNodeUrl;
    }

    protected void init(URI advocateUID) throws Exception {
        this.advocateUID = advocateUID;
        CouchRepository<NetworkMapItem> repo = CouchDbHelper.repoNetworkMapItem();
        QueryBasic q = new QueryBasic();
        q.getSelector().put(NetworkMapItem.FIELD_NODE_UID, advocateUID.toString());
        this.ownNetworkMapItem = repo.read(q).get(0);
        byte[] k = ownNetworkMapItem.getPublicKeyB64();
        this.hostPublicKey = AsymStoreKey.blank();
        this.hostPublicKey.assembleKey(k);

    }

    protected URI getAdvocateUID() {
        return advocateUID;
    }

    protected AsymStoreKey getHostPublicKey() {
        return hostPublicKey;
    }

    protected NetworkMapItem getOwnNetworkMapItem() {
        return ownNetworkMapItem;
    }

    @Override
    protected String getPrimaryAdminUsername() {
        return super.getPrimaryAdminUsername();
    }

    @Override
    protected String getNodeRoot() {
        return super.getNodeRoot();
    }

    @Override
    protected String getDbPrefix() {
        return super.getDbPrefix();
    }

    @Override
    protected URL getDbUrl() {
        return super.getDbUrl();
    }

    @Override
    protected String getDbUsername() {
        return super.getDbUsername();
    }

    @Override
    protected String getDbPassword() {
        return super.getDbPassword();
    }
}
