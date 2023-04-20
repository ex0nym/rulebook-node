package io.exonym.x0basic;

import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.NetworkMapNodeOverview;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

public class StartNode implements Closeable {
    
    private static final Logger logger = LogManager.getLogger(StartNode.class);
    private KeyManager keyManager;
    private ConflictResolver conflictResolver;
    private ExoMatrixWriter writer;
    private Acker acker;
    private JoinIn joinIn0;
    private JoinIn joinIn1;
    private ViolationIn violationIn;
    private BlobManager blobManager;
    private UpdateQueue queue;
    private Resender resender;
    private UdpListener listener;

    private X0Properties props;

    public StartNode() {
        logger.info("UDP Listener Waiting for XNode to Start");
        try {
            logger.info("Checking Configuration");
            waitForNode();

        } catch (Exception e) {
            logger.error("Critical Error - It's likely that environment variables weren't set properly", e);

        }
    }

    private void ready() throws IOException {
        try {
            keyManager = new KeyManager();
            conflictResolver = new ConflictResolver();

            writer = new ExoMatrixWriter(conflictResolver.getPipe());
            acker = new Acker(keyManager, this.props.getAdvocateUID());

            joinIn0 = new JoinIn(0,
                    acker.getPipe(), writer.getPipe(), keyManager);
            joinIn1 = new JoinIn(1,
                    acker.getPipe(), writer.getPipe(), keyManager);

            ArrayList<ArrayBlockingQueue<Msg>> pipesToJoin = new ArrayList<>();
            pipesToJoin.add(joinIn0.getPipe());
            pipesToJoin.add(joinIn1.getPipe());

            violationIn = new ViolationIn(acker.getPipe(), keyManager);
            blobManager = new BlobManager();
            queue = new UpdateQueue(pipesToJoin,
                    violationIn.getPipe(),
                    blobManager.getPipe(),
                    acker.getPipe());

            resender = new Resender();

            listener = new UdpListener(queue.getPipe());

        } catch (Exception e) {
            logger.error("Fatal Error", e);
            System.exit(-9);

        }
    }

    private void waitForNode() {
        boolean started = false;
        try {
            synchronized (this){
                logger.debug("Configuration seems valid; waiting 10s for other resources to start.");
                this.wait(5000);

            }
            props = X0Properties.getInstance();
            CouchRepository<NetworkMapNodeOverview> repo = CouchDbHelper.repoNetworkMapSourceData();
            QueryBasic q = QueryBasic.selectType(NetworkMapNodeOverview.TYPE_NETWORK_MAP_NODE_OVERVIEW);

            while(started==false) {
                synchronized (this) {
                    try {
                        NetworkMapNodeOverview status = repo.read(q).get(0);
                        ArrayList<String> acceptableStates = new ArrayList<>();
                        acceptableStates.add(NetworkMapNodeOverview.GLOBAL_STATE_THIS_NODE_LISTED);
                        acceptableStates.add(NetworkMapNodeOverview.GLOBAL_STATE_DEFINED_SOURCE_LISTED__THIS_HOST_UNLISTED);

                        if (!acceptableStates.contains(status.getCurrentGlobalState())){
                            logger.info("Node not yet initialized: Status " + status.getCurrentGlobalState() +
                                    "\n\tWill activate when /whois currentGlobalState is: "
                                    + acceptableStates);

                            this.wait(30000);

                        } else {
                            started = true;
                            props.init(status.getAdvocateUID());

                        }
                    } catch (NoDocumentException e) {
                        logger.info("Node coming online - waiting 30s");
                        this.wait(30000);

                    } catch (Exception e) {
                        logger.debug("Error - waiting 60s", e);
                        this.wait(60000);

                    }
                }
            }
            ready();

        } catch (ConnectException e) {
            logger.info("Stopping Multicast Features due to unavailable databases.  " +
                    "Configure Node First.  If the node is configured restart after the Node is stable.");

        } catch (Exception e) {
            logger.info("Error", e);

        }
    }

    public static void main(String[] args) throws IOException {
        logger.info("UDP Listener Starting" + DateTime.now());
        StartNode start = new StartNode();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logger.debug("Shutdown UDP Listener");
                start.close();

            } catch (IOException e) {
                logger.error("Error during shut down... ", e);

            }
        }));

    }

    @Override
    public void close() throws IOException {
        try {
            logger.debug("Attempting to close X0Multi");
            if (listener!=null) {
                this.listener.close();
            }  if (resender!=null){
                this.resender.close();
            } if (joinIn0!=null){
                this.joinIn0.close();
            } if (joinIn1!=null){
                this.joinIn1.close();
            } if (acker!=null){
                this.acker.close();
            } if (writer!=null){
                this.writer.close();
            } if (keyManager!=null){
                this.keyManager.close();
            } if (this.queue!=null){
                this.queue.close();
            }
        } catch (Exception e) {
            logger.error("Error", e);

        }
    }
}
