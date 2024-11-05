package io.exonym.actor.actions;

import io.exonym.abc.util.JaxbHelper;
import io.exonym.lite.pojo.Rulebook;
import io.exonym.lite.standard.Const;
import io.exonym.lite.standard.WhiteList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class MyTrustNetworks {

    private final MyTrustNetworkAndKeys lead;
    private final MyTrustNetworkAndKeys moderator;

    private Rulebook rulebook;

    private static final Logger logger = LogManager.getLogger(MyTrustNetworks.class);


    public MyTrustNetworks() {
        lead = openTrustNetwork(true);
        moderator = openTrustNetwork(false);
        Path toRb = Path.of(Const.PATH_OF_STATIC, Const.RULEBOOK_JSON);
        try {
            this.rulebook = JaxbHelper.jsonToClass(Files.readString(toRb), Rulebook.class);

        } catch (Exception e) {
            logger.warn("---------------- Unable to open rulebook ----------------");

        }
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

    public Rulebook getRulebook() {
        return rulebook;
    }

}
