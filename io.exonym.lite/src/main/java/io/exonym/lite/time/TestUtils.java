package io.exonym.lite.time;

import com.google.gson.JsonObject;
import io.exonym.lite.standard.DirectoryWithFiles;
import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class TestUtils {

    public final static String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    public final static String NUMERIC = "01234567890";
    public final static String ALPHA_NUMERIC = "0123456789001234567890012345678900123456789001" +
            "234567ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    public final static String ALPHA_NUMERIC_SPECIAL = "ABCDEFGHIJ01234567890!@#$%^&*()";

    public static String randomFromString(String seed, int length){
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int rnd = (int) (Math.random() * 100);
            int target = rnd % seed.length();
            result.append(seed.charAt(target));

        }
        return result.toString();

    }

    public static String curlEquivalent(String url, JsonObject json){
        String j = json.toString();
        String prefix = "curl -X POST \"" + url + "\" -H 'ContentType: application/json' -d \"";
        j = j.replaceAll("\"", "'");
        return prefix + j + "\"";

    }

    public static void main(String[] args) throws Exception {
        String kid = System.getenv("SPECTRA_API_KID");
        String key = System.getenv("SPECTRA_API_KEY");
        if (key==null){
            throw new Exception();

        }

        DirectoryWithFiles dir = new DirectoryWithFiles(
                new File("//Users//mikeharris//IdeaProjects//plus.spectra.demo//uploads//demo//source//"),
                ".html");

        File[] files = dir.getFiles();
        System.out.println("files" + files.length);


        for (File file : files){
            try (BufferedReader br = new BufferedReader(new FileReader(file))){
                StringBuilder builder = new StringBuilder();
                String ln = null;
                while((ln=br.readLine())!=null){
                    builder.append(ln);

                }
                // System.out.println(builder.toString());

                JsonObject cmd0 = new JsonObject();
                cmd0.addProperty("cmd", "setup-file");
                cmd0.addProperty("kid", kid);
                cmd0.addProperty("key", key);
                cmd0.addProperty("file", Base64.encodeBase64String(
                        builder.toString().getBytes(StandardCharsets.UTF_8)));

                String curl = TestUtils.curlEquivalent("http://localhost:8081/spectrify", cmd0);
                System.out.println(curl);

            } catch (FileNotFoundException e) {
                System.out.println("Error" + e);

            }
        }
    }
}
