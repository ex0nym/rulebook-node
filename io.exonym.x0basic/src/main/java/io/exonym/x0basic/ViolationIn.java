package io.exonym.x0basic;

import com.cloudant.client.org.lightcouch.NoDocumentException;
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

import java.util.concurrent.ArrayBlockingQueue;

public class ViolationIn extends ModelCommandProcessor {
    
    private static final Logger logger = LogManager.getLogger(ViolationIn.class);


    private final ArrayBlockingQueue<Msg> pipeToAck;
    private final CouchRepository<ExoMatrix> exonymMap;
    private final CouchRepository<Vio> vioMap;

    private final QueryStandard queryNetwork = new QueryStandard();
    private final QueryBasic queryVio = new QueryBasic();
    private final KeyManager keyManager;

    /**
     */
    protected ViolationIn(ArrayBlockingQueue<Msg> pipeToAck, KeyManager keyManager) throws Exception {
        super(10, "ViolationIn", 60000);
        this.pipeToAck=pipeToAck;
        this.keyManager=keyManager;
        this.exonymMap = CouchDbHelper.repoExonymMap();
        this.vioMap = CouchDbHelper.repoVioMap();

    }

    @Override
    protected void receivedMessage(Msg msg) {
        try {
            if (msg instanceof ExoNotify){
                ExoNotify notify = (ExoNotify) msg;
                authenticate(notify);
                updateVioMap(notify);
                pipeToAck.put(msg);

            }
        } catch (Exception e) {
            logger.error("Error", e);

        }
    }

    private void authenticate(ExoNotify notify) throws Exception {
        try {
            AsymStoreKey key = keyManager.getKey(notify.getAdvocateUID());
            Authenticator.authenticateNotify(notify, key);

        } catch (NoDocumentException e) {
            logger.error("Failed to Find Host on NetworkMap" + notify.getAdvocateUID());

        } catch (Exception e) {
            throw e;

        }
    }

    private void updateVioMap(ExoNotify notify) throws Exception {
        Vio vio = new Vio();
        vio.setT(notify.getT());
        vio.setAdvocateUID(notify.getAdvocateUID());
        vio.setNibble6(notify.getNibble6());
        vio.setX0Hash(notify.getHashOfX0());
        vioMap.create(vio);

    }

    @Override
    protected ArrayBlockingQueue<Msg> getPipe() {
        return super.getPipe();

    }

    @Override
    protected void periodOfInactivityProcesses() {

    }
}
