package io.exonym.lite.pojo;

import io.exonym.lite.couchdb.AbstractCouchDbObject;

public class TestNetUser extends AbstractCouchDbObject {

    public static final String TYPE = "registrations";

    private int registrations = 0;

    public TestNetUser() {
        this.type = TYPE;
    }



    public void incrementRegistrations() {
        this.registrations++;
    }
}
