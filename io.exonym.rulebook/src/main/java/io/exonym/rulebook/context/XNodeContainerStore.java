package io.exonym.rulebook.context;


import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.exceptions.ProgrammingException;
import io.exonym.rulebook.exceptions.ItemNotFoundException;
import io.exonym.lite.pojo.TypeNames;
import io.exonym.rulebook.schema.XNodeContainerSchema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class XNodeContainerStore {

	private static XNodeContainerStore instance;
	private final CouchRepository<XNodeContainerSchema> store;


	private static final Logger logger = LogManager.getLogger(XNodeContainerStore.class);
	protected XNodeContainerStore() throws Exception {
		logger.debug("Init Container Store");
		this.store = CouchDbHelper.repoContainerStore();

	}
	
	/**
	 * 
	 * @param record
	 * @return _id of newly added object 
	 */
	public synchronized String[] add(XNodeContainerSchema record) throws Exception {
		this.store.create(record);
		return new String[] {record.get_id(), record.get_rev()};
		
	}
	
	public synchronized void update(XNodeContainerSchema schema) throws Exception {
		if (schema!=null) {
			this.store.update(schema);

		} else {
			throw new ProgrammingException("Null scheme on update");
			
		}
	}



	public synchronized XNodeContainerSchema getContainer(String username) throws Exception{
		try {
			QueryBasic q = new QueryBasic();
			q.getSelector().put(TypeNames.CONTAINERS_USERNAME, username);
			List<XNodeContainerSchema> containers = store.read(q);
			if (containers.isEmpty()){
				throw new ItemNotFoundException("Container(" + username + ")");
				
			} else {
				logger.debug("Returned " + containers.size() + " results for " + username);
				
			}
			return containers.get(0);
			
		} catch (Exception e) {
			logger.debug("Bad container name request " + username);
			throw e;
			
		}
	}

	public synchronized void delete(XNodeContainerSchema schema) throws Exception{
		this.store.delete(schema);


	}

	/* private void setupDb() {
		try {
			logger.debug("Defining fresh install");
			CloudantClient cloudant = CloudantClientFactory.createClient();
			lite.throttle();
			db = cloudant.database(dbName, true);

			lite.throttle();
			db.createIndex("{\r\n" +
					"   \"index\": {\r\n" + 
					"      \"fields\": [\r\n" + 
					"         \"username\"" +
					"      ]\r\n" + 
					"   },\r\n" +
					"   \"name\": \"admin-json-index\",\r\n" + 
					"   \"type\": \"json\"\r\n" + 
					"}");

		} catch (Exception e) {
			logger.error("Setup error", e);
			
		}
	}	 //*/

	static {
		try {
			instance = new XNodeContainerStore();

		} catch (Exception e) {
			logger.error(">>>>>>> >>>>>>>>>>> Catastrophic Error", e);

		}
	}
	
	public static XNodeContainerStore getInstance() {
		return instance;
		
	}
}
