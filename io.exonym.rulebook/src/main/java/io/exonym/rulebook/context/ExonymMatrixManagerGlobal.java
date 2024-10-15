package io.exonym.rulebook.context;


import com.google.gson.Gson;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.actions.ExonymMatrix;
import io.exonym.actor.storage.Poke;
import io.exonym.lite.connect.UrlHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.*;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.utils.storage.KeyContainer;
import io.exonym.utils.storage.KeyContainerWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

    private final NetworkMapItemModerator targetMod;
    private final String nibble6;
    private final AsymStoreKey signatureKey = AsymStoreKey.blank();
    private final NetworkMapItemModerator myMod;


    private final String root;

    public ExonymMatrixManagerGlobal(NetworkMapItemModerator targetMod,
                                     NetworkMapItemModerator nmiaForMyMod, String nibble6, String root) throws Exception {
        this.targetMod =targetMod;
        this.nibble6=nibble6;
        this.myMod = nmiaForMyMod;
        this.root=root;
        this.signatureKey.assembleKey(targetMod.getPublicKeyB64());

    }

    protected ExonymMatrix openUncontrolledList(String x0) throws Exception {
        try {
            String path = computeN6PathToFile(nibble6.substring(0, 3), nibble6, this.yList);
            String target = primaryEndPoint(this.myMod.getStaticURL0().toString(), path);
            this.matrixByteData = UrlHelper.read(new URL(target));
            openExonymMatrix(myMod.getStaticURL0().toString(), x0, this.xList, this.root);
            return matrix;

        } catch (IOException e) {
            throw new HubException("No Y-List Matrix for " + x0 + " on " + this.targetMod.getNodeUID());

        }
    }

    protected ExonymDetailedResult detailedResult(String x0) throws Exception {
        // discover x-list
        openExonymMatrix(targetMod.getStaticURL0().toString(), x0, this.xList, this.root);

        // TODO Recover from node unavailable, with accurate data from `Vio`
        ExonymMatrixRow row = matrix.findExonymRow(x0);
        ArrayList<URI> rules = matrix.getRuleUrns();
        matrix = null;
        logger.debug("detailedResult() row (not null): " + row);
        logger.debug("detailedResult() rules (not null): " + rules);
        ArrayList<Violation> violations = row.getViolations();

        ExonymDetailedResult result = new ExonymDetailedResult();
        result.setModUID(this.targetMod.getNodeUID());
        analyseViolations(violations, result);
        analyseRow(row, rules, result);
        return result;

    }

    private void analyseViolations(ArrayList<Violation> violations, ExonymDetailedResult result) {
        ArrayList<DateTime> violationTime = new ArrayList<>();
        if (violations!=null && !violations.isEmpty()){
            for (Violation v : violations){
                logger.info("Found Violation=" + JaxbHelper.gson.toJson(v));

                if (!(v.isSettled() && result.isUnsettled())){
                    result.setUnsettled(true);
                    result.getUnsettledRuleId().addAll(v.getRuleUids());

                }
                if (v.isOverride()){ // It might be unsettled, but can be overridden by lead
                    result.setUnsettled(false);
                    result.setOverridden(true);

                }
                violationTime.add(new DateTime(v.getTimestamp()));
                result.incrementOffences();

            }
            Collections.sort(violationTime);
            DateTime last = violationTime.get(violationTime.size()-1);
            result.setLastViolationTime(last);

        }
    }

    private void analyseRow(ExonymMatrixRow row, ArrayList<URI> rules, ExonymDetailedResult result) {
        // If they've not quit, and there are no unsettled violations, then they've already joined
        if (!result.isUnsettled()){
            ArrayList<URI> cont = result.getActiveControlledRules();
            ArrayList<URI> uncont = result.getActiveUncontrolledRules();
            HashMap<String, URI> xiToRj = mapXiToRj(rules, row.getExonyms());

            for (String xn : row.getExonyms()){
                if (xn.equals("null")){
                    uncont.add(xiToRj.get(xn));

                } else {
                    cont.add(xiToRj.get(xn));

                }
            }
        }

    }

    private HashMap<String, URI> mapXiToRj(ArrayList<URI> rules, ArrayList<String> exonyms) {
        HashMap<String, URI> xiToRj = new HashMap<>();
        int i = 0;
        for (URI rj : rules){
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
    protected Poke openPoke(String primaryUrl, String xOrYList) throws Exception {
        String path = computePokePathToFile(xOrYList);
        String pokeUrl = primaryEndPoint(primaryUrl, path);
        try {
            String poke = new String(
                    UrlHelper.read(new URL(pokeUrl)),
                    StandardCharsets.UTF_8);
            logger.debug(poke);

            Gson gson = new Gson();
            return gson.fromJson(poke, Poke.class);

        } catch (IOException e) {
            return handlePokeNotFound(e);

        }
    }

    @Override
    protected KeyContainerWrapper openSignatures(String primaryUrl, String nibble3, String xOrYList) throws Exception  {
        String n3Path = computeN3PathToFile(nibble3, xOrYList);
        String sigUrl = primaryEndPoint(primaryUrl, n3Path);
        try {
            this.signatureByteData = UrlHelper.read(new URL(sigUrl));
            String xml = new String(signatureByteData, StandardCharsets.UTF_8);
            logger.debug(xml);
            return new KeyContainerWrapper(
                    JaxbHelper.xmlToClass(xml, KeyContainer.class)

            );
        } catch (Exception e) {
            return handleKeyContainerNotFound(e);

        }
    }

    @Override
    protected ExonymMatrix openTargetMatrix(String primaryUrl, String nibble3, String nibble6, String xOrYList) throws Exception {
        if (this.matrixByteData==null){
            String path = computeN6PathToFile(nibble3, nibble6, xOrYList);
            String target = primaryEndPoint(primaryUrl, path);
            logger.debug(target);
            try {
                this.matrixByteData = UrlHelper.read(new URL(target));
                String json = new String(matrixByteData, StandardCharsets.UTF_8);
                Gson g = new Gson();
                return g.fromJson(json, ExonymMatrix.class);

            } catch (Exception e) {
                return handleMatrixNotFound(e, nibble6);

            }
        } else {
            String json = new String(matrixByteData, StandardCharsets.UTF_8);
            Gson g = new Gson();
            return g.fromJson(json, ExonymMatrix.class);

        }
    }

    @Override
    protected ExonymMatrix handleMatrixNotFound(Exception e, String n6) throws Exception {
        throw new UxException(ErrorMessages.NIBBLE6_NOT_FOUND);
    }

    public static void main(String[] args) throws Exception {

    }


}
