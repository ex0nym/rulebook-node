package io.exonym.lite.pojo;

import com.google.gson.JsonArray;
import io.exonym.lite.couchdb.AbstractCouchDbObject;
import io.exonym.lite.exceptions.PenaltyException;

public class ProofStore extends AbstractCouchDbObject {

    // Note that the _id should be the assigned unique index for this token.
    // add another field if the ID is insufficient.
    private byte[] tokenCompressed;
    private String endonym;

    private JsonArray mods;

    private byte[] publicKey;

    private long lastAuthTime;

    private RejoinCriteria rejoinCriteria;

    public byte[] getTokenCompressed() {
        return tokenCompressed;
    }

    public void setTokenCompressed(byte[] tokenCompressed) {
        this.tokenCompressed = tokenCompressed;
    }

    public String getEndonym() {
        return endonym;
    }

    public void setEndonym(String endonym) {
        this.endonym = endonym;
    }

    public JsonArray getMods() {
        return mods;
    }

    public void setMods(JsonArray mods) {
        this.mods = mods;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public long getLastAuthTime() {
        return lastAuthTime;
    }

    public void setLastAuthTime(long lastAuthTime) {
        this.lastAuthTime = lastAuthTime;
    }

    public RejoinCriteria getRejoinCriteria() {
        return rejoinCriteria;
    }

    public void setRejoinCriteria(RejoinCriteria rejoinCriteria) {
        this.rejoinCriteria = rejoinCriteria;
    }
}
