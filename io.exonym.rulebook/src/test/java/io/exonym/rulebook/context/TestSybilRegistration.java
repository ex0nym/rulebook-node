package io.exonym.rulebook.context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ibm.zurich.idmx.jaxb.JaxbHelperClass;
import eu.abc4trust.xml.IssuanceMessage;
import eu.abc4trust.xml.IssuanceMessageAndBoolean;
import eu.abc4trust.xml.RevocationInformation;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.actions.*;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.connect.Http;
import io.exonym.lite.pojo.XKey;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.Const;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.PassStore;
import io.exonym.utils.storage.KeyContainer;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;

public class TestSybilRegistration {

    private Http client;
    public static final String APP_KEY_B64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjNLD5DXC69poOOfa9MT4jPDIU5VOJ13luG70HsSnkNtDWtIF3CKJkjI9jUc6zadqn1CREPUNABXM95Bp2YRx/r4WOcMIBHkfJS3a7Hbwi32NlcFuMgYaZmtDOEFp9FJU+xZTDmXJ5oEovl+xT31tyhQm3f9BT1p68hKSuUbwpFkd1foley+KhpOFGIqv4J7qpda9K+lLqF6J9j/x+bhCp32IPSJ7SdKYGzQpEQdn5XVK60TcjNbVtGmF9I+hTp1knbGQ6XJUMi5yiudKCfQxIuUsYkE3+QtXYzCdvPFj+6KVJM8Z7mP5Bt/v00ughpIk/HWUW4AEwaQ7yYGGTAnRHwIDAQAB";
    
    private static final Logger logger = LogManager.getLogger(TestSybilRegistration.class);
    private static final String usernameMjh = "mjh";
    private static final String emailMjh = "yawn@exonym.io";
    private static final String password = "password";

    private static final String targetUrl = "http://exonym-x-03:8080/";
    private static final String registerUrl = targetUrl + "register";

    private Gson gson = new Gson();

    private final AsymStoreKey appKey;


    public TestSybilRegistration() throws Exception {
        appKey = AsymStoreKey.blank();
        appKey.assembleKey(Base64.decodeBase64(APP_KEY_B64));
        client = new Http();


    }

    @BeforeAll
    static void beforeAll() throws Exception {
        logger.debug("TEST >>>>>>>>>>>>>>>>>>> Setting up");
        XContainerJSON x = new XContainerJSON(usernameMjh, true);
        ExonymOwnerTest owner = new ExonymOwnerTest(x);
        PassStore store = new PassStore(password, false);
        owner.openContainer(store);
        owner.setupContainerSecret(store.getEncrypt(), store.getDecipher());

        XKey key = XKey.createNew(store.getEncrypt());
        KeyContainer keyContainer = new KeyContainer();
        keyContainer.getKeyPairs().add(key);
        x.saveLocalResource(keyContainer);

    }

    @Test
    void walletRegistrationTestNet() {
        logger.debug("TEST >>>>>>>>>>>>>>>>>>> Test Net Registration");
        try {
            PkiExternalResourceContainer external = PkiExternalResourceContainer.getInstance();
            LocalNetworkMap map = new LocalNetworkMap();
            if (!map.networkMapExists()){
                map.spawn();
            }
            external.setNetworkMapAndCache(map, new Cache());
            URI sourceURL = URI.create("https://trust.exonym.io/sybil").resolve(Const.LEAD);
            URI advocateURL = URI.create("https://trust.exonym.io/sybil").resolve(Const.MODERATOR);
            NodeVerifier advocate = NodeVerifier.openNode(advocateURL,
                    false, false);
            NodeVerifier source = NodeVerifier.openNode(sourceURL,
                    true, false);
            UIDHelper helper = advocate.getUidHelperForMostRecentIssuerParameters();

            XContainerJSON x = new XContainerJSON(usernameMjh);
            ExonymOwnerTest owner = new ExonymOwnerTest(x);
            PassStore store = new PassStore(password, false);
            owner.openContainer(store);
            owner.addCredentialSpecification(source.getCredentialSpecification());
            owner.addIssuerParameters(
                    advocate.getIssuerParameters(helper.getIssuerParametersFileName()));
            RevocationInformation ri = advocate.getRevocationInformation(helper.getRevocationInformationFileName());
            owner.addRevocationInformation(ri.getRevocationAuthorityParametersUID(), ri);

            JsonObject j = new JsonObject();
            String context = UUID.randomUUID().toString();
            j.addProperty("testNet", true);
            j.addProperty("hello", context);
            j.addProperty("sybilClass", "person");

            String response = client.basicPost(registerUrl, j);
            logger.info(response);
            HashMap<String, String> result = gson.fromJson(response, HashMap.class);;
            String imabString = new String(Base64.decodeBase64(result.get("imab")));
            IssuanceMessageAndBoolean imab = (IssuanceMessageAndBoolean) JaxbHelperClass.deserialize(imabString).getValue();

            IssuanceMessage message = owner.issuanceStep(imab, store.getEncrypt());

            String xmlResponse = JaxbHelper.serializeToXml(message, IssuanceMessage.class);
            j = new JsonObject();
            j.addProperty("testNet", true);
            j.addProperty("hello", context);
            j.addProperty("im", Base64.encodeBase64String(xmlResponse.getBytes(StandardCharsets.UTF_8)));

            response = client.basicPost(registerUrl, j);
            logger.info(response);
            result = gson.fromJson(response, HashMap.class);;
            imabString = new String(Base64.decodeBase64(result.get("imab")));
            imab = (IssuanceMessageAndBoolean) JaxbHelperClass.deserialize(imabString).getValue();

            owner.issuanceStep(imab, store.getEncrypt());

        } catch (Exception e) {
            logger.debug("Error", e);
            assert false;

        }
    }

    private String computeDeviceId(String usernameMjh, String emailMjh) {
        return CryptoUtils.computeSha256HashAsHex(usernameMjh + emailMjh);

    }

    private AsymStoreKey openUserKey(XContainerJSON x, PassStore store) throws Exception {
        KeyContainer keys = x.openResource("keys.xml");
        XKey key = keys.getKeyPairs().get(0);
        return AsymStoreKey.build(key.getPublicKey(),
                key.getPrivateKey(), store.getDecipher());

    }

    private String generateHelloToken(String deviceId) {
        return Base64.encodeBase64String(appKey.encrypt(
                deviceId.getBytes(StandardCharsets.UTF_8)));

    }

    private String generateAuthenticationToken(AsymStoreKey userKey, String deviceId) {
        DateTime dt = DateTime.now(DateTimeZone.UTC);
        String t = dt.getMillis() + "";
        return Base64.encodeBase64String(userKey.encryptWithPrivateKey(
                t.getBytes(StandardCharsets.UTF_8)));

    }

    @AfterAll
    static void afterAll() throws Exception {
        logger.debug("TEST >>>>>>>>>>>>>>>>>>> Take down");
        XContainerJSON x = new XContainerJSON(usernameMjh);
        x.delete();

    }

}
