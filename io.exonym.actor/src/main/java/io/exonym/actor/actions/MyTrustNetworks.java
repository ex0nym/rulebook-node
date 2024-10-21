package io.exonym.actor.actions;

import io.exonym.lite.standard.WhiteList;

import java.net.URI;

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

    public boolean isMyNode(URI nodeUid){
        if (nodeUid!=null){
            if (WhiteList.isModeratorUid(nodeUid)){
                if (this.isModerator()){
                    URI uid = this.getModerator().getTrustNetwork()
                            .getNodeInformation().getNodeUid();
                    return uid.equals(nodeUid);

                } else {
                    return false;
                }
            } else if (WhiteList.isLeadUid(nodeUid)) {
                if (this.isLeader()){
                    URI uid = this.getLead().getTrustNetwork()
                            .getNodeInformation().getNodeUid();
                    return uid.equals(nodeUid);

                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            throw new NullPointerException();

        }
    }



}
