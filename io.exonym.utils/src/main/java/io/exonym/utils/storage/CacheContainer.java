package io.exonym.utils.storage;

import java.net.URI;

public interface CacheContainer {

    public <T> T open(URI material) throws Exception;
    public <T> T open(String filename) throws Exception;

    public void store(Object material) throws Exception;
    public void clear() throws Exception;

    public AbstractXContainer getContainer() throws Exception;


}
