package io.exonym.actor.actions;

import io.exonym.utils.storage.AbstractIdContainer;
import io.exonym.utils.storage.CacheContainer;
import java.io.FileNotFoundException;
import java.net.URI;

public class Cache implements CacheContainer {

    private IdContainerJSON x;

    // todo wednesday - make compatible with web containers.

    public Cache() throws Exception {
        try {
            x = new IdContainerJSON("cache", true);

        } catch (Exception e) {
            x = new IdContainerJSON("cache", false);

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
        x = new IdContainerJSON("cache", true);

    }

    @Override
    public AbstractIdContainer getContainer() throws Exception {
        return this.x;
    }
}
