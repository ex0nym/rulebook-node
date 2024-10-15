package io.exonym.lite.authenticators;

import com.beust.jcommander.internal.Nullable;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.google.gson.JsonObject;
import io.exonym.lite.connect.WebUtils;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.IApiKey;
import io.exonym.lite.pojo.IUser;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.PassStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public abstract class StandardAuthenticator {

    protected ConcurrentHashMap<String, PassStore> sessionAdministratorPassStore = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<String, PassStore> sessionPrimaryPassStore = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<String, PassStore> sessionMemberPassStore = new ConcurrentHashMap<>();

    protected ConcurrentHashMap<String, IApiKey> kidToKey = new ConcurrentHashMap<>();

    private boolean primaryAdminSetup = false;
    
    private static final Logger logger = LogManager.getLogger(StandardAuthenticator.class);


    protected StandardAuthenticator() {

    }

    protected synchronized PassStore getPassStore(HttpServletRequest request) throws Exception {
        return getPassStore(request, null);

    }

    protected synchronized PassStore getPassStore(HttpServletRequest request, String context) throws Exception {
        String id = (context != null ? context : request.getSession().getId());
        logger.debug("Finding Store for Context " + id);
        if (sessionAdministratorPassStore.containsKey(id)) {
            return sessionAdministratorPassStore.get(id);

        } else if (sessionPrimaryPassStore.containsKey(id)) {
            return sessionPrimaryPassStore.get(id);

        } else if (sessionMemberPassStore.containsKey(id)) {
            return sessionMemberPassStore.get(id);

        } else {
            throw new AuthenticationException();

        }
    }


    protected synchronized void endSession(HttpServletRequest request,
                                           @Nullable HttpServletResponse response) throws Exception {
        endSession(request, response, null);

    }

    protected synchronized void endSession(HttpServletRequest request,
                                           @Nullable HttpServletResponse response,
                                           String context) throws Exception {
        String id = (context != null ? context : request.getSession().getId());
        PassStore store = null;
        if (sessionAdministratorPassStore.containsKey(id)) {
            store = sessionAdministratorPassStore.remove(id);

        } else if (sessionPrimaryPassStore.containsKey(id)) {
            store = sessionPrimaryPassStore.remove(id);

        } else if (sessionMemberPassStore.containsKey(id)) {
            store = sessionMemberPassStore.remove(id);

        }
        if (response != null) {
            JsonObject o = new JsonObject();
            o.addProperty("complete", store != null);
            WebUtils.respond(response, o);

        }
    }

    protected synchronized boolean isPrimaryAdministrator(String sessionId) {
        return this.sessionPrimaryPassStore.containsKey(sessionId);

    }

    protected synchronized boolean isAdministrator(String sessionId) {
        return this.sessionAdministratorPassStore.containsKey(sessionId);

    }

    protected synchronized boolean isMember(String sessionId) {
        return this.sessionMemberPassStore.containsKey(sessionId);

    }

    protected void changePassword(HashMap<String, String> in,
                                  HttpServletRequest request, HttpServletResponse resp) throws UxException {
        try {
            String username = in.get("username");
            String newPass = in.get("password");

            if (newPass != null && username != null) {
                IUser user = openUser(username);
                // If the user hasn't previously authenticated, this throws failed to auth
                getPassStore(request, in.get("context"));
                String v = CryptoUtils.computeSha256HashAsHex(newPass);
                user.setV(v);
                user.setRequiresPassChange(false);
                updateUser(user);
                endSession(request, null, in.get("context"));

                if (in.containsKey("context")) {
                    testSession(user, newPass, in.get("context"), resp);

                } else {
                    openSession(user, newPass, request, resp);

                }
            } else {
                throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, "username", "newPass");

            }
        } catch (Exception e) {
            throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, e);

        }
    }

    protected abstract void updateUser(IUser user) throws Exception;

    protected synchronized void authenticateAdministratorOrMember(HashMap<String, String> in,
                                                                  HttpServletRequest request,
                                                                  HttpServletResponse resp) throws UxException {
        try {
            String username = in.get("username");
            String password = in.get("password");
            if (username != null && password != null) {
                IUser user = openUser(username);
                String v = CryptoUtils.computeSha256HashAsHex(password);

                if (v.equals(user.getV())) {
                    if (in.containsKey("context")) {
                        logger.debug(">>>>>>>>>>>>>>>>>>>>> <<<<<<<<<<<<<<<<<<<<<<<<<<<");
                        logger.debug("\t\t\t Using Test Session");
                        logger.debug(">>>>>>>>>>>>>>>>>>>>> <<<<<<<<<<<<<<<<<<<<<<<<<<<");
                        testSession(user, password, in.get("context"), resp);

                    } else {
                        openSession(user, password, request, resp);

                    }
                } else {
                    throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE);
                }
            } else {
                throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, "username", "password");

            }
        } catch (Exception e) {
            throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, e);

        }
    }

    /**
     * Test method allowing workaround of new session of cors.  Can be deleted when complete.
     *
     * @param user
     * @param password
     * @param context
     * @param resp
     * @throws Exception
     */
    protected void testSession(IUser user, String password, String context, HttpServletResponse resp) throws Exception {
        PassStore p = new PassStore(password, false);
        p.setUsername(user.getUsername());
        logger.debug("Using Context in Test Session " + context);

        if (user.getType().equals(IUser.I_USER_PRIMARY_ADMIN)) {
            logger.debug("Primary Admin");
            this.sessionPrimaryPassStore.put(context, p);

        } else if (user.getType().equals(IUser.I_USER_ADMIN)) {
            logger.debug("Secondary Admin");
            this.sessionAdministratorPassStore.put(context, p);

        } else if (user.getType().equals(IUser.I_USER_MEMBER)) {
            logger.debug("Member");
            this.sessionMemberPassStore.put(context, p);

        } else {
            throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE);

        }

        JsonObject o = new JsonObject();
        if (user.isRequiresPassChange()) {
            p.setUsername("reset");
            o.addProperty("pass", true);
            WebUtils.respond(resp, o);

        } else {
            WebUtils.success(resp);

        }

    }

    protected void openSession(IUser user, String password,
                               HttpServletRequest request, @Nullable HttpServletResponse resp) throws Exception {
        PassStore p = new PassStore(password, false);
        p.setUsername(user.getUsername());
        String id = request.getSession().getId();
        logger.debug("Attempting to add - " + user.getType());

        if (user.getType().equals(IUser.I_USER_PRIMARY_ADMIN)) {
            this.sessionPrimaryPassStore.put(id, p);

        } else if (user.getType().equals(IUser.I_USER_ADMIN)) {
            this.sessionAdministratorPassStore.put(id, p);

        } else if (user.getType().equals(IUser.I_USER_MEMBER)) {
            this.sessionMemberPassStore.put(id, p);

        } else {
            throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, "Member");

        }
        if (resp != null) {
            JsonObject o = new JsonObject();
            if (user.isRequiresPassChange()) {
                p.setUsername("reset");
                o.addProperty("pass", true);
                WebUtils.respond(resp, o);

            } else {
                WebUtils.success(resp);

            }
        } else {
            logger.debug("Response was Null - must be handled elsewhere");

        }
    }

    protected abstract IUser openUser(String username) throws Exception;

    protected synchronized void authenticateApiKey(HashMap<String, String> in) throws Exception {
        String kid = in.get("kid");
        String key = in.get("key");
        authenticateApiKey(kid, key);

    }

    protected synchronized void authenticateApiKey(String kid, String key) throws Exception {
        try {
            if (kid != null && key != null) {
                try {
                    if (!kidToKey.containsKey(kid)) {
                        IApiKey user = openApiKey(kid);
                        kidToKey.put(kid, user);

                    }
                    IApiKey user = kidToKey.get(kid);
                    if (user != null) {
                        String u = CryptoUtils.computeSha256HashAsHex(key);

                        if (!u.equals(user.getKey())) {
                            throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, "key or kid was incorrect");

                        }
                    } else {
                        throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, "kid or key was unknown");

                    }
                } catch (NoDocumentException e) {
                    throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, "kid or key was incorrect");

                }
            } else {
                throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, "Entry Level Failure");

            }
        } catch (Exception e) {
            throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, e);

        }
    }

    protected abstract IApiKey openApiKey(String kid) throws Exception;

    protected boolean isPrimaryAdminSetup() {
        return primaryAdminSetup;
    }

    protected void setPrimaryAdminSetup(boolean primaryAdminSetup) {
        this.primaryAdminSetup = primaryAdminSetup;
    }

}
