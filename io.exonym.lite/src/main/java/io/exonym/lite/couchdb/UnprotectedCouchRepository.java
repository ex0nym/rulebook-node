package io.exonym.lite.couchdb;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.query.QueryBuilder;
import com.cloudant.client.api.query.QueryResult;
import com.cloudant.client.org.lightcouch.NoDocumentException;

import java.util.List;

public class UnprotectedCouchRepository<T> extends ProtectedCouchRepository<T> {

    public UnprotectedCouchRepository(Database db, Class clazz) throws Exception {
        super(db, clazz);
    }

    /**
     * Adds a new item to the database and updates this object with the couchdb identifiers.
     *
     * @param item
     * @throws Exception
     */
    @Override
    public synchronized void create(AbstractCouchDbObject item) throws Exception {
        super.create(item);
    }

    @Override
    public synchronized void addIndex(CouchIndex index) {
        super.addIndex(index);
    }

    @Override
    public synchronized void bulkAdd(List items) throws Exception {
        super.bulkAdd(items);
    }

    @Override
    public synchronized List read(String[] _ids) throws Exception {
        return super.read(_ids);
    }

    @Override
    public synchronized Database getDb() {
        return super.getDb();
    }

    @Override
    public synchronized List read(Query query, int limit) {
        return super.read(query, limit);
    }

    @Override
    protected synchronized QueryResult read(QueryBuilder query) {
        return super.read(query);
    }

    @Override
    public synchronized QueryResult read(QueryBuilder query, int limit, int skip) {
        return super.read(query, limit, skip);
    }

    /**
     * @param query
     * @param limit the limit to the number of results returned
     * @param skip  the number of results you want to skip
     * @return
     */
    @Override
    public synchronized List read(Query query, int limit, int skip) {
        return super.read(query, limit, skip);
    }

    @Override
    public synchronized List read(Query query) {
        return super.read(query);
    }

    /**
     * Checks on whether updates are allowed must be performed before
     * calling this method.
     *
     * @param item
     */
    @Override
    public synchronized void update(AbstractCouchDbObject item) throws Exception {
        super.update(item);
    }

    @Override
    public long totalRecords() {
        return super.totalRecords();
    }

    @Override
    public synchronized void delete(AbstractCouchDbObject item) throws Exception {
        super.delete(item);
    }

    @Override
    public synchronized void ensureFullCommit() {
        super.ensureFullCommit();
    }
}
