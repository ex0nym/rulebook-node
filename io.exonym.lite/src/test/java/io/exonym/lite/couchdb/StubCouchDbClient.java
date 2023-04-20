package io.exonym.lite.couchdb;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;

/**
 * This needs to exist in any package that requires db access - here it's just for tests.
 */
public class StubCouchDbClient {
    
    private static final Logger logger = LogManager.getLogger(StubCouchDbClient.class);

    private static CloudantClient instance = null;

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
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeHierarchyAdapter(byte[].class, new Base64TypeAdapter());

        return ClientBuilder.url(new URL(System.getenv("DB_URL")))
                .username("admin")
                .password(System.getenv("DB_PASSWORD"))
                .gsonBuilder(builder)
                .build();

    }
}
