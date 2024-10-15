package io.exonym.rulebook.context;

import com.cloudant.client.org.lightcouch.DocumentConflictException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.google.gson.JsonObject;
import com.ibm.zurich.idmx.jaxb.JaxbHelperClass;
import com.sun.xml.ws.util.ByteArrayBuffer;
import eu.abc4trust.cryptoEngine.CryptoEngineException;
import eu.abc4trust.xml.*;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.actions.*;
import io.exonym.actor.storage.RevocationRequest;
import io.exonym.actor.storage.RevocationRequestWrapper;
import io.exonym.actor.storage.RevocationResponse;
import io.exonym.helpers.BuildCredentialSpecification;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.connect.Http;
import io.exonym.lite.connect.WebUtils;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.*;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.Const;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.PassStore;
import io.exonym.lite.time.DateHelper;
import io.exonym.utils.storage.IdContainer;
import io.exonym.utils.storage.KeyContainer;
import io.exonym.utils.storage.KeyContainerWrapper;
import io.exonym.utils.storage.TrustNetwork;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

@WebServlet("/revoke")
public class RevokeServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(RevokeServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String in = WebUtils.buildParamsAsString(req);
            RevocationRequestWrapper revocationReq = JaxbHelper.gson.fromJson(in, RevocationRequestWrapper.class);
            if (revocationReq.getKid()!=null){
                IAuthenticator auth = IAuthenticator.getInstance();
                auth.authenticateApiKey(
                        revocationReq.getKid(), revocationReq.getKey());
                logger.debug("Completed Authentication");
                verifyRequests(revocationReq);
                revoke(revocationReq, resp);

            } else {
                inNetworkModRevocationRequest(revocationReq, req, resp);

            }
        } catch (UxException e) {
            logger.debug("error", e);
            JsonObject o = new JsonObject();
            o.addProperty("error", e.getMessage());
            WebUtils.respond(resp, o);

        } catch (Exception e) {
            logger.error("General API Error", e);
            WebUtils.processError(e, resp);

        }
    }

    private void verifyRequests(RevocationRequestWrapper revocationReq) throws UxException {
        ArrayList<RevocationRequest> rs = revocationReq.getRequests();
        int i = 0;
        for (RevocationRequest r : rs){
            byte[] bytes = r.getDescriptionOfEvidence().getBytes(StandardCharsets.UTF_8);
            if (bytes.length>RevocationRequest.MAX_LENGTH_OF_DESCRIPTION_BYTES) {
                throw new UxException(ErrorMessages.REVOCATION_EVIDENCE_DESCRIPTION_MUST_BE_190_BYTES_OR_LESS, "index=" + i);

            }
            if (r.getEndonymToken()==null){
                throw new UxException(ErrorMessages.BLANK_TOKEN_DISCOVERED, "index=" + i);

            }
            if (r.getRuleUri().isEmpty()){
                throw new UxException(ErrorMessages.BLANK_RULE_URI, "index=" + i);

            }
            i++;
        }
    }

    private void inNetworkModRevocationRequest(RevocationRequestWrapper rr, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        byte[] sig = rr.getSignature();
        if (sig==null){
            throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, "No signature");
        }
        try {
            JoinSupportSingleton join = null;
            NetworkMapItem nmi = null;
            boolean verified = false;
            String hash = RevocationRequestWrapper.signatureOn(rr.getRequests());
            join = JoinSupportSingleton.getInstance();
            NetworkMapWeb nmw = join.getNetworkMap();
            nmi = nmw.nmiForNode(rr.getModerator());
            AsymStoreKey key = AsymStoreKey.blank();
            key.assembleKey(nmi.getPublicKeyB64());
            verified = key.verifySignature(hash.getBytes(StandardCharsets.UTF_8), sig);
            if (verified){
                prepareToRevokeAll(nmi.getNodeUID(), join, rr.getRequests());

            } else {
                throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE);

            }
        } catch (UxException e) {
            throw e;

        } catch (Exception e) {
            throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, e);

        }
    }

    private void prepareToRevokeAll(URI requestingMod, JoinSupportSingleton join,
                                    ArrayList<RevocationRequest> requests) throws Exception {

        ArrayList<RevocationAndViolation> ravs = new ArrayList<>();

        URI[] targetModAndRai = null;

        URI myModUid = join.getMyModeratorHelper().getModeratorUid();

        for (RevocationRequest r : requests){
            PresentationToken pt = decompressToken(r.getEndonymToken());
            if (targetModAndRai==null){
                targetModAndRai = computeTargetModAndRai(pt);
            }
            if (targetModAndRai[0].equals(myModUid)){
                Violation v = computeViolation(requestingMod, r);
                RevocationAndViolation rav = new RevocationAndViolation();
                rav.setViolation(v);
                rav.setPresentationToken(pt);
                rav.setRevocationRequest(r);
                rav.setRaUid(targetModAndRai[1]);
                ravs.add(rav);

            } else {
                logger.warn("Incorrect mod " + targetModAndRai[0]);

            }
        }
        RulebookNodeProperties props = RulebookNodeProperties.instance();

        PassStore store = new PassStore(props.getNodeRoot(), false);

        AsymStoreKey myKey = NetworkPublicKeyManager.getInstance()
                .openMyModKey(store);

        revokeThisModeratorsTokens(requestingMod, ravs, store, myKey, props);

    }

    /**
     * Global revocation disincentivised by appeal.
     *
     * @param rr
     * @param resp
     *
     */
    private void revoke(RevocationRequestWrapper rr, HttpServletResponse resp) throws Exception {

        RulebookNodeProperties props = RulebookNodeProperties.instance();
        PassStore store = new PassStore(props.getNodeRoot(), false);
        NetworkPublicKeyManager publicKeys = NetworkPublicKeyManager.getInstance();
        AsymStoreKey key = NetworkPublicKeyManager.getInstance()
                .openMyModKey(store);

        ArrayList<RevocationRequest> requests = rr.getRequests();
        HashMap<URI, ArrayList<RevocationAndViolation>> toProcess = new HashMap<>();

        UIDHelper helper = JoinSupportSingleton.getInstance()
                .getMyModeratorHelper();

        URI myModUid = helper.getModeratorUid();
        int modCount = 0;
        int invalidTokens = 0;
        int thisModTokens = 0;

        for (RevocationRequest request : requests){
            PresentationToken pt = decompressToken(request.getEndonymToken());
            Violation violation = computeViolation(myModUid, request);
            URI[] targetModAndRai = computeTargetModAndRai(pt);
            AsymStoreKey modPk = publicKeys.getKey(targetModAndRai[0]);
            String b64Enc = Base64.encodeBase64String(
                    modPk.encrypt(request.getDescriptionOfEvidence()
                        .getBytes(StandardCharsets.UTF_8)));

            request.setDescriptionOfEvidence(b64Enc);

            if (targetModAndRai!=null){
                ArrayList<RevocationAndViolation> ravs = toProcess.get(targetModAndRai[0]);

                RevocationAndViolation rav = new RevocationAndViolation();
                rav.setRevocationRequest(request);
                rav.setViolation(violation);
                rav.setPresentationToken(pt);
                rav.setRaUid(targetModAndRai[1]);

                if (ravs!=null){
                    ravs.add(rav);

                } else {
                    ravs = new ArrayList<>();
                    ravs.add(rav);
                    toProcess.put(targetModAndRai[0], ravs);
                    modCount++;

                }
            } else {
                logger.warn("NON-RULEBOOK-TOKEN");
                invalidTokens++;

            }
        }
        for (URI targetMod : toProcess.keySet()){
            ArrayList<RevocationAndViolation> ravs = toProcess.get(targetMod);
            if (targetMod.equals(myModUid)){
                revokeThisModeratorsTokens(myModUid, ravs, store, key, props);
                thisModTokens = ravs.size();

            } else {
                try {
                    wrapSignAndSend(targetMod, myModUid, ravs, key);

                } catch (Exception e) {
                    logger.info(targetMod + " was not found on the network map", e);

                }
            }
        }
        RevocationResponse revocationResponse = new RevocationResponse();
        revocationResponse.setInvalidTokens(invalidTokens);
        revocationResponse.setModCount(modCount);
        revocationResponse.setThisModTokens(thisModTokens);
        revocationResponse.setTotalTokens(requests.size() - invalidTokens);
        resp.getWriter().write(JaxbHelper.gson.toJson(revocationResponse));

    }


    private URI[] computeTargetModAndRai(PresentationToken pt) {
        PresentationTokenDescription ptd = pt.getPresentationTokenDescription();
        ArrayList<CredentialInToken> cits = (ArrayList<CredentialInToken>) ptd.getCredential();

        for (CredentialInToken cit : cits){
            URI i = cit.getIssuerParametersUID();
            if (!Rulebook.isSybil(i)){
                try {
                    return new URI[] {
                            UIDHelper.computeModUidFromMaterialUID(i),
                            UIDHelper.transformMaterialUid(i, "ra")
                    };

                } catch (Exception e) {
                    logger.warn("Error at Target ModUID Computation: " + i);

                }
                break;
            }
        }
        return null;
    }

    private PresentationToken decompressToken(String endonymToken) throws Exception {
        byte[] compressed = Base64.decodeBase64(
                endonymToken.getBytes(StandardCharsets.UTF_8));
        String xml = new String(WebUtils.decompress(compressed), StandardCharsets.UTF_8);
        return (PresentationToken) JaxbHelperClass.deserialize(xml).getValue();

    }

    private Violation computeViolation(URI requestingModUid, RevocationRequest r) {
        Violation v = new Violation();
        v.setRequestingModUid(requestingModUid);
        v.setRuleUids(r.getRuleUrns());
        v.setTimestamp(DateHelper.currentIsoUtcDateTime());
        logger.info("Adding timestamp to Violation: " + v.getTimestamp());
        return v;

    }

    private void revokeThisModeratorsTokens(URI requestingMod,
                                            ArrayList<RevocationAndViolation> ravs,
                                            PassStore store,
                                            AsymStoreKey myKey,
                                            RulebookNodeProperties props) throws Exception {
        JoinSupportSingleton join = JoinSupportSingleton.getInstance();
        UIDHelper myNode = join.getMyModeratorHelper();

        ExonymIssuer issuer = join.getMyModIssuer();
        RevocationAndViolation sample = ravs.get(0);
        ExonymInspector ins = join.getMyModInspector(sample.getPresentationToken(), store);

        ArrayList<BigInteger> handles = new ArrayList<>();
        URI ra = sample.getRaUid();
        ArrayList<Violation> violations = new ArrayList<>();
        ArrayList<Vio> vios = new ArrayList<>();
        CouchRepository<IUser> userRepo = CouchDbHelper.repoUsersAndAdmins();
        ArrayList<IUser> tidy = new ArrayList<>();

        for (RevocationAndViolation rav : ravs){
            BigInteger handle = discoverHandle(ins, rav.getPresentationToken());
            String hh = CryptoUtils.computeSha256HashAsHex(handle.toByteArray());

            try {
                IUser user = userRepo.read(hh);
                tidy.add(user);

                Violation violation = rav.getViolation();
                String x0 = user.getX0();
                violation.setX0(x0);
                violations.add(violation);

                Vio vio = new Vio();
                vio.setModOfVioUid(myNode.getModeratorUid());
                vio.setT(violation.getTimestamp());
                vio.setX0Hash(CryptoUtils.computeSha256HashAsHex(x0));
                vio.setNibble6(x0.substring(0,6));
                vio.setRuleUids(violation.getRuleUids());
                vio.set_id(Vio.index(vio));

                // This may be the same, but can be a third-party moderator request.
                // if third party, the evidence is stored at the third party.
                vio.setModUidOfRequestor(requestingMod);

                // TODO handle bad key.
                byte[] descBytes = Base64.decodeBase64(
                        rav.getRevocationRequest().getDescriptionOfEvidence()
                                .getBytes(StandardCharsets.UTF_8));

                String desc = new String(myKey.decipher(descBytes), StandardCharsets.UTF_8);
                vio.setDescriptionOfEvidence(desc);

                vios.add(vio);

            } catch (NoDocumentException e) {
                logger.warn("Unable to find x0 for handle__ " + hh);

            }
            handles.add(handle);

        }
        RevocationInformation ri = issuer.revocationBulkValidHandles(ra, handles, store.getDecipher());

        NodeManagerWeb nodeManager = new NodeManagerWeb(myNode.getLeadName());

        // Update everything in bulk after ALL success.
        String raiHash = publishLocalRevocationData(ri, myNode.getRevocationInfoParams(), nodeManager, myKey);
        logger.info(raiHash + "--------- TODO --------");
        join.getMyModContainer().saveLocalResource(ri, true);
        issuer.clearStale();
        join.reopen();
        updateLocalExonymMap(join, props, violations);

        // TODO update raiHash raiB64AndSigB64[2] on WhoIs

        // TODO update local map /network on receipt.
        //        publishPrai(raiB64AndSigB64[0], raiB64AndSigB64[1], myNode.getModeratorUid());
        handleViosForBroadcast(vios);
        publishPraiResign(ri, myKey, myNode.getModeratorUid());
        publishViolations(vios, myNode.getModeratorUid(), myKey);
        deleteRevokedMembers(userRepo, tidy);

    }

    private void handleViosForBroadcast(ArrayList<Vio> vios) {
        try {
            CouchRepository<Vio> repo = CouchDbHelper.repoVio();

            for (Vio vio : vios){
                try {
                    repo.create(vio);

                } catch (DocumentConflictException e) {
                    Vio dbVio = repo.read(vio.get_id());
                    vio.set_rev(dbVio.get_rev());
                    repo.update(vio);

                }
                vio.setDescriptionOfEvidence(null);
                vio.blankIdAndRev();

            }
        } catch (Exception e) {
            logger.info("Error writing vios locally:", e );
        }
    }

    private void deleteRevokedMembers(CouchRepository<IUser> userRepo, ArrayList<IUser> tidy) {
        for (IUser user : tidy){
            try {
                userRepo.delete(user);
            } catch (Exception e) {
                logger.warn("Could not delete user with raiHash=" + user.get_id());
            }
        }
    }

    private void publishPraiResign(RevocationInformation ri, AsymStoreKey myKey, URI myModeratorUid) throws Exception {
        ExonymIssuer.removeRevocationHistory(ri);
        String riString = IdContainer.convertObjectToXml(ri);


        byte[] riSign = NodeVerifier.stripStringToSign(riString).getBytes();
        byte[] sig = myKey.sign(riSign);

        String sigB64 = Base64.encodeBase64String(sig);
        String raiB64 = Base64.encodeBase64String(riString.getBytes(StandardCharsets.UTF_8));

        ExoNotify notify = new ExoNotify();
        notify.setT(DateHelper.currentIsoUtcDateTime());
        notify.setNodeUid(myModeratorUid);
        notify.setRaiSigB64(sigB64);
        notify.setRaiB64(raiB64);
        notify.setType(ExoNotify.TYPE_MOD);

        try {
            NotificationPublisher publisher = NotificationPublisher.getInstance();
            publisher.getPipe().put(notify);

        } catch (InterruptedException e) {
            logger.debug("Interrupted", e);

        }
    }

    /**
     *
     * @param ri
     * @param nodeManager
     * @param key
     * @return {b64 encoded raiXml, sigOnXmlB64, raiHashOfXml }
     * @throws Exception
     */
    protected String publishLocalRevocationData(RevocationInformation ri, URI raiUid, NodeManager nodeManager, AsymStoreKey key) throws Exception {

        MyTrustNetworkAndKeys myTrustNetwork = new MyTrustNetworkAndKeys(false);
        TrustNetwork tn = myTrustNetwork.getTrustNetwork();

        String riString = IdContainerJSON.convertObjectToXml(ri);
        String raiHash = CryptoUtils.computeSha256HashAsHex(riString);
        String niString = JaxbHelper.serializeToXml(
                myTrustNetwork.getTrustNetwork(),
                TrustNetwork.class);

        byte[] riSign = NodeVerifier.stripStringToSign(riString).getBytes();
        byte[] niSign = NodeVerifier.stripStringToSign(niString).getBytes();

        byte[] rai = riString.getBytes();
        byte[] ni = niString.getBytes();

        KeyContainerWrapper kcPublic = myTrustNetwork.getKcw();
        byte[] sig = key.sign(riSign);

        HashMap<URI, ByteArrayBuffer> toSign = new HashMap<>();

        toSign.put(raiUid, new ByteArrayBuffer(riSign));
        toSign.put(tn.getNodeInformationUid(), new ByteArrayBuffer(niSign));

        URI baseUrl = tn.getNodeInformation().getRulebookNodeUrl();
        Path modPath = Path.of(Const.STATIC, Const.MODERATOR);
        URI nodeUrl = URI.create(baseUrl.toString() + modPath);

        nodeManager.signatureUpdateXml(key, toSign, kcPublic, nodeUrl);

        String xml = JaxbHelper.serializeToXml(kcPublic.getKeyContainer(), KeyContainer.class);

        nodeManager.publish(nodeUrl, xml.getBytes(), Const.SIGNATURES_XML);
        nodeManager.publish(nodeUrl, rai, IdContainerJSON.uidToXmlFileName(raiUid));
        nodeManager.publish(nodeUrl, ni, IdContainerJSON.uidToXmlFileName(Const.TRUST_NETWORK_UID));

        return raiHash;

    }

