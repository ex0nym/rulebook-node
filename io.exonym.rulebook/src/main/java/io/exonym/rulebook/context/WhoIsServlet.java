package io.exonym.rulebook.context;

import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.pojo.NetworkMapNodeOverview;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/whois")
public class WhoIsServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(WhoIsServlet.class);
    private final CouchRepository<NetworkMapNodeOverview> repo;
    private final QueryBasic query = QueryBasic.selectType(NetworkMapNodeOverview.TYPE_NETWORK_MAP_NODE_OVERVIEW);
    private final Gson gson;

    public WhoIsServlet() {
        logger.debug("New Instance - WhoIsServlet");
        CouchRepository<NetworkMapNodeOverview> tmp;
        gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            tmp = CouchDbHelper.repoNetworkMapLeadOverview();

        } catch (Exception e) {
            logger.error("WhoIsServlet Failure", e);
            tmp=null;

        }
        this.repo = tmp;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            NetworkMapNodeOverview r = repo.read(query).get(0);
            r.set_id(null); r.set_rev(null); r.setType(null);
//             ArrayList<URI> sources = new ArrayList<>();
//            for (URI s : r.getSources().keySet()){
//                sources.add(s);
//
//            }
//            r.setListeningToSources(sources);
            r.setLeads(null);
            resp.getWriter().write(gson.toJson(r));

        } catch (NoDocumentException e) {
            resp.getWriter().write("{NETWORK_MAP_NOT_FOUND}");

        } catch (Exception e) {
            resp.getWriter().write("{UNEXPECTED_NETWORK_MAP_ERROR}");

        }
    }
}
