package io.exonym.rulebook.context;

import io.exonym.abc.util.JaxbHelper;
import io.exonym.lite.connect.WebUtils;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.parallel.ExpiringHashMap;
import io.exonym.lite.pojo.ProofStore;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.WhiteList;
import io.exonym.lite.time.Timing;
import io.exonym.rulebook.schema.AuthenticationWrapper;
import io.exonym.rulebook.schema.SsoChallenge;
import io.exonym.rulebook.schema.SsoConfigWrapper;
import io.exonym.rulebook.schema.SsoConfiguration;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@WebServlet("/auth-status/*")
public class AuthStatusServlet extends HttpServlet {
    private static final Logger logger = LogManager.getLogger(AuthStatusServlet.class);
    private static long timeoutChallenge = 4000;
    private static long timeoutSig = 2500;

    private VerifySupportSingleton verify;

    private SsoConfiguration ssoConfig;

    private ExpiringHashMap<String, SsoChallenge> challenges;
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String kid = req.getHeader("kid");
            String key = req.getHeader("key");
            String[] path = req.getPathInfo().split("/");

            if (path.length>1){
                if (kid!=null || key!=null){
                    String response = getAuthStatus(kid, key, path);
                    WebUtils.respond(resp, response);

                } else {
                    String challenge = setupChallenge(path);
                    WebUtils.respond(resp, challenge);

                }
            } else {
                throw new UxException(ErrorMessages.INCORRECT_PARAMETERS);

            }
        } catch (UxException e) {
            WebUtils.processError(e, resp);

        } catch (Exception e) {
            WebUtils.processError(new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, e), resp);

        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String[] path = req.getPathInfo().split("/");

            if (path.length>1){
                String in = WebUtils.buildParamsAsString(req);
                logger.debug("Should be an xml token= " + in);
                SsoChallenge challenge = challenges.get(path[1]);

                if (challenge!=null){
                    challenge.setIndex(CryptoUtils.computeMd5HashAsHex(path[1]));
                    challenge.setToken(Base64.encodeBase64String(
                            in.getBytes(StandardCharsets.UTF_8)));
                    verify.verifyToken(challenge);
                    WebUtils.success(resp);

                } else {
                    throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, "Timeout or unexpected token");

                }
            } else {
                throw new Exception();

            }
        } catch (UxException e) {
            WebUtils.processError(e, resp);

        } catch (Exception e) {
            WebUtils.processError(new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, e), resp);

        }
    }


    private String setupChallenge(String[] path) throws UxException {
        try {
            if (path.length < 4){
                String epsilon = CryptoUtils.computeMd5HashAsHex(path[1]);
                String sig = path[2];
                CouchRepository<ProofStore> repo = CouchDbHelper.repoProofs();
                ProofStore proof = repo.read(epsilon);
                AsymStoreKey k = AsymStoreKey.blank();
                k.assembleKey(proof.getPublicKey());
                logger.info("Received Sig:" + sig);
                byte[] decoded = Hex.decodeHex(sig);
                byte[] deciphered = k.decipherWithPublicKey(decoded);
                long t0 = Long.parseLong(new String(deciphered, StandardCharsets.UTF_8));
                boolean expired = Timing.hasBeen(t0, timeoutSig);

                if (!expired){
                    SsoChallenge c = SsoChallenge.newChallenge(ssoConfig);
                    AuthenticationWrapper<SsoChallenge> challengeWrapper = AuthenticationWrapper
                            .wrapToWrapper(c, SsoChallenge.class);
                    this.challenges.put(path[1], c);
                    return challengeWrapper.getLink();

                } else {
                    throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, "Stale");

                }
            } else {
                throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, "No signature");

            }
        } catch (UxException e) {
            throw e;

        } catch (Exception e) {
            throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, e);

        }
    }

    private String getAuthStatus(String kid, String key, String[] path) throws Exception {
        IAuthenticator.getInstance()
                .authenticateApiKey(kid, key);

        CouchRepository<ProofStore> repo = CouchDbHelper.repoProofs();
        ProofStore proof = repo.read(CryptoUtils.computeMd5HashAsHex(path[1]));

        if (proof.getLastAuthTime()==0){
            if (proof.getRejoinCriteria()!=null){
                return JaxbHelper.gson.toJson(proof.getRejoinCriteria());

            } else {
                return "" + 0; // unauthenticated.

            }
        } else {
            return "" + proof.getLastAuthTime();

        }
    }

    @Override
    public void init() throws ServletException {
        RulebookNodeProperties props = RulebookNodeProperties.instance();

        SsoConfigWrapper define = new SsoConfigWrapper();
        String rulebookToVerify =  props.getRulebookToVerifyUrn();

        if (WhiteList.isRulebookUid(rulebookToVerify)){
            URI rulebookUid = URI.create(rulebookToVerify);
            define.requireRulebook(rulebookUid);
            challenges = new ExpiringHashMap<>(timeoutChallenge);
            ssoConfig = define.getConfig();
            verify = VerifySupportSingleton.getInstance();

        } else {
            outputError();

        }
    }

    private void outputError() {
        logger.error(">>>>>>>>>>>>>>>> ");
        logger.error("> ");
        logger.error("> ERROR AT VERIFY END-POINT: NO RULEBOOK_URN DEFINED IN ENV-VARIABLES");
        logger.error("> ");
        logger.error("> ");
    }
}
