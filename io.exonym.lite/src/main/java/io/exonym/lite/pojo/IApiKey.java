package io.exonym.lite.pojo;

import io.exonym.lite.couchdb.AbstractCouchDbObject;

public class IApiKey extends AbstractCouchDbObject {

    private String uuid;
    public static final String FIELD_API_KEY_UUID = "uuid";

    // key = sha256(k);
    private String key;

    public IApiKey() {
        this.setType(IUser.I_USER_API_KEY);

    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
