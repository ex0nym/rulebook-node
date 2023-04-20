package io.exonym.x0basic;

import com.cloudant.client.api.Database;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.lite.couchdb.AbstractCouchDbObject;
import io.exonym.lite.couchdb.ProtectedCouchRepository;
import io.exonym.lite.couchdb.Query;

import java.util.List;

public class CouchRepository<T> extends ProtectedCouchRepository<T> {

    public CouchRepository(Database db, Class<T> clazz) throws Exception {
        super(db, clazz);
    }

    @Override
    protected synchronized void create(AbstractCouchDbObject item) throws Exception {
        super.create(item);
    }

    @Override
    protected synchronized void bulkAdd(List<T> items) throws Exception {
        super.bulkAdd(items);
    }

    @Override
    protected synchronized T read(String _id) throws NoDocumentException {
        return super.read(_id);
    }

    @Override
    protected synchronized List<T> read(Query query, int limit) {
        return super.read(query, limit);
    }

    @Override
    protected synchronized List<T> read(Query query) {
        return super.read(query);
    }

    @Override
    protected synchronized void update(AbstractCouchDbObject item) throws Exception {
        super.update(item);
    }

    @Override
    protected synchronized void delete(AbstractCouchDbObject item) throws Exception {
        super.delete(item);
    }

    @Override
    protected synchronized void ensureFullCommit() {
        super.ensureFullCommit();
    }
}
