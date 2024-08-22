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

@WebServlet("/revoke")
public class RevokeServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(RevokeServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            HashMap<String, String> in = WebUtils.buildParams(req, resp);
            IAuthenticator auth = IAuthenticator.getInstance();
            auth.authenticateApiKey(in);
            logger.debug("Completed Authentication");
            revoke(in, req, resp);


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

}
