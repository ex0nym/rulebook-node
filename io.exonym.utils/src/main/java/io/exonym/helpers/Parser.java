package io.exonym.helpers;

import com.ibm.zurich.idmx.exception.SerializationException;
import com.ibm.zurich.idmx.jaxb.JaxbHelperClass;
import eu.abc4trust.xml.*;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.lite.pojo.IssuanceSigma;
import io.exonym.utils.storage.IdContainer;
import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;

public class Parser {

    public static final String PRESENTATION_POLICY = "presentationPolicy";
    public static final String PRESENTATION_TOKEN = "presentationToken";


    public static IssuanceMessageAndBoolean parseIssuanceMessageAndBoolean(String imabB64) throws SerializationException {
        return (IssuanceMessageAndBoolean) JaxbHelperClass.deserialize(b64ToXml(imabB64)).getValue();

    }

    public static String parseIssuanceMessageAndBoolean(IssuanceMessageAndBoolean imab) throws Exception {
        return Base64.encodeBase64String(IdContainer.convertObjectToXml(imab).getBytes(StandardCharsets.UTF_8));

    }

    public static String parseIssuanceMessage(IssuanceMessage im) throws Exception {
        String xmlResponse = JaxbHelper.serializeToXml(im, IssuanceMessage.class);
        return Base64.encodeBase64String(xmlResponse.getBytes(StandardCharsets.UTF_8));

    }

    public static PresentationToken parsePresentationToken(String pt)throws Exception {
        return (PresentationToken) JaxbHelperClass.deserialize(b64ToXml(pt)).getValue();
    }

    public static String parseIssuanceResult(IssuanceSigma issuanceResult) throws Exception {
        issuanceResult.setHello(null);
        issuanceResult.setIm(null);
        issuanceResult.setImab(null);
        return JaxbHelper.serializeToJson(issuanceResult, IssuanceSigma.class);

    }

    public static String b64ToXml(String b64){
        return new String(Base64.decodeBase64(b64.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);

    }

    public static String parsePresentationToken(PresentationToken token) throws Exception {
        return Base64.encodeBase64String(
                IdContainer.convertObjectToXml(token)
                .getBytes(StandardCharsets.UTF_8));

    }

    public static String parsePresentationPolicyAlt(PresentationPolicyAlternatives ppa) throws Exception {
        return Base64.encodeBase64String(
                IdContainer.convertObjectToXml(ppa)
                        .getBytes(StandardCharsets.UTF_8));

    }

    public static String parseIssuancePolicy(IssuancePolicy policy) throws Exception {
        return Base64.encodeBase64String(
                IdContainer.convertObjectToXml(policy)
                        .getBytes(StandardCharsets.UTF_8));

    }

    public static IssuancePolicy parseIssuancePolicy(String policy) throws Exception {
        String p = new String(Base64.decodeBase64(policy));
        return (IssuancePolicy) JaxbHelperClass.deserialize(p).getValue();

    }

}
