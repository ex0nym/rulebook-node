package io.exonym.rulebook.context;

import eu.abc4trust.xml.RevocationInformation;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.actions.*;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.connect.UrlHelper;
import io.exonym.lite.pojo.*;
import io.exonym.lite.standard.Const;
import io.exonym.lite.standard.WhiteList;
import io.exonym.lite.time.DateHelper;
import io.exonym.rulebook.schema.IdContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    static{
        try {
            props = RulebookNodeProperties.instance();
            props.getNodeRoot();
            networkMap = new NetworkMapWeb();
            Cache cache = new Cache();
            PkiExternalResourceContainer external = PkiExternalResourceContainer.getInstance();
            external.setNetworkMapAndCache(networkMap, cache);

        } catch (Exception e) {
            logger.info("Error", e);

        }
    }
    

    @BeforeAll
    static void beforeAll() throws Exception {

    }

    @Test
    void name() {
        try {
            URI modUid = URI.create("urn:rulebook:trustworthy-leaders:exonym:exonym-leads:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691");
            new NodeVerifier(modUid);

        } catch (Exception e) {
            logger.error("Error", e);
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
            WiderTrustNetworkManagement wtn = new WiderTrustNetworkManagement();
            wtn.setupWiderTrustNetwork();
            wtn.addLead(URI.create("http://exonym-x-03:8080/static/lead/"), false);
//            wtn.addLead(URI.create("http://exonym-x-03:8081/static/lead/"), false);

            wtn.publish();

        } catch (Exception e) {
            logger.info("Error", e);
        }

    }

}
