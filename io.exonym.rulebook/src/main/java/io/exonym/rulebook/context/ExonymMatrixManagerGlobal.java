package io.exonym.rulebook.context;


import io.exonym.actor.actions.ExonymMatrix;
import io.exonym.actor.storage.Poke;
import io.exonym.lite.connect.UrlHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.*;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.utils.storage.KeyContainerWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


public class ExonymMatrixManagerGlobal extends ExonymMatrixManagerAbstract {
    
    private static final Logger logger = LogManager.getLogger(ExonymMatrixManagerGlobal.class);

    // initialSpawning  - as part of the network, a signature on a timestamp allows request of the reduced map.

    // source adds a host

    // find an exonym
    // check for violations
    // prepare requester for response

    // source removes a host

    // host becomes corrupt

    private final NetworkMapItemModerator targetAdvocate;
    private final String nibble6;
    private final AsymStoreKey signatureKey = AsymStoreKey.blank();
    private final NetworkMapItemModerator myAdvocate;


    private final String root;

    public ExonymMatrixManagerGlobal(NetworkMapItemModerator targetAdvocate, NetworkMapItemModerator nmiaForMyAdvocate, String nibble6, String root) throws Exception {
        this.targetAdvocate=targetAdvocate;
        this.nibble6=nibble6;
        this.myAdvocate = nmiaForMyAdvocate;
        this.root=root;
        this.signatureKey.assembleKey(targetAdvocate.getPublicKeyB64());

    }

    protected ExonymMatrix openUncontrolledList(String x0) throws Exception {
        try {
            String path = computeN6PathToFile(nibble6.substring(0, 3), nibble6, this.yList);
            String target = primaryEndPoint(this.myAdvocate.getStaticURL0().toString(), path);
            this.matrixByteData = UrlHelper.read(new URL(target));
            openExonymMatrix(myAdvocate.getStaticURL0().toString(), x0, this.xList, this.root);
            return matrix;

        } catch (IOException e) {
            throw new HubException("No Y-List Matrix for " + x0 + " on " + this.targetAdvocate.getNodeUID());

        }
    }

    protected ExonymDetailedResult detailedResult(String x0) throws Exception {
        // discover x-list
        openExonymMatrix(targetAdvocate.getStaticURL0().toString(), x0, this.xList, this.root);
        ExonymMatrixRow row = matrix.findExonymRow(x0);
        ArrayList<String> rules = matrix.getRuleUrns();
        matrix = null;
        logger.debug("Should not be null: " + row);
        logger.debug("Should not be null: " + rules);
        ArrayList<Violation> violations = row.getViolations();

        ExonymDetailedResult result = new ExonymDetailedResult();
        result.setAdvocateUID(this.targetAdvocate.getNodeUID());
        analyseViolations(violations, result);
        analyseRow(row, rules, result);
        return result;

    }

    private void analyseViolations(ArrayList<Violation> violations, ExonymDetailedResult result) {
        ArrayList<DateTime> violationTime = new ArrayList<>();
        if (violations!=null && !violations.isEmpty()){
            for (Violation v : violations){
                if (!v.isSettled() && !result.isUnsettled()){
                    result.setUnsettled(true);
                    result.setUnsettledRuleId(v.getRuleUrn());

                }
                violationTime.add(new DateTime(v.getTimestamp()));
                result.incrementOffences();

            }
            Collections.sort(violationTime);
            DateTime last = violationTime.get(violationTime.size()-1);
            result.setLastViolationTime(last);

        }
    }

    private void analyseRow(ExonymMatrixRow row, ArrayList<String> rules, ExonymDetailedResult result) {
        // The user may have quit after a violation and before it was reported.
        if (row.isQuit()){
            result.setQuit(true);

        } else {
            // If they've not quit, and there are no unsettled violations, then they are a member
            if (!result.isUnsettled()){
                ArrayList<String> cont = result.getActiveControlledRules();
                ArrayList<String> uncont = result.getActiveUncontrolledRules();
                HashMap<String, String> xiToRj = mapXiToRj(rules, row.getExonyms());

                for (String xn : row.getExonyms()){
                    if (xn.equals("null")){
                        uncont.add(xiToRj.get(xn));

                    } else {
                        cont.add(xiToRj.get(xn));

                    }
                }
            }
        }
    }

    private HashMap<String, String> mapXiToRj(ArrayList<String> rules, ArrayList<String> exonyms) {
        HashMap<String, String> xiToRj = new HashMap<>();
        int i = 0;
        for (String rj : rules){
            String xi = exonyms.get(i);
            xiToRj.put(xi, rj);
            i++;

        }
        return xiToRj;

    }

    @Override
    protected void authenticate(String root, Poke poke, KeyContainerWrapper kcw,
                                ExonymMatrix matrix, String nibble3,
                                String nibble6) throws Exception {
        authPoke(poke, signatureKey);
        authSigs(poke, nibble3, signatureKey);
        authMatrix(kcw, nibble6, signatureKey);
        this.poke = poke;
        this.kcw = kcw;
        this.matrix = matrix;
        this.matrixByteData = null;
        this.signatureByteData = null;

    }

    @Override
    protected Poke handlePokeNotFound(Exception e) throws Exception {
        throw new UxException(ErrorMessages.POKE_NOT_FOUND, e);
    }

    @Override
    protected KeyContainerWrapper handleKeyContainerNotFound(Exception e) throws Exception {
        throw new UxException(ErrorMessages.SIGNATURES_NOT_FOUND);
    }

    @Override
    protected ExonymMatrix handleMatrixNotFound(Exception e, String n6) throws Exception {
        throw new UxException(ErrorMessages.NIBBLE6_NOT_FOUND);
    }

    public static void main(String[] args) throws Exception {

    }
}
