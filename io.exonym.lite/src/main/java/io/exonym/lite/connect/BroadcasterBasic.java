package io.exonym.lite.connect;

import com.google.gson.Gson;
import io.exonym.lite.pojo.ExoNotify;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;

public class BroadcasterBasic implements AutoCloseable {


    private static final Logger logger = LogManager.getLogger(BroadcasterBasic.class);
    private final DatagramChannel channel;
    private byte[] bytesToBroadcast;

    public BroadcasterBasic(ExoNotify notify) throws IOException {
        this.channel = DatagramChannel.open();
        this.bytesToBroadcast = assembleBroadcastBytes(notify);

    }

    public static byte[] assembleBroadcastBytes(ExoNotify notify) {
        Gson gson = new Gson();
        String json = gson.toJson(notify);
        byte[] bytesToBroadcast = json.getBytes(StandardCharsets.UTF_8);
        int len = bytesToBroadcast.length;
        json = len + json;
        return json.getBytes(StandardCharsets.UTF_8);

    }

    public void send(InetSocketAddress address) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.put(bytesToBroadcast);
        bb.flip();
        channel.send(bb, address);

    }

    @Override
    public void close() throws Exception {
        this.channel.close();

    }
}
