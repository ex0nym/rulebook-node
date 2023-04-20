package io.exonym.lite.couchdb;

import io.exonym.lite.couchdb.AbstractCouchDbObject;

public class StubIdentityDocument extends AbstractCouchDbObject {

    private byte[] face;
    private String challengeType;

    public StubIdentityDocument() {
        this.setType("identity-doc");
    }

    public String getChallengeType() {
        return challengeType;
    }

    public void setChallengeType(String challengeType) {
        this.challengeType = challengeType;
    }

    public byte[] getFace() {
        return face;
    }

    public void setFace(byte[] face) {
        this.face = face;
    }
}
