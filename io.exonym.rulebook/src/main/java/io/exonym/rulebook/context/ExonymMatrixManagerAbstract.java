package io.exonym.rulebook.context;

import com.google.gson.Gson;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.actions.ExonymMatrix;
import io.exonym.actor.actions.RulebookNodeProperties;
import io.exonym.actor.storage.Poke;
import io.exonym.actor.storage.SignatureOn;
import io.exonym.lite.connect.UrlHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.XKey;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.utils.storage.KeyContainer;
import io.exonym.utils.storage.KeyContainerWrapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public abstract class ExonymMatrixManagerAbstract {

    private static final Logger logger = LogManager.getLogger(ExonymMatrixManagerAbstract.class);

    protected final io.exonym.actor.actions.RulebookNodeProperties props = RulebookNodeProperties.instance();

    protected final String xList = "/exox";
    protected final String yList = "/exoy";
    protected static final int NYM_LENGTH = 1024;

    protected byte[] signatureByteData = null;
    protected byte[] matrixByteData = null;

    protected Poke poke = null;
    protected KeyContainerWrapper kcw = null;

    protected ExonymMatrix matrix = null;

    protected void openExonymMatrix(String primaryUrl, String x0, String xOrYList, String root) throws Exception {
        this.matrix = null; this.poke = null; this.kcw = null;
        String[] nibbles = ExonymMatrix.extractNibbles(x0);
        Poke poke = openPoke(primaryUrl, xOrYList);
        logger.info("t of poke=" + poke.getT());
        KeyContainerWrapper kcw = openSignatures(primaryUrl, nibbles[0], xOrYList);
        ExonymMatrix matrix = openTargetMatrix(primaryUrl, nibbles[0], nibbles[1], xOrYList);
        authenticate(root, poke, kcw, matrix, nibbles[0], nibbles[1]);

    }

    protected void openExonymMatrix(String primaryUrl, String x0Hash, String n6, String xOrYList, String root) throws Exception {
        logger.info("Using x0PrimePath");
        this.matrix = null; this.poke = null; this.kcw = null;
        String[] nibbles = ExonymMatrix.extractNibbles(n6);
        Poke poke = openPoke(primaryUrl, xOrYList);
        logger.info("t of poke=" + poke.getT());
        KeyContainerWrapper kcw = openSignatures(primaryUrl, nibbles[0], xOrYList);
        ExonymMatrix matrix = openTargetMatrix(primaryUrl, nibbles[0], nibbles[1], xOrYList);
        authenticate(root, poke, kcw, matrix, nibbles[0], nibbles[1]);

    }


    protected abstract void authenticate(String root, Poke poke, KeyContainerWrapper kcw,
                                         ExonymMatrix matrix, String nibble3,
                                         String nibble6) throws Exception;

    protected abstract Poke openPoke(String primaryUrl, String xOrYList) throws Exception;

    protected abstract Poke handlePokeNotFound(Exception e) throws Exception;

    protected abstract KeyContainerWrapper openSignatures(String primaryUrl, String nibble3, String xOrYList) throws Exception;

    protected abstract KeyContainerWrapper handleKeyContainerNotFound(Exception e) throws Exception ;

    protected abstract ExonymMatrix openTargetMatrix(String primaryUrl, String nibble3, String nibble6, String xOrYList) throws Exception;

    protected abstract ExonymMatrix handleMatrixNotFound(Exception e, String n6) throws Exception;

    protected void authPoke(Poke poke, AsymStoreKey signatureKey) throws UxException {
        try {
            byte[] toSign = SignatureOn.poke(poke);
            byte[] signature = Base64.decodeBase64(
                    poke.getSignatures().get(Poke.SIGNATURE_ON_POKE));
            signatureKey.verifySignature(toSign, signature);

        } catch (Exception e) {
            throw new UxException(ErrorMessages.DB_TAMPERING, e);

        }
    }

    protected void authSigs(Poke poke, String nibble3, AsymStoreKey signatureKey) throws Exception {
        byte[] signature = Base64.decodeBase64(poke.getSignatures().get(nibble3));
        signatureKey.verifySignature(signatureByteData, signature);

    }

    protected void authMatrix(KeyContainerWrapper kcw, String nibble6, AsymStoreKey signatureKey) throws Exception {
        XKey sig = kcw.getKey(URI.create(nibble6));
        signatureKey.verifySignature(matrixByteData, sig.getSignature());

    }

    protected String computePokePathToFile(String xOrYList) {
        return xOrYList + "/poke.json";

    }

    protected String computeN3PathToFile(String nibble3, String xOrYList) {
        return xOrYList + "/" + nibble3 + "/" + nibble3 + ".xml";

    }

    protected String computeN6PathToFile(String nibble3, String nibble6, String xOrYList) {
        return xOrYList + "/" + nibble3 + "/" + nibble6 + ".json";

    }

    protected String primaryEndPoint(String url, String targetLeadsWithForwardSlash) {
        return url + targetLeadsWithForwardSlash;

    }

    private String[] dualEndPoints(String primaryUrl, String failoverUrl, String targetLeadsWithForwardSlash) {
        String p = primaryUrl + targetLeadsWithForwardSlash;
        String f = failoverUrl + targetLeadsWithForwardSlash;
        return new String[] {p, f};

    }

    protected class SignatureAndSerializedFile {

        private final byte[] sig;
        private final String file;

        public SignatureAndSerializedFile(byte[] sig, String file) {
            this.sig = sig;
            this.file = file;
        }

        public byte[] getSig() {
            return sig;
        }

        public String getFile() {
            return file;
        }
    }
}
