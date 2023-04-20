package io.exonym.lite.standard;

import io.exonym.lite.parallel.ModelCommandProcessor;
import io.exonym.lite.parallel.Msg;
import io.exonym.lite.time.Timing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

public class AbstractCleanUpList<T> extends ModelCommandProcessor {

    private ConcurrentHashMap<T, Long> list = new ConcurrentHashMap<>();
    
    private static final Logger logger = LogManager.getLogger(AbstractCleanUpList.class);

    /**
     */
    public AbstractCleanUpList(long periodMs) {
        super(1, "clean-up", periodMs);
    }

    public void add(T item){
        list.put(item, Timing.currentTime());
    }

    public T removeFromList(T item){
        if (list.remove(item)!=null) {
            return item;

        } else {
            return null;

        }
    }

    @Override
    protected void close() throws Exception {
        list.clear();
        super.close();
    }

    @Override
    protected void periodOfInactivityProcesses() {
        for (T t : list.keySet()){
            long t0 = Long.valueOf(list.get(t));
            if (Timing.hasBeen(t0, this.getTimeoutMs())){
                list.remove(t);
                logger.debug("Removed Item " + t);

            }
        }
    }

    @Override
    protected void receivedMessage(Msg msg) {
        throw new RuntimeException("Not for use");

    }
}
