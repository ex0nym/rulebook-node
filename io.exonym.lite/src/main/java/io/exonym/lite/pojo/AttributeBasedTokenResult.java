package io.exonym.lite.pojo;

import java.util.ArrayList;

public class AttributeBasedTokenResult {

    private byte[] message;

    private ArrayList<DecodedAttributeToken> disclosedAttributes = new ArrayList<>();

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }

    public ArrayList<DecodedAttributeToken> getDisclosedAttributes() {
        return disclosedAttributes;
    }
}
