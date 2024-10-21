package io.exonym.rulebook.context;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import io.exonym.actor.actions.MyTrustNetworks;
import io.exonym.lite.authenticators.StandardAuthenticator;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.IApiKey;
import io.exonym.lite.pojo.IUser;
import io.exonym.lite.standard.PassStore;

import io.exonym.rulebook.exceptions.ItemNotFoundException;
import io.exonym.lite.pojo.NodeData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class IAuthenticator extends StandardAuthenticator {
    
    private static final Logger logger = LogManager.getLogger(IAuthenticator.class);


    /*
     *
     */
    private static IAuthenticator instance;

    static{
        instance = new IAuthenticator();
    }

    protected static IAuthenticator getInstance() {
        return instance;

    }

    private URI nodeUid = null;

    private HashSet<String> issuanceAuthenticated = new HashSet<>();

    private IAuthenticator() {}

    @Override
    protected IUser openUser(String username) throws Exception {
        CouchRepository<IUser> repo = openUserRepo();

        QueryBasic q = new QueryBasic();
        q.getSelector().put(IUser.FIELD_USERNAME, username);
        return repo.read(q).get(0);

    }

    @Override
    protected void updateUser(IUser user) throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.getDbUsers(), true);
        CouchRepository<IUser> repo = new CouchRepository<>(db, IUser.class);
        repo.update(user);

    }

    @Override
    protected IApiKey openApiKey(String kid) throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.getDbUsers(), true);
        CouchRepository<IApiKey> repo = new CouchRepository<>(db, IApiKey.class);
        QueryBasic q = new QueryBasic();
        q.getSelector().put(IApiKey.FIELD_API_KEY_UUID, kid);
        return repo.read(q).get(0);

    }

  private CouchRepository<IUser> openUserRepo() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.getDbUsers(), true);
        return new CouchRepository<>(db, IUser.class);

    }

    @Override
    protected synchronized void authenticateApiKey(HashMap<String, String> in) throws Exception {
        super.authenticateApiKey(in);
    }

    @Override
    protected synchronized PassStore getPassStore(HttpServletRequest request) throws Exception {
        return super.getPassStore(request);
    }

    @Override
    protected synchronized void endSession(HttpServletRequest request, HttpServletResponse response) throws Exception {
        super.endSession(request, response);
    }

    @Override
    protected synchronized boolean isPrimaryAdministrator(String sessionId) {
        return super.isPrimaryAdministrator(sessionId);
    }

    @Override
    protected synchronized boolean isAdministrator(String sessionId) {
        return super.isAdministrator(sessionId);
    }

    @Override
    protected void changePassword(HashMap<String, String> in, HttpServletRequest request, HttpServletResponse resp) throws UxException {
        super.changePassword(in, request, resp);
    }

    @Override
    protected synchronized void authenticateAdministratorOrMember(HashMap<String, String> in, HttpServletRequest request, HttpServletResponse resp) throws UxException {
        super.authenticateAdministratorOrMember(in, request, resp);
    }

    @Override
    protected boolean isPrimaryAdminSetup() {
        return super.isPrimaryAdminSetup();
    }

    @Override
    protected void setPrimaryAdminSetup(boolean primaryAdminSetup) {
        super.setPrimaryAdminSetup(primaryAdminSetup);
    }

    @Override
    protected synchronized void authenticateApiKey(String kid, String key) throws Exception {
        super.authenticateApiKey(kid, key);
    }

    public HashSet<String> getIssuanceAuthenticated() {
        return issuanceAuthenticated;
    }

}
