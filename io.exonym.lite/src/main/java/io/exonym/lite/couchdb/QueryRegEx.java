package io.exonym.lite.couchdb;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class QueryRegEx extends Query{

    private final JsonObject query = new JsonObject();

    private final Gson gson = new Gson();


    public QueryRegEx(String fieldName, String criteria) {
        JsonObject selector = new JsonObject();
        query.add(Query.SELECTOR, selector);
        JsonObject field = new JsonObject();
        selector.add(fieldName, field);
        JsonObject value = new JsonObject();
        field.addProperty(Query.REG_EX, criteria);

    }

    @Override
    public String getJson() {
        return gson.toJson(this.query);

    }
}
