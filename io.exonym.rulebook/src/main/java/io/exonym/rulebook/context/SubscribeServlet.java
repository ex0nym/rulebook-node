package io.exonym.rulebook.context;

import com.ibm.zurich.idmx.jaxb.JaxbHelperClass;
import eu.abc4trust.xml.IssuanceMessage;
import eu.abc4trust.xml.IssuanceMessageAndBoolean;
import eu.abc4trust.xml.IssuanceToken;
import io.exonym.lite.connect.WebUtils;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.utils.ExtractObject;
import io.exonym.utils.storage.XContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@WebServlet("/subscribe/*")
public class SubscribeServlet extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(SubscribeServlet.class);


    private final ConcurrentHashMap<String, JoinProcessor> joinRequests = new ConcurrentHashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            JoinProcessor join = new JoinProcessor();
            String path = req.getPathInfo();
            String response = null;
            if (path == null) {
                response = join.joinChallenge(false);

            } else {
                String[] request = path.split("/");
                if (request.length > 1) {
                    if (request[1].equals("qr")) {
                        response = join.joinChallenge(true);

                    } else {
                        logger.debug("Ignoring request " + request);

                    }
                }
                if (response == null) {
                    throw new UxException(ErrorMessages.URL_INVALID, path, "" + request.length);

                }
            }
            joinRequests.put(join.getHashOfNonce(), join);
            WebUtils.respond(resp, response);

        } catch (UxException e) {
            WebUtils.processError(e, resp);

        } catch (Exception e) {
            WebUtils.processError(new UxException(ErrorMessages.URL_INVALID, e), resp);

        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String xml = WebUtils.buildParamsAsString(req);
            IssuanceMessage message = (IssuanceMessage) JaxbHelperClass.deserialize(xml).getValue();

            IssuanceToken issuanceToken = ExtractObject.extract(message.getContent(), IssuanceToken.class);
            assert issuanceToken != null;
            byte[] nonce = issuanceToken.getIssuanceTokenDescription()
                    .getPresentationTokenDescription().getMessage().getNonce();
            String hashOfNonce = CryptoUtils.computeSha256HashAsHex(nonce);
            JoinProcessor join = joinRequests.get(hashOfNonce);
            if (join != null) {
                IssuanceMessageAndBoolean imab = join.join(message, issuanceToken);
                String response = XContainer.convertObjectToXml(imab);
                resp.getWriter().write(response);

            } else {
                throw new UxException(ErrorMessages.UNEXPECTED_TOKEN_FOR_THIS_NODE);

            }
        } catch (UxException e) {
            WebUtils.processError(e, resp);

        } catch (Exception e) {
            WebUtils.processError(
                    new UxException(ErrorMessages.SERVER_SIDE_PROGRAMMING_ERROR, e),
                    resp);

        }
    }


}
