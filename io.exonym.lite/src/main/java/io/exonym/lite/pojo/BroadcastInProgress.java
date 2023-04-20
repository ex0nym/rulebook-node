package io.exonym.lite.pojo;

import io.exonym.lite.couchdb.AbstractCouchDbObject;

import java.net.URI;

public class BroadcastInProgress extends AbstractCouchDbObject {

    public static final String FIELD_CONTEXT = "context";
    public static final String FIELD_HOST_UUID = "advocateUID";

    private String context;
    private URI advocateUID;
    private ExoNotify notify;
    private String address;
    private int port;
    private int count = 0;

    public BroadcastInProgress() {
        this.type = "broadcast";

    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public URI getAdvocateUID() {
        return advocateUID;
    }

    public void setAdvocateUID(URI advocateUID) {
        this.advocateUID = advocateUID;
    }

    public ExoNotify getNotify() {
        return notify;
    }

    public void setNotify(ExoNotify notify) {
        this.notify = notify;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void incrementSendCount(){
        this.count++;
    }
}
