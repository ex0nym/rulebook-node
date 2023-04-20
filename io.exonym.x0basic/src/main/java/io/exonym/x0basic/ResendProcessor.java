package io.exonym.x0basic;

import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.lite.connect.BroadcasterBasic;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.couchdb.ResultPager;
import io.exonym.lite.pojo.BroadcastInProgress;
import io.exonym.lite.pojo.ExoNotify;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;

public class ResendProcessor extends ResultPager<BroadcastInProgress> implements AutoCloseable{

    private static final Logger logger = LogManager.getLogger(ResendProcessor.class);
    private final DatagramChannel channel;

    public ResendProcessor() throws Exception {
        super(QueryBasic.selectType("broadcast"), 100, CouchDbHelper.repoBroadcasts());
        this.channel = DatagramChannel.open();
        logger.debug("Running resend protocol");

    }

    @Override
    protected void processResultSet(List<BroadcastInProgress> broadcasts) {
        for (BroadcastInProgress broadcast : broadcasts){
            try {
                InetSocketAddress target = new InetSocketAddress(broadcast.getAddress(), broadcast.getPort());
                send(broadcast.getNotify(), target);
                broadcast.incrementSendCount();
                ((CouchRepository<BroadcastInProgress>)this.repo)
                        .update(broadcast);

            } catch (Exception e) {
                logger.error("Failed to Update Broadcast", e);

            }
        }
    }

    private void send(ExoNotify notify, InetSocketAddress target) throws IOException {
        byte[] toSend = BroadcasterBasic.assembleBroadcastBytes(notify);
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.put(toSend);
        bb.flip();
        channel.send(bb, target);

    }

    @Override
    protected void execute() throws NoDocumentException {
        super.execute();

    }

    @Override
    public void close() throws Exception {
        this.channel.close();

    }
}
