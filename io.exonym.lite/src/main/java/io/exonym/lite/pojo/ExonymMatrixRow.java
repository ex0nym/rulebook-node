package io.exonym.lite.pojo;

import io.exonym.lite.time.DateHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class ExonymMatrixRow {

    private static final Logger logger = LogManager.getLogger(ExonymMatrixRow.class);
    private boolean quit = false;
    private ArrayList<String> exonyms = new ArrayList<>();
    private ArrayList<Violation> violations;

    // Timestamp of the added row
    private String t;

    public ExonymMatrixRow() {
        this.t = DateHelper.currentIsoUtcDateTime();

    }

    public ArrayList<String> getExonyms() {
        return exonyms;
    }

    public void setExonyms(ArrayList<String> exonyms) {
        this.exonyms = exonyms;
    }

    public ArrayList<Violation> getViolations() {
        if (violations==null){
             violations = new ArrayList<>();
        }
        return violations;
    }

    public void setViolations(ArrayList<Violation> violations) {
        this.violations = violations;
    }

    public String getT() {
        return t;
    }

    public void setT(String t) {
        this.t = t;
    }

    public boolean isQuit() {
        return quit;
    }

    public void setQuit(boolean quit) {
        this.quit = quit;
    }

    public void addExonyms(ArrayList<String> nyms) {
        for (String n : nyms){
            logger.debug(n);
            this.exonyms.add(n);

        }
    }
}
