package io.exonym.lite.standard;

import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;

import java.util.HashMap;

public class Forms {

    public static String extractMandatory(HashMap<String, String> in, String target) throws UxException {
        String result = in.get(target);
        if (result!=null){
            return result;

        } else {
            throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, target);

        }
    }

    public static boolean mustAgree(HashMap<String, String> in, String target) throws UxException {
        String result = in.get(target);
        if (result!=null){
            if (result.equals("true")){
                return true;

            } else {
                throw new UxException(ErrorMessages.AGREE_TO_TERMS, target);

            }
        } else {
            throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, target);


        }
    }

}
