package io.exonym.actor.actions;

import io.exonym.utils.storage.AbstractXContainer;
import io.exonym.utils.storage.CacheContainer;
import java.io.FileNotFoundException;
import java.net.URI;

public class Cache implements CacheContainer {

    private XContainerJSON x;

    // todo wednesday - make compatible with web containers.

    public Cache() throws Exception {
        try {
            x = new XContainerJSON("cache", true);

        } catch (Exception e) {
            x = new XContainerJSON("cache", false);

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
        x = new XContainerJSON("cache", true);

    }

    @Override
    public AbstractXContainer getContainer() throws Exception {
        return this.x;
    }
}
