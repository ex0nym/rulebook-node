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
        String[] nibbles = extractNibbles(x0);
        Poke poke = openPoke(primaryUrl, xOrYList);
        KeyContainerWrapper kcw = openSignatures(primaryUrl, nibbles[0], xOrYList);
        ExonymMatrix matrix = openTargetMatrix(primaryUrl, nibbles[0], nibbles[1], xOrYList);
        authenticate(root, poke, kcw, matrix, nibbles[0], nibbles[1]);

    }

    protected abstract void authenticate(String root, Poke poke, KeyContainerWrapper kcw,
                                         ExonymMatrix matrix, String nibble3,
                                         String nibble6) throws Exception;

    private Poke openPoke(String primaryUrl, String xOrYList) throws Exception {
        String path = computePokePathToFile(xOrYList);
        String pokeUrl = primaryEndPoint(primaryUrl, path);
        try {
            String poke = new String(
                    UrlHelper.read(new URL(pokeUrl)),
                    StandardCharsets.UTF_8);
            logger.debug(poke);

            Gson gson = new Gson();
            return gson.fromJson(poke, Poke.class);

        } catch (IOException e) {
            return handlePokeNotFound(e);

        }
    }

    protected abstract Poke handlePokeNotFound(Exception e) throws Exception;

    private KeyContainerWrapper openSignatures(String primaryUrl, String nibble3, String xOrYList) throws Exception {
        String n3Path = computeN3PathToFile(nibble3, xOrYList);
        String sigUrl = primaryEndPoint(primaryUrl, n3Path);
        try {
            this.signatureByteData = UrlHelper.read(new URL(sigUrl));
            String xml = new String(signatureByteData, StandardCharsets.UTF_8);
            logger.debug(xml);
            return new KeyContainerWrapper(
                    JaxbHelper.xmlToClass(xml, KeyContainer.class)

            );
        } catch (Exception e) {
            return handleKeyContainerNotFound(e);

        }
    }

    protected abstract KeyContainerWrapper handleKeyContainerNotFound(Exception e) throws Exception ;

    private ExonymMatrix openTargetMatrix(String primaryUrl, String nibble3, String nibble6, String xOrYList) throws Exception {
        if (this.matrixByteData==null){
            String path = computeN6PathToFile(nibble3, nibble6, xOrYList);
            String target = primaryEndPoint(primaryUrl, path);
            logger.debug(target);
            try {
                this.matrixByteData = UrlHelper.read(new URL(target));
                String json = new String(matrixByteData, StandardCharsets.UTF_8);
                Gson g = new Gson();
                return g.fromJson(json, ExonymMatrix.class);

            } catch (Exception e) {
                return handleMatrixNotFound(e, nibble6);

            }
        } else {
            String json = new String(matrixByteData, StandardCharsets.UTF_8);
            Gson g = new Gson();
            return g.fromJson(json, ExonymMatrix.class);

        }
    }

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

    public static String[] extractNibbles(String exonymHex) {
        if (exonymHex==null){
            throw new NullPointerException();

        }
        String nibble2 = exonymHex.substring(0, 3);
        String nibble4 = exonymHex.substring(0, 6);
        return new String[] {nibble2, nibble4};

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
