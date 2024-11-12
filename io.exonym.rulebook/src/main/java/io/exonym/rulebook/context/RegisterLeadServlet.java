package io.exonym.rulebook.context;

import io.exonym.actor.actions.WiderTrustNetworkManagement;
import io.exonym.lite.connect.WebUtils;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.standard.WhiteList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

@WebServlet("/registerLead")
public class RegisterLeadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write("Error: GET not implemented : use POST");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if (RulebookNodeProperties.instance().isAllowLeadPublication()){
                HashMap<String, String> in = WebUtils.buildParams(req, resp);
                if (in.containsKey("test")){
                    addToLeadList(in, resp);

                } else {
                    verifyProof(in, resp);

                }
            } else {
                throw new UxException(ErrorMessages.INCORRECT_PARAMETERS,
                        "This node does not accept open publications to their test source list");

            }
        } catch (UxException e) {
            WebUtils.processError(e, resp);

        } catch (Exception e) {
            WebUtils.processError(new UxException(ErrorMessages.SERVER_SIDE_PROGRAMMING_ERROR, e), resp);

        }
    }

    private void addToLeadList(HashMap<String, String> in, HttpServletResponse resp) throws Exception {
        try {
            String target = in.get("sourceUrl");
            if (WhiteList.isLeadUrl(target)){
                WiderTrustNetworkManagement wtn = new WiderTrustNetworkManagement();
                wtn.openWiderTrustNetwork();
                wtn.addLead(URI.create(target), !in.containsKey("test"));
                wtn.publish();
                WebUtils.success(resp);

            } else {
                throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, "Unexpected URL", target);

            }
        } catch (FileNotFoundException e) {
            throw new UxException(ErrorMessages.RULEBOOK_FAILED_TO_VERIFY_OR_NOT_FOUND, e);

        }
    }

    private void verifyProof(HashMap<String, String> in, HttpServletResponse resp) throws UxException {
        throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE);
     // TODO verify proof of honesty under trustworthy-leads rulebook
     // if currently honest, add them.

    }
}
