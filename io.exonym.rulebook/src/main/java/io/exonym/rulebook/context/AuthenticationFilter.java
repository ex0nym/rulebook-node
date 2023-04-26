package io.exonym.rulebook.context;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.actor.XContainerExternal;
import io.exonym.actor.actions.PkiExternalResourceContainer;
import io.exonym.actor.actions.XContainerJSON;
import io.exonym.lite.connect.WebUtils;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.IUser;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.time.DateHelper;
import io.exonym.rulebook.schema.CacheNodeContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;

@WebFilter(filterName = "authFilter")
public class AuthenticationFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(AuthenticationFilter.class);
    private RulebookNodeProperties props = RulebookNodeProperties.instance();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            testFileWrite();

            // updOpen();
            // testSecureRandom();
            setup();
            NetworkMapWeb map = new NetworkMapWeb();
            map.spawn();
            PkiExternalResourceContainer.getInstance()
                    .setNetworkMapAndCache(map, CacheNodeContainer.getInstance());
            XContainerJSON.openSystemParameters();
            XContainerExternal.openSystemParameters();

            logger.info("NetworkMap should have initialized");

            // Removed due to super-seeded by the UDP protocols.
            // new SyncNetwork();

        } catch (Exception e) {
            logger.error("Failed to start network synchronization", e);

        }
        ServletContext c = filterConfig.getServletContext();
        Enumeration<String> names = filterConfig.getInitParameterNames();

        while (names.hasMoreElements()) {
            logger.debug("Init Parameter Names " + names.nextElement());

        }
        logger.debug("Node Started --" + c.getServletContextName() + "--");

    }

    private void testFileWrite() throws IOException {
        Path writeLocation = Path.of("/var", "www", "html", "replication");
        Files.createDirectories(writeLocation);
        Path filePath = writeLocation.resolve("test.json");
        Files.write(filePath,
                ("{'test':'If you can read this, the Rulebook Node successfully wrote to the replication directory'," +
                        "'time':'" + DateHelper.currentIsoUtcDateTime() + "'}")
                        .getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE);
        logger.info("Written file" + filePath);

    }

    private void testSecureRandom() {
        logger.debug("Secure Random utils test");
        logger.debug(CryptoUtils.generateNonce(6));
        logger.debug(CryptoUtils.generateNonce(12));
        logger.debug(CryptoUtils.generateNonce(24));
        logger.debug("6, 12, 24 - end");
    }

    private void updOpen() throws Exception {
        DatagramChannel channel = DatagramChannel.open();
        channel.socket().bind(new InetSocketAddress(9999));

    }

    private void setup() throws Exception {
        CloudantClient client = CouchDbClient.instance();
        Database db = client.database(CouchDbHelper.getDbUsers(), true);
        CouchRepository<IUser> repo = new CouchRepository<>(db, IUser.class);

        String url = RulebookNodeProperties.instance().getRulebookNodeURL();
        logger.debug("RULEBOOK_NODE_URL " + url);

        try {
            QueryBasic q = QueryBasic.selectType(IUser.I_USER_PRIMARY_ADMIN);
            repo.read(q);

        } catch (NoDocumentException e) {
            CouchDbHelper.initDb();
            setupPrimaryAdmin(repo);

        } catch (CouchDbException e) {
            logger.error("Please restart the webserver - this error is caused by establishing the database: ", e);

        } catch (Exception e) {
            logger.error(">>>>>>>>>>>>>>>>>> Catastrophic failure", e);

        }
    }


    private void setupPrimaryAdmin(CouchRepository<IUser> repo) throws Exception {

        String password = CryptoUtils.tempPassword();
        IUser user = new IUser();
        user.setType(IUser.I_USER_PRIMARY_ADMIN);
        user.setV(CryptoUtils.computeSha256HashAsHex(
                CryptoUtils.computeSha256HashAsHex(password)));
        String username = RulebookNodeProperties.instance().getPrimaryAdminUsername();
        user.setUsername(username);
        user.setRequiresPassChange(true);
        repo.create(user);
        logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        logger.info(">");
        logger.info("> Primary Administrator Created with username(" + username + ")");
        logger.info(">");
        logger.info("> Password:  " + password);
        logger.info(">");
        logger.info("> You will be prompted to change your password when you log in.");
        logger.info(">");
        logger.info(">");
        logger.info("");
        logger.info("");
        logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        IAuthenticator.getInstance().setPrimaryAdminSetup(true);

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        response.setContentType("text/json; charset=utf8");
        HttpServletRequest req = (HttpServletRequest) request;
//		kill((Enumeration<String>) req.getHeaderNames(), req, (HttpServletResponse) response);

        String auth = props.getAuthorizedDomain();
        String node = props.getRulebookNodeURL();

        logger.debug("Cors Filter Authorised Domain=" + auth);

        if (auth != null) {
            HttpServletResponse res = (HttpServletResponse) response;

            String origin = req.getHeader("origin");
            String url = req.getRequestURL().toString();

            // This isn't needed for dev or prod as far as I can tell ... waiting to delete.
//			res.setHeader("Access-Control-Allow-Origin", auth);
//			res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
//			res.setHeader("Access-Control-Allow-Headers", "User-Agent, Keep-Alive, Content-Type");

            if (url.endsWith("/ssi") || url.endsWith("/ssi/")) {
                logger.debug("ssi request origin=" + origin + " url=" + url);
                chain.doFilter(request, response);

            } else if (origin == null || origin.equals("null")) {
                String referer = req.getHeader("referer");
                logger.debug("no origin referrer=" + referer + " url=" + url);
                if (referer != null && referer.startsWith(auth)) {
                    chain.doFilter(request, response);

                } else if (referer != null && referer.startsWith(node)) {
                    chain.doFilter(request, response);

                } else {
                    logger.warn("KILLING no origin referrer=" + referer + " node=" + node);
                    kill(req.getHeaderNames(), req, res);

                }
            } else if (origin.equals("exonym-node")) {
                logger.debug("origin =" + origin + " url=" + url);
                if (url.endsWith("/paywall")) {
                    chain.doFilter(request, response);

                } else if (url.endsWith("/issuer")) {
                    chain.doFilter(request, response);

                } else {
                    logger.warn("KILLING origin=" + origin + " url=" + url);
                    kill(req.getHeaderNames(), req, res);

                }
            } else if (origin.equals("spectra-app")) {
                logger.debug("origin =" + origin + " url=" + url);
                if (url.endsWith("/issuer")) {
                    chain.doFilter(request, response);

                } else if (url.endsWith("/ssi")) {
                    chain.doFilter(request, response);

                } else {
                    logger.warn("KILLING origin=" + origin + " url=" + url);
                    kill(req.getHeaderNames(), req, res);

                }
            } else if (origin.startsWith(auth)) {
                logger.debug("correct origin=" + origin + " url=" + url);
                chain.doFilter(request, response);

            } else if (origin.startsWith(node)) {
                logger.debug("correct origin=" + origin + " url=" + url + " node=" + node);
                chain.doFilter(request, response);

            } else {
                logger.warn("KILLING origin=" + origin + " url=" + url);
                kill(req.getHeaderNames(), req, res);

            }
        } else {
            logger.debug("Filter called for uri " + req.getRequestURI());
            chain.doFilter(request, response);

        }
    }

    private void kill(Enumeration<String> headerNames, HttpServletRequest req, HttpServletResponse response) {
        logger.debug("Trying to output headers");
        while (headerNames.hasMoreElements()) {
            logger.debug(headerNames.nextElement());

        }
        WebUtils.processError(new UxException(ErrorMessages.CORS_CUSTOM), response);

//		response.reset();

    }

    @Override
    public void destroy() {
        logger.info("Cleaning up API Context");
//		try {
//			ChallengeCleanup.getInstance().close();
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
    }
}
