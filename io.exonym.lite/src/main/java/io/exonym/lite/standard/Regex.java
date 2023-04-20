package io.exonym.lite.standard;

import java.util.regex.Pattern;

public class Regex {

    public static Pattern forInterpretationInRule(){
        return Pattern.compile("_\\[\\d\\]([\\w\\s;:',\\.]+?)_");

    }

}
