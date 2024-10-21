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

@WebServlet("/join/*")
public class JoinServlet extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(JoinServlet.class);

    private final ConcurrentHashMap<String, JoinProcessor> joinRequests = new ConcurrentHashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if (RulebookNodeProperties.instance().isOpenSubscription()) {
                joinChallenge(req, resp);

            } else {
                JoinSupportSingleton support = JoinSupportSingleton.getInstance();
                Rulebook rulebook = support.getRulebookVerifier().getDisplayRulebook();
                WebUtils.respond(resp, JaxbHelper.gson.toJson(rulebook, Rulebook.class));

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
            response = join.joinChallenge(false, false);

        } else {
            String[] request = path.split("/");
            if (request.length > 1) {
                if (request[1].equals("qr")) {
                    response = join.joinChallenge(true, false);

                } else {
                    logger.debug("Ignoring request " + request);

                }
            }
            if (response == null) {
                throw new UxException(ErrorMessages.URL_INVALID, path, "" + request.length);

            }
        }
        joinRequests.put(join.getHashOfNonce(), join);
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
            join = joinRequests.remove(hashOfNonce);

            if (join != null) {
                /**
                 *
                 */
                IssuanceMessageAndBoolean imab = join.join(message, issuanceToken);
                /**
                 */
                String response = IdContainer.convertObjectToXml(imab);
                resp.getWriter().write(response);

            } else {
                throw new UxException(ErrorMessages.UNEXPECTED_TOKEN_FOR_THIS_NODE);

            }
        } catch (PenaltyException e) {
            penalty(e.getReport(), join, message, issuanceToken, resp);

        } catch (UxException e) {
            WebUtils.processError(e, resp);

        } catch (Exception e) {
            WebUtils.processError(
                    new UxException(ErrorMessages.SERVER_SIDE_PROGRAMMING_ERROR, e),
                    resp);

        }
    }

    private void penalty(ApplicantReport report, JoinProcessor join, IssuanceMessage message, IssuanceToken issuanceToken, HttpServletResponse resp) {
        try {
            RejoinCriteria rejoinCriteria = join.evalPenaltyReport(report, message, issuanceToken);
            String json = JaxbHelper.gson.toJson(rejoinCriteria);
            resp.getWriter().write(json);

        } catch (UxException e) {
            WebUtils.processError(e, resp);

        } catch (Exception e) {
            WebUtils.processError(
                    new UxException(ErrorMessages.SERVER_SIDE_PROGRAMMING_ERROR, e),
                    resp);

        }
    }
}
