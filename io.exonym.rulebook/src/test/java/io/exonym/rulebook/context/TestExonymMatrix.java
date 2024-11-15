package io.exonym.rulebook.context;

import eu.abc4trust.xml.RevocationInformation;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.actions.*;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.connect.UrlHelper;
import io.exonym.lite.pojo.*;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.Const;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.WhiteList;
import io.exonym.lite.time.DateHelper;
import io.exonym.rulebook.schema.IdContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;

public class TestExonymMatrix {
    
    private static final Logger logger = LogManager.getLogger(TestExonymMatrix.class);

    private static NetworkMapWeb networkMap;
    private static RulebookNodeProperties props;

    public static final String PASSWORD = "dmsdfwsfgjfjgdfTdg";

    public static final String KEY_DO_NOT_USE = "{\n" +
            "  \"publicKey\": \"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt+vufZajumJ2CV+ZSfeVRxH5z4JkcapwCGAxsFzvVuFUWqpPS8ruqOF9EQAXA4ho0S41QHAevq0RwWlB2ODLr7l6pDqtfQ1DR3EtJ5rnVTXN7h6qrs53YGfvSgpFf+PawfpYDw3xEwMeHpjZw0N/LwjoSUzWUGH1dhf/nCFyrAmQw1SB/Qbz8TnjwIpxfqMXFLB9Vn9OhJWEbs62BUSP815vaaRBsrBmAFYo/UV/s6mHGNFF3IheXyNZqHwgpsdyRSC3BxVEoDerE1rHI+V9nhGjhm3uPd04omB3l2yg/I9N09NRYcfCqyoBeGQa5SKMEGUSUwV41pFybvaT/XopEwIDAQAB\",\n" +
            "  \"privateKey\": \"es5cV3YGQL1Dv/7UJYg/MUOrAwLJdBMukvVSmGFwQH15Fox3d0py22FnLVWWS7E1yqW9p8bAbzCoOUYluVJdEAo8KoyuocuPHiBYeAkBJaUCinygcesz1cwvXiiQpluwIxV1xeHZRHoDxNJveGOez4LW5X2WtXXBffG2LEXG3qHxGqPeu1VwyQ5fBj+oUaoERGzIojX1TJioqEwSMCMk10rJ17YUgOw0hOmO0soPGaUcxcILuIQnNsu6d9W8wYzjkYJSIrHzsKL2H445TLpe6EfllM6y3PQEQScYkz/oQfAJHgTQlGwZe/o6zAjk4n/mmC+OfK+fjKOvbIF92idKZAj0+JZP7eqt8eHCEXuj5BL55RUZelHPDbVi/1BYo9TN8BbxMoeYJjieFWMVNCwB6zggLXYVeFRKov3hpk27lXCutr/bsBNiF/2442eGZ/1/AfodTvaKpImHvFsElRT7pWgN1Xs7SzKW21kCuwLluUHjfCN2zsP7VZlft0y2LXulUEEmL+10tmYqj8mlpAe81fXGtbSdUYeSV/B4VHqkUvxKdzAcZQBi/yV7qkVfVjqAxEfv/aN5vxTRNQsCXlUjgquxXg9fa5j+1/UMXnGRNUvB5Pda2H4jLzGPNDZhE7VlMNNBAubOwELZedIZHEAOY4UQQUk4dphtLncClzaEH7wNzXFd0dZMVv3bt31gacIzjne7ofQxT3hKr1P3z8b2i2gF6MTLnw4FF5kklqsfz1DvX0AIMhoWnR+6oFHidTM2KFB+PRflPupCdPht9XTxSiJtG7RO6m4gILCP8wBqGDjpRQopDhXR1J1WaTFxGIh4sgW37WJMO5TLh7JcOt6reKAWvVRW8baRbr2deWbDNq8/NjTUNI1k3bn/agfKfrJGctXaCX0v7ENZGfLnH6dm7kopcp8/RAxiQXgnEc8GlNSvCx/DUOvjc6utfxz4qUbX1Gb44iEqGMaGVozywsIALXz70qLtx3nmKRxq9XricMIG1Ai2OyvKLYEL6Dt+3IwK5UHs2NcbU+lhEH0ZTVrJkdIJU38zkAcs8GApktGrCLi2VXR8y3miX+sx7bH+bdLBfJe/+b5J5i/rAXUPubWsmY+dQxP9qYjnH/ncQW8/eMepKRGv4s29VFkySp3TbO1D/Uvrpb3S1pIGDW3qc1SVbFb08OSPnVumcNvlTtpuNtrzTN5HjjXoI63Ru5HsL6IrGoIzWGQFiPG8123DXpSRdhxFYKBYyDWLDYF096xlnSAEKpLMw4UZDr2vR2CxS/8x5+Y+rnsCNyMSJER/bOwwpIYgFRaqga8vWdrNm/irw+A987I3OdW6zijW2w0+N6OU2r3MkCoamxhweQOgkaVrEN/TzEoStSBThVH0ZsgqGgPBIh6I17FvYRxePBTrmgzOx+53ZSNXcbN22qALBqpBuk+19Nk2JQvf6TmK9766DdAmFSpkEjATTNUdEA3dHXRbbCr2v9a3ElQvGWA9XvovrJqiQtp5EXFCdWMnEPvjIlgk2Cj91/V6oh6nYYdzYUsoBlovvQiWbKhuwBEQ+44ORhLDuTpLZIdqJBcMg47DOEF7iL3UL/nwmlSvREiPEVaYEsxU7myhRCXzelMgMfel1daTdhUjjFUzndSl2qhnAP0\\u003d\"\n" +
            "}";

