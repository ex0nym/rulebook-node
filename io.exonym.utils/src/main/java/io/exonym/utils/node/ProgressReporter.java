package io.exonym.utils.node;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class ProgressReporter implements Runnable {

    private static final Logger logger = LogManager.getLogger(ProgressReporter.class);
    private final String[] stages;
    private LinkedList<String> complete = new LinkedList<>();
    private ArrayList<String> pending = new ArrayList<>();
    private Object syncObjIn = new Object();
    private Object syncObjOut = new Object();
    private Exception exception = null;
    private String wrapMessage = null;
    private Thread thread;

    public ProgressReporter(String[] stages) {
        if (stages==null || stages.length==0){
            throw new NullPointerException();

        }
        this.stages = stages;
        for (String item : stages){
            this.pending.add(item);

        }
        thread = new Thread(this, "Progress Reporter");
        thread.start();

    }

    private synchronized void addComplete(String c){
        this.complete.add(c);

    }

    public synchronized String takeComplete(){
        try {
            return this.complete.removeFirst();

        } catch (NoSuchElementException e) {
            return null;

        }
    }

    public String setNextResponse(Object objFromOuterRim) throws Exception {
        if (complete.isEmpty()){
            synchronized (syncObjOut){
                logger.debug("Request for response in");
                syncObjOut.wait(90000);
                logger.debug("Request for response Notify Received or Timeout");

            }
        }
        if (!complete.isEmpty()){
            return this.takeComplete();

        } else {
            return null;

        }
    }

    public ArrayList<String> getPending(){
        return pending;

    }

    public void setComplete(String stage){
        logger.debug("Received STAGE COMPLETE " + stage);
        synchronized (syncObjIn) {
            syncObjIn.notify();

        }
    }

    public synchronized void wrap(String wrap){
        this.wrapMessage = wrap;
        synchronized (syncObjOut){
            syncObjOut.notify();

        }
    }

    public synchronized  boolean isFinished(){
        return this.wrapMessage!=null;

    }

    public String getWrapMessage() {
        return wrapMessage;

    }

    @Override
    public void run() {
        try {
            logger.info("Started monitoring for Progress Updates");
            for (String update: stages){
                synchronized (syncObjIn){
                    logger.info("PROGRESS - Waiting for " + update);
                    syncObjIn.wait();
                    logger.info("PROGRESS - RECEIVED " + update);
                    this.addComplete(update);

                    synchronized (syncObjOut){
                        logger.debug("Sending notify to SYNC OBJECT");
                        syncObjOut.notify();

                    }
                }
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted Progress Monitor - Ended");

        } catch (Exception e) {
            logger.error("Error", e);

        }
    }

    public void close(){
        this.thread.interrupt();

    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
        synchronized (syncObjOut){
            syncObjOut.notify();

        }
    }
}
