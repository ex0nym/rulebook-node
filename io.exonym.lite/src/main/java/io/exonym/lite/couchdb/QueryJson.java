package io.exonym.lite.couchdb;

import com.google.gson.JsonObject;

public class QueryJson extends Query {

    private final JsonObject json;

    public QueryJson(JsonObject json) {
        this.json = json;

    }

    @Override
    public String getJson() {
        return json.toString();
    }

    @Override
    public String toString() {
        return super.toString();

    }
}
