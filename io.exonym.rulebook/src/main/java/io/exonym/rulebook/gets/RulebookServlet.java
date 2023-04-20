package io.exonym.rulebook.gets;

import com.google.gson.JsonObject;
import io.exonym.lite.connect.WebUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/rulebook")
public class RulebookServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(RulebookServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            resp.sendRedirect("/");

        } catch (Exception e) {
            logger.error("Error", e);
            JsonObject o = new JsonObject();
            o.addProperty("error", "Failed to initialize - Node Configuration Error - Check Server Logs");
            WebUtils.respond(resp, o);

        }
    }

}
