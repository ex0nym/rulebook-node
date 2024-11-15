package io.exonym.rulebook.context;

import io.exonym.actor.storage.RootProperties;
import io.exonym.actor.storage.SFTPLogonData;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.standard.AsymStoreKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;

public class RulebookNodeProperties extends RootProperties {
    
    private static final Logger logger = LogManager.getLogger(RulebookNodeProperties.class);

    private AsymStoreKey appDistributionKey = null;

    protected RulebookNodeProperties() throws Exception {
        super();

    }

    /*
     * Singleton
     */
    private static RulebookNodeProperties instance;

    static {
        try {
            instance = new RulebookNodeProperties();

        } catch (Exception e) {
            logger.error("Instantiation Error", e);

        }
    }

    public static RulebookNodeProperties instance() {
        return instance;

    }

    @Override
    protected String getDbUsername() {
        return super.getDbUsername();
    }

    @Override
    protected String getDbPassword() {
        return super.getDbPassword();
    }

    @Override
    protected SFTPLogonData getPrimarySftpCredentials() {
        return super.getPrimarySftpCredentials();
    }

    @Override
    protected String getPrimaryDomain() {
        return super.getPrimaryDomain();
    }

    @Override
    protected String getPrimaryStaticDataFolder() {
        return super.getPrimaryStaticDataFolder();
    }

    @Override
    protected String getNodeSupportNumber() {
        return super.getNodeSupportNumber();
    }

    @Override
    protected String getSpawnWiderNetworkFrom() {
        return super.getSpawnWiderNetworkFrom();
    }

    @Override
    protected String getIsoCountryCode() {
        return super.getIsoCountryCode();
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
    protected String getPrimaryAdminUsername() {
        return super.getPrimaryAdminUsername();
    }

    @Override
    protected String getNodeRoot() {
        return super.getNodeRoot();
    }


    @Override
    protected String getAuthorizedDomain() {
        return super.getAuthorizedDomain();
    }

    @Override
    protected SFTPLogonData getTokenTransfer() {
        return super.getTokenTransfer();
    }

    @Override
    protected boolean isOpenSubscription() {
        return super.isOpenSubscription();
    }

    @Override
    public String getRulebookNodeURL() {
        return super.getRulebookNodeURL();
    }

    @Override
    public boolean isAllowLeadPublication() {
        return super.isAllowLeadPublication();
    }

    @Override
    protected String getMqttBroker() {
        return super.getMqttBroker();
    }

    @Override
    protected String messageOnFail(String env, String message) throws UxException {
        return super.messageOnFail(env, message);
    }

    @Override
    protected String optional(String env, String def) {
        return super.optional(env, def);
    }

    @Override
    protected String mandatory(String env) throws UxException {
        return super.mandatory(env);
    }

    @Override
    public String getRulebookToVerifyUrn() {
        return super.getRulebookToVerifyUrn();
    }

    @Override
    protected String getMqttPassword() {
        return super.getMqttPassword();
    }
}
