package io.exonym.lite.couchdb;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.FindByIndexOptions;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.api.query.QueryBuilder;
import com.cloudant.client.api.query.QueryResult;
import com.cloudant.client.api.views.AllDocsRequest;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.lite.exceptions.ProgrammingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * The repository should be extended into the package where it is used for security reasons.
 *
 * @param <T>
 */
public class ProtectedCouchRepository<T> {

    private static final Logger logger = LogManager.getLogger(ProtectedCouchRepository.class);
    private final Class<T> clazz;
    private final Database db;

    protected ProtectedCouchRepository(Database db, Class<T> clazz) throws Exception {
        if (db==null){
            throw new Exception("Database client failure");

        }
        this.clazz = clazz;
        this.db = db;


    }

    /**
     * Adds a new item to the database and updates this object with the couchdb identifiers.
     *
     * @param item
     * @throws Exception
     */
    protected synchronized void create(AbstractCouchDbObject item) throws Exception {
        Response r = db.save(item);
        if (r.getError()!=null){
            throw new Exception(r.getError() + " reason:" + r.getReason() + " status code: " + r.getStatusCode());

        }
        item.set_id(r.getId());
        item.set_rev(r.getRev());

    }

    protected synchronized void addIndex(CouchIndex index){
        db.createIndex(index.toJson());

    }


    protected synchronized void bulkAdd(List<T> items) throws Exception {
        List<Response> responses = db.bulk(items);

        for(T item : items){
            AbstractCouchDbObject item0 = (AbstractCouchDbObject)item;
            Response r = responses.get(items.indexOf(item));
            item0.set_id(r.getId());
            item0.set_rev(r.getRev());
            if (r.getError()!=null){
                throw new Exception(r.getError() + " reason:" + r.getReason() + " status code: " + r.getStatusCode());

            }
        }
    }

    protected synchronized T read(String _id) throws NoDocumentException {
        T t = db.find(clazz, _id);
        return t;

    }

    protected synchronized List<T> read(String[] _ids) throws Exception {
        if (_ids!=null){
            if (_ids.length!=0){
                AllDocsRequest a = db.getAllDocsRequestBuilder().keys(_ids).includeDocs(true).build();
                List<T> result =  a.getResponse().getDocsAs(clazz);
                if (result.isEmpty()){
                    throw new NoDocumentException("Searched for " + _ids.length + " records");

                }
                return result;

            } else {
                throw new NoDocumentException("Size = 0");

            }
        } else {
            throw new NoDocumentException("Null");

        }
    }

    protected synchronized Database getDb(){
        return this.db;
    }

    protected synchronized List<T> read(Query query, int limit) {
        return read(query, limit, 0);

    }

    protected synchronized QueryResult<T> read(QueryBuilder query){
        return read(query, 25, 0);

    }

    protected synchronized QueryResult<T> read(QueryBuilder query, int limit, int skip){
        query.limit(limit);
        query.skip(skip);
        QueryResult<T> r = db.query(query.build(), clazz);
        return r;

    }

    /**
     *
     * @param query
     * @param limit the limit to the number of results returned
     * @param skip the number of results you want to skip
     * @return
     */
    protected synchronized List<T> read(Query query, int limit, int skip){
        String s = query.getJson();
        List<T> result = db.findByIndex(s, clazz,
                new FindByIndexOptions()
                        .limit(limit).skip(skip));

        if (result.isEmpty()){
            throw new NoDocumentException(s);

        }
        return result;

    }

    protected synchronized List<T> read(Query query){
        String s = query.getJson();
        logger.debug(s);
        List<T> result = db.findByIndex(s, clazz);

        if (result.isEmpty()){
            throw new NoDocumentException(s);

        }
        logger.debug("Returned " + result.size() + " results for Query: " + s);
        return result;

    }

    /**
     * Checks on whether updates are allowed must be performed before
     * calling this method.
     *
     * @param item
     */
    protected synchronized void update(AbstractCouchDbObject item) throws Exception {
        if (item==null){
            throw new ProgrammingException("Null update object");

        } if (item.get_id()==null || item.get_rev()==null){
            throw new ProgrammingException("Attempting to update a new object");

        }
        Response r = db.update(item);
        if (r.getError()!=null){
            throw new Exception("Error: " + r.getError() + " Reason: " + r.getReason());

        }
        item.set_rev(r.getRev());
        item.set_id(r.getId());

    }

    protected long totalRecords(){
        return this.db.info().getDocCount();

    }

    protected synchronized void delete(AbstractCouchDbObject item) throws Exception {
        if (item==null){
            throw new ProgrammingException("Null update object");

        } if (item.get_id()==null || item.get_rev()==null){
            throw new ProgrammingException("_id and _rev are needed");

        }
        logger.debug("Attempting to remove " + item.get_id());
        db.remove(item.get_id(), item.get_rev());

        item.set_id(null);
        item.set_rev(null);

    }

    protected synchronized void ensureFullCommit(){
        db.ensureFullCommit();

    }
}
