package io.exonym.x0basic;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import io.exonym.lite.pojo.*;

public class CouchDbHelper {

    private static X0Properties props = X0Properties.getInstance();
    public static final String EXONYM_MAP = "exonym_map";
    public static final String VIOLATIONS = "violations";
    public static final String ROOT_KEY = "users";
    public static final String CHECKING = "udp_in";
    public static final String BROADCASTS = "udp_out";

    protected static String get(String name){
        String prefix = props.getDbPrefix();
        if (prefix!=null){
            return prefix + "_" + name;

        } else {
            return name;

        }
    }

    public static String getNetworkMap(){
        String prefix = props.getDbPrefix();
        if (prefix!=null){
            return prefix + "_network";

        } else {
            return "network";

        }
    }

    protected static CouchRepository<NetworkMapItem> repoNetworkMapItem() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.getNetworkMap(), true);
        return new CouchRepository<>(db, NetworkMapItem.class);

    }

    protected static CouchRepository<NetworkMapNodeOverview> repoNetworkMapSourceData() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.getNetworkMap(), true);
        return new CouchRepository<>(db, NetworkMapNodeOverview.class);

    }


    protected static CouchRepository<BroadcastInProgress> repoBroadcasts() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.get(BROADCASTS), true);
        return new CouchRepository<>(db, BroadcastInProgress.class);

    }

    protected static CouchRepository<ExoMatrix> repoExonymMap() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.get(EXONYM_MAP), true);
        return new CouchRepository<>(db, ExoMatrix.class);

    }

    protected static CouchRepository<Vio> repoVioMap() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.get(VIOLATIONS), true);
        return new CouchRepository<>(db, Vio.class);

    }

    protected static CouchRepository<ExoMatrix> repoApiKey() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.get(EXONYM_MAP), true);
        return new CouchRepository<>(db, ExoMatrix.class);

    }

    protected static CouchRepository<XKey> repoRootKey() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.get(ROOT_KEY), true);
        return new CouchRepository<>(db, XKey.class);

    }
}
