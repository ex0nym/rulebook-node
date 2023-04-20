package io.exonym.lite.pojo;

public class Violation {

//    {rule:"r0", t:"2021-09-01T13:00:00", settled:true}
     private String ruleUrn;
     private String timestamp;
     private boolean settled;

    public String getRuleUrn() {
        return ruleUrn;
    }

    public void setRuleUrn(String ruleUrn) {
        this.ruleUrn = ruleUrn;
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
}
