package io.exonym.lite.pojo;

import io.exonym.lite.standard.CryptoUtils;
import org.apache.commons.codec.binary.Base64;

public class Challenge {

    // Authenticate Node Request
    private String kid;
    private String key;

    // Authenticate User Request
    private String appUuid;
    private String token;

    private final String nonce;
    private Prove prove = new Prove();
    private Join join = new Join();

    public Challenge() {
        this.nonce = Base64.encodeBase64String(
                CryptoUtils.generateNonce(6));
    }

    public String getNonce() {
        return nonce;
    }

    public Prove getProve() {
        return prove;
    }

    public void setProve(Prove prove) {
        this.prove = prove;
    }

    public Join getJoin() {
        return join;
    }

    public void setJoin(Join join) {
        this.join = join;
    }

    public String getKid() {
        return kid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getAppUuid() {
        return appUuid;
    }

    public void setAppUuid(String appUuid) {
        this.appUuid = appUuid;
    }

}
