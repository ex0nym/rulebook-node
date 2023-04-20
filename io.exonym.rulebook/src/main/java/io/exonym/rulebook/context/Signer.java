package io.exonym.rulebook.context;

import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.PassStore;
import io.exonym.utils.storage.KeyContainer;
import io.exonym.utils.storage.KeyContainerWrapper;
import io.exonym.lite.pojo.XKey;
import io.exonym.lite.pojo.NodeData;
import io.exonym.rulebook.schema.XNodeContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

public class Signer {

    private final static Signer instance;

    
    private static final Logger logger = LogManager.getLogger(Signer.class);
    
    private final ConcurrentHashMap<String, AsymStoreKey> nodeUidToRootKey = new ConcurrentHashMap<>();

    private void openKey(String nodeUid, PassStore store) throws Exception {
        NodeStore ns = NodeStore.getInstance();
        NodeData node = ns.openThisAdvocate();
        logger.debug("node name" + node.getName() + " " + node.get_id());

        XNodeContainer x = new XNodeContainer(node.getName());
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

//    protected void checkSignature(String nodeUid, byte[] sig, byte[] data, PassStore store) throws Exception {
//        if (!nodeUidToRootKey.containsKey(nodeUid)){
//            openKey(nodeUid, store);
//
//        }
//        AsymStoreKey key = nodeUidToRootKey.get(nodeUid);
//        if (!key.verifySignature(data, sig)){
//            throw new UxException("Signature did not verify");
//
//        }
//    }

    static{
        instance = new Signer();

    }

    protected static Signer getInstance() {
        return instance;
    }

    private Signer() {
    }
}
