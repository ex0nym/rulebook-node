package io.exonym.lite.pojo;

import java.net.URI;
import java.util.ArrayList;

public class ExonymResult {

    private ArrayList<URI> hostUuids = new ArrayList<>();

    private String nibble6;
    private String x0;
    private String x0Hash;

    public String getX0Hash() {
        return x0Hash;
    }

    public void setX0Hash(String x0Hash) {
        this.x0Hash = x0Hash;
    }

    public String getNibble6() {
        return nibble6;
    }

    public void setNibble6(String nibble6) {
        this.nibble6 = nibble6;
    }

    public String getX0() {
        return x0;
    }

    public void setX0(String x0) {
        this.x0 = x0;
    }

    public ArrayList<URI> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(ArrayList<URI> hostUuids) {
        this.hostUuids = hostUuids;
    }
}
