package io.exonym.lite.authenticators;

import com.google.gson.JsonObject;
import io.exonym.lite.connect.WebUtils;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.IApiKey;
import io.exonym.lite.pojo.IUser;
import io.exonym.lite.pojo.TypeNames;
import io.exonym.lite.standard.CryptoUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public abstract class SysAdminServlet extends HttpServlet {

    public static final String CMD_ADD_API_KEY = "add-api-key";
    public static final String CMD_REMOVE_API_KEY = "delete-api-key";
    public static final String CMD_ADD_ADMIN = "add-admin";
    public static final String CMD_REMOVE_ADMIN = "delete-admin";
    public static final String CMD_GET_ALL_ADMINS = "get-all-admins";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            HashMap<String, String> in = WebUtils.buildParams(req, resp);
            String ctx = in.get("context");
            String id = (ctx!=null && ctx.length() > 0 ? ctx : req.getSession().getId());
            StandardAuthenticator auth = openAuthenticator();
            if (auth.isPrimaryAdministrator(id)) {
                String cmd = in.get("cmd");
                if (cmd.equals(CMD_ADD_ADMIN)) {
                    addAdmin(in, resp);

                } else if (cmd.equals(CMD_REMOVE_ADMIN)) {
                    removeAdmin(in, resp);

                } else if (cmd.equals(CMD_ADD_API_KEY)) {
                    addApiKey(resp);

                } else if (cmd.equals(CMD_REMOVE_API_KEY)) {
                    deleteApiKey(in, resp);

                } else if (cmd.equals(CMD_GET_ALL_ADMINS)) {
                    administratorList();

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

    protected abstract void serverSpecificCommands(String cmd, HashMap<String, String> in,
                                                   HttpServletRequest req, HttpServletResponse resp) throws UxException;

    protected void administratorList(){

    }

    protected abstract void deleteApiKey(HashMap<String, String> in, HttpServletResponse resp) throws UxException;

    protected abstract void removeAdmin(HashMap<String, String> in, HttpServletResponse resp) throws UxException;

    protected abstract StandardAuthenticator openAuthenticator();

    private void addAdmin(HashMap<String, String> in, HttpServletResponse resp) throws UxException {
        String username = in.get("username");
        if (username == null) {
            throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, "username", "privileges");

        }
        String privileges = in.get("privileges");
        String pwd = CryptoUtils.tempPassword();
        IUser user = new IUser();
        user.setType(IUser.I_USER_ADMIN);
        user.setRequiresPassChange(true);
        user.setV(
                CryptoUtils.computeSha256HashAsHex(
                        CryptoUtils.computeSha256HashAsHex(pwd)
                )
        );
        user.setPrivileges(privileges);
        user.setUsername(username);
        addUser(user);
        JsonObject o = new JsonObject();
        o.addProperty("username", username);
        o.addProperty("password", pwd);
        WebUtils.respond(resp, o);

    }

    protected abstract void addUser(IUser user) throws UxException;

    private void addApiKey(HttpServletResponse resp) throws UxException {
        UUID uuid = java.util.UUID.randomUUID();
        String pwd = CryptoUtils.computeSha256HashAsHex(
                CryptoUtils.generateCode(20));
        IApiKey apiKey = new IApiKey();
        apiKey.setType(IUser.I_USER_API_KEY);
        apiKey.setUuid(uuid.toString());
        apiKey.setKey(CryptoUtils.computeSha256HashAsHex(pwd));
        JsonObject o = new JsonObject();
        o.addProperty("kid", apiKey.getUuid());
        o.addProperty("key", pwd);
        createApiKey(apiKey);
        WebUtils.respond(resp, o);

    }

    protected abstract void createApiKey(IApiKey apiKey) throws UxException;


}
