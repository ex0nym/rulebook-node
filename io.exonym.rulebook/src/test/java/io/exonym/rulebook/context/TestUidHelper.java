package io.exonym.rulebook.context;

import io.exonym.helpers.UIDHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.net.URI;

public class TestUidHelper {
    
    private static final Logger logger = LogManager.getLogger(TestUidHelper.class);

    // valid
    URI lead = URI.create("urn:rulebook:trustworthy-leaders:exonym:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691");
    URI mod = URI.create("urn:rulebook:trustworthy-leaders:exonym:interpretation:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691");
    URI i = URI.create("urn:rulebook:trustworthy-leaders:exonym:interpretation:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691:350491d6:i");
    URI raiInput = URI.create("urn:rulebook:trustworthy-leaders:exonym:interpretation:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691:350491d6:ra:revocationAuthority:1:rai");


    @Test
    void testLead() {
        try {
            UIDHelper helper = new UIDHelper(lead);
            testHelperCore(helper);

        } catch (Exception e) {
            logger.info("error", e);
            assert false;
        }

    }

    @Test
    void testModerator() {
        try {
            UIDHelper helper = new UIDHelper(mod);
            testHelperCore(helper);

        } catch (Exception e) {
            logger.info("error", e);
            assert false;
        }
    }


    @Test
    void testIssuer() {
        try {
            UIDHelper helper = new UIDHelper(i);
            testHelperCore(helper);
            testHelperDetail(helper);

        } catch (Exception e) {
            logger.info("error", e);
            assert false;
        }
    }

    @Test
    void testRai() {
        try {
            UIDHelper helper = new UIDHelper(raiInput);
            testHelperCore(helper);
            testHelperDetail(helper);

        } catch (Exception e) {
            logger.info("error", e);
            assert false;
        }
    }

    @Test
    void testStatic() {
        URI rai = UIDHelper.transformMaterialUid(i, "rai");
        logger.info(rai);
        assert rai.equals(rai);



    }

    @Test
    void rulebookHash() {
        try {
            assert UIDHelper.computeRulebookHashUid(raiInput).equals(rulebookHash);
            assert UIDHelper.computeRulebookHashUid(mod).equals(rulebookHash);
            assert UIDHelper.computeRulebookHashUid(lead).equals(rulebookHash);
            assert UIDHelper.computeRulebookHashUid(i).equals(rulebookHash);
            assert UIDHelper.computeRulebookHashUid(raiInput).equals(rulebookHash);
            try {
                UIDHelper.computeRulebookHashUid(modName);

            } catch (UxException e) {
                logger.info(e.getMessage());
                assert e.getMessage().startsWith(ErrorMessages.INCORRECT_PARAMETERS);

            }
        } catch (UxException e) {
            logger.info("Error", e);
            assert false;

        }
    }

    @Test
    void testNodeNames() {
        assert UIDHelper.isLeadUid(lead);
        assert !UIDHelper.isLeadUid(mod);
        assert !UIDHelper.isLeadUid(raiInput);
        assert !UIDHelper.isLeadUid(i);
        assert !UIDHelper.isLeadUid(ic);

        assert UIDHelper.isModeratorUid(mod);
        assert !UIDHelper.isModeratorUid(lead);
        assert !UIDHelper.isModeratorUid(raiInput);
        assert !UIDHelper.isModeratorUid(i);
        assert !UIDHelper.isModeratorUid(pp);

    }

    @Test
    void topics() {
        try {
            String t0 = UIDHelper.computeRulebookTopicFromUid(i);
            logger.info(t0);
            assert t0.equals(topicRulebook);

            t0 = UIDHelper.computeRulebookTopicFromUid(raiInput);
            logger.info(t0);
            assert t0.equals(topicRulebook);

            t0 = UIDHelper.computeRulebookTopicFromUid(mod);
            logger.info(t0);
            assert t0.equals(topicRulebook);

            t0 = UIDHelper.computeRulebookTopicFromUid(lead);
            logger.info(t0);
            assert t0.equals(topicRulebook);

        } catch (UxException e) {
            logger.info("Error", e);
            assert false;

        }
    }

