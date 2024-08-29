package io.exonym.lite.pojo;


import io.exonym.lite.parallel.Msg;

import java.net.URI;
import java.nio.charset.StandardCharsets;

public class ExoNotify implements Msg {

    public static final String TYPE_JOIN = "JOIN";

    public static final String TYPE_LEAD = "LEAD";
    public static final String TYPE_VIOLATION = "VIOLATION";

    public static final String TYPE_ACK = "ACK";

    private String type;
    private URI nodeUID;
    private String t;
    private String nibble6;
    private String hashOfX0;
    private String sigB64;

    private String ppB64;

    private String raiB64;

    private String ppSigB64;

    private String raiSigB64;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public URI getNodeUID() {
        return nodeUID;
    }

    public void setNodeUID(URI nodeUID) {
        this.nodeUID = nodeUID;
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

    public String getPpB64() {
        return ppB64;
    }

    public void setPpB64(String ppB64) {
        this.ppB64 = ppB64;
    }

    public String getRaiB64() {
        return raiB64;
    }

    public void setRaiB64(String raiB64) {
        this.raiB64 = raiB64;
    }

    public String getPpSigB64() {
        return ppSigB64;
    }

    public void setPpSigB64(String ppSigB64) {
        this.ppSigB64 = ppSigB64;
    }

    public String getRaiSigB64() {
        return raiSigB64;
    }

    public void setRaiSigB64(String raiSigB64) {
        this.raiSigB64 = raiSigB64;
    }

    public static byte[] signatureOn(ExoNotify notify){
        return (notify.getT()
                + notify.getNibble6()
                + notify.getType()
                + notify.getHashOfX0()
                + notify.getNodeUID())
                .getBytes(StandardCharsets.UTF_8);

    }

    public static byte[] signatureOnAckAndOrigin(ExoNotify notify){
        return (notify.getType()
                + notify.getT()
                + notify.getNodeUID())
                .getBytes(StandardCharsets.UTF_8);

    }

    @Override
    public String toString() {
        return this.type + " " + (this.hashOfX0==null ? this.getNodeUID() : this.hashOfX0);
    }
}
