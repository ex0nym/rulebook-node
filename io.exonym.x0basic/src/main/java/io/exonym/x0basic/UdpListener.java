package io.exonym.x0basic;

import io.exonym.lite.parallel.ModelLoopedInstruction;
import io.exonym.lite.parallel.Msg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;

public class UdpListener extends ModelLoopedInstruction {

    private static final Logger logger = LogManager.getLogger(UdpListener.class);
    private final DatagramChannel channel;
    private final ArrayBlockingQueue<Msg> pipeOut;
    private boolean first = true;

    protected UdpListener(ArrayBlockingQueue<Msg> pipeToUpdateQueue) throws IOException {
        super("UdpListener");
        int port = X0Properties.getInstance().getUdpPort();
        this.channel = DatagramChannel.open();
        channel.socket().bind(new InetSocketAddress(port));
        this.pipeOut = pipeToUpdateQueue;
        this.start();
        logger.debug("------------------------------------------");
        logger.debug("Listening on :" + this.channel.getLocalAddress());
        logger.debug("-");
        logger.debug("-");
        logger.debug("-");

    }

    protected ByteBuffer listen() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.clear();
        channel.receive(buffer);
        return buffer;

    }

    @Override
    protected void close() {
        super.close();
    }

    @Override
    protected void loop() {
        try {
            ByteBuffer buffer = this.listen();
            String in = new String(buffer.array(), StandardCharsets.UTF_8);
            if (first){
                logger.debug("Start");
                first=false;

            }
            BroadcastStringIn msg = new BroadcastStringIn(in);
            logger.debug(in);
            pipeOut.put(msg);

        } catch (IOException e) {
            logger.error("Error", e);

        } catch (InterruptedException e) {
            logger.error("Interrupted");

        }
    }
}