    @Test
    void fromMaterial() {
        try {
            URI modUid = UIDHelper.computeModUidFromMaterialUID(raiInput);
            logger.info(modUid);
            assert modUid.equals(this.mod);

            modUid = UIDHelper.computeModUidFromMaterialUID(i);
            logger.info(modUid);
            assert modUid.equals(this.mod);

            URI leadUid = UIDHelper.computeLeadUidFromModUid(modUid);
            logger.info(leadUid);
            assert leadUid.equals(this.lead);

            assert UIDHelper.computeLeadNameFromModOrLeadUid(modUid).equals(leadName);
            assert UIDHelper.computeLeadNameFromModOrLeadUid(leadUid).equals(leadName);
            assert UIDHelper.computeLeadNameFromModOrLeadUid(i).equals(leadName);

        } catch (Exception e) {
            logger.info("Error", e);
            assert false;

        }
    }

    private void testHelperCore(UIDHelper helper) {
        helper.out();
        if (helper.getModeratorName()!=null){
            assert helper.getModeratorName().equals(modName);
            assert helper.getRulebookModTopic().equals(topicMod);

        }
        assert helper.getLeadName().equals(leadName);
        assert helper.getRulebookUID().equals(rulebookId);
        assert helper.getRulebookFileName().equals(rulebookFilename);
        assert helper.getRulebookTopic().equals(topicRulebook);
        assert helper.getRulebookLeadTopic().equals(topicLead);
        assert helper.getCredentialSpec().equals(c);
        assert helper.getCredentialSpecFileName().equals(cXml);
        assert helper.getPresentationPolicy().equals(pp);
        assert helper.getPresentationPolicyFileName().equals(ppXml);

    }

    private void testHelperDetail(UIDHelper helper){
        assert helper.getIssuedCredential().equals(ic);
        assert helper.getIssuedCredentialFileName().equals(icXml);

        assert helper.getRevocationAuthority().equals(ra);
        assert helper.getRevocationAuthorityFileName().equals(raXml);

        assert helper.getRevocationInfoParams().equals(rai);
        assert helper.getRevocationInformationFileName().equals(raiXml);

        assert helper.getIssuancePolicy().equals(ip);
        assert helper.getIssuancePolicyFileName().equals(ipXml);

        assert helper.getInspectorParams().equals(ins);
        assert helper.getInspectorParamsFileName().equals(insXml);

        assert helper.getIssuerParametersFileName().equals(ixml);
        assert helper.getIssuerParameters().equals(i);

    }


    // expeceted

    String rulebookHash = "9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691";
    String leadName = "exonym";
    String modName = "interpretation";
    URI rulebookId = URI.create("urn:rulebook:trustworthy-leaders:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691");
    String rulebookFilename = "trustworthy-leaders.9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691.json";
    String topicRulebook = "rulebook/trustworthy-leaders/9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691";
    String topicLead = "rulebook/trustworthy-leaders/9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691/exonym";
    String topicMod = "rulebook/trustworthy-leaders/9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691/exonym/interpretation";
    URI c = URI.create("urn:rulebook:trustworthy-leaders:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691:c");
    String cXml = "trustworthy-leaders.9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691.c.xml";
    URI pp = URI.create("urn:rulebook:trustworthy-leaders:exonym:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691:pp");
    String ppXml = "trustworthy-leaders.exonym.9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691.pp.xml";
    URI ic = URI.create("urn:rulebook:trustworthy-leaders:exonym:interpretation:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691:350491d6:ic");
    String icXml = "trustworthy-leaders.exonym.interpretation.9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691.350491d6.ic.xml";
    URI ra = URI.create("urn:rulebook:trustworthy-leaders:exonym:interpretation:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691:350491d6:ra");
    URI rai = URI.create("urn:rulebook:trustworthy-leaders:exonym:interpretation:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691:350491d6:rai");
    String raXml = "trustworthy-leaders.exonym.interpretation.9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691.350491d6.ra.xml";
    String raiXml = "trustworthy-leaders.exonym.interpretation.9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691.350491d6.rai.xml";
    URI ip = URI.create("urn:rulebook:trustworthy-leaders:exonym:interpretation:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691:350491d6:ip");
    String ipXml = "trustworthy-leaders.exonym.interpretation.9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691.350491d6.ip.xml";
    URI ins = URI.create("urn:rulebook:trustworthy-leaders:exonym:interpretation:9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691:ins");
    String insXml = "trustworthy-leaders.exonym.interpretation.9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691.ins.xml";
    String ixml = "trustworthy-leaders.exonym.interpretation.9f87ae0387e1ac0c1c6633a90ad674f9564035624f490fe92aba28c911487691.350491d6.i.xml";


}
