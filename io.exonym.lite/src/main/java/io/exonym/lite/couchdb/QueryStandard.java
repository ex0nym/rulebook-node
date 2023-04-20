package io.exonym.lite.couchdb;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;

public class QueryStandard extends Query {

    private Gson gson = new Gson();
    private JsonObject result = new JsonObject();
    private JsonObject selector = new JsonObject();
    private JsonArray fields;

    public QueryStandard() {
        this.result.add("selector", selector);

    }

    public void addCriteria(String fieldsName, String criteria){
        this.selector.addProperty(fieldsName, criteria);

    }

    public void addFieldSelector(String field){
        if (fields==null){
            this.fields = new JsonArray();
            this.result.add("fields", fields);

        }
        this.fields.add(field);

    }

    @Override
    public String getJson() {
        return gson.toJson(result);
    }
}