    private static AsymStoreKey doNotUse;

//    static{
//        try {
//            props = RulebookNodeProperties.instance();
//            props.getNodeRoot();
//            networkMap = new NetworkMapWeb();
//            Cache cache = new Cache();
//            PkiExternalResourceContainer external = PkiExternalResourceContainer.getInstance();
//            external.setNetworkMapAndCache(networkMap, cache);
//
//        } catch (Exception e) {
//            logger.info("Error", e);
//
//        }
//    }
    

    @BeforeAll
    static void beforeAll() throws Exception {
        String md5 = CryptoUtils.computeMd5HashAsHex("7e98cf1bbbc84009a2e543d5690c4b7b180bba5e813e8aeb63bbfb8a9a3b62ca29b0370955cb998b50425ff5e8263e50a18675775f9ac668dd0bcdecaa7369cb129cc9054047e0d4453430f73b85cf5b800bba88c0e775b44e84202eb7b91b123e4ea5ee48891a29290c63bdaf0a2b34e4cf0f2867f9ece2aadc9db7a672f0b8a4f399962642ec30d2ba505f656547abbe9cd3d05494743cf6b0e6c69e179c249c5bf89b750eb76f9298e124da39dba4f4ddba5a0ab5306f2deb9cdd69312ee80a95b5beb4cd75a151691d8731b9fa48d53b1fa31099427efaafd7ed90d38af14e2eb4ffb8c52f3c24fade090b3fa7379b2f8820591c87d96ae98005329b86ad");
        String sha = CryptoUtils.computeSha256HashAsHex("7e98cf1bbbc84009a2e543d5690c4b7b180bba5e813e8aeb63bbfb8a9a3b62ca29b0370955cb998b50425ff5e8263e50a18675775f9ac668dd0bcdecaa7369cb129cc9054047e0d4453430f73b85cf5b800bba88c0e775b44e84202eb7b91b123e4ea5ee48891a29290c63bdaf0a2b34e4cf0f2867f9ece2aadc9db7a672f0b8a4f399962642ec30d2ba505f656547abbe9cd3d05494743cf6b0e6c69e179c249c5bf89b750eb76f9298e124da39dba4f4ddba5a0ab5306f2deb9cdd69312ee80a95b5beb4cd75a151691d8731b9fa48d53b1fa31099427efaafd7ed90d38af14e2eb4ffb8c52f3c24fade090b3fa7379b2f8820591c87d96ae98005329b86ad");
        logger.info(md5);
        logger.info(sha);
        System.exit(1);
//        try {
//            XKey k = XKey.createNew(PASSWORD);
//            logger.info(JaxbHelper.gson.toJson(k));
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }

