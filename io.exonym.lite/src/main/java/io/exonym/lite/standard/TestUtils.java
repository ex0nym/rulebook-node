package io.exonym.lite.standard;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class TestUtils {


    public static ArrayList<String> fakePseudonyms(int size){
        return fakePseudonyms(size, null);

    }

    public static ArrayList<String> fakePseudonyms(int size, String forceRoot){
        HashSet<String> start = new HashSet<>();
        Random rnd = new Random();
        boolean force = forceRoot!=null;
        while (start.size()<size){
            byte[] one = new byte[512];
            rnd.nextBytes(one);
            String nym = CryptoUtils.toHex(one);
            start.add(nym);

        }
        ArrayList<String> list = new ArrayList<>();
        for (String n0 : start){
            if (force){
                // logger.debug("Forcing Root " + forceRoot);
                n0 = forceRoot + n0.substring(0, n0.length()-forceRoot.length());
                force = false;

            }
            list.add(n0);
            // logger.debug(n0);

        }
        return list;

    }


}