//    private void publishPrai(String raiB64, String sigB64, URI myModeratorUid) {
//        ExoNotify notify = new ExoNotify();
//        notify.setT(DateHelper.currentIsoUtcDateTime());
//        notify.setNodeUID(myModeratorUid);
//        notify.setRaiSigB64(sigB64);
//        notify.setRaiB64(raiB64);
//        notify.setType(ExoNotify.TYPE_MOD);
//
//        try {
//            NotificationPublisher publisher = NotificationPublisher.getInstance();
//            publisher.getPipe().put(notify);
//
//        } catch (InterruptedException e) {
//            logger.debug("Interrupted", e);
//
//        }
//    }


    private void updateLocalExonymMap(JoinSupportSingleton join, RulebookNodeProperties props,
                                      ArrayList<Violation> violations) throws Exception {
        ExonymMatrixManagerLocal exonymLocal = new ExonymMatrixManagerLocal(
                join.getMyModContainer(), join.getMyRules(),
                join.getMyModerator(),props.getNodeRoot());

        for (Violation v : violations){
            try {
                exonymLocal.addViolation(v.getX0(), v);

            } catch (Exception e) {
                logger.info(v.getX0() + " violation could not be updated." , e);

            }
        }
    }



    private void publishViolations(ArrayList<Vio> vios,
                                   URI myModUid,
                                   AsymStoreKey key) {
        ExoNotify notify = new ExoNotify();
        notify.setT(DateHelper.currentIsoUtcDateTime());
        notify.setNodeUid(myModUid);
        notify.setVios(vios);
        notify.setType(ExoNotify.TYPE_VIOLATION);
        byte[] toSign = ExoNotify.signatureOn(notify);
        byte[] sig = key.sign(toSign);
        notify.setSigB64(Base64.encodeBase64String(sig));

        try {
            NotificationPublisher publisher = NotificationPublisher.getInstance();
            publisher.getPipe().put(notify);

        } catch (InterruptedException e) {
            logger.debug("Interrupted", e);

        }
    }





    private BigInteger discoverHandle(ExonymInspector ins, PresentationToken pt) throws UxException {
        try {
            ArrayList<Attribute> atts = (ArrayList<Attribute>) ins.inspect(pt);
            HashMap<URI, Attribute> map = new HashMap<>();

            logger.debug("There were " + atts.size()
                    + " attribute(s) produced as a result of the inspection");

            for (Attribute a : atts) {
                map.put(a.getAttributeDescription().getType(), a);

            }
            if (!atts.isEmpty()){
                Attribute a = map.get(BuildCredentialSpecification.REVOCATION_HANDLE_UID);
                return new BigInteger("" + a.getAttributeValue());

            } else {
                throw new UxException(ErrorMessages.TOKEN_INVALID, "No attributes returned");

            }
        } catch (CryptoEngineException e) {
            logger.error("Error inspecting token", e);
            return null;

        } catch (UxException e) {
            throw e;

        }
    }

    private void wrapSignAndSend(URI targetModUid, URI myModUid, ArrayList<RevocationAndViolation> ravs, AsymStoreKey key) throws Exception {
        ArrayList<RevocationRequest> thirdParty = new ArrayList<>();
        for (RevocationAndViolation rav : ravs){
            thirdParty.add(rav.getRevocationRequest());

        }
        RevocationRequestWrapper rrw = new RevocationRequestWrapper();
        rrw.setRequests(thirdParty);
        rrw.setModerator(myModUid);
        String hashToSign = RevocationRequestWrapper.signatureOn(thirdParty);
        byte[] sig = key.sign(hashToSign.getBytes(StandardCharsets.UTF_8));
        rrw.setSignature(sig);
        NetworkMapWeb nmw = JoinSupportSingleton.getInstance().getNetworkMap();
        NetworkMapItem nmim = nmw.nmiForNode(targetModUid);
        URI target = nmim.getRulebookNodeURL().resolve("revoke");
        logger.info("sending revocation request to " + target);
        Http http = new Http();
        String responseFromMod = http.basicPost(target.toString(), JaxbHelper.gson.toJson(rrw));
        logger.info(responseFromMod);
        http.close();

    }

    private class RevocationAndViolation {
        RevocationRequest revocationRequest;
        Violation violation;
        PresentationToken presentationToken;
        URI raUid;

        public RevocationRequest getRevocationRequest() {
            return revocationRequest;
        }

        public void setRevocationRequest(RevocationRequest revocationRequest) {
            this.revocationRequest = revocationRequest;
        }

        public Violation getViolation() {
            return violation;
        }

        public void setViolation(Violation violation) {
            this.violation = violation;
        }

        public PresentationToken getPresentationToken() {
            return presentationToken;
        }

        public void setPresentationToken(PresentationToken presentationToken) {
            this.presentationToken = presentationToken;
        }

        public URI getRaUid() {
            return raUid;
        }

        public void setRaUid(URI raUid) {
            this.raUid = raUid;
        }
    }



}
