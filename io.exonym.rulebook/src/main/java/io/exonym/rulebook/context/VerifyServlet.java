package io.exonym.rulebook.context;

import com.cloudant.client.org.lightcouch.DocumentConflictException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.abc4trust.xml.*;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.actions.ExonymOwner;
import io.exonym.helpers.Parser;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.connect.WebUtils;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.IssuanceSigma;
import io.exonym.lite.pojo.ProofStore;
import io.exonym.lite.pojo.Rulebook;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.Form;
import io.exonym.lite.time.Timing;
import io.exonym.rulebook.schema.EndonymToken;
import io.exonym.rulebook.schema.RulebookAuth;
import io.exonym.rulebook.schema.SsoChallenge;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@WebServlet("/verify/*")
public class VerifyServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(VerifyServlet.class);

    private VerifySupportSingleton verify;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String kid = req.getHeader("kid");
            String key = req.getHeader("key");
            IAuthenticator.getInstance().authenticateApiKey(kid, key);
            String[] path = req.getPathInfo().split("/");
            if (path.length==2){
                // get public key for [1]
                String kGamma = getUsersPublicKey(path[1]);
                WebUtils.respond(resp, kGamma);

            } else if (path.length==3){
                // store public key [2] for [1]
                storeUsersPublicKey(path[1], path[2]);
                WebUtils.success(resp);

            }
        } catch (UxException e) {
            WebUtils.processError(e, resp);

        } catch (Exception e) {
            WebUtils.processError(new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, e), resp);

        }
    }

    private void storeUsersPublicKey(String index, String keyHex) throws Exception {
        CouchRepository<ProofStore> repo = CouchDbHelper.repoProofs();
        index = CryptoUtils.computeMd5HashAsHex(index);
        byte[] decoded = Hex.decode(keyHex);
        ProofStore ps = new ProofStore();
        ps.set_id(index);
        ps.setPublicKey(decoded);
        try {
            repo.create(ps);

        } catch (DocumentConflictException e) {
            ProofStore existing = repo.read(ps.get_id());
            existing.setPublicKey(decoded);
            repo.update(existing);

        }
    }

    private String getUsersPublicKey(String index) {
        String md5 = CryptoUtils.computeMd5HashAsHex(index);
        try {
            CouchRepository<ProofStore> repo = CouchDbHelper.repoProofs();
            return Form.toHex(repo.read(md5).getPublicKey());

        } catch (Exception e) {
            logger.debug("Error", e);
            return "NO_KEY_WITH_INDEX:" + index + " md5=" + md5;

        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String kid = req.getHeader("kid");
            String key = req.getHeader("key");

            if (kid!=null && key!=null){
                String json = WebUtils.buildParamsAsString(req);
                SsoChallenge challengeAndToken = JaxbHelper.gson
                        .fromJson(json, SsoChallenge.class);

                if (challengeAndToken.getToken()!=null){
                    IAuthenticator auth = IAuthenticator.getInstance();
                    auth.authenticateApiKey(kid, key);
                    String nym = verify.verifyToken(challengeAndToken);

                    if (nym!=null){
                        WebUtils.respond(resp, nym);
                    } else {
                        WebUtils.success(resp);
                    }
                } else {
                    throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, "no token");

                }
            } else {
                throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, "kid", "key");

            }
        } catch (UxException e) {
            WebUtils.processError(e, resp);

        } catch (Exception e) {
            WebUtils.processError(new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, e), resp);

        }
    }


    @Override
    public void init() throws ServletException {
        verify = VerifySupportSingleton.getInstance();

    }
}
