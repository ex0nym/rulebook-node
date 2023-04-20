package io.exonym.actor.storage;

import io.exonym.lite.time.DateHelper;

import java.util.HashMap;

public class Poke {

    public static final String SIGNATURE_ON_POKE = "signature";
    private String t = DateHelper.currentIsoUtcDateTime();
    private HashMap<String, String> signatures = new HashMap<>();

    public String getT() {
        return t;
    }

    public void setT(String t) {
        this.t = t;
    }

    public HashMap<String, String> getSignatures() {
        return signatures;
    }

    public void setSignatures(HashMap<String, String> signatures) {
        this.signatures = signatures;
    }
}
