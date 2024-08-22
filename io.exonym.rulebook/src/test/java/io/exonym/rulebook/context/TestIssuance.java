package io.exonym.rulebook.context;

import eu.abc4trust.xml.*;
import io.exonym.actor.VerifiedClaim;
import io.exonym.actor.actions.*;
import io.exonym.helpers.BuildCredentialSpecification;
import io.exonym.helpers.BuildIssuancePolicy;
import io.exonym.helpers.BuildPresentationPolicy;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.pojo.AttributeBasedTokenResult;
import io.exonym.lite.pojo.Namespace;
import io.exonym.lite.pojo.Rulebook;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.PassStore;
import io.exonym.lite.time.Timing;
import io.exonym.rulebook.schema.XNodeContainer;
import io.exonym.uri.UriDataType;
import io.exonym.uri.UriEncoding;
import io.exonym.utils.ExtractObject;
import io.exonym.utils.RulebookVerifier;
import io.exonym.utils.storage.ImabAndHandle;
import io.exonym.utils.storage.XContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class TestIssuance {
    
    private static final Logger logger = LogManager.getLogger(TestIssuance.class);

    public static final String issuerUsername = "issuer";
    public static final String owner0Username = "owner0";
    public static final String owner1Username = "owner1";
    public static final String password = "password";

    @BeforeAll
    static void beforeAll() throws Exception {
        Cache cache = new Cache();
        NetworkMapWeb networkMapWeb = new NetworkMapWeb();
        PkiExternalResourceContainer.getInstance()
                .setNetworkMapAndCache(networkMapWeb,cache);

        new XContainerJSON(issuerUsername, true);
        new XContainerJSON(owner0Username, true);
        new XContainerJSON(owner1Username, true);


    }

    @AfterAll
    static void afterAll() throws Exception {
//        XContainerJSON x = new XContainerJSON(issuerUsername);
//        x.delete();
//        XContainerJSON o = new XContainerJSON(owner0Username);
//        o.delete();
//        XContainerJSON o0 = new XContainerJSON(owner1Username);
//        o0.delete();

    }

    @Test
    void issuerOpen() {
        try {
            RulebookNodeProperties props = RulebookNodeProperties.instance();
            XNodeContainer node = new XNodeContainer("baseline");
            ExonymIssuer issuer = new ExonymIssuer(node);
            PassStore store = new PassStore(props.getNodeRoot(), false);
            issuer.openContainer(store.getDecipher());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void sybilCredentialIssuance() {
        try {
            Cache cache = new Cache();
            PkiExternalResourceContainer external = PkiExternalResourceContainer.getInstance();
            external.setNetworkMapAndCache(new NetworkMapWeb(), cache);

            RulebookVerifier sybilRulebook = new RulebookVerifier(new URL("https://trust.exonym.io/sybil-rulebook.json"));
            Rulebook rulebook = sybilRulebook.getRulebook();
            CredentialSpecification sybilCredSpec = BuildCredentialSpecification.buildSybilCredentialSpecification(sybilRulebook);

            String xml = XContainer.convertObjectToXml(sybilCredSpec);
            logger.debug(xml);
            String rulebookHash = UIDHelper.stripPrefix(rulebook.getRulebookId());

            URI sybilIssuerUID = URI.create(Namespace.URN_PREFIX_COLON
                    +"sybil:testnet:" +
                    rulebookHash + ":" +
                    UUID.randomUUID().toString().split("-")[0]
                    + ":i"
            );
            UIDHelper sybilHelper = new UIDHelper(sybilIssuerUID);

            PassStore store = new PassStore(password, false);

            XContainerJSON issuerX = new XContainerJSON(issuerUsername);
            ExonymIssuerTest sybilIssuer = new ExonymIssuerTest(issuerX);
            sybilIssuer.openContainer(store.getDecipher());

            sybilIssuer.setupAsRevocationAuthority(sybilHelper.getIssuerParameters(), store.getEncrypt());
            sybilIssuer.addCredentialSpecification(sybilCredSpec);

            sybilIssuer.setupAsCredentialIssuer(sybilHelper.getCredentialSpec(), sybilHelper.getIssuerParameters(),
                    sybilHelper.getRevocationAuthority(), store.getEncrypt());

            //
            // Owner (Sybil)
            //
            ExonymOwnerTest owner = setupExistingOwner(owner0Username, store);
            issueSybil(sybilIssuer, owner, store, sybilCredSpec, sybilHelper.getIssuerParameters(), rulebook);
            showSybilCredential(owner, rulebook, sybilHelper, cache, external);

            //
            // Owner0 (Sybil)
            //
            ExonymOwnerTest owner0 = setupExistingOwner(owner1Username, store);
            issueSybil(sybilIssuer, owner0, store, sybilCredSpec, sybilHelper.getIssuerParameters(), rulebook);
            showSybilCredential(owner0, rulebook, sybilHelper, cache, external);

            //
            //  Setup as issuer of rulebook
            //
            RulebookVerifier rulebookRulebook = new RulebookVerifier(new URL("https://trust.exonym.io/sources-rulebook.json"));

            String rootIssuerUid = Namespace.URN_PREFIX_COLON + "badass:badass-two:" +
                    UIDHelper.computeRulebookHashFromRulebookId(
                            rulebookRulebook.getRulebook().getRulebookId()) + "1213:i" ;
            UIDHelper rulebookHelper = new UIDHelper(rootIssuerUid);
            rulebookHelper.out();

            BuildCredentialSpecification builder = new BuildCredentialSpecification(
                    rulebookHelper.getCredentialSpec(), true);

            CredentialSpecification credSpec = builder.getCredentialSpecification();
            XContainerJSON xIssuer = new XContainerJSON(issuerUsername);
            ExonymIssuerTest issuerRulebook = new ExonymIssuerTest(xIssuer);

            issuerRulebook.addCredentialSpecification(credSpec);

            issuerRulebook.setupAsRevocationAuthority(
                    rulebookHelper.getIssuerParameters(), store.getEncrypt());

            issuerRulebook.setupAsCredentialIssuer(
                    rulebookHelper.getCredentialSpec(),
                    rulebookHelper.getIssuerParameters(),
                    rulebookHelper.getRevocationAuthority(),
                    store.getEncrypt());

            joinRulebook(owner, issuerRulebook, sybilIssuerUID,
                    store, rulebookHelper, rulebookRulebook, external);

            joinRulebook(owner0, issuerRulebook, sybilIssuerUID,
                    store, rulebookHelper, rulebookRulebook, external);

            // Build presention policy
            // prove returning Presentation Token
            // revoke based on Presentation Token
            // attempt to


        } catch (Exception e) {
            logger.debug("Error", e);
            assert false;

        }
    }

    private PresentationPolicy standardProofOfHonesty(URI sybilIssuerUID, UIDHelper issuerHelper){
        return null;

    }

    private void joinRulebook(ExonymOwnerTest owner, ExonymIssuerTest issuerSecond, URI sybilIssuerUID, PassStore store,
                              UIDHelper issuerHelper, RulebookVerifier vSource, PkiExternalResourceContainer external) throws Exception {

        PresentationPolicy pp = JoinHelper.baseJoinPolicy(
                vSource, sybilIssuerUID, external, CryptoUtils.generateNonce(32));

        BuildIssuancePolicy bip1  = new  BuildIssuancePolicy(
                pp, issuerHelper.getCredentialSpec(), issuerHelper.getIssuerParameters());

        IssuancePolicy issuancePolicy1 = bip1.getIssuancePolicy();

        VerifiedClaim vc = new VerifiedClaim( // open credSpec.
                issuerSecond.getContainer().openResource(
                        issuerHelper.getCredentialSpec()));

        IssuanceMessageAndBoolean imabRb = issuerSecond.issueInit(vc,issuancePolicy1,store.getEncrypt(),URI.create(UUID.randomUUID().toString()));
        IssuanceMessage imRb = owner.issuanceStep(imabRb, store.getEncrypt());
        ImabAndHandle fin = issuerSecond.issueStep(imRb, store.getEncrypt());
        owner.issuanceStep(fin.getImab(), store.getEncrypt());

    }

    private void showSybilCredential(ExonymOwnerTest owner, Rulebook rulebook, UIDHelper helper,
                                     Cache cache, PkiExternalResourceContainer external) throws Exception {
        BuildPresentationPolicy bpp = new BuildPresentationPolicy(URI.create("urn:sybil:pp"), external);
        bpp.makeInteractive();
        ArrayList<URI> cspecs = new ArrayList<>();
        cspecs.add(rulebook.computeCredentialSpecId());
        ArrayList<CredentialInPolicy.IssuerAlternatives.IssuerParametersUID> issuers = new ArrayList<>();
        CredentialInPolicy.IssuerAlternatives.IssuerParametersUID iuid = new CredentialInPolicy.IssuerAlternatives.IssuerParametersUID();
        issuers.add(iuid);
        iuid.setValue(helper.getIssuerParameters());
        iuid.setRevocationInformationUID(helper.getRevocationInfoParams());

        AttributeDescription ad = BuildCredentialSpecification
                .createAttributeDescription(UriDataType.ANY_URI, UriEncoding.STRING_PRIME,
                        URI.create(Namespace.URN_PREFIX_COLON + "sybil-class"));

        bpp.addPseudonym("urn:rulebook:anon", false, "urn:io:exonym");
        bpp.addCredentialInPolicy(cspecs, issuers, "urn:io:exonym:sybil", URI.create("urn:io:exonym"));
        bpp.addDisclosableAttributeForCredential(rulebook.computeCredentialSpecId(), ad);
        PresentationPolicyAlternatives ppa = bpp.getPolicyAlternatives();
        String policyXml = XContainer.convertObjectToXml(ppa);
        logger.debug(policyXml);

        PresentationTokenDescription ptd = owner.canProveClaimFromPolicy(ppa);
        PresentationToken token = owner.proveClaim(ptd, ppa);

        TokenVerifier tokenVerifier = new TokenVerifier(cache, external);
        AttributeBasedTokenResult tokenResult = tokenVerifier.verifyTokenWithAttributes(ppa, token);
        HashMap<URI, Object> values = tokenResult.getDisclosedAttributes().get(0).getDisclosedValues();
        for (URI k : values.keySet()){
            logger.debug(k + " " + values.get(k));

        }

    }

    private ExonymOwnerTest setupExistingOwner(String ownerUsername, PassStore store) throws Exception {
        XContainerJSON ownerX = new XContainerJSON(ownerUsername);
        ExonymOwnerTest owner = new ExonymOwnerTest(ownerX);
        owner.openContainer(store);
        owner.setupContainerSecret(store.getEncrypt(), store.getDecipher());
        return owner;

    }

    private void issueSybil(ExonymIssuerTest issuer, ExonymOwnerTest owner0, PassStore store, CredentialSpecification cs, URI issuerUid, Rulebook rulebook) throws Exception {
        VerifiedClaim claim = new VerifiedClaim(cs);
        HashMap<URI, Object> map = claim.getLabelValuesMap();
        List<URI> labels = claim.getLabels();
        labels.get(0);
        map.put(labels.get(0), Rulebook.SYBIL_CLASS_PERSON);

        BuildIssuancePolicy bip = new BuildIssuancePolicy(null, rulebook.computeCredentialSpecId(), issuerUid);
        bip.addPseudonym(Namespace.URN_PREFIX_COLON + "sybil", true, "urn:io:exonym", null);

        IssuancePolicy issuancePolicy = bip.getIssuancePolicy();


        URI context0 = URI.create("urn:" + UUID.randomUUID());
        IssuanceMessageAndBoolean imab0 = issuer.issueInit(claim, issuancePolicy, store.getEncrypt(), context0);
        IssuanceMessage message0 = owner0.issuanceStep(imab0, store.getEncrypt());
        ImabAndHandle imabAndHandle0 = issuer.issueStep(message0, store.getEncrypt());
        owner0.issuanceStep(imabAndHandle0.getImab(), store.getEncrypt());

        logger.debug("Issued=" + imabAndHandle0.getHandle() + " " + imabAndHandle0.getIssuerUID());

    }

    @Test
    void testFlatIssuance() {
        try {
            String rootIssuerUid = "urn:exonym:asdbabsdbasda:asda:test-asdads";
            String sourceUid = "urn:exonym:asdbabsdbasda:asda";

            URI iUid = URI.create(rootIssuerUid + ":i");
            URI cUid = URI.create(sourceUid + ":c");
            URI icUid = URI.create(rootIssuerUid + ":ic");
            URI raUid = URI.create(rootIssuerUid + ":ra");
            URI raiUid = URI.create(rootIssuerUid + ":rai");
            URI ipUid = URI.create(rootIssuerUid + ":ip");

            BuildCredentialSpecification builder = new BuildCredentialSpecification(cUid, true);
            CredentialSpecification credSpec = builder.getCredentialSpecification();
            XContainerJSON xIssuer = new XContainerJSON(issuerUsername);
            ExonymIssuerTest issuer = new ExonymIssuerTest(xIssuer);
            PassStore store = new PassStore(password, false);

            issuer.addCredentialSpecification(credSpec);
            issuer.setupAsRevocationAuthority(iUid, store.getEncrypt());
            issuer.setupAsCredentialIssuer(cUid, iUid, raUid, store.getEncrypt());

            IssuancePolicy ip = xIssuer.openResource(ipUid);
            IssuerParameters i = xIssuer.openResource(iUid);
            RevocationAuthorityParameters ra = xIssuer.openResource(raUid);
            RevocationInformation rai = xIssuer.openResource(raiUid);

            logger.info("Defining Main Container");
            XContainerJSON xUser = new XContainerJSON(owner0Username);
            ExonymOwnerTest owner = new ExonymOwnerTest(xUser);
            owner.openContainer(store);
            owner.addCredentialSpecification(credSpec);
            owner.addIssuerParameters(i);
            owner.addRevocationAuthorityParameters(ra);
            owner.addRevocationInformation(raiUid, rai);

            logger.info("Defined Container");

            VerifiedClaim claim = new VerifiedClaim(credSpec);

            // ISSUER - INIT (Step 1)
            long t = Timing.currentTime();
            IssuanceMessageAndBoolean imab = issuer.issueInit(claim, ip, store.getEncrypt(), URI.create("ctx"));
            logger.debug("Has been "  + Timing.hasBeenMs(t));

            // 			OWNER - Step A
            IssuanceMessage im = owner.issuanceStep(imab, store.getEncrypt());
            IssuanceToken token = ((JAXBElement<IssuanceToken>)im.getContent().get(0)).getValue();
            List<Object> extraction = new ArrayList<>();
            IssuanceToken issuanceToken = ExtractObject.extract(im.getContent(), IssuanceToken.class);
            List<PseudonymInToken> nyms = issuanceToken.getIssuanceTokenDescription()
                    .getPresentationTokenDescription().getPseudonym();

            URI issuerUID = token.getIssuanceTokenDescription()
                        .getCredentialTemplate().getIssuerParametersUID();

            // ISSUER - Step (Step 2)
            ImabAndHandle result = issuer.issueStep(im, store.getEncrypt());

            // 			OWNER - Step B
            owner.issuanceStep(result.getImab(), store.getEncrypt());

            if (!result.getImab().isLastMessage()) {
                throw new Exception();

            }
            Credential cred = xUser.openResource(icUid, store.getDecipher());
            BigInteger handle = (BigInteger) cred.getCredentialDescription().getAttribute().get(0).getAttributeValue();
            logger.debug("handle=" + handle);

        } catch (Exception e) {
            logger.error("Error", e);

        }
    }
}
