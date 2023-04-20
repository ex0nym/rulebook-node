package io.exonym.rulebook.schema;

import io.exonym.utils.storage.AbstractXContainer;
import io.exonym.utils.storage.CacheContainer;

import java.io.FileNotFoundException;
import java.net.URI;

public class CacheNodeContainer implements CacheContainer {

    private static CacheNodeContainer instance;

    static {
        try {
            instance = new CacheNodeContainer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private XNodeContainer x;

    public static CacheNodeContainer getInstance(){
        return instance;
    }

    private CacheNodeContainer() throws Exception {
        try {
            x = new XNodeContainer("cache", true);

        } catch (Exception e) {
            x = new XNodeContainer("cache", false);

        }
    }

    @Override
    public <T> T open(URI material) throws Exception {
        try {
            return x.openResource(material);

        } catch (FileNotFoundException e) {
            return null;

        }
    }

    @Override
    public <T> T open(String filename) throws Exception {
        try {
            return x.openResource(filename);

        } catch (FileNotFoundException e) {
            return null;

        }
    }

    @Override
    public void store(Object material) throws Exception {
        x.saveLocalResource(material, true);

    }

    public void clear() throws Exception {
        x.delete();
        x = new XNodeContainer("cache", true);

    }

    @Override
    public AbstractXContainer getContainer() throws Exception {
        return x;
    }
}
