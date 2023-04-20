package io.exonym.lite.pojo;

import io.exonym.lite.couchdb.AbstractCouchDbObject;

import java.net.URI;
import java.util.ArrayList;

public class ExoMatrix extends AbstractCouchDbObject {

    public static final String FIELD_HOST_UUID = "hostUuid";
    public static final String FIELD_NIBBLE6 = "nibble6";
    public static final String FIELD_X0 = "x0Hash";

    private URI hostUuid;
    private String nibble6;
    private ArrayList<String> x0Hash = new ArrayList<>();

    public URI getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(URI hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getNibble6() {
        return nibble6;
    }

    public void setNibble6(String nibble6) {
        this.nibble6 = nibble6;
    }

    public ArrayList<String> getX0Hash() {
        return x0Hash;
    }

    public void setX0Hash(ArrayList<String> x0Hash) {
        this.x0Hash = x0Hash;
    }

    @Override
    public int hashCode() {
        return this.hostUuid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ExoMatrix){
            ExoMatrix m = (ExoMatrix)obj;
            if (m.getHostUuid()!=null){
                return m.getHostUuid().equals(this.hostUuid);

            } else {
                return false;

            }
        } else if (obj instanceof String){
            return obj.equals(this.hostUuid);

        } else {
            return false;
        }
    }
}
