package io.exonym.rulebook.context;

import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.ibm.zurich.idmx.jaxb.JaxbHelperClass;
import eu.abc4trust.xml.IssuanceMessage;
import eu.abc4trust.xml.IssuanceMessageAndBoolean;
import eu.abc4trust.xml.IssuanceToken;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.lite.connect.WebUtils;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.PenaltyException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.*;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.time.DateHelper;
import io.exonym.utils.ExtractObject;
import io.exonym.utils.storage.IdContainer;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@WebServlet("/metamod/*")
public class MetaModServlet extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(MetaModServlet.class);

    private final ConcurrentHashMap<String, JoinProcessor> appealRequests = new ConcurrentHashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if (RulebookNodeProperties.instance().isOpenSubscription()) { // check MyTrustNetworks for Lead
                joinChallenge(req, resp); // same because we need to establish the issues through the auth.

            } else {
                JoinSupportSingleton support = JoinSupportSingleton.getInstance();
                Rulebook rulebook = support.getRulebookVerifier().getDisplayRulebook(); // find rules from rNs
                WebUtils.respond(resp, JaxbHelper.gson.toJson(rulebook, Rulebook.class)); // respond with history.

            }
        } catch (UxException e) {
            WebUtils.processError(e, resp);

        } catch (Exception e) {
            WebUtils.processError(new UxException(ErrorMessages.URL_INVALID, e), resp);

        }
    }

    private void joinChallenge(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JoinProcessor join = new JoinProcessor();
        String path = req.getPathInfo();
        String response = null;
        if (path == null) {
            response = join.joinChallenge(false);

        } else {
            String[] request = path.split("/");
            if (request.length > 1) {
                if (request[1].equals("qr")) {
                    response = join.joinChallenge(true);

                } else {
                    logger.debug("Ignoring request " + request);

                }
            }
            if (response == null) {
                throw new UxException(ErrorMessages.URL_INVALID, path, "" + request.length);

            }
        }
        appealRequests.put(join.getHashOfNonce(), join);
        WebUtils.respond(resp, response);

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        IssuanceMessage message = null;
        IssuanceToken issuanceToken = null;
        JoinProcessor join = null;

        try {
            String xml = WebUtils.buildParamsAsString(req);
            message = (IssuanceMessage) JaxbHelperClass.deserialize(xml).getValue();
            issuanceToken = ExtractObject.extract(
                    message.getContent(), IssuanceToken.class);

            assert issuanceToken != null;
            byte[] nonce = issuanceToken.getIssuanceTokenDescription()
                    .getPresentationTokenDescription().getMessage().getNonce();
            String hashOfNonce = CryptoUtils.computeSha256HashAsHex(nonce);
            join = appealRequests.remove(hashOfNonce);

            if (join != null) {
                /**
                 *
                 *
                 *
                 */
                IssuanceMessageAndBoolean imab = join.join(message, issuanceToken);
                /**
                 *
                 */
                String response = IdContainer.convertObjectToXml(imab);
                resp.getWriter().write(response);

            } else {
                throw new UxException(ErrorMessages.UNEXPECTED_TOKEN_FOR_THIS_NODE);

            }
        } catch (PenaltyException e) {
            /**
             *
             * RejoinCriteria rejoinCriteria = join.evalPenaltyReport(e.getReport(), message, issuanceToken);
             *
             */
            evalPenaltyReport(e.getReport(), message, issuanceToken, join, resp);
            /**
             *
             */
        } catch (UxException e) {
            WebUtils.processError(e, resp);

        } catch (Exception e) {
            WebUtils.processError(
                    new UxException(ErrorMessages.SERVER_SIDE_PROGRAMMING_ERROR, e),
                    resp);

        }
    }

    private void evalPenaltyReport(ApplicantReport report,
                                   IssuanceMessage message,
                                   IssuanceToken token,
                                   JoinProcessor join,
                                   HttpServletResponse resp) {
        try {
            JoinSupportSingleton joinSupport = JoinSupportSingleton.getInstance();
            RulebookGovernor governor = joinSupport.getGovernor();

            DateTime tOffence = report.getMostRecentOffenceTimeStamp();
            String t0 = DateHelper.isoUtcDateTime(tOffence);
            String x0Hash = CryptoUtils.computeSha256HashAsHex(report.getX0());

            Vio vio = targetWithHistory(x0Hash, report.getN6(), t0);
            ArrayList<Penalty> penalties = governor.getPenaltiesMaxIndex0(
                    vio.getHistoric());

            RejoinCriteria rejoin = applyPenalty(penalties.get(0), t0, vio);

            if (rejoin.isCanRejoin()){
                settlePenalty(rejoin, join, message, token, vio, resp);

            } else {
                throw new UxException(JaxbHelper.gson.toJson(rejoin));

            }
        } catch (UxException e) {
            WebUtils.processError(e, resp);

        } catch (Exception e) {
            WebUtils.processError(
                    new UxException(ErrorMessages.SERVER_SIDE_PROGRAMMING_ERROR, e),
                    resp);

        }

    }

    private void settlePenalty(RejoinCriteria rejoin, JoinProcessor join,
                               IssuanceMessage message, IssuanceToken token,
                               Vio vio, HttpServletResponse resp) throws UxException {
        try {
            IssuanceMessageAndBoolean imab = join.rejoin(message, token, vio);
            String xml = IdContainer.convertObjectToXml(imab);
            String imabB64 = Base64.encodeBase64String(
                    xml.getBytes(StandardCharsets.UTF_8));
            rejoin.setImabFinalB64(imabB64);
            String json = JaxbHelper.gson.toJson(rejoin);
            resp.getWriter().write(json);

        } catch (Exception e) {
            throw new UxException(ErrorMessages.SERVER_SIDE_PROGRAMMING_ERROR, e);

        }
    }

    private RejoinCriteria applyPenalty(Penalty penalty, String t0, Vio vio) throws UxException {
        RejoinCriteria result = new RejoinCriteria();
        String type = penalty.getType();
        result.setPenaltyType(type);
        // N.B: this will need to be modified when cascading revocation is implemented.
        result.getRevokedModerators().add(vio.getModOfVioUid());

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

    private Vio targetWithHistory(String x0Hash, String n6, String t0) throws UxException {
        try {
            QueryBasic q = new QueryBasic();
            q.getSelector().put(Vio.FIELD_X0_HASH, x0Hash);
            ArrayList<Vio> vios = (ArrayList<Vio>) CouchDbHelper.repoVio().read(q);
            ArrayList<URI> previous = new ArrayList<>();
            Vio thisVio = null;

            for (Vio vio : vios){
                if (vio.getNibble6().equals(n6)){
                    if (t0.equals(vio.getTimeOfViolation())){
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


}
