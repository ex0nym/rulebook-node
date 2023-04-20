package io.exonym.lite.couchdb;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class Query {

    public static final String GREATER_THAN_OR_EQUAL = "$gte";
    public static final String GREATER_THAN = "$gt";
    public static final String EQUAL = "$eq";
    public static final String OR = "$or";
    public static final String AND = "$and";
    public static final String NOT = "$ne";

    public static final String LESS_THAN_OR_EQUAL = "$lte";
    public static final String LESS_THAN = "$lt";

    public static final String REG_EX = "$regex";

    public static final String SELECTOR = "selector";
    public static final String ELEMENT_MATCH = "$elemMatch";

    public static final String DIRECTION_ASC = "asc";
    public static final String DIRECTION_DSC = "dsc";

    public abstract String getJson();

    protected JsonArray sort = null;

    @Override
    public String toString() {
        return getJson();

    }

    public void addSortField(String field, String DIRECTION){
        if (sort==null){
            sort = new JsonArray();

        }
        JsonObject o = new JsonObject();
        o.addProperty(field, DIRECTION);
        sort.add(o);

    }
}
