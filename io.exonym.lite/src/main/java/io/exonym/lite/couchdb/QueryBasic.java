package io.exonym.lite.couchdb;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashMap;

// As opposed to RegEx Selector
public class QueryBasic extends Query {

    private Gson gson = new Gson();

    private HashMap<String, String> selector = new HashMap<>();


    public HashMap<String, String> getSelector() {
        return selector;

    }

    public void setSelector(HashMap<String, String> selector) {
        this.selector = selector;

    }

    public static QueryBasic selectType(String TYPE_NAME) {
        QueryBasic query = new QueryBasic();
        HashMap<String, String> selector = new HashMap<>();
        if (TYPE_NAME!=null){
            selector.put("type", TYPE_NAME);
        }
        query.setSelector(selector);
        return query;

    }

    @Override
    public String getJson() {
        Proxy proxy = new Proxy(this.selector);
        return gson.toJson(proxy);

    }

    private class Proxy {
        private final HashMap<String, String> selector;

        public Proxy(HashMap<String, String> selector) {
            this.selector = selector;
        }

        public HashMap<String, String> getSelector() {
            return selector;
        }
    }
}
