package io.exonym.lite.pojo;

import java.math.BigInteger;
import java.net.URI;
import java.util.AbstractList;
import java.util.List;

public class IssuanceSigma {

    private String hello;
    private String im;
    private String imab;
    private String presentationToken;
    private String presentationPolicy;
    private BigInteger h;
    private URI issuerUid;
    private String error;
    private boolean testNet = false;



    private String sybilClass;
    private String[] info;

    public String getHello() {
        return hello;
    }

    public void setHello(String hello) {
        this.hello = hello;
    }

    public String getIm() {
        return im;
    }

    public void setIm(String im) {
        this.im = im;
    }

    public String getImab() {
        return imab;
    }

    public void setImab(String imab) {
        this.imab = imab;
    }

    public boolean isTestNet() {
        return testNet;
    }

    public void setTestNet(boolean testNet) {
        this.testNet = testNet;
    }

    public BigInteger getH() {
        return h;
    }

    public void setH(BigInteger h) {
        this.h = h;
    }

    public URI getIssuerUid() {
        return issuerUid;
    }

    public void setIssuerUid(URI issuerUid) {
        this.issuerUid = issuerUid;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getSybilClass() {
        return sybilClass;
    }

    public void setSybilClass(String sybilClass) {
        this.sybilClass = sybilClass;
    }

    public String[] getInfo() {
        return info;
    }

    public void setInfo(String[] info) {
        this.info = info;
    }

    public String getPresentationToken() {
        return presentationToken;
    }

    public void setPresentationToken(String presentationToken) {
        this.presentationToken = presentationToken;
    }

    public String getPresentationPolicy() {
        return presentationPolicy;
    }

    public void setPresentationPolicy(String presentationPolicy) {
        this.presentationPolicy = presentationPolicy;
    }
}
