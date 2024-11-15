package io.exonym.rulebook.context;

import com.cloudant.client.org.lightcouch.DocumentConflictException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.abc4trust.xml.*;
import io.exonym.actor.actions.*;
import io.exonym.exceptions.PolicyNotSatisfiedException;
import io.exonym.helpers.Parser;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.NetworkMapItemModerator;
import io.exonym.lite.pojo.ProofStore;
import io.exonym.lite.pojo.Rulebook;
import io.exonym.lite.time.DateHelper;
import io.exonym.rulebook.schema.EndonymToken;
import io.exonym.rulebook.schema.IdContainer;
import io.exonym.rulebook.schema.RulebookAuth;
import io.exonym.rulebook.schema.SsoChallenge;
import io.exonym.utils.storage.TrustNetwork;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VerifySupportSingleton {

    private static final Logger logger = LogManager.getLogger(VerifySupportSingleton.class);

    private ExonymOwner owner;

    private static VerifySupportSingleton instance;

    static {
        instance = new VerifySupportSingleton();

    }

    private VerifySupportSingleton(){
        try {
            IdContainer id = new IdContainer("cache");
            owner = new ExonymOwner(id);
            owner.openContainer();
            MyTrustNetworks mtn = new MyTrustNetworks();

            if (mtn.isModerator()){
                TrustNetwork tn = mtn.getModerator().getTrustNetwork();
                TrustNetworkWrapper tnw = new TrustNetworkWrapper(tn);
                URI issuerUid = tnw.getMostRecentIssuerParameters();
                UIDHelper helper = new UIDHelper(issuerUid);
                loadOwner(owner, helper);

                NetworkMapWeb map = (NetworkMapWeb) PkiExternalResourceContainer
                        .getInstance().getNetworkMap();
                Rulebook rulebook =mtn.getRulebook();
                if (rulebook!=null){
                    if (rulebook.getDescription().isProduction()){
                        NetworkMapItemModerator nmim = map.nmiForSybilMainNet();
                        helper = new UIDHelper(nmim.getLastIssuerUID());


                    }
                } else {
                    NetworkMapItemModerator nmim = map.nmiForSybilModTest();
                    helper = new UIDHelper(nmim.getLastIssuerUID());

                }
                loadOwner(owner, helper);
            }

        } catch (Exception e) {
            logger.info("Error", e);

        }
    }

    protected ExonymOwner getOwner() {
        return owner;
    }

    protected void loadOwner(ExonymOwner owner, UIDHelper helper) throws Exception {
        owner.openResourceIfNotLoaded(helper.getCredentialSpec());
        owner.openResourceIfNotLoaded(helper.getIssuerParameters());
        owner.openResourceIfNotLoaded(helper.getRevocationInfoParams());
        owner.openResourceIfNotLoaded(helper.getRevocationAuthority());
        if (!Rulebook.isSybil(helper.getRulebookUID())){
            owner.openResourceIfNotLoaded(helper.getInspectorParams());
        }
    }


    protected static VerifySupportSingleton getInstance(){
        return instance;
    }

    protected String verifyToken(SsoChallenge challengeAndToken) throws Exception {
        try {
            logger.info("Verifying Token");

            PresentationToken token = Parser.parsePresentationToken(
                    challengeAndToken.getToken());

            PresentationPolicyAlternatives ppa = juxtaposeTokenAndSsoChallenge(
                    challengeAndToken, token);
            owner.verifyClaim(ppa, token);

            JsonObject result = new JsonObject();
            byte[] compressedToken = produceEndonym(token, result);

            if (challengeAndToken.getIndex()!=null){
                logger.info("Proof verified... compressing token and producing identifiers and storing");
                storeToken(challengeAndToken.getIndex(), compressedToken, result);

            } else {
                logger.info("Proof verified... compressing token and producing identifiers and responding with token for storage");
                result.addProperty("tokenCompressedB64", Base64.encodeBase64String(compressedToken));

            }
            return result.toString();

        } catch (PolicyNotSatisfiedException e) {
            throw new UxException(ErrorMessages.REVOKED_OR_MOD_NOT_ACCEPTED_TRY_JOIN, e);

        }
    }

    private byte[] produceEndonym(PresentationToken token, JsonObject result) {
        try {
            PresentationTokenDescription ptd = token.getPresentationTokenDescription();
            List<PseudonymInToken> nyms = ptd.getPseudonym();
            PseudonymInToken target = null;
            for (PseudonymInToken pit : nyms){
                if (pit.isExclusive()){
                    target = pit;
                    break;
                }
            }
            if (target!=null){
                URI endonym = EndonymToken.endonymForm(target.getScope(), target.getPseudonymValue());
                result.addProperty("endonym", endonym.toString());

            }
            List<CredentialInToken> creds = ptd.getCredential();
            JsonArray mods = new JsonArray();
            for (CredentialInToken cit : creds){
                try {
                    URI mod = UIDHelper.computeModUidFromMaterialUID(cit.getIssuerParametersUID());
                    if (!Rulebook.isSybil(mod)){
                        mods.add(mod.toString());
                    }
                } catch (Exception e) {
                    logger.warn("Error", cit.getIssuerParametersUID());

                }
            }
            result.add("mods", mods);
            EndonymToken t = EndonymToken.build(null, token);
            return t.getCompressedPresentationToken();

        } catch (UxException e) {
            logger.info("Error", e);
            result.addProperty("rulebook-node-store-token-error", e.getMessage());
            return null;

        }
    }

    private void storeToken(String index, byte[] compressedTc, JsonObject result) {
        try {
            CouchRepository<ProofStore> repo = CouchDbHelper.repoProofs();
            ProofStore ps = new ProofStore();
            JsonElement nym = result.get("endonym");
            ps.setEndonym(nym!=null ? nym.getAsString() : null);
            ps.setTokenCompressed(compressedTc);
            ps.setLastAuthTime(DateHelper.getCurrentUtcMillis());
            JsonArray array = result.get("mods").getAsJsonArray();
            ps.setMods(array);
            ps.set_id(index);
            try {
                repo.create(ps);

            } catch (DocumentConflictException e) {
                ProofStore existing = repo.read(ps.get_id());
                ps.setPublicKey(existing.getPublicKey());
                ps.set_rev(existing.get_rev());
                repo.update(ps);

            }
        } catch (Exception e) {
            logger.info("Error", e);
            result.addProperty("store-token-error",
                    "Check Rulebook Node Logs" + e.getMessage());

        }
    }

    private PresentationPolicyAlternatives juxtaposeTokenAndSsoChallenge(SsoChallenge c, PresentationToken pt) throws Exception {
        PresentationPolicy policy = checkPseudonymAndChallenge(c, pt);
        PresentationTokenDescription ptd = pt.getPresentationTokenDescription();

        if (c.isSybil() || !c.getHonestUnder().isEmpty()){
            HashMap<String, CredentialInToken> rulebookIdToCredentialMap = checkSybil(policy, ptd);

            if (!c.getHonestUnder().isEmpty()){
                checkRulebooks(c, rulebookIdToCredentialMap);

            }
        }
        PresentationPolicyAlternatives ppa = new PresentationPolicyAlternatives();
        ppa.getPresentationPolicy().add(policy);
        return ppa;
    }

    private void checkRulebooks(SsoChallenge c, HashMap<String, CredentialInToken> rcMap) throws Exception {

        HashMap<String, RulebookAuth> requests = c.getHonestUnder();

        for (String rulebook : requests.keySet()){
            RulebookAuth auth = requests.get(rulebook);
            CredentialInToken cit = rcMap.get(rulebook);
            URI modUid = UIDHelper.computeModUidFromMaterialUID(cit.getIssuerParametersUID());
            logger.debug("Got ModID at check rulebooks= " + modUid);

            if (auth.getModBlacklist().contains(modUid)){
                throw new UxException(ErrorMessages.BLACKLISTED_MODERATOR);

            }
            URI leadUID = UIDHelper.computeLeadUidFromModUid(modUid);
            logger.debug("Got LeadID at check rulebooks= " + leadUID);

            if (auth.getLeadBlacklist().contains(leadUID)){
                throw new UxException(ErrorMessages.BLACKLISTED_LEAD);

            }
        }
    }

    private PresentationPolicy checkPseudonymAndChallenge(SsoChallenge c, PresentationToken pt) throws HubException, UxException {
        String domain = c.getDomain()!=null ? c.getDomain().toString() : null;
        PresentationTokenDescription ptd = pt.getPresentationTokenDescription();
        byte[] nonce = pt.getPresentationTokenDescription().getMessage().getNonce();
        JsonObject o = JsonParser.parseString(new String(nonce)).getAsJsonObject();
        String challengeIn = o.get("c").getAsString();
        logger.debug(challengeIn + " "+ c.getChallenge());

        if (!c.getChallenge().equals(challengeIn)){
            throw new UxException(ErrorMessages.TOKEN_INVALID);

        }
        List<PseudonymInToken> nyms = ptd.getPseudonym();
        PresentationPolicy pp = new PresentationPolicy();
        pp.setMessage(ptd.getMessage());
        pp.setPolicyUID(ptd.getPolicyUID());
        boolean hasBasis = false;
        boolean hasExclusive = false;

        for (PseudonymInToken nym : nyms){
            if (nym.getScope().equals(domain)){
                if (nym.isExclusive() && !hasExclusive){
                    pp.getPseudonym().add(Parser.nymInTokenToPolicy(nym));
                    URI endonym = EndonymToken.endonymForm(nym.getScope(), nym.getPseudonymValue());
                    EndonymToken et = EndonymToken.build(endonym, pt);
                    hasExclusive = true;

                }
            } else {
                if (!nym.isExclusive()){
                    pp.getPseudonym().add(Parser.nymInTokenToPolicy(nym));
                    hasBasis = true;

                }
            }
        }
        if (hasExclusive && hasBasis || (hasBasis && domain==null)){
            return pp;

        } else {
            throw new HubException(ErrorMessages.UNEXPECTED_PSEUDONYM_REQUEST);

        }
    }

    private HashMap<String, CredentialInToken> checkSybil(PresentationPolicy buildingPolicy,
                                                          PresentationTokenDescription ptd) throws Exception {

        boolean foundSybil = false;
        HashMap<String, CredentialInToken> map = new HashMap<>();
        List<CredentialInPolicy> credentials = new ArrayList<>();

        for (CredentialInToken cit : ptd.getCredential()){

            logger.debug("Issuer UID:" + cit.getIssuerParametersUID());

            URI rid = UIDHelper.computeRulebookUidFromNodeUid(
                    cit.getIssuerParametersUID());

            logger.debug("Rulebook ID:" + rid);

            map.put(rid.toString(), cit);
            credentials.add(Parser.credentialInTokenToPolicy(cit));
            URI issuerUid = cit.getIssuerParametersUID();
            if (Rulebook.isSybil(issuerUid)){
                foundSybil = true;

            }
        }
        buildingPolicy.getCredential().addAll(credentials);

        if (foundSybil){
            return map;

        } else {
            throw new HubException(ErrorMessages.SYBIL_WARN);

        }

    }


}
