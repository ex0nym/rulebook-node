package io.exonym.rulebook.context;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.org.lightcouch.CouchDbException;
import io.exonym.lite.couchdb.UnprotectedCouchRepository;
import io.exonym.lite.pojo.*;
import io.exonym.lite.pojo.XKey;
import io.exonym.rulebook.schema.*;

public class CouchDbHelper {

    public static final String EXONYM_MAP = "exonym_map";
    public static final String VIOLATIONS = "violations";
    public static final String APPEALS = "appeals";
    public static final String LEADS = "leads";

    protected static String get(String name){
        String prefix = RulebookNodeProperties.instance().getDbPrefix();
        if (prefix==null || prefix.length()==0){
            return name;

        } else {
            return prefix + "_" + name;

        }
    }

    public static String getNetworkMap(){
        String prefix = RulebookNodeProperties.instance().getDbPrefix();
        if (prefix!=null){
            return prefix + "_network";

        } else {
            return "network";

        }
    }

    public static String getDbUsers(){
        String prefix = RulebookNodeProperties.instance().getDbPrefix();
        if (prefix!=null){
            return prefix + "_users";

        } else {
            return "users";

        }
    }

    public static String getDbContainers(){
        String prefix = RulebookNodeProperties.instance().getDbPrefix();
        if (prefix!=null){
            return prefix + "_containers";

        } else {
            return "containers";

        }
    }

    public static String getDbNode(){
        String prefix = RulebookNodeProperties.instance().getDbPrefix();
        if (prefix!=null){
            return prefix + "_node";

        } else {
            return "node";

        }
    }



    protected static CouchRepository<NetworkMapItem> repoNetworkMapItem() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.getNetworkMap(), true);
        return new CouchRepository<>(db, NetworkMapItem.class);

    }

    protected static CouchRepository<NetworkMapItemLead> repoNetworkMapItemSource() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.getNetworkMap(), true);
        return new CouchRepository<>(db, NetworkMapItemLead.class);

    }

    protected static CouchRepository<NetworkMapItemModerator> repoNetworkMapItemAdvocate() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.getNetworkMap(), true);
        return new CouchRepository<>(db, NetworkMapItemModerator.class);

    }

    protected static CouchRepository<NetworkMapNodeOverview> repoNetworkMapLeadOverview() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.getNetworkMap(), true);
        return new CouchRepository<>(db, NetworkMapNodeOverview.class);

    }

    protected static CouchRepository<IApiKey> repoApiKey() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.getDbUsers(), true);
        return new CouchRepository<>(db, IApiKey.class);

    }

    protected static CouchRepository<IUser> repoUsersAndAdmins() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.getDbUsers(), true);
        return new CouchRepository<>(db, IUser.class);

    }


    protected static CouchRepository<ExoMatrix> repoExoMatrix() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.get(EXONYM_MAP), true);
        return new CouchRepository<>(db, ExoMatrix.class);

    }

    protected static CouchRepository<Appeal> repoAppeals() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.get(APPEALS), true);
        return new CouchRepository<>(db, Appeal.class);

    }


    protected static CouchRepository<Vio> repoVio() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.get(VIOLATIONS), true);
        return new CouchRepository<>(db, Vio.class);

    }

    protected static void initDb() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        try {
            Database db = client.database("_users", false);
            new CouchRepository<>(db, IUser.class).ensureFullCommit();

        } catch (CouchDbException e) {
            client.database("_users", true);

        }
    }

    protected static CouchRepository<IdContainerSchema> repoContainerStore() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.getDbContainers(), true);
        return new CouchRepository<>(db, IdContainerSchema.class);

    }

    protected CouchRepository<IApiKey> openApiKeyRepo() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.getDbUsers(), true);
        return new CouchRepository<>(db, IApiKey.class);
    }


}
