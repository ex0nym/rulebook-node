package io.exonym.lite.pojo;


import io.exonym.lite.parallel.Msg;

import java.net.URI;
import java.nio.charset.StandardCharsets;

public class ExoNotify implements Msg {

    public static final String TYPE_JOIN = "JOIN";
    public static final String TYPE_SOURCE = "SOURCE";
    public static final String TYPE_ACK = "ACK";
    public static final String TYPE_VIOLATION = "VIOLATION";

    private String type;
    private URI advocateUID;
    private String t;
    private String nibble6;
    private String hashOfX0;
    private String sigB64;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public URI getAdvocateUID() {
        return advocateUID;
    }

    public void setAdvocateUID(URI advocateUID) {
        this.advocateUID = advocateUID;
    }

    public String getT() {
        return t;
    }

    public void setT(String t) {
        this.t = t;
    }

    public String getNibble6() {
        return nibble6;
    }

    public void setNibble6(String nibble6) {
        this.nibble6 = nibble6;
    }

    public String getHashOfX0() {
        return hashOfX0;
    }

    public void setHashOfX0(String hashOfX0) {
        this.hashOfX0 = hashOfX0;
    }

    public String getSigB64() {
        return sigB64;
    }

    public void setSigB64(String sigB64) {
        this.sigB64 = sigB64;
    }

    public static byte[] signatureOn(ExoNotify notify){
        return (notify.getT()
                + notify.getNibble6()
                + notify.getType()
                + notify.getHashOfX0()
                + notify.getAdvocateUID())
                .getBytes(StandardCharsets.UTF_8);

    }

    public static byte[] signatureOnAckAndOrigin(ExoNotify notify){
        return (notify.getType()
                + notify.getT()
                + notify.getAdvocateUID())
                .getBytes(StandardCharsets.UTF_8);

    }

    @Override
    public String toString() {
        return this.type + " " + (this.hashOfX0==null ? this.getAdvocateUID() : this.hashOfX0);
    }
}
