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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ExonymMatrix {


    private static final Logger logger = LogManager.getLogger(ExonymMatrix.class);
    private URI rootRule;
    private ArrayList<URI> ruleUrns = null;
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

    public ExonymMatrixRow findExonymRow(String x0Orx0Hash){
        if (x0Orx0Hash!=null){

            if (x0Orx0Hash.length()!=64){
                x0Orx0Hash = CryptoUtils.computeSha256HashAsHex(x0Orx0Hash);

            }
            logger.debug("x0Hash at findExonymRow()=" + x0Orx0Hash);
            return this.matrix.get(x0Orx0Hash);

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

//    public void removeExonymRow(ExonymMatrixRow remove){
//        if (remove!=null && remove.getExonyms()!=null && !remove.getExonyms().isEmpty()){
//            String x0 = remove.getExonyms().get(0);
//            String root = CryptoUtils.computeSha256HashAsHex(x0);
//            ExonymMatrixRow row = this.matrix.get(root);
//
//        } else {
//            throw new NullPointerException();
//
//        }
//    }

    public URI getRuleDidForIndex(int i){
        return ruleUrns.get(i);

    }

    public String toJson(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this, ExonymMatrix.class);

    }

    public static ExonymMatrix init(ArrayList<URI> ruleDids, String nibble6) throws Exception {
        if (ruleDids!=null && !ruleDids.isEmpty()){
            ExonymMatrix result = new ExonymMatrix(nibble6);
            result.rootRule = ruleDids.get(0);
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

    public URI getRootRule() {
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

    public static String[] extractNibbles(String exonymHex) {
        if (exonymHex==null){
            throw new NullPointerException();

        }
        String nibble3 = exonymHex.substring(0, 3);
        String nibble6 = exonymHex.substring(0, 6);
        return new String[] {nibble3, nibble6};

    }


    //    {
    //        rules:["r0", "r1", "r2"],
    //        aaaa1..x0:
    //              {exonyms: [x0, x1, x2], t:"2021-02-01T12:32:12Z",
    //                  violations: [{rule:"r0", t:"2021-09-01T13:00:00", settled:true}]
    //               },
    //        aaaa0..x0:{exonyms: [null, null, x2]},
    //    }


    public ArrayList<URI> getRuleUrns() {
        return ruleUrns;
    }
}
