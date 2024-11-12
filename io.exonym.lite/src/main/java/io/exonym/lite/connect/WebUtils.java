package io.exonym.lite.connect;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.exonym.lite.couchdb.Base64TypeAdapter;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.ProgrammingException;
import io.exonym.lite.exceptions.UxException;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.UUID;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

public class WebUtils {

    private static final Logger logger = LogManager.getLogger(WebUtils.class);
    public final static Gson GSON;

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeHierarchyAdapter(byte[].class, new Base64TypeAdapter());
        GSON = builder.setPrettyPrinting().create();

    }

    public static HashMap<String, String> buildParams(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            String s = buildParamsAsString(request);
            int truc = Math.min(s.length(), 1024);
            String truncated = s.substring(0, truc);
            logger.debug("Received from client=" + truncated);
            if (s.startsWith("{")){
                return parseJsonToMap(s);

            } else {
                return parseKeyValuePairsToMap(s);

            }
        } catch (Exception e) {
            throw e;

        }
    }

    public static String buildParamsAsString(HttpServletRequest req) throws Exception {
        InputStream inputStream = req.getInputStream();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[30];
        int r=0;
        while( r >= 0 ) {
            r = inputStream.read(buffer);
            if( r >= 0 ) outputStream.write(buffer, 0, r);

        }
        outputStream.close();
        return outputStream.toString();
    }

    public static HashMap<String, String> parseKeyValuePairsToMap(String s) {
        HashMap<String, String> result = new HashMap<>();
        if (s!=null && s.length()>0){
            String[] kvp = s.split("&");
            for (String kv : kvp){
                String[] items = kv.split("=");
                if (items.length == 2){
                    result.put(items[0], items[1]);

                } else if (items.length ==1){
                    result.put(items[0], "null");

                } else {
                    logger.warn("Parsing Error - Unexpected length was " + items.length);

                }
            }
        }
        return result;
    }

    public static UUID extractUuid(String uuid) throws UxException {
        try {
            if (uuid != null) {
                return UUID.fromString(uuid);

            } else {
                throw new UxException(ErrorMessages.INVALID_UUID, new UxException("Null UUID"));

            }
        } catch (Exception e) {
            if (e.getCause()==null){
                e.initCause(new UxException(uuid));

            }
            throw new UxException(ErrorMessages.INVALID_UUID, e);

        }
    }

    public static int[] extractItemsAndPage(HashMap<String, String> in) throws UxException {
        String i = in.get("items");
        String p = in.get("page");
        try {
            if (i!=null && p!=null){
                int items = Integer.parseInt(i);
                int page = Integer.parseInt(p);
                return new int[] {items, page};

            } else {
                return new int[] {10, 0};

            }
        } catch (NumberFormatException e) {
            throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, e, "items", "page", "Must be Integers");

        }
    }


        public static void processError(Exception e, HttpServletResponse response) {
            try {
                logger.error("Error before throws back to client", e);
                JsonObject json = new JsonObject();
                json.addProperty("error", e.getMessage());
                if (e instanceof UxException){
                    UxException u = (UxException)e;
                    if (u.hasInfo()){
                        json.add("info", u.getInfo());

                    }
                }
                respond(response, json);

            } catch (Exception e2) {
                logger.error("Error was originally " + e.getMessage(), e2);

            }
        }

        public static void forwardToPage(HttpServletResponse response, String htmlPage) {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("forward", htmlPage);
                response.getWriter().write(json.toString());

            } catch (Exception e2) {
                logger.error("Error", e2);

            }
        }

        public static void success(HttpServletResponse response) {
            JsonObject o = new JsonObject();
            o.addProperty("success", true);
            respond(response, o);

        }

        public static void respond(HttpServletResponse response, JsonObject json) {

            try {
                if (json==null) {
                    throw new NullPointerException();

                } if (json.entrySet().isEmpty()) {
                    throw new ProgrammingException("JSON Entry Set null");

                }
//                logger.debug("Sending Response " + json.toString().substring(0,
//                        Math.min(json.toString().length(), 1024)));

                response.getWriter().write(json.toString());
                response.flushBuffer();

            } catch (Exception e) {
                logger.error("Error", e);

            }
        }

    public static byte[] compress(byte[] in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DeflaterOutputStream defl = new DeflaterOutputStream(out);
        defl.write(in);
        defl.finish();
        defl.flush();
        defl.close();
        return out.toByteArray();

    }

    public static byte[] decompress(byte[] in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InflaterOutputStream infl = new InflaterOutputStream(out);
        infl.write(in);
        infl.flush();
        infl.close();

        return out.toByteArray();

    }

    public static void respond(HttpServletResponse response, String json) {
            try {
                if (json==null) {
                    throw new NullPointerException();

                }
                // logger.debug("Sending Response " + json.toString());
                response.getWriter().write(json);

            } catch (Exception e) {
                logger.error("Error", e);

            }
        }

        public static HashMap<String, String> parseJsonToMap(String json){
           Gson gson = new Gson();
           Type type = new TypeToken<HashMap<String, String>>(){}.getType();
           return gson.fromJson(json, type);

        }

        public static String parseMapToJson(HashMap<String, String> map){
            Gson gson = new Gson();
            return gson.toJson(map);

        }

        public static String generateSixDigitCode(){
            SecureRandom sr = new SecureRandom();
            String code = "";
            while (code.length()<7){
                code += sr.nextInt(9);
                if (code.length()==3){
                    code += " ";

                }
            }
            return code;

        }

        @Deprecated
        public static String processHttpPostRequest(String url, String json) throws Exception {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()){
                HttpPost httpPost = new HttpPost(url);

                httpPost.addHeader("content-type", "application/x-www-form-urlencoded");

                StringEntity params = new StringEntity(json);
                httpPost.setEntity(params);

                logger.debug(httpPost.getURI());
                logger.debug("HTTP Req " + httpPost.getRequestLine());

                ResponseHandler<String> responseHandler = arg0 -> {
                    int status = arg0.getStatusLine().getStatusCode();
                    if (status>=200 && status <=300) {
                        HttpEntity entity = arg0.getEntity();
                        return (entity != null ? EntityUtils.toString(entity) : null);

                    } else if (status==500){
                        String error = arg0.getStatusLine().getReasonPhrase();
                        HashMap<String, String> err = new HashMap<>();
                        err.put("error", error);
                        return GSON.toJson(err);

                    } else {
                        throw new ClientProtocolException("Error Code " + status);

                    }
                };
                return httpClient.execute(httpPost, responseHandler);

            } catch (Exception e) {
                throw e;

            }
        }

        @Deprecated
        public static String processHttpGetRequest(String url, String json) throws Exception {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()){
                HttpGet httpGet = new HttpGet(url);
                StringEntity params = new StringEntity(json);
                httpGet.addHeader("content-type", "application/x-www-form-urlencoded");
                logger.debug(httpGet.getURI());
                logger.debug("HTTP Req " + httpGet.getRequestLine());

                ResponseHandler<String> responseHandler = arg0 -> {
                    int status = arg0.getStatusLine().getStatusCode();
                    if (status>=200 && status <=300){
                        HttpEntity entity = arg0.getEntity();
                        return ( entity!=null ? EntityUtils.toString(entity) : null );

                    } else {
                        throw new ClientProtocolException("Error Code " + status);

                    }
                };
                return httpClient.execute(httpGet, responseHandler);

            } catch (Exception e) {
                throw e;

            }
        }

        public static BigInteger toBigInteger(String hex){
            return new BigInteger(hex, 16);

        }

        public static String toHex(BigInteger bigInteger){
            return String.format("%040x", bigInteger);

        }

        public static String getFullPath(HttpServletRequest request){

            String path = request.getScheme() + "://" +
                    request.getServerName() +
                    ("http".equals(request.getScheme()) && request.getServerPort() == 80
                            || "https".equals(request.getScheme()) && request.getServerPort() == 443 ? ""
                            : ":" + request.getServerPort() ) +
                    request.getContextPath();
            logger.info("Computed full path to be = " + path);
            if (path.contains("localhost")){
                path = path.replace("localhost", "exonym-x-03");

            } else {
                logger.warn("Replacing http:// with https:// due to Tomcat/Docker error");
                path = path.replace("http://", "https://");

            }
            return path;

        }

        public static HashMap<String, String> splitQueryString(String query) {
            try {
                HashMap<String, String> result = new HashMap<String, String>();
                String[] pairs = query.split("&");

                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    result.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));

                }
                return result;

            } catch (Exception e) {
                logger.error("Error", e);
                return null;

            }
        }

    public static String getMandatory(HashMap<String, String> in, String attribute) throws UxException {
        String r = in.get(attribute);
        if (r==null){
            throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, attribute);
        }
        return r;
    }

    public static String decodeB64ToUtf8String(HashMap<String, String> in, String att) throws UxException {
        return new String(
                Base64.decodeBase64(    getMandatory(in, att)   )
                , StandardCharsets.UTF_8);

    }

    public static void logDebugProtect(String message, Object obj){
        if (logger.isDebugEnabled()){
            logger.debug(message + " " + GSON.toJson(obj));
        }
    }
}