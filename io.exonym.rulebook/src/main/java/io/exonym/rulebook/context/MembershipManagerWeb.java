package io.exonym.rulebook.context;

import io.exonym.actor.actions.MembershipManager;
import io.exonym.actor.actions.IdContainerJSON;
import io.exonym.actor.actions.MyTrustNetworks;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.rulebook.schema.IdContainer;
import io.exonym.utils.storage.TrustNetwork;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class MembershipManagerWeb extends MembershipManager {
    
    private static final Logger logger = LogManager.getLogger(MembershipManagerWeb.class);

    private static MembershipManagerWeb instance;

    static {
        try {
            MyTrustNetworks myTrustNetworks = new MyTrustNetworks();
            if (myTrustNetworks.isModerator()){
                TrustNetwork tn = myTrustNetworks.getModerator().getTrustNetwork();
                instance = new MembershipManagerWeb(tn.getNodeInformation().getNodeName());

            } else {
                throw new UxException(ErrorMessages.INCORRECT_MODERATOR, "This node is not a mod");

            }
        } catch (Exception e) {
            logger.error("Critical Error: FAILED TO START MEMBERSHIP MANAGER", e);

        }
    }

    protected MembershipManagerWeb(String networkName) throws Exception {
        super(networkName);

    }

    @Override
    protected IdContainerJSON initializeContainer(String username) throws Exception {
        return new IdContainer(username);

    }

    public static MembershipManagerWeb getInstance() {
        return instance;
    }


}
