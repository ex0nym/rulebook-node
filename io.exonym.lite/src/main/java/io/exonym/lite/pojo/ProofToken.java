package io.exonym.lite.pojo;

import io.exonym.lite.couchdb.AbstractCouchDbObject;

public class ProofToken extends AbstractCouchDbObject {

    private String internalReference;
    private String nonce;
    private String b64Token;

    public String getInternalReference() {
        return internalReference;
    }

    public void setInternalReference(String internalReference) {
        this.internalReference = internalReference;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getB64Token() {
        return b64Token;
    }

    public void setB64Token(String b64Token) {
        this.b64Token = b64Token;
    }
}
