package io.exonym.rulebook.context;

import io.exonym.actor.actions.MyTrustNetworks;
import io.exonym.actor.actions.NodeManager;
import io.exonym.lite.couchdb.QueryStandard;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.pojo.NetworkMapItem;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.Const;
import io.exonym.lite.standard.PassStore;
import io.exonym.rulebook.schema.IdContainer;
import io.exonym.utils.storage.KeyContainer;
import io.exonym.utils.storage.KeyContainerWrapper;
import io.exonym.utils.storage.TrustNetwork;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
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

    private final MyTrustNetworks myTrustNetworks = new MyTrustNetworks();

    private NetworkPublicKeyManager(){
        try {
            this.repo= CouchDbHelper.repoNetworkMapItem();
            this.queryNetwork.addFieldSelector(NetworkMapItem.FIELD_PUBLIC_KEY);

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

    protected synchronized AsymStoreKey getKey(URI modUid) throws Exception {
        if (keys.containsKey(modUid.toString())){
            return keys.get(modUid.toString());

        } else {
            logger.debug("Opening Key " + modUid);
            queryNetwork.addCriteria(NetworkMapItem.FIELD_NODE_UID, modUid.toString());
            byte[] keyBytes = repo.read(queryNetwork).get(0).getPublicKeyB64();
            AsymStoreKey key = AsymStoreKey.blank();
            key.assembleKey(keyBytes);
            this.keys.put(modUid.toString(), key);
            return key;

        }
    }

    public AsymStoreKey openMyModKey(PassStore store) throws Exception {
        if (myTrustNetworks.isModerator()){
            String username = myTrustNetworks.getModerator()
                    .getTrustNetwork().getNodeInformation().getNodeName();

            IdContainer id = new IdContainer(username);

            KeyContainerWrapper kcPrivate = new KeyContainerWrapper(
                    (KeyContainer) id.openResource(Const.KEYS));

            return NodeManager.openKey(kcPrivate.getKey(KeyContainerWrapper.TN_ROOT_KEY), store);

        } else {
            throw new HubException("NOT_A_MODERATOR");

        }
    }

    public AsymStoreKey openMyLeadKey(PassStore store) throws Exception {
        if (myTrustNetworks.isLeader()){
            String username = myTrustNetworks.getLead()
                    .getTrustNetwork().getNodeInformation().getNodeName();

            IdContainer id = new IdContainer(username);

            KeyContainerWrapper kcPrivate = new KeyContainerWrapper(
                    (KeyContainer) id.openResource(Const.KEYS));

            return NodeManager.openKey(kcPrivate.getKey(
                    KeyContainerWrapper.TN_ROOT_KEY), store);

        } else {
            throw new HubException("NOT_A_LEAD");

        }
    }



    public MyTrustNetworks getMyTrustNetworks() {
        return myTrustNetworks;
    }
}