        XKey x = JaxbHelper.gson.fromJson(KEY_DO_NOT_USE, XKey.class);
        doNotUse = XKey.assembleAsym(PASSWORD, x);


    }




    @Test
    void name() {

//        String uid = UUID.randomUUID().toString()
//                .replaceAll("-", "");

        String uid = "ttt";

        String target = "wss://ps.cyber30.io:";

        DummySubscriber ds = new DummySubscriber("rulebook/mmo/4ccbdf03787d137fc360a193ba950eb77d6b150f99b69280a11dc084f29a2f72/#", "wss://ps.cyber30.io:9000", uid);
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.exit(1);


        MqttClient mqttClient = null;
        try {
            String username = "653d42b6f7e0f00432fcad834b293571";
            mqttClient = new MqttClient(target  + "9001", username);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                    logger.info("Lost");
                }

                @Override
                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                    logger.info("Delivery complete");
                }
            });

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setPassword("mhWSVIMm4Y4Tbzd7z0Xn9PO525DZtWcYLLCqBeLa6Do=".toCharArray());
            String jwt = doNotUse.generateJwt(username, 1);
            options.setUserName(username);
            mqttClient.connect(options);

            MqttMessage m = new MqttMessage("message from publisher".getBytes(StandardCharsets.UTF_8));
            m.setQos(2);
            m.setRetained(true);

            mqttClient.publish("/hello", m);

        } catch (Exception e) {
            logger.info("Error", e);

        } finally {
            if (mqttClient!=null){
                try {
                    if (mqttClient.isConnected()){
                        mqttClient.disconnect();
                    }
                } catch (MqttException e) {
                    logger.info("Error", e);
                }
            }
        }
    }



    @Test
    void matrixManagerLocal() {
        try {
            boolean isIssuer = WhiteList.isIssuerUid("urn:rulebook:trustworthy-leaders:exonym:interpretation:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691:350491d6:i");
            URI rai = URI.create("urn:rulebook:trustworthy-leaders:exonym:interpretation:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691:350491d6:ra:revocationInformation:15");
            UIDHelper helper = new UIDHelper(rai);

            URI nodeUrl = URI.create("http://localhost:8080");
            Path modPath = Path.of(Const.PATH_OF_STATIC, Const.MODERATOR);
            String p = nodeUrl.toString() + modPath;
            logger.info(p);

            byte[] bytes = UrlHelper.read(new URL("https://trust.exonym.io/leads-rulebook-test.json"));

            Rulebook rb = JaxbHelper.gson.fromJson(new String(bytes, StandardCharsets.UTF_8), Rulebook.class);

            NetworkMapItemModerator nmim = (NetworkMapItemModerator)
                    networkMap.nmiForNode(TestTools.MOD1_UID);

            IdContainer id = new IdContainer(nmim.getModeratorName());
            RevocationInformation ri = id.openResource(
                    "trustworthy-leaders.exonym.interpretation.9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691.350491d6.rai.xml");


            ExonymMatrixManagerLocal local = new ExonymMatrixManagerLocal(
                    id, rb.computeRuleUris(), nmim, props.getNodeRoot());

            String r0 = rb.getRules().get(0).getId();
            ArrayList<URI> rules = new ArrayList<>();
            rules.add(URI.create(r0));

            Violation violation = new Violation();
            String x0 ="39d1dd7754c523ed187cebd0682d48710def25a791f7adbd9b1a0d4941d008262ef81255d063c6614f27783a4c4a5119040d3c9acc57764da18fc65a7746e9dbcb2c9ba66739a4a01e3c704a621f1e0d2e20c12a3fc26eec7bde7d8ddad61f80adbcb8fb4d721838c662332d07a93ffe1c960e4b3b8b3dc67a83cf3ec7dd27dc9f8cae410bdddc107faf4e788259f5d91569896bb3db86ab32667e7daafe0cd523db995409774ea8a4a2d9f822c07c0958d1873782876e14bc7dc79536e21490ac84dd52ff885ece418fe8814a39721852c444fd6dd534c5ade03d811cedfceb079aea019fc3ca78ee051a02491cd39f039a009582838c514d3ed247ce581c";
            violation.setX0(x0);
            violation.setRuleUids(rules);

            violation.setNibble6("39d1dd");
            violation.setTimestamp(DateHelper.currentIsoUtcDateTime());
            violation.setRequestingModUid(helper.getModeratorUid());

            local.addViolation(x0, violation);

            logger.info("");

        } catch (Exception e) {
            logger.error("Error", e);
            

        }
    }

    @Test
    void setupWiderTrustNetwork() {
        try {
            PkiExternalResourceContainer external = PkiExternalResourceContainer.getInstance();
            NetworkMapWeb mnw = new NetworkMapWeb();
            Cache cache = new Cache();
            external.setNetworkMapAndCache(mnw, cache);

            WiderTrustNetworkManagement wtn = new WiderTrustNetworkManagement();
            wtn.setupWiderTrustNetwork();
            wtn.addLead(URI.create("https://t1.node.sybil.cyber30.io/static/lead/"), true);
            wtn.addLead(URI.create("https://t1.cyber30.io/static/lead/"), true);

            wtn.publish();

        } catch (Exception e) {
            logger.info("Error", e);
        }

    }

}
