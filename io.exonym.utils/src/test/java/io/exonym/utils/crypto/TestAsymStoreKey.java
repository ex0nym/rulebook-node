package io.exonym.utils.crypto;

import io.exonym.abc.util.JaxbHelper;
import io.exonym.lite.pojo.XKey;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.PassStore;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TestAsymStoreKey {
    
    private static final Logger logger = LogManager.getLogger(TestAsymStoreKey.class);
    private static byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);

    @Test
    void testPem() {
        try {
            AsymStoreKey key = new AsymStoreKey();
            String k = key.getPublicKeyAsPem();
            System.out.println(k);

        } catch (IOException e) {
            logger.error("Error", e);
            assert false;


        }
    }

    @Test
    void generateJwt() {
        AsymStoreKey key = new AsymStoreKey();
        String jwt = key.generateJwt("mqtt-client", 60);
        System.out.println(jwt);
        try {
            key.verifyJwt(jwt, null);
            assert true;

        } catch (Exception e) {
            logger.error("Error", e);
            assert false;
        }
        try {
            key.verifyJwt(jwt, "test");
            assert false;

        } catch (Exception e) {
            logger.error("Error", e);
            assert true;
        }
        try {
            key.verifyJwt(jwt, "mqtt-client");
            assert true;

        } catch (Exception e) {
            logger.error("Error", e);
            assert false;
        }


    }

    @Test
    void serializeReadUsePublicKeyThenPrivate() {
        try {
            AsymStoreKey keyIn = new AsymStoreKey();
            byte[] sig = keyIn.sign(bytes);

            XKey out = new XKey();
            out.setPrivateKey(keyIn.getEncryptedEncodedForm("password"));
            out.setPublicKey(keyIn.getPublicKey().getEncoded());
            out.setSignature(sig);
            //             Serialize
            String xml = JaxbHelper.serializeToXml(out, XKey.class);
            logger.info(xml);

            XKey in = JaxbHelper.xmlToClass(xml, XKey.class);
            //            blank()
            AsymStoreKey keyOut = AsymStoreKey.blank();

            //      Open only public key  -> [encrypt]
            keyOut.assembleKey(in.getPublicKey());
            byte[] enc = keyOut.encrypt(bytes);
            assert(encrypted(enc, bytes));

            // Try deciphering
            try {
                keyOut.decipher(enc);
                logger.error("Expected a Failure because the private key isn't loaded");

            } catch (Exception e) {
                assert(true);

            }
            Cipher cipher = CryptoUtils.generatePasswordCipher(Cipher.DECRYPT_MODE, "password", null);
            keyOut.assembleKey(in.getPublicKey(), in.getPrivateKey(), cipher);
            use(keyOut);

            byte[] signature = out.getSignature();
            assert(keyOut.verifySignature(bytes, signature));

        } catch (Exception e) {
            logger.error("Error", e);
            assert(false);

        }
    }

    @Test
    void newquay() {
        try {
            AsymStoreKey key = new AsymStoreKey();
            use(key);

        } catch (Exception e) {
            e.printStackTrace();
            assert (false);
        }

    }



    @Test
    void staticMethods() {
        try {
            AsymStoreKey key = new AsymStoreKey();
            byte[] enc = AsymStoreKey.encrypt(bytes, key.getPublicKey());
            assert(encrypted(enc, bytes));

            PassStore ps = new PassStore("password", false);

            byte[] encSk = key.getEncryptedEncodedForm(ps.getEncrypt());

            AsymStoreKey in = AsymStoreKey.tryKey(encSk, ps.getDecipher());
            byte[] sig = in.sign(bytes);
            in.assembleKey(key.getPublicKey());
            assert(in.verifySignature(bytes, sig));

            use(in);

            String b64 =  key.getPublicKeyB64();
            AsymStoreKey reassemble = AsymStoreKey.blank();
            reassemble.assembleKey(b64);
            assert reassemble.getPublicKey() !=null;


            try {
                PassStore psWrong = new PassStore("wrong", false);
                AsymStoreKey k = AsymStoreKey.tryKey(encSk, psWrong.getDecipher());
                assert(k==null);

            } catch (Exception e) {
                logger.error("Er)", e);
                assert(false);

            }
        } catch (Exception e) {
            logger.debug("Error", e);
            assert(false);

        }
    }

    // Save
    //            build()
    //            getEncryptedEncodedForm
    //            getEncryptedEncodedForm
    //            save
    //            open
    //            open
    //            tryKey
    //      isKeyPair



    private static void use(AsymStoreKey key) throws Exception {
        //            encrypt(byte)
        byte[] encrypted0 = key.encrypt(bytes);
        assert(encrypted(encrypted0, bytes));
        try {
            key.decipherWithPublicKey(encrypted0);
            assert(false);

        } catch (Exception e) {
            logger.info("PASSED: Tried to decipher with a public key instead of a private and got an exception" + e.getMessage());

        }
        //            decipher(byte)
        byte[] deciphered0 = key.decipher(encrypted0);
        assert(deciphered(deciphered0, bytes));

        //            encryptWithPrivateKey
        byte[] encrypted1 = key.encryptWithPrivateKey(bytes);
        assert(encrypted(encrypted1, bytes));
        try {
            key.decipher(encrypted1);
            assert(false);

        } catch (Exception e) {
            logger.info("PASSED: Tried to decipher with a private key instead of a public and got an exception"  + e.getMessage());
            
        }
        //            decipherWithPublicKey
        byte[] deciphered1 = key.decipherWithPublicKey(encrypted1);
        assert(deciphered(deciphered1, bytes));

        //            sign
        byte[] signed = key.sign(bytes);

        //            verifySignature
        assert(key.verifySignature(bytes, signed));

    }

    private static boolean encrypted(byte[] encrypted, byte[] bytes) throws Exception {
        String b64Target = Base64.encodeBase64String(bytes);
        String b64Enc = Base64.encodeBase64String(encrypted);
        logger.info("TARGET: " + b64Target + " ENCRYPTED: " + b64Enc);
        if (b64Target.equals(b64Enc)){
            throw new Exception("Epic Fail - Not Encrypted " + b64Enc + " && " + b64Target + " should not be the same");

        }
        return true;
    }

    private static boolean deciphered(byte[] deciphered0, byte[] bytes) throws Exception {
        String b64Target = Base64.encodeBase64String(bytes);
        String b64Dec = Base64.encodeBase64String(deciphered0);
        logger.info("TARGET: " + b64Target + " DECIPHERED: " + b64Dec);
        if (!b64Target.equals(b64Dec)){
            throw new Exception("Epic Fail - Not Deciphered " + b64Dec + " && " + b64Target + " should be the same");

        }
        return true;

    }
}
