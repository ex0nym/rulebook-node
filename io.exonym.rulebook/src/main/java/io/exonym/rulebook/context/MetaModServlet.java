package io.exonym.rulebook.context;

import com.google.gson.JsonObject;
import com.ibm.zurich.idmx.jaxb.JaxbHelperClass;
import eu.abc4trust.xml.IssuanceMessage;
import eu.abc4trust.xml.IssuanceToken;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.actions.MyTrustNetworks;
import io.exonym.lite.connect.WebUtils;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.time.Timing;
import io.exonym.rulebook.schema.Appeal;
import io.exonym.utils.ExtractObject;
import io.exonym.utils.storage.NodeInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@WebServlet("/meta-mod/*")
public class MetaModServlet extends HttpServlet {
    private static final Logger logger = LogManager.getLogger(MetaModServlet.class);
    private final ConcurrentHashMap<String, JoinProcessor> appealRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionToChallenge = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> challengeToSession = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Appeal> sessionToAppeals = new ConcurrentHashMap<>();

    private final long timeout = 10000;

    private MyTrustNetworks myTrustNetworks;
    private JoinSupportSingleton joinSupport;

    private JsonObject redirectResponse;


    @Override
    public void init() throws ServletException {
        this.myTrustNetworks = new MyTrustNetworks();

        if (myTrustNetworks.isLeader()) {
            this.joinSupport = JoinSupportSingleton.getInstance();

        } else if (myTrustNetworks.isModerator()) {
            NodeInformation ni = myTrustNetworks.getModerator()
                    .getTrustNetwork().getNodeInformation();

            String url = ni.getStaticLeadUrl0().getAuthority();
            url += "metamod";

            redirectResponse = new JsonObject();
            redirectResponse.addProperty("error", ErrorMessages.INCORRECT_MODERATOR);
            redirectResponse.addProperty("leadUid", ni.getLeadUid().toString());
            redirectResponse.addProperty("appealUrl", url);

        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            if (myTrustNetworks.isLeader()) {
                joinChallenge(req, resp);

            } else {
                if (myTrustNetworks.isModerator()) {
                    WebUtils.respond(resp, redirectResponse);

                } else {
                    throw new UxException(ErrorMessages.RULEBOOK_NODE_NOT_INITIALIZED);

                }
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
            response = join.joinChallenge(false, true);

        } else {
            String[] request = path.split("/");

            if (request.length > 1) {
                if (request[1].equals("qr")) {
                    response = join.joinChallenge(true, true);

                } else {
                    logger.info("Ignoring request " + request);

                }
            }
            if (response == null) {
                throw new UxException(ErrorMessages.URL_INVALID, path, "" + request.length);

            }
        }
        appealRequests.put(join.getHashOfNonce(), join);
        sessionToChallenge.put(req.getSession().getId(), join.getHashOfNonce());
        WebUtils.respond(resp, response);

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String in = WebUtils.buildParamsAsString(req);
            if (in != null) {
                if (in.startsWith("{\"probe")) {
                    registerProbe(req, resp);

                } else {
                    buildReport(in, resp);

                }
            }
        } catch (UxException e) {
            WebUtils.processError(e, resp);

        } catch (Exception e) {
            WebUtils.processError(
                    new UxException(ErrorMessages.SERVER_SIDE_PROGRAMMING_ERROR, e),
                    resp);

        }
    }

    private void registerProbe(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String session = req.getSession().getId();
        String challenge = this.sessionToChallenge.get(session);

        if (challenge!=null){
            this.challengeToSession.put(challenge, session);

            synchronized (session){
                try {
                    long t0 = Timing.currentTime();
                    session.wait(timeout);

                    if (!Timing.hasBeen(t0, timeout)){
                        if (this.sessionToAppeals.containsKey(session)){
                            Appeal report = this.sessionToAppeals.get(session);
                            String json = JaxbHelper.gson.toJson(report);
                            WebUtils.respond(resp, json);

                        } else {
                            throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE);

                        }
                    } else {
                        throw new UxException(ErrorMessages.TIME_OUT);

                    }
                } catch (InterruptedException e) {
                    logger.info("Interrupted", e);

                }
            }
        } else {
            throw new UxException(ErrorMessages.UNEXPECTED_TOKEN_FOR_THIS_NODE);

        }
    }

    private void buildReport(String xml, HttpServletResponse resp) throws Exception {
        IssuanceMessage message = null;
        IssuanceToken issuanceToken = null;
        JoinProcessor join = null;
        String session = null;

        try {
            message = (IssuanceMessage) JaxbHelperClass.deserialize(xml).getValue();
            issuanceToken = ExtractObject.extract(
                    message.getContent(), IssuanceToken.class);

            assert issuanceToken != null;
            byte[] nonce = issuanceToken.getIssuanceTokenDescription()
                    .getPresentationTokenDescription()
                    .getMessage().getNonce();

            String hashOfNonce = CryptoUtils.computeSha256HashAsHex(nonce);
            logger.debug("challenge=" + hashOfNonce);

            join = appealRequests.remove(hashOfNonce);
            session = this.challengeToSession.get(hashOfNonce);

            if (session!=null){

                if (join != null) {
                    Appeal appeal = join.searchAndVerify(message, issuanceToken);
                    this.sessionToAppeals.put(session, appeal);
                    WebUtils.success(resp);

                    synchronized (session){
                        session.notify();

                    }
                } else {
                    throw new UxException(ErrorMessages.UNEXPECTED_TOKEN_FOR_THIS_NODE, "Join");

                }
            } else {
                throw new UxException(ErrorMessages.UNEXPECTED_TOKEN_FOR_THIS_NODE, "Session");

            }
        } catch (Exception e) {
            throw e;

        }
    }
}