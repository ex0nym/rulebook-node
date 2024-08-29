package io.exonym.rulebook.context;

import io.exonym.actor.actions.MyTrustNetworkAndKeys;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.PassStore;
import io.exonym.lite.standard.WhiteList;
import io.exonym.utils.storage.KeyContainer;
import io.exonym.utils.storage.KeyContainerWrapper;
import io.exonym.lite.pojo.XKey;
import io.exonym.rulebook.schema.IdContainer;
import io.exonym.utils.storage.TrustNetwork;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

public class ModeratorNotificationSigner {

    private final static ModeratorNotificationSigner instance;
    private static final Logger logger = LogManager.getLogger(ModeratorNotificationSigner.class);
    private final ConcurrentHashMap<String, AsymStoreKey> nodeUidToRootKey = new ConcurrentHashMap<>();

    private void openKey(String nodeUid, PassStore store) throws Exception {
        TrustNetwork tn = null;
        if (WhiteList.isModeratorUid(nodeUid)){
            MyTrustNetworkAndKeys mts = new MyTrustNetworkAndKeys(false);
            tn = mts.getTrustNetwork();

        } else {
            MyTrustNetworkAndKeys mts = new MyTrustNetworkAndKeys(true);
            tn = mts.getTrustNetwork();

        }
        logger.debug("Node name " + tn.getNodeInformation().getNodeUid());

        IdContainer x = new IdContainer(tn.getNodeInformation().getNodeName());

        KeyContainer kcSecret = x.openResource("keys.xml");

        KeyContainerWrapper kcwSecret = new KeyContainerWrapper(kcSecret);
        XKey xkey = kcwSecret.getKey(KeyContainerWrapper.TN_ROOT_KEY);
        AsymStoreKey key = AsymStoreKey.build(xkey.getPublicKey(),
                xkey.getPrivateKey(), store.getDecipher());

        nodeUidToRootKey.put(nodeUid, key);

    }

    protected byte[] sign(byte[] bytesToSign, String nodeUid, PassStore store) throws Exception {
        if (!nodeUidToRootKey.containsKey(nodeUid)){
            openKey(nodeUid, store);

        }
        AsymStoreKey key = nodeUidToRootKey.get(nodeUid);
        if (key==null){
            throw new HubException("Null Key at Signer");

        } if (bytesToSign!=null && bytesToSign.length==0){
            throw new HubException("Null bytes to sign");

        } if (bytesToSign==null){
            throw new HubException("Null bytes to sign");

        }
        return key.sign(bytesToSign);

    }

    static{
        instance = new ModeratorNotificationSigner();

    }

    protected static ModeratorNotificationSigner getInstance() {
        return instance;
    }

    private ModeratorNotificationSigner() {
    }
}
