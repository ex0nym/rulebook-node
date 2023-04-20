package io.exonym.x0basic;

import io.exonym.lite.parallel.ModelCommandProcessor;
import io.exonym.lite.parallel.Msg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Resender extends ModelCommandProcessor {
    
    private static final Logger logger = LogManager.getLogger(Resender.class);

    protected Resender() {
        super(1, "Resender", 10l * 60l * 1000l);
    }

    @Override
    protected void periodOfInactivityProcesses() {
        try (ResendProcessor resender = new ResendProcessor()){
            resender.execute();

        } catch (Exception e){
            logger.debug("Resender Exception===" + e.getMessage());

        }
    }

    @Override
    protected void receivedMessage(Msg msg) {

    }

    @Override
    protected void close() throws Exception {
        super.close();
    }
}
