package io.exonym.x0basic;

import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.lite.parallel.ModelCommandProcessor;
import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.ExoNotify;
import io.exonym.lite.standard.AsymStoreKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;

public class JoinIn extends ModelCommandProcessor {

    private static final Logger logger = LogManager.getLogger(JoinIn.class);
    private final KeyManager keyManager;
    private final ArrayBlockingQueue<Msg> pipeToAck;
    private final ArrayBlockingQueue<Msg> pipeToWriter;

    /**
     *
     */
    protected JoinIn(int threadIndex,
                     ArrayBlockingQueue<Msg> pipeToAck,
                     ArrayBlockingQueue<Msg> pipeToWriter,
                     KeyManager keyManager) throws Exception {
        super(Constants.FLUX_CAPACITY, "JoinIn" + threadIndex, 60000);
        this.pipeToAck=pipeToAck;
        this.pipeToWriter=pipeToWriter;
        this.keyManager=keyManager;

    }

    @Override
    protected void receivedMessage(Msg msg) {
        try {
            if (msg instanceof ExoNotify){
                ExoNotify notify = (ExoNotify) msg;
                authenticate(notify);
                logger.debug(notify);
                this.pipeToWriter.put(msg);
                this.pipeToAck.put(msg);

            }
        } catch (Exception e) {
            StackTraceElement ele = e.getStackTrace()[0];
            logger.debug("Error from - (" + ele.getFileName() + ") line:"
                    + ele.getLineNumber());

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

    @Override
    protected void periodOfInactivityProcesses() {

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
