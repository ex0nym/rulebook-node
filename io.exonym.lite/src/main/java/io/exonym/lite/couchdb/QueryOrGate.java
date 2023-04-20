package io.exonym.lite.couchdb;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collection;

public class QueryOrGate extends Query {

    /*
    {
        "selector": {
            "_id": {
                "$or": [
                    "9d7d7a1e0c11406387d65daa516b6735",
                    "7ea409b09d984f9d8af3e465d5f6fa3d"
                ]
            }
        }
    }

//*/

    private JsonObject item = new JsonObject();

    public QueryOrGate(String field, String... or) {
        JsonObject selector = new JsonObject();
        item.add("selector", selector);
        JsonObject f = new JsonObject();
        selector.add(field, f);
        JsonArray options = new JsonArray();
        f.add(Query.OR, options);
        for (String item: or){
            options.add(item);

        }
    }

    public QueryOrGate(String field, Collection<String> or) {
        JsonObject selector = new JsonObject();
        item.add("selector", selector);
        JsonObject f = new JsonObject();
        selector.add(field, f);
        JsonArray options = new JsonArray();
        f.add(Query.OR, options);
        for (String item: or){
            options.add(item);

        }
    }

    // TODO: 06.04.21 add and condition

    @Override
    public String getJson() {
        return item.toString();

    }
}