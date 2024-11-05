package io.exonym.rulebook.context;

import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.actor.actions.MyTrustNetworks;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.parallel.ModelCommandProcessor;
import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.ExoMatrix;
import io.exonym.lite.pojo.ExoNotify;
import io.exonym.lite.pojo.Vio;
import io.exonym.lite.standard.Const;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class ExoMatrixWriter extends ModelCommandProcessor {

    private static final Logger logger = LogManager.getLogger(ExoMatrixWriter.class);
    private final ArrayList<ExoMatrix> toWrite = new ArrayList<>();
    private final CouchRepository<ExoMatrix> exonymMap;
    private final CouchRepository<Vio> vioRepo;
    private final QueryBasic queryExo = new QueryBasic();
    private final MyTrustNetworks myTrustNetworks;

    private final ExoMatrix myModTmpMatrix;


    protected ExoMatrixWriter(MyTrustNetworks myTrustNetworks) throws Exception {
        super(Const.FLUX_CAPACITY, "ExoMatrixWriter", 1000);
        this.exonymMap = CouchDbHelper.repoExoMatrix();
        this.vioRepo = CouchDbHelper.repoVio();
        this.myTrustNetworks = myTrustNetworks;
        if (myTrustNetworks.isModerator()){
            URI myMod = myTrustNetworks.getModerator()
                    .getTrustNetwork()
                    .getNodeInformation()
                    .getNodeUid();
            myModTmpMatrix = ExoMatrix.withModUid(myMod);

        } else {
            myModTmpMatrix = ExoMatrix.withModUid(URI.create("urn:not:a:mod"));

        }
    }

    @Override
    protected void receivedMessage(Msg msg) {
        if (msg instanceof ExoNotify) {
            ExoNotify notify = (ExoNotify) msg;
            try {
                boolean wasModeratedByMe = writeExoAndDetectMyself(notify);
                Vio vio = resolveVio(notify);

                if (vio!=null && wasModeratedByMe){
                    resolveLocalExonymMatrix(notify, vio);
                }
            } catch (Exception e) {
                logger.error("Error", e);

            }
        }
    }

    private void resolveLocalExonymMatrix(ExoNotify notify, Vio vio) {
        try {
            JoinSupportSingleton join = JoinSupportSingleton.getInstance();
            ExonymMatrixManagerLocal localMatrix = join.getExonymMatrixManagerLocal();
            localMatrix.resolveViolation(vio, false);

        } catch (Exception e) {
            logger.info("Error", e);

        }
    }

    private Vio resolveVio(ExoNotify notify) {
        if (notify.getTimeOfViolation()!=null){
            try {
                List<Vio> vios = vioRepo.read(queryExo);
                String tOfVio = notify.getTimeOfViolation();
                Vio resolved = null;

                for (Vio vio : vios){
                    if (vio.getTimeOfViolation().equals(tOfVio)){
                        resolved = vio;
                        break;
                    }
                }
                resolved.setReissued(true);
                vioRepo.update(resolved);
                return resolved;

            } catch (Exception e) {
                logger.warn("There are no violations for this x0Hash "
                        + notify.getHashOfX0(), e);
                return null;

            }
        } else {
            logger.debug("Join @ resolveViolation and not Rejoin");
            return null;

        }
    }

    private boolean writeExoAndDetectMyself(ExoNotify notify) throws Exception {
        int indexMyMod = -1;
        try {
            HashMap<String, String> selector = queryExo.getSelector();
            selector.put(ExoMatrix.FIELD_NIBBLE6, notify.getNibble6());
            List<ExoMatrix> matrix = this.exonymMap.read(queryExo, 25);
            for (ExoMatrix m : matrix){
                logger.debug("Detect myself " + m.getNibble6() + " " + m.getModUid() + " " + m.equals(myModTmpMatrix) );
                logger.debug("MyMod=" + myModTmpMatrix.getModUid());
            }
            if (!matrix.isEmpty()){
                ExoMatrix sender = ExoMatrix.withModUid(notify.getNodeUid());
                int index = matrix.indexOf(sender);
                indexMyMod = matrix.indexOf(myModTmpMatrix);

                logger.debug("Found index for existing matrix at writeExo(): " + index);
                logger.debug("Found index for my node (): " + indexMyMod);

                if (index>-1){
                    ExoMatrix matrixMod = matrix.get(index);
                    logger.debug("Found matrix for mod = " + matrixMod.toString());
                    boolean joiningSameNode = matrixMod.getX0Hash()
                            .contains(notify.getHashOfX0());

                    if (!joiningSameNode){
                        logger.warn("x0' was not in the Exonym Map and should have been - adding.");
                        matrixMod.getX0Hash().add(notify.getHashOfX0());
                        exonymMap.update(matrixMod);

                    }
                } else {
                    throw new NoDocumentException("See Below");

                }
            } else {
                throw new NoDocumentException("See Below");

            }
            return indexMyMod > -1;

        } catch (NoDocumentException e) {
            ExoMatrix matrix = new ExoMatrix();
            matrix.setNibble6(notify.getNibble6());
            matrix.setModUid(notify.getNodeUid());
            matrix.getX0Hash().add(notify.getHashOfX0());
            matrix.set_id(matrix.index());

            this.toWrite.add(matrix);
            if (this.toWrite.size()>501){
                bulkAdd();

            }
            return indexMyMod > -1;

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
