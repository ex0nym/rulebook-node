package io.exonym.lite.couchdb;

import com.cloudant.client.api.CloudantClient;
import static org.junit.jupiter.api.Assertions.*;

import com.cloudant.client.api.Database;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TestCouchInterface {

    private static final Logger logger = LogManager.getLogger(TestCouchInterface.class);
    private static Database db = null;
    static String dbName = "test" + Math.random(); // untested change

    @Test
    void create0(){
        try {
            assertNotNull(db);
            ProtectedCouchRepository<StubIdentityDocument> repo = new ProtectedCouchRepository<>(db, StubIdentityDocument.class);
            StubIdentityDocument i0 = new StubIdentityDocument();
            i0.setFace(new byte[] {1,4,3,2});
            repo.create(i0);
            assertNotNull(i0.get_id());
            assertNotNull(i0.get_rev());
            StubIdentityDocument i1 = repo.read(i0.get_id());
            assertNotNull(i1);
            assertNotNull(i1.getFace());

        } catch (Exception e) {
            logger.error("Error", e);
            fail(e.getMessage());

        }
    }

    @Test
    void createBulk(){
        try {
            ArrayList<StubIdentityDocument> ids = new ArrayList<>();
            ids.add(new StubIdentityDocument());
            ids.add(new StubIdentityDocument());
            ids.add(new StubIdentityDocument());
            ProtectedCouchRepository<StubIdentityDocument> repo = new ProtectedCouchRepository<>(db, StubIdentityDocument.class);
            for(StubIdentityDocument id : ids){
                id.setType("tester");

            }
            repo.bulkAdd(ids);

            for(StubIdentityDocument id : ids){
                logger.info(id);
                assertNotNull(id.get_id());
                assertNotNull(id.get_rev());

            }
            QueryBasic query = new QueryBasic();
            HashMap<String, String> selector = new HashMap<>();
            selector.put("type", "tester");
            query.setSelector(selector);
            List<StubIdentityDocument> docs = repo.read(query);
            logger.info("Results returned size : expected 3 " + docs.size());
            assert(docs.size()==3);

            selector.put("type", "zero");
            docs = repo.read(query);
            logger.info("Results returned size - expected zero " + docs.size());
            assert(false);

        } catch (NoDocumentException e) {
            assert(true);

        } catch (Exception e) {
            logger.info("Error", e);
            fail();

        }
    }

    @Test
    void read(){
        try {
            ProtectedCouchRepository<StubIdentityDocument> repo = new ProtectedCouchRepository<>(db, StubIdentityDocument.class);
            repo.read("56456rh");
            fail("Expected NoDocumentException");

        } catch (NoDocumentException e) {
            assertNotNull(e);

        } catch (Exception e){
            logger.error("Error", e);
            fail("Expected No Document Exception");

        }
    }

    @Test
    void update(){
        try {
            ProtectedCouchRepository<StubIdentityDocument> repo = new ProtectedCouchRepository<>(db, StubIdentityDocument.class);
            StubIdentityDocument id0 = new StubIdentityDocument();
            id0.setChallengeType("three-fingers");
            repo.create(id0);
            String r = id0.get_rev();
            assertNotNull(id0.get_id());
            id0.setChallengeType("two-fingers");
            repo.update(id0);
            assert(!id0.get_rev().equals(r));

            StubIdentityDocument i1 = repo.read(id0.get_id());
            assert(i1.getChallengeType().equals("two-fingers"));

        } catch (Exception e) {
            logger.error("Error", e);
            fail();


        }
    }

    @Test
    void delete(){
        try {
            ProtectedCouchRepository<StubIdentityDocument> repo = new ProtectedCouchRepository<>(db, StubIdentityDocument.class);
            StubIdentityDocument id0 = new StubIdentityDocument();
            id0.setChallengeType("nine-fingers");
            repo.create(id0);
            StubIdentityDocument i1 = repo.read(id0.get_id());
            assert (i1.getChallengeType().equals("nine-fingers"));
            repo.delete(i1);
            assert (i1.get_id() == null);
            assert (i1.get_rev() == null);
            repo.read(id0.get_id());
            assert(false);

        } catch (NoDocumentException e){
            assert(true);

        } catch (Exception e) {
            fail();

        }
    }

    @BeforeAll
    static void basicConnection(){
        try {
            CloudantClient client = StubCouchDbClient.instance();
            db = client.database(dbName, true);
            assertNotNull(db.getDBUri());

        } catch (Exception e){
            logger.error("Error", e);
            fail(e.getMessage());

        }
    }

    @AfterAll
    static void takeDown(){
        logger.info("Taking Down Test Data: " + dbName);
        StubCouchDbClient.instance().deleteDB(dbName);

    }
}
