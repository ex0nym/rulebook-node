package io.exonym.lite.parallel;

import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.Form;
import io.exonym.lite.time.Timing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ExpiringHashMap<T, U> extends ModelCommandProcessor {

    private static final Logger logger = LogManager.getLogger(ExpiringHashMap.class);
    private final ConcurrentHashMap<T, U> map = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<T, Long> time = new ConcurrentHashMap<>();

    public ExpiringHashMap(long timeoutMs) {
        super(1, "ExpiringHashMap", timeoutMs);
    }

    public void put(T t, U u){
        logger.info(t + "---t,u---" +u);
        this.map.put(t, u);
        this.time.put(t, Timing.currentTime());

    }

    public U get(T t){
        return this.map.get(t);

    }

    public U remove(T t){
        this.time.remove(t);
        return this.map.remove(t);
    }

    @Override
    protected void periodOfInactivityProcesses() {
        ArrayList<T> tidy = new ArrayList<>();
        for (T t : time.keySet()){
            long t0 = time.get(t);
            if (Timing.hasBeen(t0, this.getTimeoutMs())) {
                map.remove(t);
                tidy.add(t);
            }
        }
        for (T t : tidy){
            time.remove(t);
        }
    }

    @Override
    protected void receivedMessage(Msg msg) {

    }

    public static String generateNonce(int size){
        return Form.toHex(CryptoUtils.generateNonce(size));

    }

    @Override
    public void close() throws Exception {
        super.close();
    }
}
