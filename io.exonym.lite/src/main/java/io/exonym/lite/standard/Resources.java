package io.exonym.lite.standard;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Resources {

    public static String getResource(String fileName, ClassLoader classLoader) throws Exception {
        InputStream resource = classLoader.getResourceAsStream(fileName);
        byte[] bytes = new byte[resource.available()];
        resource.read(bytes);
        return new String(bytes, StandardCharsets.UTF_8);

    }
}
