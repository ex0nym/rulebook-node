package io.exonym.lite.authenticators;

import io.exonym.lite.exceptions.UxException;

import java.net.MalformedURLException;
import java.net.URL;

public class RootPropertyFeatures {

    private final String primaryAdminUsername;
    private final String nodeRoot;

    private final String dbPrefix;

    private final URL dbUrl;
    private final String dbUsername;
    private final String dbPassword;

    protected RootPropertyFeatures() throws UxException, MalformedURLException {
        this.dbUrl = new URL(mandatory("DB_URL"));
        this.dbUsername = mandatory("DB_USERNAME");
        this.dbPassword= mandatory("DB_PASSWORD");

        this.dbPrefix=optional("RULEBOOK_NODE_PREFIX", null);

        String source = "Before setting up as a Host or Source, set this parameter a strong password.";
        this.nodeRoot = messageOnFail("RULEBOOK_NODE_ROOT", source);

        String admin = "Set an administrator username that is not obvious.";
        this.primaryAdminUsername = messageOnFail("PRIMARY_ADMIN_USERNAME", admin);

    }

    protected String messageOnFail(String env, String message) throws UxException {
        try {
            return mandatory(env);

        } catch (UxException e) {
            throw new UxException("'" + env + "' is mandatory: " + message);

        }
    }

    protected String optional(String env, String def) {
        String var = System.getenv(env);
        if (var!=null){
            return var.trim();

        } else {
            return def;

        }
    }

    protected String mandatory(String env) throws UxException {
        String var = System.getenv(env);
        if (var==null || var.equals("")){
            throw new UxException("The environment variable '" + env + "' has not been set and is mandatory.");

        } else {
            return var.trim();

        }
    }

    protected String getPrimaryAdminUsername() {
        return primaryAdminUsername;
    }

    protected String getNodeRoot() {
        return nodeRoot;
    }

    protected String getDbPrefix() {
        return dbPrefix;
    }

    protected URL getDbUrl() {
        return dbUrl;
    }

    protected String getDbUsername() {
        return dbUsername;
    }

    protected String getDbPassword() {
        return dbPassword;
    }
}
