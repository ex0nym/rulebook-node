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
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

@WebServlet("/registerSource")
public class RegisterSourceServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write("use post");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if (RulebookNodeProperties.instance().isOpenSourcePublication()){
                HashMap<String, String> in = WebUtils.buildParams(req, resp);
                if (in.containsKey("test")){
                    addToSourcesList(in, resp);

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
            throw new RuntimeException(e);

        }
    }

    private void addToSourcesList(HashMap<String, String> in, HttpServletResponse resp) throws Exception {
        String target = in.get("sourceUrl");
        if (WhiteList.isSourceUrl(target)){
            WiderTrustNetworkManagement wtn = new WiderTrustNetworkManagement();
            wtn.openWiderTrustNetwork();
            wtn.addSource(URI.create(target), !in.containsKey("test"));
            wtn.publish();
            WebUtils.success(resp);

        } else {
            throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, "Unexpected URL", target);

        }
    }

    private void verifyProof(HashMap<String, String> in, HttpServletResponse resp) throws UxException {
        throw new UxException(ErrorMessages.FAILED_TO_AUTHORIZE);
        // // verify proof of honesty under sources rulebook
        // // if corrently honest, add them.

    }
}
