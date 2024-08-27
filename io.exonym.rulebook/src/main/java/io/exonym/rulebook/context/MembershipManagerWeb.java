package io.exonym.rulebook.context;

import io.exonym.actor.actions.MembershipManager;
import io.exonym.actor.actions.IdContainerJSON;
import io.exonym.rulebook.schema.IdContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class MembershipManagerWeb extends MembershipManager {
    
    private static final Logger logger = LogManager.getLogger(MembershipManagerWeb.class);

    private static MembershipManagerWeb instance;

    static {
        try {
            instance = new MembershipManagerWeb(
                    IAuthenticator.getInstance()
                            .getNetworkNameForNode());

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
