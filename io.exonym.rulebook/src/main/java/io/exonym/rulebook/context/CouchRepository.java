package io.exonym.rulebook.context;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.query.QueryBuilder;
import com.cloudant.client.api.query.QueryResult;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.lite.couchdb.AbstractCouchDbObject;
import io.exonym.lite.couchdb.CouchIndex;
import io.exonym.lite.couchdb.ProtectedCouchRepository;
import io.exonym.lite.couchdb.Query;

import java.util.List;

public class CouchRepository<T> extends ProtectedCouchRepository<T> {

    protected CouchRepository(Database db, Class clazz) throws Exception {
        super(db, clazz);
    }

    @Override
    protected synchronized void create(AbstractCouchDbObject item) throws Exception {
        super.create(item);
    }

    @Override
    protected synchronized void bulkAdd(List items) throws Exception {
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
    protected synchronized void addIndex(CouchIndex index) {
        super.addIndex(index);
    }

    @Override
    protected synchronized List<T> read(String[] _ids) throws Exception {
        return super.read(_ids);
    }

    @Override
    protected synchronized QueryResult<T> read(QueryBuilder query) {
        return super.read(query);
    }

    @Override
    protected synchronized QueryResult<T> read(QueryBuilder query, int limit, int skip) {
        return super.read(query, limit, skip);
    }

    @Override
    protected synchronized List<T> read(Query query, int limit, int skip) {
        return super.read(query, limit, skip);
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

    @Override
    protected synchronized Database getDb() {
        return super.getDb();
    }

    @Override
    protected long totalRecords() {
        return super.totalRecords();
    }
}
