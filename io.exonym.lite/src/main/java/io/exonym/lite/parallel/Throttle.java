package io.exonym.lite.parallel;

import io.exonym.lite.time.Timing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Throttle {

    private static final Logger logger = LogManager.getLogger(Throttle.class);
    private long last = Timing.currentTime();
    private long pause = 0;

    public synchronized void setThrottle(long ms){
        logger.warn("GLOBAL THROTTLE of "+ms+
                "ms. This is a singleton object and so any throttle is applied anywhere where this object is callled.");
        this.pause = ms;

    }

    public synchronized  void throttle(){
        if (pause==0){
            return;

        }
        synchronized (this){
            long since = Timing.hasBeenMs(last);
            if (since < pause){
                try {
                    this.wait(pause - since);

                } catch (InterruptedException e) {
                    logger.info("Interrupt");

                }
            }
        }
        last = Timing.currentTime();

    }

    private static Throttle instance;

    static {
        try {
            instance = new Throttle();

        } catch (Exception e){
            logger.error("Initialize Error on Throttle", e);

        }
    }

    public static Throttle getInstance(){
        return instance;

    }
}
