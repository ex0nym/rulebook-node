package io.exonym.lite.connect;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;

public class Http implements AutoCloseable {


    private static final Logger logger = LogManager.getLogger(Http.class);
    private final HashMap<String, String> headerData = new HashMap<>();

    private final CloseableHttpClient client;
    private HttpContext context = null;
    public final static Gson gson = new Gson();
    private final RequestConfig config;

    public Http() {
        int timeout = 60;
        config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000).build();
        // this.client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        this.client = HttpClients.createDefault();

    }

    public void newContext(){
        CookieStore cookieStore = new BasicCookieStore();
        this.context = new BasicHttpContext();
        this.context.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

    }


    public String basicPost(String url, JsonObject json) throws IOException {
        String j = gson.toJson(json);
        return basicPost(url, j);

    }

    public String basicPost(String url, String json) throws IOException {
        if (context==null){
            this.newContext();

        }
        HttpPost post = new HttpPost(url);
        post.setConfig(config);
//        post.setHeader("User-Agent",
//                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
//        post.addHeader("Accept", "application/json");
//        post.addHeader("Accept-Language", "en-US,en;q=0.5");
//        post.addHeader("Access-Control-Request-Headers", "content-type");
//        post.addHeader("Access-Control-Request-Method", "POST");
//        post.addHeader("Method", "POST");
//        post.addHeader("content-type", "application/json");
        extendHeaderData(post);

        StringEntity entity = new StringEntity(json);
        post.setEntity(entity);
        ResponseBasic r = new ResponseBasic();
        // logger.debug(context.toString());
        return client.execute(post, r, context);

    }

    public String basicGet(String url) throws IOException{
        if (context==null){
            this.newContext();

        }
        logger.debug(url);
        HttpGet get = new HttpGet(url);
        get.setConfig(config);
//        get.setHeader("User-Agent",
//                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
//        get.addHeader("content-type", "application/json");
//        get.addHeader("Accept", "text/plain");
//        get.addHeader("Method", "GET");
        extendHeaderData(get);


        ResponseBasic r = new ResponseBasic();
        return client.execute(get, r, context);

    }

    private void extendHeaderData(HttpRequestBase request) {
        for (String key : headerData.keySet()){
            request.addHeader(key, headerData.get(key));

        }
    }

    public HashMap<String, String> getHeaderData() {
        return headerData;
    }

    @Override
    public void close() throws Exception {
        if (client!=null){
            client.close();

        }
    }

    public static Gson getGson() {
        return gson;
    }
}
