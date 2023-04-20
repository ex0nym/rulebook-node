package io.exonym.utils;

import java.util.HashMap;

public class OcrHelper {

    HashMap<String, String> numbers = new HashMap<>();

    public OcrHelper() {
        numbers.put("5", "9");
        numbers.put("9", "5");
    }
}
