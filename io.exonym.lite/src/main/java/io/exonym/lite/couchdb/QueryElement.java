package io.exonym.lite.couchdb;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class QueryElement extends Query {

    private final JsonObject query = new JsonObject();

    private final Gson gson = new Gson();

    /**
     *
     * @param fieldName
     * @param criteria
     * @param QUERY_OPERATOR
     */
    public QueryElement(String fieldName, String criteria, String QUERY_OPERATOR) {
        JsonObject selector = new JsonObject();
        query.add(Query.SELECTOR, selector);
        JsonObject elementMatch = new JsonObject();
        selector.add(fieldName, elementMatch);
        JsonObject value = new JsonObject();
        elementMatch.add(Query.ELEMENT_MATCH, value);
        value.addProperty(QUERY_OPERATOR, criteria);

    }

    @Override
    public String getJson() {
        return gson.toJson(this.query);

    }
}


