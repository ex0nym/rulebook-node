package io.exonym.rulebook.context;

import io.exonym.actor.actions.MyTrustNetworkAndKeys;
import io.exonym.actor.actions.MyTrustNetworks;
import io.exonym.lite.couchdb.QueryStandard;
import io.exonym.lite.parallel.ModelCommandProcessor;
import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.NetworkMapItem;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.utils.storage.TrustNetwork;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkPublicKeyManager {
    
    private static final Logger logger = LogManager.getLogger(NetworkPublicKeyManager.class);
    
    private static NetworkPublicKeyManager instance;
    
    static {
        instance = new NetworkPublicKeyManager();
        
    }

    protected static NetworkPublicKeyManager getInstance(){
        return instance;
    }
    private final ConcurrentHashMap<String, AsymStoreKey> keys = new ConcurrentHashMap<>();
    private CouchRepository<NetworkMapItem> repo;
    private final QueryStandard queryNetwork = new QueryStandard();

    private NetworkPublicKeyManager(){
        try {
            this.repo= CouchDbHelper.repoNetworkMapItem();
            this.queryNetwork.addFieldSelector(NetworkMapItem.FIELD_PUBLIC_KEY);
            MyTrustNetworks myTrustNetworks = new MyTrustNetworks();

            if (myTrustNetworks.isLeader()){
                TrustNetwork tn = myTrustNetworks.getLead().getTrustNetwork();
                AsymStoreKey key = myTrustNetworks.getLead().getMyPublicKey();
                keys.put(tn.getNodeInformation().getNodeUid().toString(), key);

            } if (myTrustNetworks.isModerator()){
                TrustNetwork tn = myTrustNetworks.getModerator().getTrustNetwork();
                AsymStoreKey key = myTrustNetworks.getModerator().getMyPublicKey();
                keys.put(tn.getNodeInformation().getNodeUid().toString(), key);

            }
        } catch (Exception e) {
            logger.info("Error at Public Key Manage - MUST FIX: ", e);

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
}




