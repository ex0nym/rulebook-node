package io.exonym.lite.pojo;

import org.joda.time.DateTime;

import java.net.URI;
import java.util.ArrayList;

public class ExonymDetailedResult {

    // repeat offences
    // time ban effective until
    //
    boolean unsettled = false;
    boolean overridden = false;
    private final ArrayList<URI> unsettledRuleId = new ArrayList<>();
    private int offences;
    private URI modUID;
    private DateTime lastViolationTime;
    private final ArrayList<URI> activeControlledRules = new ArrayList<>();
    private final ArrayList<URI> activeUncontrolledRules = new ArrayList<>();


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

    public boolean isOverridden() {
        return overridden;
    }

    public void setOverridden(boolean overridden) {
        this.overridden = overridden;
    }

    public ArrayList<URI> getActiveControlledRules() {
        return activeControlledRules;
    }

    public ArrayList<URI> getActiveUncontrolledRules() {
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

    public ArrayList<URI> getUnsettledRuleId() {
        return unsettledRuleId;
    }
}
