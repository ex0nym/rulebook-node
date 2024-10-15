package io.exonym.rulebook.context;

import io.exonym.lite.pojo.ExoNotify;
import io.exonym.lite.standard.AsymStoreKey;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Authenticator {


    private static final Logger logger = LogManager.getLogger(Authenticator.class);

    protected static void authenticateNotify(ExoNotify notify, AsymStoreKey hostKey) throws Exception {
        byte[] toSign = ExoNotify.signatureOn(notify);
        common(notify);
        verifyAndJoin(notify);
        verify(toSign, notify, hostKey);

    }

    private static void common(ExoNotify notify) throws Exception {
        if (notify.getT()==null){
            throw new Exception();

        }
        if (notify.getNodeUid()==null){
            throw new Exception();

        }
    }

    private static void verifyAndJoin(ExoNotify notify) throws Exception {
        if (notify.getType().equals(ExoNotify.TYPE_JOIN)){
            if (notify.getHashOfX0()==null){
                throw new Exception();

            } if (notify.getNibble6()==null){
                throw new Exception();

            }
        }
    }

    private static void verify(byte[] toSign, ExoNotify notify, AsymStoreKey hostKey) throws Exception {
        logger.debug(notify.getNodeUid() + " using Key " + hostKey.getPublicKeyB64());
        byte[] sig = Base64.decodeBase64(notify.getSigB64());
        hostKey.verifySignature(toSign, sig);

    }
}
