package io.exonym.lite.couchdb;

import com.cloudant.client.org.lightcouch.NoDocumentException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public abstract class ResultPager<T> {

    private static final Logger logger = LogManager.getLogger(ResultPager.class);
    protected final Query query;
    private final int pageSize;
    protected final ProtectedCouchRepository<T> repo;

    public ResultPager(Query query, int pageSize,
                          ProtectedCouchRepository<T> repo) throws Exception {
        this.query = query;
        this.pageSize = pageSize;
        this.repo = repo;

    }

    /**
     * Call after instantiation to execute
     *
     * @throws NoDocumentException only when the query failed to return results on first execute
     */
    protected void execute() throws NoDocumentException {
        try {
            firstBatch();
            int skip = pageSize;
            int i = 1;
            while(true){
                List<T> results = this.repo.read(this.query, this.pageSize, skip*i);
                processResultSet(results);
                i++;

            }
        } catch (NoDocumentException e) {
            logger.debug("Reached the end of the result set");

        } catch (Exception e) {
            logger.error("Database failure - cannot execute result pager", e);

        }
    }



    private void firstBatch() throws Exception {
        try {
            List<T> results = this.repo.read(this.query, this.pageSize, 0);
            processResultSet(results);

        } catch (NoDocumentException e) {
            logger.debug("There are no results for the query");
            throw e;

        }
    }

    protected abstract void processResultSet(List<T> results) throws Exception ;

}
