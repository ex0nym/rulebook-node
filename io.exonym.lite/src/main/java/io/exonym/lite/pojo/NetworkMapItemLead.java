package io.exonym.lite.pojo;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class NetworkMapItemLead extends NetworkMapItem {

    private boolean defaultAllow = true;

    private HashSet<URI> moderatorsForLead = new HashSet<>();

    public boolean isDefaultAllow() {
        return defaultAllow;
    }

    public void setDefaultAllow(boolean defaultAllow) {
        this.defaultAllow = defaultAllow;
    }


    public HashSet<URI> getModeratorsForLead() {
        return moderatorsForLead;
    }

    public void setModeratorsForLead(HashSet<URI> moderatorsForLead) {
        this.moderatorsForLead = moderatorsForLead;
    }
}
