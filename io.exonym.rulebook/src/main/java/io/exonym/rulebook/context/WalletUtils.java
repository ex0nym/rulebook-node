package io.exonym.rulebook.context;

import com.ibm.zurich.idmx.exception.SerializationException;
import com.ibm.zurich.idmx.jaxb.JaxbHelperClass;
import eu.abc4trust.xml.PresentationPolicy;
import eu.abc4trust.xml.PresentationPolicyAlternatives;
import eu.abc4trust.xml.PresentationToken;
import eu.abc4trust.xml.PseudonymInToken;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.connect.UrlHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.rulebook.schema.EndonymToken;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

public class WalletUtils {
    
    private final static Logger logger = Logger.getLogger(WalletUtils.class.getName());



    protected static <T> T deserialize(String object) throws UxException {
        if (UrlHelper.isXml(object.getBytes(StandardCharsets.UTF_8))){
            try {
                return (T) JaxbHelperClass.deserialize(object).getValue();

            } catch (SerializationException e) {
                throw new UxException(ErrorMessages.TOKEN_INVALID, e,
                        "The presentation policy was XML, but it was not an Issuance Policy");

            }
        } else { // base64
            try {
                byte[] decodedB64 = Base64.decodeBase64(object);
                String xml = new String(decodedB64, StandardCharsets.UTF_8);
                return (T) JaxbHelperClass.deserialize(xml).getValue();

            } catch (Exception e) {
                throw new UxException(ErrorMessages.TOKEN_INVALID, e,
                        "The Issuance Policy was neither XML nor Base64 encoded XML");

            }
        }
    }



    public static PresentationPolicyAlternatives openPPA(PresentationPolicy policy) throws UxException {
        PresentationPolicyAlternatives ppa = new PresentationPolicyAlternatives();
        ppa.getPresentationPolicy().add(policy);
        return ppa;

    }

    public static String decodeCompressedB64(String b64) throws IOException {
        byte[] decom = decompress(
                Base64.decodeBase64(
                b64.getBytes(StandardCharsets.UTF_8)));
        return new String(decom, StandardCharsets.UTF_8);
    }

    public static String decodeUncompressedB64(String b64) throws IOException {
        return new String(Base64.decodeBase64(b64), StandardCharsets.UTF_8);
    }

    public HashMap<String, UIDHelper> populateHelpers(ArrayList<String> issuerUids) throws Exception {
        HashMap<String, UIDHelper> helpers = new HashMap<>();
        for (String issuer : issuerUids){
            helpers.put(issuer, new UIDHelper(issuer));

        }
        return helpers;
    }


    public static String isolateUniversalLinkContent(String uLink) throws Exception {
        if (uLink!=null){
            String[] parts = uLink.split("\\?");
            if (parts.length==2){
                return parts[1];

            } else if (parts.length==1){
                return parts[0];

            }
        }
        throw new UxException(ErrorMessages.UNEXPECTED_PSEUDONYM_REQUEST,
                "Blind-sided by the universal link");

    }


    public static <T> ArrayList<T> wrapInList(T item){
        ArrayList<T> list = new ArrayList<>();
        list.add(item);
        return list;
    }

    public static <T> ArrayList<T> emptyList(){
        return new ArrayList<>();
    }


    public static ArrayList<URI> extractPseudonyms(PresentationToken verifiedToken) throws UxException {
        List<PseudonymInToken> nyms = verifiedToken.getPresentationTokenDescription().getPseudonym();
        ArrayList<URI> result = new ArrayList<>();
        for (PseudonymInToken nym : nyms){
            if (nym.isExclusive()){
                URI endonym = EndonymToken.endonymForm(nym.getScope(), nym.getPseudonymValue());
                result.add(endonym);

            }
        }
        return result;
    }

    public static byte[] compress(byte[] in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DeflaterOutputStream defl = new DeflaterOutputStream(out);
        defl.write(in);
        defl.finish();
        defl.flush();
        defl.close();
        return out.toByteArray();

    }

    public static byte[] decompress(byte[] in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InflaterOutputStream infl = new InflaterOutputStream(out);
        infl.write(in);
        infl.flush();
        infl.close();

        return out.toByteArray();

    }

}

