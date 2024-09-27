package io.exonym.lite.pojo;

import org.joda.time.DateTime;

import java.net.URI;
import java.util.ArrayList;

public class ExonymDetailedResult {

    // repeat offences
    // time ban effective until
    //
    boolean unsettled = false;
    boolean quit = false;
    private String unsettledRuleId = null;
    private int offences;
    private URI modUID;
    private DateTime lastViolationTime;
    private final ArrayList<String> activeControlledRules = new ArrayList<>();
    private final ArrayList<String> activeUncontrolledRules = new ArrayList<>();

    public String getUnsettledRuleId() {
        return unsettledRuleId;
    }

    public void setUnsettledRuleId(String unsettledRuleId) {
        this.unsettledRuleId = unsettledRuleId;
    }

    public boolean isUnsettled() {
        return unsettled;
    }

    public void setUnsettled(boolean unsettled) {
        this.unsettled = unsettled;
    }

    public int getOffences() {
        return offences;
    }

    public void incrementOffences() {
        this.offences++;

    }

    public boolean isQuit() {
        return quit;
    }

    public void setQuit(boolean quit) {
        this.quit = quit;
    }

    public ArrayList<String> getActiveControlledRules() {
        return activeControlledRules;
    }

    public ArrayList<String> getActiveUncontrolledRules() {
        return activeUncontrolledRules;
    }

    public void setOffences(int offences) {
        this.offences = offences;
    }

    public URI getModUID() {
        return modUID;
    }

    public void setModUID(URI modUID) {
        this.modUID = modUID;
    }

    public DateTime getLastViolationTime() {
        return lastViolationTime;
    }

    public void setLastViolationTime(DateTime lastViolationTime) {
        this.lastViolationTime = lastViolationTime;
    }
}
