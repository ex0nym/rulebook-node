package io.exonym.rulebook.context;

import com.cloudant.client.org.lightcouch.NoDocumentException;
import eu.abc4trust.xml.*;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.VerifiedClaim;
import io.exonym.actor.actions.ExonymMatrix;
import io.exonym.actor.actions.NodeVerifier;
import io.exonym.actor.actions.TokenVerifier;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.connect.WebUtils;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.PenaltyException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.*;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.Form;
import io.exonym.lite.standard.PassStore;
import io.exonym.lite.standard.QrCode;
import io.exonym.lite.time.DateHelper;
import io.exonym.rulebook.schema.Appeal;
import io.exonym.rulebook.schema.AppealTransaction;
import io.exonym.rulebook.schema.IdContainer;
import io.exonym.rulebook.schema.RuleForAppeal;
import io.exonym.utils.storage.ImabAndHandle;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class JoinProcessor {

    private static final Logger logger = LogManager.getLogger(JoinProcessor.class);
    private final VerifiedClaim claim;

    private JoinSupportSingleton support = JoinSupportSingleton.getInstance();

    private RulebookNodeProperties props = RulebookNodeProperties.instance();

    private final HashMap<URI, String> riToXj = new HashMap<>();

    private String hashOfNonce = null;


    public JoinProcessor() throws Exception {
        if (support!=null){
            claim = new VerifiedClaim(support.getMyModCs());

        } else {
            throw new UxException(ErrorMessages.RULEBOOK_NODE_NOT_INITIALIZED,
                    "This Rulebook Node is not fully defined.",
                    "Please be patient while the administrator sets up this node");

        }
    }

    protected String joinChallenge(boolean qr, boolean isAppeal) throws Exception {
        try {
            IssuanceMessageAndBoolean imab = initIssuance(isAppeal);
            Rulebook challengedRulebook = support.getRulebookVerifier().getDeepCopy();
            String xml = IdContainer.convertObjectToXml(imab);
            byte[] compressed = WebUtils.compress(xml.getBytes(StandardCharsets.UTF_8));
            String b64Xml = Base64.encodeBase64String(compressed);
            String link = Namespace.UNIVERSAL_LINK_JOIN_REQUEST + b64Xml;

            if (qr){
                String qrImage = QrCode.computeQrCodeAsPngB64(link, 300);
                challengedRulebook.setChallengeB64(qrImage);

            }
            challengedRulebook.setLink(link);
            return JaxbHelper.gson.toJson(challengedRulebook, Rulebook.class);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);

        }
    }

    private IssuanceMessageAndBoolean initIssuance(boolean appeal) throws Exception {
        IssuancePolicy policy = support.buildIssuancePolicy(appeal);
        byte[] nonce = policy.getPresentationPolicy().getMessage().getNonce();
        this.hashOfNonce = CryptoUtils.computeSha256HashAsHex(nonce);

        String xml = io.exonym.utils.storage.
                IdContainer.convertObjectToXml(policy);

        logger.debug("Challenge for joiner: " + xml);

        return support.getMyModIssuer().issueInit(claim, policy,
                support.getStore().getEncrypt(),
                URI.create("urn:" + UUID.randomUUID()));

    }

    protected IssuanceMessageAndBoolean rejoin(IssuanceMessage im,
                                               IssuanceToken issuanceToken, Vio vio) throws Exception {

        PresentationTokenDescription ptd = issuanceToken
                .getIssuanceTokenDescription()
                .getPresentationTokenDescription();

        ImabAndHandle imab = support.getMyModIssuer().issueStep(
                im, support.getStore().getDecipher());

        String x0 = buildExonymMap(ptd);
        addMemberToDb(imab, x0);
        publishExonymMapAndBroadcast(new ArrayList<>(), vio); // see register potential honesty claims.
        return imab.getImab();

    }

    protected RejoinCriteria evalPenaltyReport(ApplicantReport report,
                                     IssuanceMessage message,
                                     IssuanceToken token, boolean isOverride) throws UxException {

        logger.info("Evaluating the penalty report: isOverride=" + isOverride);
        String x0Hash = CryptoUtils.computeSha256HashAsHex(report.getX0());
        DateTime tOffence = report.getMostRecentOffenceTimeStamp();
        String t0 = DateHelper.isoUtcDateTime(tOffence);
        Vio vio = targetWithHistory(x0Hash, report.getN6(), t0);
        JoinSupportSingleton joinSupport = JoinSupportSingleton.getInstance();
        RulebookGovernor governor = joinSupport.getGovernor();

        ArrayList<Penalty> penalties = governor.getPenaltiesMaxIndex0(
                vio.getHistoric());

        RejoinCriteria rejoin = applyPenalty(penalties.get(0), t0, vio);

        if (isOverride){
            rejoin.setCanRejoin(true);
            rejoin.setPenaltyType(null);
            rejoin.setBannedLiftedUTC("Overridden");
            rejoin.setAppealUrl(null);

        }
        if (rejoin.isCanRejoin()){
            return settlePenalty(rejoin, message, token, vio);

        } else {
            return rejoin;

        }
    }

    private RejoinCriteria settlePenalty(RejoinCriteria rejoinCriteria, IssuanceMessage message,
                                         IssuanceToken token,
                                         Vio vio) throws UxException {
        try {
            IssuanceMessageAndBoolean imab = rejoin(message, token, vio);
            String xml = io.exonym.utils.storage.IdContainer.convertObjectToXml(imab);
            String imabB64 = Base64.encodeBase64String(
                    xml.getBytes(StandardCharsets.UTF_8));
            rejoinCriteria.setImabFinalB64(imabB64);
            return rejoinCriteria;

        } catch (Exception e) {
            throw new UxException(ErrorMessages.SERVER_SIDE_PROGRAMMING_ERROR, e);

        }
    }

    private RejoinCriteria applyPenalty(Penalty penalty, String t0, Vio vio) throws UxException {
        RejoinCriteria result = new RejoinCriteria();
        String type = penalty.getType();
        result.setPenaltyType(type);
        // N.B: this will need to be modified when cascading revocation is implemented.
        logger.debug("Vio@Penalty" + vio);
        result.getRevokedModerators().add(vio.getModOfVioUid());
        result.setTovutc(vio.getTimeOfViolation());
        result.setX0Hash(vio.getX0Hash());
        result.setNibble6(vio.getNibble6());

        if (Penalty.TYPE_TIME_BAN.equals(type)){
            return applyTimeban(result, penalty, t0);

        } else if (Penalty.TYPE_NONE.equals(type)){
            result.setCanRejoin(true);
            return result;

        } else {
            throw new UxException("PENALTY_TYPE_NOT_IMPLEMENTED");

        }
    }

    private RejoinCriteria applyTimeban(RejoinCriteria criteria,
                                        Penalty penalty, String t0) {

        if (Penalty.DEN_TEMP_PERMANENT.equals(penalty.getDenomination())){
            criteria.setCanRejoin(false);
            criteria.setPenaltyType(Penalty.DEN_TEMP_PERMANENT);

        } else {
            ZonedDateTime timeOfBan = ZonedDateTime.parse(t0,
                    DateTimeFormatter.ISO_ZONED_DATE_TIME);


            ChronoUnit timeUnit = ChronoUnit.valueOf(
                    penalty.getDenomination().toUpperCase());

            int size = penalty.getQuantity();
            double coeff = (penalty.getOffenceCount() - 1) * penalty.getRepeatOffenceMultiplier();
            double b = coeff > 0 ? size * coeff : size;
            int banTime = (int) Math.ceil(b);

            ZonedDateTime banLiftedAt = timeOfBan.plus(banTime, timeUnit);
            criteria.setBannedLiftedUTC(banLiftedAt.toString());
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));

            boolean isAfter = now.isAfter(banLiftedAt);

            criteria.setCanRejoin(isAfter);
            logger.info("Banned@" + t0  + " Lifted@" + banLiftedAt + " Now@" + now);

        }
        return criteria;

    }

    private Vio targetWithHistory(String x0Hash, String n6, String tov) throws UxException {
        try {
            QueryBasic q = new QueryBasic();
            q.getSelector().put(Vio.FIELD_X0_HASH, x0Hash);
            ArrayList<Vio> vios = (ArrayList<Vio>) CouchDbHelper.repoVio().read(q);
            ArrayList<URI> previous = new ArrayList<>();
            Vio thisVio = null;

            for (Vio vio : vios){
                if (vio.getNibble6().equals(n6)){
                    if (tov.equals(vio.getTimeOfViolation())){
                        thisVio = vio;

                    } else {
                        previous.addAll(vio.getRuleUids());

                    }
                } else {
                    logger.info("Ignoring hash collision " + vio.getNibble6() + " " + n6);

                }
            }
            if (thisVio!=null){
                ArrayList<URI> thisViolationsRules = thisVio.getRuleUids();
                HashMap<String, Integer> history = thisVio.getHistoric();
                if (history.isEmpty()){
                    for (URI rN : thisViolationsRules){
                        int count = Collections.frequency(previous, rN);
                        history.put(rN.toString(), count);

                    }
                    CouchDbHelper.repoVio().update(thisVio);
                }
                return thisVio;

            } else {
                throw new HubException("Couldn't find this violation");

            }
        } catch (NoDocumentException e) {
            throw new UxException(ErrorMessages.DB_TAMPERING, e);

        } catch (Exception e) {
            throw new UxException(ErrorMessages.SERVER_SIDE_PROGRAMMING_ERROR);

        }
    }

    protected Appeal searchAndVerify(IssuanceMessage im, IssuanceToken issuanceToken) throws Exception {
        // Verify token is real before revealing information.
        support.getMyModIssuer().issueStep(
                im, support.getStore().getDecipher());

        PresentationTokenDescription ptd = issuanceToken
                .getIssuanceTokenDescription()
                .getPresentationTokenDescription();

        ApplicantReport report = generateApplicantReport(ptd);
        return initAppeal(report);

    }

    private Appeal initAppeal(ApplicantReport report) throws UxException {
        if (report.isUnresolvedOffences()){
            Appeal result = new Appeal();
            result.setTimeOfViolation(DateHelper.isoUtcDateTime(
                    report.getMostRecentOffenceTimeStamp()));

            ArrayList<ExonymDetailedResult> relevant = isolateRelevantResults(
                    report.getDetailedResults());
            ExonymDetailedResult target = relevant.get(0);
            Violation mostRecentViolation = target.getViolations().get(0);

            ArrayList<URI> unsettledRules = new ArrayList<>();
            RulebookGovernor governor = support.getGovernor();
            for (ExonymDetailedResult detail : relevant){
                unsettledRules.addAll(detail.getUnsettledRuleId());

            }
            for (URI unsettled : unsettledRules){
                RuleForAppeal ruleForAppeal = new RuleForAppeal();
                RulebookItem ri = governor.getRule(unsettled.toString());
                ruleForAppeal.setRuleUid(unsettled);
                ruleForAppeal.setCurrentPenalty(ri.getPenalty());
                ruleForAppeal.setRuleOriginal(ri.getDescription());
                result.getTargetRules().add(ruleForAppeal);

            }
            result.setModOfVioUid(target.getModUID());
            result.setRequestingModUid(
                    mostRecentViolation.getRequestingModUid());

            result.setNibble6(report.getN6());
            result.setOpenForAppeal(true);

            Vio vio = targetWithHistory(
                    CryptoUtils.computeSha256HashAsHex(report.getX0()),
                    report.getN6(),
                    result.getTimeOfViolation());

            Penalty appliedPenalty = governor
                    .getPenaltiesMaxIndex0(
                    vio.getHistoric()).get(0);

            RejoinCriteria rejoin = applyPenalty(appliedPenalty, result.getTimeOfViolation(), vio);
            result.setBanLifted(rejoin.getBannedLiftedUTC());
            String tov = DateHelper.isoUtcDateTime(report.getMostRecentOffenceTimeStamp().getMillis());
            String x0Hash = CryptoUtils.computeSha256HashAsHex(report.getX0());
            rejoin.setTovutc(tov);
            result.setX0Hash(x0Hash);
            rejoin.setX0Hash(x0Hash);

            rejoin.setNibble6(report.getN6());
            result.setNibble6(report.getN6());

            AppealTransaction history0 = new AppealTransaction();
            history0.setActor(AppealTransaction.ACTOR_PRODUCER);
            history0.setTimestamp(DateHelper.getCurrentUtcMillis());
            history0.setDateTime(DateHelper.isoUtcDateTime(history0.getTimestamp()));
            result.setStatus(Appeal.STATUS_RAISING);
            result.getHistory().add(history0);

            return result;

        } else {
            throw new UxException(ErrorMessages.NO_OUTSTANDING_APPEALS);

        }
    }

    private ArrayList<ExonymDetailedResult> isolateRelevantResults(
            ArrayList<ExonymDetailedResult> detailedResults) throws UxException {
        ArrayList<ExonymDetailedResult> relevant = new ArrayList<>();

        for (ExonymDetailedResult detail : detailedResults){
            if (detail.isUnsettled() && !detail.isOverridden()){
                relevant.add(detail);
            }
        }
        if (relevant.size()>1){
            logger.warn("Selecting the most recent incident for reporting");

        } else if (relevant.isEmpty()){
            throw new UxException(ErrorMessages.NO_OUTSTANDING_APPEALS, "relevant was empty.");

        }
        return relevant;
    }


    protected IssuanceMessageAndBoolean join(IssuanceMessage im,
                                             IssuanceToken issuanceToken) throws Exception {

        PresentationTokenDescription ptd = issuanceToken
                .getIssuanceTokenDescription()
                .getPresentationTokenDescription();

        // This will throw the penalty exception.
        ArrayList<String> uncontrolled = verifyConditionsToJoin(ptd);

        ImabAndHandle imab = support.getMyModIssuer().issueStep(
                im, support.getStore().getDecipher());

        String x0 = buildExonymMap(ptd);
        addMemberToDb(imab, x0);
        publishExonymMapAndBroadcast(uncontrolled, null); // see register potential honesty claims.
        return imab.getImab();

    }

    private void addMemberToDb(ImabAndHandle imab, String x0) throws Exception {
        try {
            logger.info("Adding user to the database");
            String[] nibbles = ExonymMatrix.extractNibbles(x0);
            String hashOfH = CryptoUtils.computeSha256HashAsHex(
                    imab.getHandle().toByteArray());
            IUser user = new IUser();
            user.setType(IUser.I_USER_MEMBER);
            user.setX0(x0);
            user.setNibble6(nibbles[1]);
            user.set_id(hashOfH);
            CouchDbHelper.repoUsersAndAdmins().create(user);

        } catch (Exception e) {
            throw e;

        }
    }

    private String buildExonymMap(PresentationTokenDescription token) {
        List<PseudonymInToken> nyms = token.getPseudonym();
        String x0 = null;
        for (PseudonymInToken nym : nyms){
            String xj = Form.toHex(nym.getPseudonymValue());
            if (nym.isExclusive() && x0==null) {
                x0 = xj;
            }
            String ri = nym.getScope();
            this.riToXj.put(URI.create(ri), xj);

        }
        return x0;
    }

    /**
     * This isn't complete.
     *
     * TODO when users can join multiple rules.
     *
     * @param verifier
     * @param token
     * @return
     * @throws Exception
     */
    private ArrayList<URI> registerPotentialHonestyClaim(TokenVerifier verifier,
                                                            PresentationToken token) throws Exception {
        PresentationTokenDescription ptd = token.getPresentationTokenDescription();
        List<CredentialInToken> credentials = ptd.getCredential();
        URI myLeadUid = support.getMyModerator().getLeadUID();

        for (CredentialInToken cit : credentials){
            URI ip = cit.getIssuerParametersUID();
            String ips = ip.toString();

            if (!(ips.contains("sybil") && ips.contains("anticlone"))) { // todo error
                UIDHelper helper = new UIDHelper(ip);

                if (helper.getLeadUid().equals(myLeadUid)){
                    return loadVerifierWithCurrentProofsOfHonesty(verifier, ip, token);

                } else {
                    throw new UxException(ErrorMessages.PROOF_IS_OUT_OF_SCOPE,
                            helper.getLeadUid().toString(), myLeadUid.toString());
                }
            }
        }
        return new ArrayList<>();
    }


    @Deprecated
    private ArrayList<URI> loadVerifierWithCurrentProofsOfHonesty(TokenVerifier verifier,
                                                                     URI issuerUid, PresentationToken token) throws Exception {
        UIDHelper uids = new UIDHelper(issuerUid);

        NodeVerifier n0 = new NodeVerifier(support.getMyModerator().getNodeUID());

        n0.loadTokenVerifierFromNodeVerifier(verifier, uids);

        /**
         * If the user is already honest we must establish what rules we control
         *
         */
        return collectUncontrolledRules(uids.getModeratorUid(), token);
//        throw new RuntimeException("There was this funny noKeyHere test that you didn't understand - TODO");
//
//        if (!files.containsKey("noKeyHere")){
//            throw new UxException(ErrorMessages.TOKEN_INVALID, "Not implemented current proofs of honesty");
//
//        }
    }

    private ArrayList<URI> collectUncontrolledRules(URI host, PresentationToken token) throws Exception {
        ArrayList<URI> uncontrolledRules = new ArrayList<>();
        byte[] unverifiedX0Bytes = token
                .getPresentationTokenDescription()
                .getPseudonym().get(0)
                .getPseudonymValue();

        String unverifiedX0 = Form.toHex(unverifiedX0Bytes);
        String n6 = unverifiedX0.substring(0, 6);
        ExonymMatrixManagerGlobal global = new ExonymMatrixManagerGlobal(
                (NetworkMapItemModerator) support.getNetworkMap().nmiForNode(host),
                support.getMyModerator(), n6, this.props.getNodeRoot());

        try {
            ExonymMatrix matrix = global.openUncontrolledList(unverifiedX0);
            ExonymMatrixRow row = matrix.findExonymRow(unverifiedX0);
            ArrayList<URI> nodeRules = matrix.getRuleUrns();

            int n = 0;
            for (String xn : row.getExonyms()) {
                if (!xn.equals("null")){
                    uncontrolledRules.add(nodeRules.get(n));

                }
                n++;
            }
        } catch (HubException e) {
            logger.warn("Expected Error if the target has no uncontrolled rules\n\t\t" + e.getMessage());

        }
        return uncontrolledRules;
    }

    private ApplicantReport generateApplicantReport(PresentationTokenDescription token) throws Exception {
        ApplicantReport applicantReport = performSearch(token, support.getMyRules());
        if (applicantReport==null){
            throw new UxException(ErrorMessages.NO_OUTSTANDING_APPEALS);

        } else {
            WebUtils.logDebugProtect("generateApplicantReport(): ", applicantReport);

        }
        return applicantReport;

    }

    private ArrayList<String> verifyConditionsToJoin(PresentationTokenDescription token) throws Exception {
        ArrayList<String> result = new ArrayList<>();
        ApplicantReport applicantReport = performSearch(token, support.getMyRules());

        WebUtils.logDebugProtect("verifyConditionsToJoin(applicantReport) ", applicantReport);

        if (applicantReport!=null){
            if (!applicantReport.getExceptions().isEmpty()){
                throw applicantReport.getExceptions();

            }
            if (applicantReport.isMember()){
                ArrayList<URI> previous = applicantReport.getPreviousHosts();
                String[] p = new String[previous.size()];
                int i = 0;
                for (URI a : previous){
                    p[i] = a.toString();
                    i++;

                }
                throw new UxException(ErrorMessages.ALREADY_JOINED, p);

            }
            if (applicantReport.isUnresolvedOffences()){
                PenaltyException e = new PenaltyException(ErrorMessages.BANNED_UNTIL);
                e.setReport(applicantReport);
                throw e;

            }
            ExonymDetailedResult mostRecent = applicantReport.getDetailedResults().get(0);
            if (mostRecent.isOverridden()){
                PenaltyException e = new PenaltyException(ErrorMessages.OVERRIDDEN_CAN_REISSUE);
                e.setReport(applicantReport);
                throw e;

            }
        }
        // TODO note that the report needs to return a list of the rules that are uncontrolled
        return result;

    }

    private ApplicantReport performSearch(PresentationTokenDescription token, ArrayList<URI> rules) throws Exception {
        ExonymSearch network = new ExonymSearch(token, rules,
                CouchDbHelper.repoExoMatrix(), support.getNetworkMap(),
                this.props.getNodeRoot());

        ExonymResult result = network.search();

        WebUtils.logDebugProtect("Will expand results if not null: ", result);

        if (result!=null){
            return network.expandResults(result);

        } else {
            return null;

        }
    }


    private void publishExonymMapAndBroadcast(ArrayList<String> uncontrolled, Vio vio) throws Exception {
        ExonymMatrixRowAndX0 xyLists = buildXYLists(uncontrolled);
        ExonymMatrixManagerLocal local = new ExonymMatrixManagerLocal(
                support.getMyModContainer(),
                support.getMyRules(), support.getMyModerator(),
                props.getNodeRoot());

        local.addExonymRow(vio, xyLists.getExox(), xyLists.getExoy());
        broadcastJoin(xyLists.getN6(), xyLists.getX0(), vio);

    }

    private ExonymMatrixRowAndX0 buildXYLists(ArrayList<String> uncontrolledRules) {
        ExonymMatrixRowAndX0 result = new ExonymMatrixRowAndX0();
        ArrayList<String> u = new ArrayList<>();
        ArrayList<String> c = new ArrayList<>();
        ArrayList<URI> myRules = support.getMyRules();
        String x0 = riToXj.get(myRules.get(0));
        String x0Hash = CryptoUtils.computeSha256HashAsHex(x0);
        String n6 = x0.substring(0,6);

        for (URI ri : myRules){
            if (uncontrolledRules.contains(ri)){
                u.add(riToXj.get(ri));
                c.add("null");

            } else {
                u.add("null");
                c.add(riToXj.get(ri));

            }
        }
        ExonymMatrixRow controlled = new ExonymMatrixRow();
        controlled.addExonyms(c);
        ExonymMatrixRow uncontrolled = new ExonymMatrixRow();
        uncontrolled.addExonyms(u);
        result.setExox(controlled);
        result.setExoy(uncontrolled);
        result.setN6(n6);
        result.setX0(x0Hash);
        if (uncontrolledRules.isEmpty()){
            result.setExox(controlled);
            result.setExoy(null);

        } else {
            result.setExox(controlled);
            result.setExoy(uncontrolled);

        }
        return result;
    }

    protected void broadcastJoin(String n6, String x0Hash, Vio vio) throws Exception {
        ExoNotify notify = new ExoNotify();
        notify.setType(ExoNotify.TYPE_JOIN);
        notify.setNibble6(n6);
        notify.setHashOfX0(x0Hash);
        notify.setNodeUid(support.getMyModerator().getNodeUID());
        notify.setT(DateHelper.currentIsoUtcDateTime());
        notify.setTimeOfViolation(vio!=null ? vio.getTimeOfViolation() : null);
        signAndSend(notify);

    }

    protected void signAndSend(ExoNotify notify) {
        try {
            byte[] toSign = ExoNotify.signatureOn(notify);
            RulebookNodeProperties props = RulebookNodeProperties.instance();
            PassStore store = new PassStore(props.getNodeRoot(), false);
            ModeratorNotificationSigner signer = ModeratorNotificationSigner.getInstance();
            byte[] sigBytes = signer.sign(toSign,
                    support.getMyModerator().getNodeUID().toString(), store);

            String sig = Base64.encodeBase64String(sigBytes);
            notify.setSigB64(sig);

            NotificationPublisher.getInstance()
                    .getPipe().put(notify);

        } catch (Exception e) {
            throw new RuntimeException(e);

        }
    }

    public String getHashOfNonce() {
        return hashOfNonce;
    }
}
