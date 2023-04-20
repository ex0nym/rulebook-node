package io.exonym.lite.authenticators;

import com.google.gson.JsonObject;
import io.exonym.lite.connect.WebUtils;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.standard.PassStore;

import javax.security.sasl.AuthenticationException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

public abstract class AuthenticateServlet extends HttpServlet {

    public static final String CMD_CHANGE_PASSWORD = "change-password";
    public static final String CMD_SESSION = "session";
    public static final String CMD_LOGOFF = "logoff";
    public static final String CMD_IS_LOGGED_IN = "is-logged-in";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            HashMap<String, String> in = WebUtils.buildParams(req, resp);
            String cmd = in.get("cmd");
            if (cmd!=null){
                StandardAuthenticator auth = openAuthenticator();
                if (cmd.equals(CMD_SESSION)){
                    auth.authenticateAdministratorOrMember(in, req, resp);

                } else if (cmd.equals(CMD_LOGOFF)){
                    auth.endSession(req, resp);

                } else if (cmd.equals(CMD_CHANGE_PASSWORD)){
                    auth.changePassword(in, req, resp);

                } else if (cmd.equals(CMD_IS_LOGGED_IN)){
                    checkAdminLoggedIn(in, req, resp);

                } else {
                    throw new UxException(ErrorMessages.UNKNOWN_COMMAND, cmd);

                }
            } else {
                resp.sendRedirect("logon.html");

            }
        } catch (UxException e) {
            WebUtils.processError(e, resp);

        } catch (Exception e) {
            WebUtils.processError(new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, e), resp);

        }
    }

    protected void checkAdminLoggedIn(HashMap<String, String> in, HttpServletRequest req, HttpServletResponse resp) {
        String ctx = in.get("context");
        boolean admin = openAuthenticator().isMember((ctx!=null ? ctx : req.getSession().getId()));
        JsonObject o = new JsonObject();
        if (admin){
            o.addProperty("admin", 0);

        } else {
            o.addProperty("admin", -1);

        }
        WebUtils.respond(resp, o);

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        JsonObject o = new JsonObject();
        try {
            StandardAuthenticator auth = openAuthenticator();
            String id = req.getSession().getId();
            HashMap<String, String> in = WebUtils.buildParams(req, resp);
            PassStore ps = auth.getPassStore(req, in.get("context"));
            if (ps.getUsername()!=null && ps.getUsername().equals("reset")){
                o.addProperty("password", ps.getUsername());

            } else if (auth.isPrimaryAdministrator(id)){
                o.addProperty("auth", 1);

            } else if (auth.isAdministrator(id)){
                o.addProperty("auth", 0);

            } else {
                o.addProperty("auth", -1);

            }
            WebUtils.respond(resp, o);

        } catch (AuthenticationException e) {
            o.addProperty("admin", true);
            WebUtils.respond(resp, o);

        } catch (Exception e) {
            WebUtils.processError(new UxException(ErrorMessages.FAILED_TO_AUTHORIZE, e), resp);

        }
    }

    protected abstract StandardAuthenticator openAuthenticator();

}
