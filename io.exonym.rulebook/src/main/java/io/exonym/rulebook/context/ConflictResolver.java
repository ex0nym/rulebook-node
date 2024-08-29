package io.exonym.rulebook.context;

import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.couchdb.UnprotectedCouchRepository;
import io.exonym.lite.parallel.ModelCommandProcessor;
import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.ExoMatrix;
import io.exonym.lite.pojo.ExoNotify;
import io.exonym.lite.pojo.Vio;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class ConflictResolver extends ModelCommandProcessor {
    
    private static final Logger logger = LogManager.getLogger(ConflictResolver.class);

    private final UnprotectedCouchRepository<ExoMatrix> exonymMap;
    private final CouchRepository<Vio> violations;

    protected ConflictResolver() throws Exception {
        super(10, "ConflictResolver", 60000l);
        this.exonymMap = CouchDbHelper.repoExoMatrix();
        this.violations = CouchDbHelper.repoVio();

    }

    // Todo cross check that the node isn't cheating.  If so, log and ignore update.
    // It's possible to receive notifications again when an ack fails.
    // Todo What about rejoining at the same node?

    private void resolve(Conflict c) {
        List<ExoMatrix> matrixList = c.getMatrices();
        ExoNotify notify = c.getNotify();
        int index = matrixList.indexOf(notify.getNodeUID());
        List<Vio> violations = vioFor(notify.getHashOfX0());
        // Is the node cheating?
        // Deleting a row in the db
        //
        // Is it just a former violation who is rejoining?



//        if (!matrixList.getX0Hash().contains(notify.getHashOfX0())){
//            matrix.getX0Hash().add(notify.getHashOfX0());
//            this.exonymMap.update(matrix);
//
//        } else {
//            // check violations
//            // if one exists for this node, allow; otherwise ignore
//
//        }

    }

    private List<Vio> vioFor(String hashOfX0) {
        try {
            QueryBasic q = new QueryBasic();
            q.getSelector().put(Vio.FIELD_X0_HASH, hashOfX0);
            return violations.read(q, 100);

        } catch (NoDocumentException e) {
            return Collections.emptyList();

        }
    }

    @Override
    protected void receivedMessage(Msg msg) {
        if (msg instanceof Conflict){
            resolve((Conflict)msg);

        } else {
            logger.debug("Got Crap at ConflictResolver " + msg);

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
