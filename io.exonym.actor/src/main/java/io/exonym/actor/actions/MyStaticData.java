package io.exonym.actor.actions;

import com.sun.xml.ws.util.ByteArrayBuffer;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.helpers.XmlHelper;
import io.exonym.lite.connect.UrlHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.XKey;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.Const;
import io.exonym.lite.time.Timing;
import io.exonym.uri.NamespaceMngt;
import io.exonym.utils.storage.KeyContainer;
import io.exonym.utils.storage.KeyContainerWrapper;
import io.exonym.utils.storage.TrustNetwork;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class MyStaticData {

    private final Path sigPath, trustNetworkPath;


    private KeyContainerWrapper kcw;

    private final static Logger logger = Logger.getLogger(MyStaticData.class.getName());

    private AsymStoreKey myPublicKey;

    private final TrustNetworkWrapper trustNetworkWrapper;

    public MyStaticData(boolean amILead) throws Exception {
        long t0 = Timing.currentTime();
        Path root = null;
        if (amILead){
            root = Path.of(Const.PATH_OF_STATIC, Const.LEAD);
        } else {
            root = Path.of(Const.PATH_OF_STATIC, Const.MODERATOR);
        }
        this.sigPath = root.resolve(Const.SIGNATURES_XML);
        this.kcw = openSignature();

//        ConcurrentHashMap<String, ByteArrayBuffer> bytes = readLocalBytes(root);
//        ConcurrentHashMap<String, ByteArrayBuffer> signedBytes = NodeVerifier.computeBytesThatWereSigned(bytes);
//        ConcurrentHashMap<String, Object> deserialized = XmlHelper.deserializeOpenXml(bytes);

        this.trustNetworkPath = root.resolve(
                IdContainerJSON.uidToXmlFileName(Const.TRUST_NETWORK_UID));

        trustNetworkWrapper = determineOutcome();

        logger.info("Time to open trust network " + Timing.hasBeenMs(t0));

    }

    private TrustNetworkWrapper determineOutcome() throws UxException {
        try {

            XKey signature = kcw.getKey(Const.TRUST_NETWORK_UID);
            XKey xkey = kcw.getKey(KeyContainerWrapper.TN_ROOT_KEY);
            myPublicKey = AsymStoreKey.blank();
            myPublicKey.assembleKey(xkey.getPublicKey());
            return openTrustNetwork(this.trustNetworkPath, signature, myPublicKey);

        } catch (InvalidKeySpecException e) {
            throw new UxException(ErrorMessages.DB_TAMPERING, e);

        } catch (UxException e) {
            throw e;

        } catch (Exception e) {
            throw new UxException(ErrorMessages.RULEBOOK_NODE_NOT_INITIALIZED, e);

        }
    }

    private TrustNetworkWrapper openTrustNetwork(Path trustNetworkPath, XKey signature, AsymStoreKey mk) throws Exception {
        try {
            String tnw = Files.readString(trustNetworkPath);
            String stripped = NodeVerifier.stripStringToSign(tnw);
            mk.verifySignature(stripped.getBytes(StandardCharsets.UTF_8), signature.getSignature());
            TrustNetwork tn = JaxbHelper.xmlToClass(tnw, TrustNetwork.class);
            return new TrustNetworkWrapper(tn);

        } catch (Exception e) {
            throw new UxException(ErrorMessages.DB_TAMPERING);

        }

    }

    private KeyContainerWrapper openSignature() throws Exception {
        String sigs = Files.readString(this.sigPath);
        KeyContainer kcPublic = JaxbHelper.xmlToClass(sigs, KeyContainer.class);
        return new KeyContainerWrapper(kcPublic);

    }

    public TrustNetworkWrapper getTrustNetworkWrapper() {
        return trustNetworkWrapper;
    }

    public AsymStoreKey getMyPublicKey() {
        return myPublicKey;
    }

    public KeyContainerWrapper getKcw() {
        return kcw;
    }
}
