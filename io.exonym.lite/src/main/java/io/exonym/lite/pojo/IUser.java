package io.exonym.lite.pojo;

import io.exonym.lite.couchdb.AbstractCouchDbObject;

import java.util.ArrayList;

public class IUser extends AbstractCouchDbObject  implements Comparable<IUser> {

    public static final String I_USER_PRIMARY_ADMIN = "primary-admin";
    public static final String I_USER_ADMIN = "admin";
    public static final String I_USER_MEMBER = "member";
    public static final String I_USER_API_KEY = "api-key";

    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_UUID = "uuid";

    private String username;
    // clientSide = sha256(password) = u;  || clientSide = sha256(password + salt) = u
    private String v; // sha256(u) = v
    private String salt;
    private byte[] publicKey;
    private boolean requiresPassChange = false;
    private boolean inactive = false;
    private String privileges = null;

    private ArrayList<String> accessFkIds = null;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getV() {
        return v;
    }

    public void setV(String v) {
        this.v = v;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public boolean isRequiresPassChange() {
        return requiresPassChange;
    }

    public void setRequiresPassChange(boolean requiresPassChange) {
        this.requiresPassChange = requiresPassChange;
    }

    public void setPrivileges(String privileges) {
        this.privileges = privileges;
    }

    public String getPrivileges() {
        return privileges;
    }

    public boolean isInactive() {
        return inactive;
    }

    public void setInactive(boolean inactive) {
        this.inactive = inactive;
    }

    public ArrayList<String> getAccessFkIds() {
        if (accessFkIds==null){
            accessFkIds = new ArrayList<>();

        }
        return accessFkIds;
    }

    public void setAccessFkIds(ArrayList<String> accessFkIds) {
        this.accessFkIds = accessFkIds;
    }

    @Override
    public int compareTo(IUser o) {
        return o.getUsername().compareTo(this.username);

    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}
