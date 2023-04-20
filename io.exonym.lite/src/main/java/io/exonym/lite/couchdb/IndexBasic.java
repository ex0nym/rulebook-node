package io.exonym.lite.couchdb;

import com.cloudant.client.api.query.JsonIndex;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class IndexBasic implements CouchIndex {

//    {
//        "index": {
//        "fields": [
//        "foo"
//      ]
//          },
//        "name": "foo-json-index",
//            "type": "json"
//    }

    private String index;

    public IndexBasic(String index) {
        this.index = index;
    }

    public IndexBasic(String[] fields, String name) {
        index= JsonIndex.builder()
                .name(name)
                .asc(fields).definition();


//        result = new JsonObject();
//        JsonObject index = new JsonObject();
//        result.add("index", index);
//        JsonArray fieldArray = new JsonArray();
//        index.add("fields", fieldArray);
//        index.addProperty("name", name);
//        index.addProperty("type", "json");
//
//        for (String f : fields){
//            fieldArray.add(f);
//
//        }
    }

    @Override
    public String toJson() {
        return index;

    }
}
