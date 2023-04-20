package io.exonym.x0basic;

import io.exonym.lite.couchdb.QueryStandard;
import io.exonym.lite.parallel.ModelCommandProcessor;
import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.NetworkMapItem;
import io.exonym.lite.standard.AsymStoreKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class KeyManager extends ModelCommandProcessor {

    private static final Logger logger = LogManager.getLogger(KeyManager.class);
    private final ConcurrentHashMap<String, AsymStoreKey> keys = new ConcurrentHashMap<>();
    private CouchRepository<NetworkMapItem> repo;
    private final QueryStandard queryNetwork = new QueryStandard();

    protected KeyManager() throws Exception {
        super(1, "KeyManager", 10000);
        try {
            this.repo= CouchDbHelper.repoNetworkMapItem();
            this.queryNetwork.addFieldSelector(NetworkMapItem.FIELD_PUBLIC_KEY);

        } catch (Exception e) {
            throw e;

        }
    }

    protected synchronized AsymStoreKey getKey(URI hostUuid) throws Exception {
        if (keys.containsKey(hostUuid.toString())){
            return keys.get(hostUuid.toString());

        } else {
            logger.debug("Opening Key " + hostUuid);
            queryNetwork.addCriteria(NetworkMapItem.FIELD_NODE_UID, hostUuid.toString());
            byte[] keyBytes = repo.read(queryNetwork).get(0).getPublicKeyB64();
            AsymStoreKey key = AsymStoreKey.blank();
            key.assembleKey(keyBytes);
            this.keys.put(hostUuid.toString(), key);
            return key;

        }
    }

    @Override
    protected void periodOfInactivityProcesses() {
        this.keys.clear();

    }

    @Override
    protected void close() throws Exception {
        super.close();
    }

    @Override
    protected ArrayBlockingQueue<Msg> getPipe() {
        return super.getPipe();
    }


    @Override
    protected void receivedMessage(Msg msg) {
        logger.debug("Not listening to messages here");

    }
}




