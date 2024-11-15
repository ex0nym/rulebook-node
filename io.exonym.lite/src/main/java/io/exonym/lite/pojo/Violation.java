package io.exonym.lite.pojo;

import java.net.URI;
import java.util.ArrayList;

public class Violation {

//  {rule:"r0", t:"2021-09-01T13:00:00", settled:true, modUid:"modUid"}
    private ArrayList<URI> ruleUids;
    private String timestamp;
    private boolean settled = false;
    private boolean override = false;
    private String x0;

    private String nibble6;

    private URI requestingModUid;

    public ArrayList<URI> getRuleUids() {
        if (ruleUids ==null){
            ruleUids = new ArrayList<>();
        }
        return ruleUids;
    }

    public void setRuleUids(ArrayList<URI> ruleUids) {
        this.ruleUids = ruleUids;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSettled() {
        return settled;
    }

    public void setSettled(boolean settled) {
        this.settled = settled;
    }

    public URI getRequestingModUid() {
        return requestingModUid;
    }

    public void setRequestingModUid(URI requestingModUid) {
        this.requestingModUid = requestingModUid;
    }

    public String getX0() {
        return x0;
    }

    public void setX0(String x0) {
        this.x0 = x0;
    }

    public String getNibble6() {
        return nibble6;
    }

    public void setNibble6(String nibble6) {
        this.nibble6 = nibble6;
    }

    public boolean isOverride() {
        return override;
    }

    public void setOverride(boolean override) {
        this.override = override;
    }

    public void blankForStorage(){
        this.nibble6 = null;
        this.x0 = null;

    }

    @Override
    public String toString() {
        return nibble6 + " " + this.x0 + " " + this.timestamp;
    }
}
