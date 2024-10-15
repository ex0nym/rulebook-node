package io.exonym.lite.pojo;

import io.exonym.lite.couchdb.AbstractCouchDbObject;
import io.exonym.lite.standard.CryptoUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ExoMatrix extends AbstractCouchDbObject {

    public static final String FIELD_MOD_UID = "modUid";
    public static final String FIELD_NIBBLE6 = "nibble6";
    public static final String FIELD_X0 = "x0Hash";

    private URI modUid;
    private String nibble6;
    private ArrayList<String> x0Hash = new ArrayList<>();

    public URI getModUid() {
        return modUid;
    }

    public void setModUid(URI modUid) {
        this.modUid = modUid;
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
        return this.modUid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ExoMatrix){
            ExoMatrix m = (ExoMatrix)obj;
            if (m.getModUid()!=null){
                return m.getModUid().equals(this.modUid);

            } else {
                return false;

            }
        } else if (obj instanceof String){
            return obj.equals(this.modUid.toString());

        } else if (obj instanceof URI){
            return obj.equals(this.modUid);

        } else {
            return false;
        }
    }

    public static ExoMatrix withModUid(URI mod){
        ExoMatrix m = new ExoMatrix();
        m.setModUid(mod);
        return m;

    }

    @Override
    public String toString() {
        return nibble6 + " " + modUid;
    }

    public String index(){
        String indexRaw = this.getNibble6() + this.getModUid();
        return CryptoUtils.computeMd5HashAsHex(indexRaw);

    }

}
