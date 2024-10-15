package io.exonym.rulebook.context;

import com.google.gson.JsonObject;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.actions.MyTrustNetworkAndKeys;
import io.exonym.actor.actions.MyTrustNetworks;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.connect.WebUtils;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.ExoNotify;
import io.exonym.lite.pojo.Vio;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.PassStore;
import io.exonym.rulebook.schema.OverrideRequest;
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
import java.net.URI;

@WebServlet("/mod/*")
public class ModServlet extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(ModServlet.class);
    private MyTrustNetworks myTrustNetworks = null;
    private URI myLeadUid = null;
    private AsymStoreKey myLeadKey = null;

    // TODO /assert.  Only /revert is implemented.
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            initLead();
            OverrideRequest override = parseInput(req);
            IAuthenticator.getInstance()
                    .authenticateApiKey(override.getKid(), override.getKey());
            override.validate();

            verifyRequest(override);

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

    private OverrideRequest parseInput(HttpServletRequest req) throws Exception {
        String in = WebUtils.buildParamsAsString(req);
        logger.info(in);
        return JaxbHelper.gson.fromJson(in, OverrideRequest.class);

    }

    private void verifyRequest(OverrideRequest override) throws Exception {
        URI modUid = override.getModOfVioUid();
        URI lead = UIDHelper.computeLeadUidFromModUid(modUid);
        if (lead.equals(myLeadUid)){
            Vio vio = openVio(override);
            broadcastOverrideRequest(vio);

        } else {
            throw new UxException(ErrorMessages.INSUFFICIENT_PRIVILEGES,
                    "Overrides can only be executed by the Lead of the Trust Network",
                    "E.g. a Lead operates an appeals process for their moderators, but not other moderators of the same Rulebook.");
        }
    }

    private void broadcastOverrideRequest(Vio vio) throws UxException {
        try {
            vio.setOverride(true);

            ExoNotify notify = new ExoNotify();
            notify.setType(ExoNotify.TYPE_OVERRIDE);
            notify.setTimeOfViolation(vio.getTimeOfViolation());
            notify.setNodeUid(myLeadUid);
            notify.setNibble6(vio.getNibble6());
            notify.setHashOfX0(vio.getX0Hash());
            notify.getVios().add(vio);
            byte[] sigOn = ExoNotify.signatureOn(notify);
            byte[] sig = myLeadKey.sign(sigOn);
            notify.setSigB64(Base64.encodeBase64String(sig));

            NotificationPublisher.getInstance()
                    .getPipe().put(notify);

        } catch (Exception e) {
            throw new UxException(ErrorMessages.INSUFFICIENT_PRIVILEGES, e, "Error at broadcast");

        }
    }

    private Vio openVio(OverrideRequest override) throws UxException {
        try {
            String id = Vio.index(override);
            Vio vio = CouchDbHelper.repoVio().read(id);
            vio.blankIdAndRev();
            vio.setHistoric(null);
            vio.setDescriptionOfEvidence(null);
            return vio;

        } catch (Exception e) {
            throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, e,
                    "Unable to find violation.");

        }
    }

    private void initLead() throws UxException {
        if (myTrustNetworks==null){
            myTrustNetworks = new MyTrustNetworks();

        }
        if (!myTrustNetworks.isLeader()){
            throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE);

        } else {
            MyTrustNetworkAndKeys tn = myTrustNetworks.getLead();
            TrustNetwork trustNetwork = tn.getTrustNetwork();
            myLeadUid = trustNetwork.getNodeInformation().getNodeUid();
            RulebookNodeProperties props = RulebookNodeProperties.instance();

            try {
                PassStore store = new PassStore(props.getNodeRoot(), false);
                NetworkPublicKeyManager keys = NetworkPublicKeyManager.getInstance();
                this.myLeadKey = keys.openMyLeadKey(store);

            } catch (Exception e) {
                logger.error("Pass Store Error", e);

            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }

}
