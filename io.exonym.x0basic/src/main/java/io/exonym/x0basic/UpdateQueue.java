package io.exonym.x0basic;

import com.google.gson.Gson;
import io.exonym.lite.parallel.ModelCommandProcessor;
import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.ExoNotify;
import io.exonym.lite.standard.WhiteList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

public class UpdateQueue extends ModelCommandProcessor {

    // ack processing
    // own broadcast processing
    // network processing


    private static final Logger logger = LogManager.getLogger(UpdateQueue.class);

    private final Gson gson = new Gson();

    private int joinIndex;
    private final int joinSize;

    private final ArrayList<ArrayBlockingQueue<Msg>> pipesToJoin;
    private final ArrayBlockingQueue<Msg> pipeToViolation;
    private final ArrayBlockingQueue<Msg> pipeToLocalBlob;
    private final ArrayBlockingQueue<Msg> pipeToAck;
    private final URI hostUuid;

    protected UpdateQueue(ArrayList<ArrayBlockingQueue<Msg>> pipesToJoin,
                          ArrayBlockingQueue<Msg> pipeToViolation,
                          ArrayBlockingQueue<Msg> pipeToLocalBlob,
                          ArrayBlockingQueue<Msg> pipeToAck) throws Exception {
        super(Constants.FLUX_CAPACITY, "UpdateQueue", 60000l);
        this.pipesToJoin=pipesToJoin;
        this.pipeToViolation=pipeToViolation;
        this.pipeToLocalBlob=pipeToLocalBlob;
        this.pipeToAck=pipeToAck;
        this.joinSize=this.pipesToJoin.size();
        this.hostUuid = X0Properties.getInstance().getAdvocateUID();

    }

    @Override
    protected void receivedMessage(Msg msg) {
        try {
            if (msg instanceof BroadcastStringIn){
                String received = ((BroadcastStringIn)msg).getValue();

                if (received!=null && received.contains("{")){
                    String length = received.substring(0, received.indexOf('{'));

                    if (WhiteList.isNumbers(length)) {
                        int l = length.length();
                        int m = Integer.parseInt(length);

                        String obj = received.substring(l, l+m);
                        ExoNotify notify = gson.fromJson(obj, ExoNotify.class);
                        distributeNotification(notify);

                    } else {
                        discard(received);

                    }
                } else {
                    discard(received);

                }
            }
        } catch (Exception e) {
            logger.error("Error at UDP In UpdateQueue", e);

        }
    }

    private void distributeNotification(ExoNotify notify) throws Exception {
        String type = notify.getType();
        if (type.equals(ExoNotify.TYPE_JOIN)){
            join(notify);

        } else if (type.equals(ExoNotify.TYPE_VIOLATION)){
            violation(notify);

        } else if (type.equals(ExoNotify.TYPE_SOURCE)){
            ackIn(notify);

        } else if (type.equals(ExoNotify.TYPE_ACK)){
            ackIn(notify);

        }
        // If message originates from own XNode, update blob.
        if (notify.getAdvocateUID().equals(this.hostUuid)){
            if (type.equals(ExoNotify.TYPE_JOIN) || type.equals(ExoNotify.TYPE_VIOLATION)) {
                blob(notify);

            }
        }
    }

    private void join(ExoNotify notify) throws Exception {
        int pipe = this.joinIndex % this.joinSize;
        this.pipesToJoin.get(pipe).put(notify);
        this.joinIndex++;

    }

    private void violation(ExoNotify notify) throws Exception {
        this.pipeToViolation.put(notify);

    }

    private void blob(ExoNotify notify) throws Exception {
        this.pipeToLocalBlob.put(notify);

    }

    private void ackIn(ExoNotify notify) throws Exception {
        this.pipeToAck.put(notify);

    }

    private void ackOut(ExoNotify notify) throws Exception {
        this.pipeToAck.put(notify);

    }

    private void discard(String received) {
        logger.info("Discarding Message: " + received);

    }

    @Override
    protected ArrayBlockingQueue<Msg> getPipe() {
        return super.getPipe();
    }

    @Override
    protected synchronized boolean isBusy() {
        return super.isBusy();
    }

    @Override
    protected void close() throws Exception {
        super.close();
    }

    @Override
    protected void periodOfInactivityProcesses() {

    }

}
