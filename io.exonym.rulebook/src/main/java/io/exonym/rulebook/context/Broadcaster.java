package io.exonym.rulebook.context;

import io.exonym.lite.couchdb.ProtectedCouchRepository;
import io.exonym.lite.pojo.BroadcastInProgress;
import io.exonym.lite.pojo.ExoNotify;

import java.util.ArrayList;

@Deprecated
public class Broadcaster extends io.exonym.lite.connect.Broadcaster {

    private final CouchRepository<BroadcastInProgress> broadcastRepo;

    public Broadcaster(ExoNotify notify, ProtectedCouchRepository networkMapRepo) throws Exception {
        super(notify, networkMapRepo);
        this.broadcastRepo = CouchDbHelper.repoBroadcast();

    }

    @Override
    protected void addToUdpOutDb(ArrayList<BroadcastInProgress> broadcasts) throws Exception {
        this.broadcastRepo.bulkAdd(broadcasts);

    }
}
