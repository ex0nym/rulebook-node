package io.exonym.actor.actions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.exonym.lite.standard.TestUtils;
import io.exonym.lite.time.DateHelper;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.pojo.ExonymMatrixRow;
import io.exonym.lite.pojo.Violation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class ExonymMatrix {


    private static final Logger logger = LogManager.getLogger(ExonymMatrix.class);
    private String rootRule;
    private ArrayList<String> ruleUrns = null;
    private HashMap<String, ExonymMatrixRow> matrix = new HashMap<>();
    private String nibble3, nibble6;

    /**
     * This class represents a single nibble6.json file
     *
     * it does not manage file writing, or signing.
     *  The ExonymMatrixManager does that.
     *
     * @param nibble6
     * @throws Exception
     */
    private ExonymMatrix(String nibble6) throws Exception {
        if (nibble6!=null && nibble6.length()==6){
            this.nibble3 = nibble6.substring(0,3);
            this.nibble6 = nibble6;

        } else {
            throw new Exception("Invalid Nibble Size - should be 6 was" + nibble6.length());

        }
    }

    public ExonymMatrixRow findExonymRow(String exonym){
        int len = exonym.getBytes(StandardCharsets.UTF_8).length;
        logger.debug(len);
        if (exonym!=null){

            if (exonym.length()!=64){
                exonym = CryptoUtils.computeSha256HashAsHex(exonym);

            }
            logger.debug(exonym);
            return this.matrix.get(exonym);

        } else {
            throw new NullPointerException();

        }
    }

    public void addExonymRow(String x0, ExonymMatrixRow row){
        if (row!=null && row.getExonyms()!=null && !row.getExonyms().isEmpty()){
            String root = CryptoUtils.computeSha256HashAsHex(x0);
            this.matrix.put(root, row);

        } else {
            throw new NullPointerException();

        }
    }

    public void removeExonymRow(ExonymMatrixRow remove){
        if (remove!=null && remove.getExonyms()!=null && !remove.getExonyms().isEmpty()){
            String x0 = remove.getExonyms().get(0);
            String root = CryptoUtils.computeSha256HashAsHex(x0);
            ExonymMatrixRow row = this.matrix.get(root);
            row.setQuit(true);

        } else {
            throw new NullPointerException();

        }
    }

    public String getRuleDidForIndex(int i){
        return ruleUrns.get(i);

    }

    public String toJson(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this, ExonymMatrix.class);

    }

    public static ExonymMatrix init(ArrayList<String> ruleDids, String nibble6) throws Exception {
        if (ruleDids!=null && !ruleDids.isEmpty()){
            ExonymMatrix result = new ExonymMatrix(nibble6);
            result.rootRule=ruleDids.get(0);
            result.ruleUrns =ruleDids;
            return result;

        } else {
            throw new NullPointerException();

        }
    }

    public static ExonymMatrix init(ExonymMatrix matrix, String nibble6) throws Exception {
        if (matrix!=null && matrix.rootRule!=null && matrix.ruleUrns !=null && !matrix.ruleUrns.isEmpty()){
            ExonymMatrix result = new ExonymMatrix(nibble6);
            result.ruleUrns = matrix.ruleUrns;
            result.rootRule = matrix.rootRule;

            return result;

        } else {
            throw new NullPointerException();

        }
    }

    public String getRootRule() {
        return rootRule;
    }


    public HashMap<String, ExonymMatrixRow> getMatrix() {
        return matrix;
    }

    public void setMatrix(HashMap<String, ExonymMatrixRow> matrix) {
        this.matrix = matrix;
    }

    public String getNibble3() {
        return nibble3;
    }

    public String getNibble6() {
        return nibble6;
    }

    //    {
    //        rules:["r0", "r1", "r2"],
    //        aaaa1..x0:
    //              {exonyms: [x0, x1, x2], t:"2021-02-01T12:32:12Z",
    //                  violations: [{rule:"r0", t:"2021-09-01T13:00:00", settled:true}]
    //               },
    //        aaaa0..x0:{exonyms: [null, null, x2]},
    //    }


    public static void main(String[] args) throws Exception {
        String base = "urn:exonym:" + UUID.randomUUID();
        String r0 = base + ":r0";
        String r1 = base + ":r1";
        String r2 = base + ":r2";

        ArrayList<String> nymSet0 = TestUtils.fakePseudonyms(3, null);
        String nibble6 = nymSet0.get(0).substring(0,6);
        logger.debug(nibble6);
        ArrayList<String> ruleSet = new ArrayList<>();
        ruleSet.add(r0);
        ruleSet.add(r1);
        ruleSet.add(r2);

        ExonymMatrix m0 = ExonymMatrix.init(ruleSet, nibble6);

        ExonymMatrixRow row = new ExonymMatrixRow();
        row.setExonyms(nymSet0);
        m0.addExonymRow(nymSet0.get(0), row);

        logger.debug(m0.toJson());

        Violation v0 = new Violation();
        v0.setTimestamp(DateHelper.currentIsoUtcDateTime());
        v0.setRuleUrn(r2);

        ExonymMatrixRow r = m0.findExonymRow(nymSet0.get(0));
        r.getViolations().add(v0);

        logger.debug(m0.toJson());

        m0.removeExonymRow(row);

        logger.debug(m0.toJson());

    }

    public ArrayList<String> getRuleUrns() {
        return ruleUrns;
    }
}
