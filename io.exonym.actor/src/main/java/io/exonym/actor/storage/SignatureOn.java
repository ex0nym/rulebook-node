package io.exonym.actor.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

public class SignatureOn {
    
    private static final Logger logger = LogManager.getLogger(SignatureOn.class);
    

    public static byte[] rules(JsonArray ruleset, String t){
        ArrayList<String> set = new ArrayList<>();
        for (JsonElement e : ruleset){
            set.add(e.getAsString());

        }
        return rules(set, t);

    }

    public static byte[] rules(ArrayList<String> ruleset, String t){
        String toSign = "";
        for (String rule : ruleset) {
            toSign += rule;

        }
        toSign += t;
        logger.debug(toSign);
        return toSign.getBytes(StandardCharsets.UTF_8);

    }

    public static byte[] device(String appUuid, String t){
        return (appUuid + ":" + t).getBytes(StandardCharsets.UTF_8);

    }

    public static byte[] poke(Poke poke) {
        String t = poke.getT();
        ArrayList<String> nibbles = new ArrayList<>();
        nibbles.addAll(poke.getSignatures().keySet());
        Collections.sort(nibbles);
        for (String n6 : nibbles){
            if (n6.length()==6){
                t += n6;

            }
        }
        return t.getBytes(StandardCharsets.UTF_8);

    }
}
