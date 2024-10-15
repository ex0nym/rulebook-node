package io.exonym.rulebook.context;

import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.actor.actions.MyTrustNetworks;
import io.exonym.lite.parallel.ModelCommandProcessor;
import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.ExoMatrix;
import io.exonym.lite.pojo.ExoNotify;
import io.exonym.lite.pojo.Vio;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;

public class OverrideResolver extends ModelCommandProcessor {
    
    private static final Logger logger = LogManager.getLogger(OverrideResolver.class);

    private final CouchRepository<ExoMatrix> exonymMap;
    private final CouchRepository<Vio> violations;

    private final MyTrustNetworks myTrustNetworks;

    private final URI myModUid;
    private final URI myLeadUid;

    protected OverrideResolver(MyTrustNetworks myTrustNetworks) throws Exception {
        super(10, "OverrideResolver", 60000l);
        this.exonymMap = CouchDbHelper.repoExoMatrix();
        this.violations = CouchDbHelper.repoVio();
        this.myTrustNetworks = myTrustNetworks;
        if (myTrustNetworks.isModerator()){
            myModUid = this.myTrustNetworks.getModerator()
                    .getTrustNetwork().getNodeInformation().getNodeUid();

        } else {
            myModUid = URI.create("urn:nom");
        }
        if (myTrustNetworks.isLeader()){
            myLeadUid = this.myTrustNetworks.getLead()
                    .getTrustNetwork().getNodeInformation().getNodeUid();

        } else {
            myLeadUid = URI.create("urn:nol");

        }
    }


    // TODO Post: MVP
    // This protocol isn't complete.  It needs to be a request for override;
    // followed by acceptance from the mod
    // then broadcast to update the network's Vios.
    // currently it just updates the local map and the Vio in a single pass.

    // if mine - apply override to ExonymMap (not the Vio in the db)
    // broadcast acceptance of the override as Rejoin
    private void applyOverride(ExoNotify notify) {
        Vio vio = vioFor(notify);
        if (vio!=null){
            if (myModUid.equals(vio.getModOfVioUid())){
                updateViolation(vio);

            }
            updateVio(vio);

        } else {
            logger.warn("No violation for " + notify);

        }
    }

    private void updateViolation(Vio vio) {
        try {
            ExonymMatrixManagerLocal matrixManager = JoinSupportSingleton.getInstance()
                    .getExonymMatrixManagerLocal();
            matrixManager.resolveViolation(vio, true);

        } catch (Exception e) {
            logger.warn("Error at Local Matrix", e);

        }
    }

    private void updateVio(Vio vio) {
        try {
            vio.setOverride(true);
            violations.update(vio);

        } catch (Exception e) {
            logger.warn("Got error from update violation", e);

        }
    }

    private Vio vioFor(ExoNotify notify) {
        try {
            logger.info(notify);
            String id = Vio.index(notify);
            logger.info("Index for vio=" + id);
            return violations.read(id);

        } catch (NoDocumentException e) {
            return null;

        }
    }

    @Override
    protected void receivedMessage(Msg msg) {
        if (msg instanceof ExoNotify){
            applyOverride((ExoNotify)msg);

        } else {
            logger.debug("Got nothing at OverrideResolver " + msg);

        }
    }

    @Override
    protected ArrayBlockingQueue<Msg> getPipe() {
        return super.getPipe();
    }

    @Override
    protected void close() throws Exception {
        super.close();
    }

    @Override
    protected void periodOfInactivityProcesses() {

    }
}
