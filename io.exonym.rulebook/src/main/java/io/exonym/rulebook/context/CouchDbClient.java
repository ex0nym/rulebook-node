package io.exonym.rulebook.context;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.google.gson.GsonBuilder;
import io.exonym.lite.couchdb.Base64TypeAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class CouchDbClient {

    private static final Logger logger = LogManager.getLogger(CouchDbClient.class);
    private static CloudantClient instance;

    static{
        try{
            instance = create();

        } catch (Exception e){
            logger.error("Error on instantiation", e);

        }
    }

    protected static CloudantClient instance(){
        return instance;

    }

    private static CloudantClient create() throws Exception {
        RulebookNodeProperties prop = RulebookNodeProperties.instance();
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeHierarchyAdapter(byte[].class, new Base64TypeAdapter());
        logger.info(prop.getDbUrl());
        logger.info(prop.getDbUsername());

        return ClientBuilder.url(prop.getDbUrl())
                .username(prop.getDbUsername())
                .password(prop.getDbPassword())
                .gsonBuilder(builder)
                .build();

    }

    public static void main(String[] args) throws Exception {
        DatagramChannel channel = DatagramChannel.open();
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.put(new byte[] {1,-4, 56});
        bb.flip();
        String at = "nu0.avocado-toast.com";
        InetSocketAddress address = new InetSocketAddress("82.165.109.4", 9090);
        channel.send(bb, address);
        logger.debug("sent");

    }
}
