package io.exonym.actor.actions;

import io.exonym.actor.storage.RootProperties;
import io.exonym.actor.storage.SFTPLogonData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;

public class RulebookNodeProperties extends RootProperties {
    
    private static final Logger logger = LogManager.getLogger(RulebookNodeProperties.class);

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
            throw new RuntimeException("Instantiation Error", e);

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
    protected String getBroadcastUrl() {
        return super.getBroadcastUrl();
    }


    @Override
    public String getRulebookNodeURL() {
        return super.getRulebookNodeURL();
    }

    @Override
    protected String getAuthorizedDomain() {
        return super.getAuthorizedDomain();
    }

    @Override
    protected SFTPLogonData getTokenTransfer() {
        return super.getTokenTransfer();
    }
}
