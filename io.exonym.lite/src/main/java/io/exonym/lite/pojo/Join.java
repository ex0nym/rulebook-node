package io.exonym.lite.pojo;

import java.net.URI;
import java.util.ArrayList;

public class Join {

    private URI modUid;
    private ArrayList<String> rules;
    private String signature;
    private String timestamp;

    public URI getModUid() {
        return modUid;
    }

    public void setModUid(URI modUid) {
        this.modUid = modUid;
    }

    public ArrayList<String> getRules() {
        if (rules ==null){
            rules = new ArrayList<>();

        }
        return rules;
    }

    public void setRules(ArrayList<String> rules) {
        this.rules = rules;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
