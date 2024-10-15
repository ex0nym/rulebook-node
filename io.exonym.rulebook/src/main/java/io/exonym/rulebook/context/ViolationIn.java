package io.exonym.rulebook.context;

import com.cloudant.client.org.lightcouch.DocumentConflictException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.actor.actions.MyTrustNetworks;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.couchdb.QueryStandard;
import io.exonym.lite.parallel.ModelCommandProcessor;
import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.ExoMatrix;
import io.exonym.lite.pojo.ExoNotify;
import io.exonym.lite.pojo.Vio;
import io.exonym.lite.standard.AsymStoreKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

public class ViolationIn extends ModelCommandProcessor {
    
    private static final Logger logger = LogManager.getLogger(ViolationIn.class);


    private final CouchRepository<ExoMatrix> exonymMap;
    private final CouchRepository<Vio> vioMap;

    private final QueryStandard queryNetwork = new QueryStandard();
    private final QueryBasic queryVio = new QueryBasic();
    private final NetworkPublicKeyManager keyManager;

    private final URI myModUid;

    /**
     */
    protected ViolationIn(NetworkPublicKeyManager keyManager, MyTrustNetworks myTrustNetworks) throws Exception {
        super(10, "ViolationIn", 60000);
        this.keyManager=keyManager;
        this.exonymMap = CouchDbHelper.repoExoMatrix();
        this.vioMap = CouchDbHelper.repoVio();
        if (myTrustNetworks.isModerator()){
            myModUid = myTrustNetworks.getModerator()
                    .getTrustNetwork()
                    .getNodeInformation()
                    .getNodeUid();

        } else {
            myModUid = URI.create("urn:not:a:mod");
        }
    }

    @Override
    protected void receivedMessage(Msg msg) {
        try {
            if (msg instanceof ExoNotify){
                ExoNotify notify = (ExoNotify) msg;
                authenticate(notify);
                updateVioMap(notify);

            }
        } catch (Exception e) {
            logger.error("Error", e);

        }
    }

    private void authenticate(ExoNotify notify) throws Exception {
        try {
            AsymStoreKey key = keyManager.getKey(notify.getNodeUid());
            Authenticator.authenticateNotify(notify, key);

        } catch (NoDocumentException e) {
            logger.error("Failed to Find Host on NetworkMap" + notify.getNodeUid());

        } catch (Exception e) {
            throw e;

        }
    }

    private void updateVioMap(ExoNotify notify) throws Exception {
        if (!notify.getNodeUid().equals(myModUid)){
            ArrayList<Vio> vios = notify.getVios();
            for (Vio vio : vios){
                vio.set_id(Vio.index(vio));
                try {
                    vioMap.create(vio);

                } catch (DocumentConflictException e) {
                    Vio dbVio = vioMap.read(vio.get_id());
                    vio.set_rev(dbVio.get_rev());
                    vioMap.update(vio);

                }
            }
        }
    }

    @Override
    protected ArrayBlockingQueue<Msg> getPipe() {
        return super.getPipe();

    }

    @Override
    protected void periodOfInactivityProcesses() {

    }
}
