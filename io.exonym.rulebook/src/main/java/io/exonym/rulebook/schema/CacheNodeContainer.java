package io.exonym.rulebook.schema;

import io.exonym.utils.storage.AbstractIdContainer;
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

    private IdContainer x;

    public static CacheNodeContainer getInstance(){
        return instance;
    }

    private CacheNodeContainer() throws Exception {
        try {
            x = new IdContainer("cache", true);

        } catch (Exception e) {
            x = new IdContainer("cache", false);

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
        x = new IdContainer("cache", true);

    }

    @Override
    public AbstractIdContainer getContainer() throws Exception {
        return x;
    }
}
