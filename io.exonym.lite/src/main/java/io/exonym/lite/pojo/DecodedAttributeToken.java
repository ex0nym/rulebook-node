package io.exonym.lite.pojo;

import java.net.URI;
import java.util.HashMap;

public class DecodedAttributeToken {

    private URI credentialSpec;
    private HashMap<URI, Object> disclosedValues = new HashMap<>();

    public URI getCredentialSpec() {
        return credentialSpec;
    }

    public void setCredentialSpec(URI credentialSpec) {
        this.credentialSpec = credentialSpec;
    }

    public HashMap<URI, Object> getDisclosedValues() {
        return disclosedValues;
    }

    public void setDisclosedValues(HashMap<URI, Object> disclosedValues) {
        this.disclosedValues = disclosedValues;
    }
}
