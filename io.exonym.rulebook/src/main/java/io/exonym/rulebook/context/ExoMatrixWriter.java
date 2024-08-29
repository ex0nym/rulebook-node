package io.exonym.rulebook.context;

import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.couchdb.UnprotectedCouchRepository;
import io.exonym.lite.parallel.ModelCommandProcessor;
import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.ExoMatrix;
import io.exonym.lite.pojo.ExoNotify;
import io.exonym.lite.standard.Const;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class ExoMatrixWriter extends ModelCommandProcessor {

    private static final Logger logger = LogManager.getLogger(ExoMatrixWriter.class);
    private final ArrayList<ExoMatrix> toWrite = new ArrayList<>();
    private final UnprotectedCouchRepository<ExoMatrix> exonymMap;
    private final QueryBasic queryExo = new QueryBasic();
    private final ArrayBlockingQueue<Msg> pipeToConflictResolver;

    protected ExoMatrixWriter(ArrayBlockingQueue<Msg> pipeToConflictResolver) throws Exception {
        super(Const.FLUX_CAPACITY, "ExoMatrixWriter", 1000);
        this.exonymMap = CouchDbHelper.repoExoMatrix();
        this.pipeToConflictResolver = pipeToConflictResolver;

    }

    @Override
    protected void receivedMessage(Msg msg) {
        if (msg instanceof ExoNotify) {
            ExoNotify notify = (ExoNotify) msg;
            try {
                write(notify);

            } catch (Exception e) {
                logger.error("Error", e);

            }
        }
    }

    private void write(ExoNotify notify) throws Exception {
        try {
            HashMap<String, String> selector = queryExo.getSelector();
            selector.put(ExoMatrix.FIELD_NIBBLE6, notify.getNibble6());
            List<ExoMatrix> matrix = this.exonymMap.read(queryExo, 200);
            if (!matrix.isEmpty()){
                Conflict conflict = new Conflict();
                conflict.setMatrices(matrix);
                conflict.setNotify(notify);
                this.pipeToConflictResolver.put(conflict);

            } else {
                throw new NoDocumentException("See Below");

            }
        } catch (NoDocumentException e) {
            ExoMatrix matrix = new ExoMatrix();
            matrix.setNibble6(notify.getNibble6());
            matrix.setHostUuid(notify.getNodeUID());
            matrix.getX0Hash().add(notify.getHashOfX0());
            this.toWrite.add(matrix);
            if (this.toWrite.size()>501){
                bulkAdd();

            }
        } catch (Exception e) {
            throw e;

        }
    }

    @Override
    protected void periodOfInactivityProcesses() {
        if (!toWrite.isEmpty()){
            logger.debug("End Minus 1");
            bulkAdd();

        }
    }

    private void bulkAdd() {
        try {
            exonymMap.bulkAdd(toWrite);
            toWrite.clear();

        } catch (Exception e) {
            logger.error("Failed to bulk write - result size=" + this.toWrite.size());

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
}
