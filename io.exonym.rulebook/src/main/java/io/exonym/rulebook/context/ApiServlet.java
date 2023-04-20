package io.exonym.rulebook.context;

import com.google.gson.JsonObject;
import io.exonym.lite.connect.WebUtils;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

@WebServlet("/api")
public class ApiServlet extends HttpServlet {


    private static final Logger logger = LogManager.getLogger(ApiServlet.class);
    public static final String CMD_SIGN = "sign";

    public static final String CMD_REVOKE = "revoke"; // against context string??

    public static final String CMD_BLACKLIST_HOST = "blacklist-host";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            HashMap<String, String> in = WebUtils.buildParams(req, resp);
            IAuthenticator auth = IAuthenticator.getInstance();
            auth.authenticateApiKey(in);
            logger.debug("Completed Authentication");
            String cmd = in.get("cmd");
            if (cmd != null) {
                if (cmd.equals(CMD_REVOKE)) {
                    revoke(in, req, resp);

                } else if (cmd.equals(CMD_BLACKLIST_HOST)) {
                    blacklistHost(in, req, resp);

                } else if (cmd.equals(CMD_SIGN)) {
                    sign(in, resp);

                } else {
                    throw new UxException("Unknown command " + cmd);

                }
            } else {
                throw new UxException("Expected a command");

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

    // Sign Materials with Root Key
    private void sign(HashMap<String, String> in, HttpServletResponse resp) throws UxException {
        throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, "Not yet implemented");

    }

    /**
     * Global revocation disincentivised by appeal.
     *
     * @param in
     * @param req
     * @param resp
     */
    private void revoke(HashMap<String, String> in,
                        HttpServletRequest req, HttpServletResponse resp) throws Exception {
        throw new UxException(ErrorMessages.INCORRECT_PARAMETERS,
                "Not yet implemented", "Please use the Control Panel");
    }

    /**
     * Adds a host to a blacklist that this service will no longer accept
     *
     * @param in
     * @param req
     * @param resp
     */
    private void blacklistHost(HashMap<String, String> in,
                               HttpServletRequest req, HttpServletResponse resp) throws Exception {
        throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, "Not yet implemented");
    }
}
