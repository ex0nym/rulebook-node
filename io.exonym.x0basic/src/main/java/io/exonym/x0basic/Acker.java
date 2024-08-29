package io.exonym.x0basic;

import com.cloudant.client.org.lightcouch.DocumentConflictException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.lite.connect.BroadcasterBasic;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.couchdb.QueryStandard;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.parallel.ModelCommandProcessor;
import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.*;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.PassStore;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ArrayBlockingQueue;

public class Acker extends ModelCommandProcessor  {
    
    private static final Logger logger = LogManager.getLogger(Acker.class);

    private final DatagramChannel channel;

    private final CouchRepository<BroadcastInProgress> ackInRepo;
    private final CouchRepository<NetworkMapItem> networkMap;
    private final URI hostUuid;

    private final QueryStandard networkMapQuery = new QueryStandard();

    private final QueryStandard ackInQuery = new QueryStandard();

    private final AsymStoreKey ownKey = AsymStoreKey.blank();
    private final KeyManager keyManager;

    protected Acker(KeyManager keyManager, URI thisAdvocateUID) throws Exception {
        super(Constants.FLUX_CAPACITY, "Acker", 60000);
//        this.broadcaster = new Broadcaster();
        this.keyManager=keyManager;
        this.channel = DatagramChannel.open();
        this.hostUuid = thisAdvocateUID;

        ackInRepo = CouchDbHelper.repoBroadcasts();
        networkMap = CouchDbHelper.repoNetworkMapItem();

        CouchRepository<XKey> repo = CouchDbHelper.repoRootKey();
        QueryBasic q = new QueryBasic();
        q.getSelector().put(XKey.FIELD_TYPE, "host");
        XKey key = repo.read(q).get(0);
        X0Properties props = X0Properties.getInstance();
        PassStore store = new PassStore(props.getNodeRoot(), false);

        ownKey.assembleKey(key.getPublicKey(), key.getPrivateKey(), store.getDecipher());

        networkMapQuery.addFieldSelector(NetworkMapItem.FIELD_RULEBOOK_NODE_URL);
//        ackInQuery.addFieldSelector("_id");
//        ackInQuery.addFieldSelector("_rev");

    }


    private void ackIn(ExoNotify notify) {
        try {
            authenticate(notify);
            ackInQuery.addCriteria(BroadcastInProgress.FIELD_CONTEXT, notify.getT());
            ackInQuery.addCriteria(BroadcastInProgress.FIELD_HOST_UUID, notify.getNodeUID().toString());
            logger.debug("Trying to Remove " + notify );
            BroadcastInProgress broadcast = ackInRepo.read(ackInQuery).get(0);
            logger.debug("Trying to Remove " + broadcast.get_id() + " " + broadcast.get_rev());
            ackInRepo.delete(broadcast);
            ackInRepo.ensureFullCommit();

        } catch (NoDocumentException e) {
            logger.debug("Received a duplicate ack " + notify, e);

        } catch (Exception e) {
            logger.error("Ack in failed " + e.getMessage());
            logger.debug("Failure", e);

        }
    }

    private void authenticate(ExoNotify notify) throws Exception {
        URI advocateUID = notify.getNodeUID();
        logger.debug("Getting Key for Advocate " + advocateUID);
        AsymStoreKey key = this.keyManager.getKey(advocateUID);
        Authenticator.authenticateAckAndSource(notify, key);

    }

    private void ackOut(ExoNotify notify) {
        try {
            logger.debug("Preparing to Ack: " + notify);
            ExoNotify ack = new ExoNotify();
            ack.setType(ExoNotify.TYPE_ACK);
            ack.setT(notify.getT());
            ack.setNodeUID(this.hostUuid);
            byte[] signed = ExoNotify.signatureOnAckAndOrigin(ack);
            byte[] sig = ownKey.sign(signed);
            ack.setSigB64(Base64.encodeBase64String(sig));
            networkMapQuery.addCriteria(NetworkMapItem.FIELD_NODE_UID, notify.getNodeUID().toString());
            NetworkMapItem target = networkMap.read(networkMapQuery).get(0);
            InetSocketAddress t0 = buildInetAddress(target.getBroadcastAddress());
            send(ack, t0);

        } catch (Exception e) {
            logger.error("Failed to ack", e);

        }
    }

    private InetSocketAddress buildInetAddress(URI multicastUrl) {
        String[] udp = multicastUrl.toString().split(":");
        int port = (udp.length==2 ? Integer.parseInt(udp[1]) : 9090);
        return new InetSocketAddress(udp[0], port);

    }

    private void send(ExoNotify ack, InetSocketAddress target) throws IOException {
        logger.debug("Sending Ack " + ack);
        byte[] toSend = BroadcasterBasic.assembleBroadcastBytes(ack);
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.put(toSend);
        ((ByteBuffer)bb).flip();
        channel.send(bb, target);

    }

    @Override
    protected void receivedMessage(Msg msg) {
        try {
            if (msg instanceof ExoNotify){
                ExoNotify notify = (ExoNotify) msg;
                String type = notify.getType();
                if (type.equals(ExoNotify.TYPE_ACK)){
                    logger.debug("AckIn " + notify);
                    ackIn(notify);

                } else if (type.equals(ExoNotify.TYPE_LEAD)){
                    sourceUpdate(notify);
                    ackOut(notify);

                } else {
                    ackOut(notify);

                }
            }
        } catch (Exception e) {
            logger.error("Error", e);

        }
    }

    private void sourceUpdate(ExoNotify notify) throws Exception {
        authenticate(notify);
        boolean isSybil = Rulebook.isSybil(notify.getNodeUID());
        try {
            CouchRepository<NetworkMapNodeOverview> repo = CouchDbHelper.repoNetworkMapSourceData();
            QueryBasic q = QueryBasic.selectType(NetworkMapNodeOverview.TYPE_NETWORK_MAP_NODE_OVERVIEW);
            boolean conflicted = true;
            while (conflicted){
                int i=0;
                try {
                    NetworkMapNodeOverview nm = repo.read(q).get(0);
                    nm.setSybilRequiresUpdate(isSybil);
                    nm.setLeadRequiresUpdate(!isSybil);
                    repo.update(nm);
                    conflicted=false; 

                } catch (DocumentConflictException e) {
                    i++;
                    if (i>5){
                        throw new HubException("Unable to update source map");

                    }
                }
            }
        } catch (NoDocumentException e) {
            logger.error("Network Map Undefined");

        } catch (Exception e) {
            throw e;

        }
    }

    @Override
    protected void periodOfInactivityProcesses() { }


    @Override
    protected ArrayBlockingQueue<Msg> getPipe() {
        return super.getPipe();
    }

    @Override
    protected void close() throws Exception {
        this.channel.close();
        super.close();
    }
}
