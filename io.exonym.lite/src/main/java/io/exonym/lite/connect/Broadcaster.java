package io.exonym.lite.connect;

import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.google.gson.Gson;
import io.exonym.lite.couchdb.ProtectedCouchRepository;
import io.exonym.lite.couchdb.QueryStandard;
import io.exonym.lite.couchdb.ResultPager;
import io.exonym.lite.pojo.BroadcastInProgress;
import io.exonym.lite.pojo.ExoNotify;
import io.exonym.lite.pojo.NetworkMapItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public abstract class Broadcaster extends ResultPager<NetworkMapItem> implements AutoCloseable{

    private static final Logger logger = LogManager.getLogger(Broadcaster.class);
    private final DatagramChannel channel;
    private byte[] bytesToBroadcast;
    private final ExoNotify notify;

    public Broadcaster(ExoNotify notify,
                       ProtectedCouchRepository networkMapRepo) throws Exception {

        super(new QueryStandard(), 100, networkMapRepo);

        this.channel = DatagramChannel.open();
        this.notify = notify;
        Gson gson = new Gson();
        String json = gson.toJson(notify);
        bytesToBroadcast = json.getBytes(StandardCharsets.UTF_8);
        int len = bytesToBroadcast.length;
        json = len + json;
        logger.debug("Setup Broadcaster with message:\n\t\t" + json);
        bytesToBroadcast = json.getBytes(StandardCharsets.UTF_8);

        ((QueryStandard)(this.query))
                .addCriteria("type", NetworkMapItem.TYPE);
        ((QueryStandard)(this.query))
                .addFieldSelector(NetworkMapItem.FIELD_BROADCAST_URL);
        ((QueryStandard)(this.query))
                .addFieldSelector(NetworkMapItem.FIELD_NODE_UID);

    }

    private void send(InetSocketAddress address)  {
        try {
            logger.debug("Sending " + address);
            ByteBuffer bb = ByteBuffer.allocate(1024);
            bb.put(bytesToBroadcast);
            ((Buffer) bb).flip();
            channel.send(bb, address);

        } catch (IOException e) {
            logger.warn("Error when sending to: " + address);
            logger.debug("Error", e);

        }
    }

    @Override
    protected void processResultSet(List<NetworkMapItem> nodes) {
        ArrayList<BroadcastInProgress> broadcasts = new ArrayList<>();

        try {
            for (NetworkMapItem target : nodes){
                URI hostUdp = target.getBroadcastAddress();
                String[] parts = hostUdp.toString().split(":");
                int port = (parts.length==2 ? Integer.parseInt(parts[1]) : 9090);
                BroadcastInProgress bip = new BroadcastInProgress();
                bip.setContext(notify.getT());
                bip.setAddress(parts[0]);
                bip.setPort(port);
                bip.setAdvocateUID(target.getNodeUID());
                bip.setNotify(notify);
                broadcasts.add(bip);

            }
            addToUdpOutDb(broadcasts);

            for (BroadcastInProgress bip : broadcasts){
                InetSocketAddress address = new InetSocketAddress(bip.getAddress(), bip.getPort());
                send(address);


            }
        } catch (Exception e) {
            logger.debug("Exception caught in broadcast process " + e.getMessage());

        }
    }

    protected abstract void addToUdpOutDb(ArrayList<BroadcastInProgress> broadcasts) throws Exception ;

    @Override
    public void execute() throws NoDocumentException {
        super.execute();
    }

    @Override
    public void close() throws Exception {
        this.channel.close();

    }
}
