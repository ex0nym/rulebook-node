package io.exonym.actor.actions;

public class MyTrustNetworks {

    private final MyTrustNetworkAndKeys lead;
    private final MyTrustNetworkAndKeys moderator;


    public MyTrustNetworks() {
        lead = openTrustNetwork(true);
        moderator = openTrustNetwork(false);
    }

    private MyTrustNetworkAndKeys openTrustNetwork(boolean isLead) {
        try {
            return new MyTrustNetworkAndKeys(isLead);

        } catch (Exception e) {
            return null;

        }
    }

    public MyTrustNetworkAndKeys getLead() {
        return lead;
    }

    public MyTrustNetworkAndKeys getModerator() {
        return moderator;
    }

    public MyTrustNetworkAndKeys getOnePrioritizeModerator() {
        return moderator!=null ? moderator : lead;
    }

    public boolean isLeader() {
        return this.lead != null;
    }

    public boolean isModerator() {
        return moderator != null;
    }

    public boolean isDefined() {
        return moderator != null || lead != null;
    }



}
