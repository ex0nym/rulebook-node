package io.exonym.lite.authenticators;

import com.google.gson.JsonObject;
import io.exonym.lite.connect.WebUtils;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.IUser;
import io.exonym.lite.standard.CryptoUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

public abstract class AdminServlet extends HttpServlet {


    private static final Logger logger = LogManager.getLogger(AdminServlet.class);
    public static final String CMD_ADD_USER = "add-user";
    public static final String CMD_DELETE_USER = "delete-user";
    public static final String CMD_ADD_PRIVILEGE = "add-privilege";
    public static final String CMD_REMOVE_PRIVILEGE = "remove-privilege";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            HashMap<String, String> in = WebUtils.buildParams(req, resp);
            String cmd = in.get("cmd");
            String ctx = in.get("context");
            String session = req.getSession().getId();
            String id = (ctx==null || ctx.equals("null") ? session : ctx);
            logger.debug("context=" + id);
            StandardAuthenticator auth = openAuthenticator();
            if (auth.isAdministrator(id)){
                if (cmd.equals(CMD_ADD_USER)) {
                    addUser(in, resp);

                } else if (cmd.equals(CMD_DELETE_USER)) {
                    deleteUser(in, resp);

                } else if (cmd.equals(CMD_ADD_PRIVILEGE)) {
                    addPrivilege(in, resp);

                } else if (cmd.equals(CMD_REMOVE_PRIVILEGE)) {
                    removePrivilege(in, resp);

                } else {
                    serverSpecificCommands(cmd, in, req, resp);

                }
            } else {
                throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE);

            }
        } catch (UxException e) {
            WebUtils.processError(e, resp);

        } catch (Exception e) {
            WebUtils.processError(new UxException(ErrorMessages.SERVER_SIDE_PROGRAMMING_ERROR, e), resp);

        }
    }

    /**
     *
     * @param cmd
     * @param in
     * @param req
     * @param resp
     * @throws UxException if the command isn't implemented
     */
    protected abstract void serverSpecificCommands(String cmd, HashMap<String, String> in, HttpServletRequest req, HttpServletResponse resp) throws UxException;

    protected abstract StandardAuthenticator openAuthenticator();

    private void addUser(HashMap<String, String> in, HttpServletResponse resp) throws UxException {
        String username = in.get("username");
        String priv = in.get("privileges");
        String password = CryptoUtils.tempPassword();


        if (username!=null){
            IUser user = new IUser();
            user.setUsername(username);
            user.setPrivileges(priv);
            user.setV(CryptoUtils.computeSha256HashAsHex(
                    CryptoUtils.computeSha256HashAsHex(password)));
            user.setRequiresPassChange(true);
            user.setType(IUser.I_USER_MEMBER);
            verifyUserDoesNotExistAndAdd(user);
            JsonObject o = new JsonObject();
            o.addProperty("username", username);
            o.addProperty("password", password);
            WebUtils.respond(resp, o);

        } else {
            throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, "username", "privileges");

        }
    }

    private void deleteUser(HashMap<String, String> in, HttpServletResponse resp) throws UxException {
        try {
            IUser target = openUser(in.get("username"));
            if (target.getType().equals(IUser.I_USER_MEMBER)){
                target.setInactive(true);
                updateUser(target);
                JsonObject o = new JsonObject();
                o.addProperty("username", target.getUsername());
                o.addProperty("status", "inactive");
                WebUtils.respond(resp, o);

            } else {
                throw new UxException(ErrorMessages.INSUFFICIENT_PRIVILEGES, "Type Error - Require a Member Username");

            }
        } catch (UxException e) {
            throw e;

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    /**
     * Update the user in the db
     * @param target
     */
    protected abstract void updateUser(IUser target) throws UxException;

    /**
     * If the username doesn't already exist, create the record.
     * @param user
     * @throws UxException
     */
    protected abstract void verifyUserDoesNotExistAndAdd(IUser user) throws UxException;

    /**
     * Open the user with the username
     *
     * @param username
     * @return
     * @throws UxException
     */
    protected abstract IUser openUser(String username) throws UxException;

    /**
     * Manage privileges
     * @param in
     * @param resp
     */
    protected abstract void addPrivilege(HashMap<String, String> in, HttpServletResponse resp) throws UxException ;

    /**
     * Manage privileges
     * @param in
     * @param resp
     */
    protected abstract void removePrivilege(HashMap<String, String> in, HttpServletResponse resp)throws UxException ;
}
