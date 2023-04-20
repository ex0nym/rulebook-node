package io.exonym.lite.pojo;

import io.exonym.lite.couchdb.AbstractCouchDbObject;

import java.net.URI;

public class Vio extends AbstractCouchDbObject {

    public static final String FIELD_X0_HASH = "x0Hash";

    private URI advocateUID;
    private String nibble6;
    private String x0Hash;
    private String t;


    public void setAdvocateUID(URI advocateUID) {
        this.advocateUID = advocateUID;
    }

    public URI getAdvocateUID() {
        return advocateUID;
    }

    public String getNibble6() {
        return nibble6;
    }

    public void setNibble6(String nibble6) {
        this.nibble6 = nibble6;
    }

    public String getX0Hash() {
        return x0Hash;
    }

    public void setX0Hash(String x0Hash) {
        this.x0Hash = x0Hash;
    }

    public String getT() {
        return t;
    }

    public void setT(String t) {
        this.t = t;
    }


}
