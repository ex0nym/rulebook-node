package io.exonym.rulebook.context;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.couchdb.QueryOrGate;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.ProgrammingException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.rulebook.exceptions.ItemNotFoundException;
import io.exonym.lite.pojo.NodeData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class NodeStore {

	private static NodeStore instance;
	
	private static final Logger logger = LogManager.getLogger(NodeStore.class);

	private CouchRepository<NodeData> store;

	protected NodeStore() {
		logger.debug("Init Node Store");
		try {
		    CloudantClient client = CouchDbClient.instance();
		    Database db = client.database(CouchDbHelper.getDbNode(), true);
		    store = new CouchRepository<>(db, NodeData.class);
			findAllNetworkNodes();

		} catch (NoDocumentException e) {
			logger.warn("Setup Error", e);

		} catch (ItemNotFoundException e) {
			logger.warn("Checking If Database Exists and No Items Were Found.");

		} catch (Exception e) {
			logger.error("Error", e);

		}
	}


	public void delete(NodeData node) throws Exception {
		store.delete(node);

	}


	public synchronized NodeData findNetworkNodeDataItem(String nodeUid) throws Exception {
		try {
//			String query = "{\"selector\": {\"nodeUrl\":\"<replace>\", \"type\":\"" + NodeData.TYPE_NETWORK_NODE + "\"}}";

            QueryBasic q = new QueryBasic();
            q.getSelector().put(NodeData.FIELD_NODE_UID, nodeUid);
            q.getSelector().put(NodeData.FIELD_TYPE, NodeData.TYPE_NETWORK_NODE);
			List<NodeData> containers = store.read(q);

			if (containers.isEmpty()) {
				throw new ItemNotFoundException("NodeData(" + nodeUid + ")");

			} else {
				logger.debug("Returned " + containers.size() + " results for " + nodeUid);

			}
			return containers.get(0);

		} catch (NoDocumentException e){
			throw new ItemNotFoundException("", e);

		} catch (Exception e) {
			logger.debug("Bad container name request " + nodeUid);
			throw e;

		}
	}

	public synchronized NodeData updateNodeDataItem(NodeData nodeData) throws Exception {
		if (nodeData == null) {
			throw new ProgrammingException("null nodeData");

		}
		this.store.update(nodeData);
		return nodeData;

	}

	public synchronized NodeData findMember(String fkUser) throws Exception {
		try {
            QueryBasic q = new QueryBasic();
            q.getSelector().put("fkUser", fkUser);

            List<NodeData> containers = store.read(q);
			if (containers.isEmpty()) {
				throw new ItemNotFoundException("NodeData(" + fkUser + ")");

			} else {
				logger.debug("Returned " + containers.size() + " results for " + fkUser);

			}
			return containers.get(0);

		} catch (NoDocumentException e){
			throw new ItemNotFoundException("", e);

		} catch (Exception e) {
			logger.debug("Bad fkUser request " + fkUser);
			throw e;

		}
	}


	public synchronized NodeData findMemberByRevocationHandleHash(String hash) throws Exception {
		try {
            QueryBasic q = new QueryBasic();
            q.getSelector().put("handle", hash);

			List<NodeData> containers = store.read(q);
			if (containers.isEmpty()) {
				throw new ItemNotFoundException("NodeData(" + hash + ")");

			} else {
				logger.debug("Returned " + containers.size() + " results for " + hash);

			}
			return containers.get(0);

		} catch (NoDocumentException e){
			throw new ItemNotFoundException("", e);

		} catch (ItemNotFoundException e) {
			throw e;

		} catch (Exception e) {
			logger.debug("Bad handle request " + hash);
			throw e;

		}
	}

	public synchronized NodeData findMemberByContainerName(String containerName) throws Exception {
        QueryBasic q = new QueryBasic();
        q.getSelector().put("name", containerName);

		try {
			List<NodeData> containers = store.read(q);
			if (containers.isEmpty()) {
				throw new ItemNotFoundException("NodeData(" + containerName + ")");

			} else {
				logger.debug("Returned " + containers.size() + " results for " + containerName);

			}
			return containers.get(0);

		} catch (NoDocumentException e){
			throw new ItemNotFoundException("", e);

		} catch (Exception e) {
			logger.debug("Bad 'name' request " + containerName);
			throw e;

		}
	}

	public synchronized NodeData openThisAdvocate() throws Exception {
		ArrayList<NodeData> nodes = findType(NodeData.TYPE_NODE);
		if (nodes.isEmpty()){
			return null;

		} else if (nodes.size()==1){
			return nodes.get(0);

		} else {
			throw new UxException(ErrorMessages.INCORRECT_PARAMETERS,
					"Server has defined itself more than once");
		}
	}

	public synchronized NodeData openThisSource() throws Exception {
		ArrayList<NodeData> nodes = findType(NodeData.TYPE_SOURCE);
		if (nodes.isEmpty()){
			return null;

		} else if (nodes.size()==1){
			return nodes.get(0);

		} else {
			throw new UxException(ErrorMessages.INCORRECT_PARAMETERS,
					"Server has defined itself more than once");
		}
	}


	public synchronized ArrayList<NodeData> findType(String TYPE) throws Exception {
		try {
            QueryBasic q = new QueryBasic();
            q.getSelector().put("type", TYPE);

			// String query = "{\"selector\": {\"type\":\"" + TYPE + "\"}}";
			List<NodeData> containers = store.read(q);
			if (containers.isEmpty()) {
				throw new ItemNotFoundException("NodeData("+TYPE+")");

			} else {
				logger.debug("Returned " + containers.size() + " results for Member");

			}
            return new ArrayList<>(containers);

		} catch (NoDocumentException e){
			throw new ItemNotFoundException("", e);

		} catch (Exception e) {
			throw e;

		}
	}

	public synchronized ArrayList<NodeData> findAllNetworkNodes(String networkName) throws Exception {
		try {
            QueryBasic q = new QueryBasic();
            q.getSelector().put("type", NodeData.TYPE_NETWORK_NODE);
            q.getSelector().put("name", networkName);

//            String query = "{\"selector\": {\"name\":\"<replace>\", \"type\":\"" + NodeData.TYPE_NETWORK_NODE + "\"}}";
//			query = query.replace("<replace>", networkName);

			List<NodeData> containers = store.read(q);
			if (containers.isEmpty()) {
				throw new ItemNotFoundException("NodeData(source)");

			} else {
				logger.debug("Returned " + containers.size() + " results for source");

			}
			return new ArrayList<>(containers);

		} catch (NoDocumentException e){
			throw new ItemNotFoundException("", e);

		} catch (Exception e) {
			throw e;

		}
	}

	public synchronized ArrayList<NodeData> findAllNetworkNodes() throws Exception {
		try {
            QueryBasic q = new QueryBasic();
            q.getSelector().put("type", NodeData.TYPE_NETWORK_NODE);
			// String query = "{\"selector\": {\"type\":\"" + NodeData.TYPE_NETWORK_NODE + "\"}}";
			List<NodeData> containers = store.read(q);
			if (containers.isEmpty()) {
				throw new ItemNotFoundException("NodeData(network-node)");

			} else {
				logger.debug("Returned " + containers.size() + " results for source");

			}
			return new ArrayList<>(containers);

		} catch (NoDocumentException e){
			throw new ItemNotFoundException("", e);

		} catch (Exception e) {
			throw e;

		}
	}

	public synchronized ArrayList<NodeData> findAllLocalNodes() throws Exception {
		try {
			// String query = "{\"selector\": {\"$or\": [{\"type\": \"node\"},{\"type\": \"source\"}]}}";
			ArrayList options = new ArrayList<>();
			options.add("node");
			options.add("source");
			QueryOrGate query = new QueryOrGate("type", options);

			List<NodeData> containers = store.read(query);
			if (containers.isEmpty()) {
				throw new ItemNotFoundException("NodeData(source)");

			} else {
				logger.debug("Returned " + containers.size() + " results for source");

			}
			return new ArrayList<>(containers);

		} catch (NoDocumentException e){
			throw new ItemNotFoundException("", e);

		} catch (Exception e) {
			throw e;

		}
	}

	// {"selector": {"$or": [{"type": "node"},{"type": "source"}]}}

	/**
	 * @param nodeData
	 * @return [_id, _rev]  of newly added object
	 * @throws HubException
	 */
	public synchronized String[] add(NodeData nodeData) throws Exception {
		if (nodeData == null) {
			throw new HubException("NodeData was null");

		}
		if (nodeData.getType() == null) {
			throw new HubException("Type was null");

		}
		if (nodeData.getNetworkName() == null) {
			throw new HubException("NetworkName was null");

		}
		if (nodeData.getName() == null) {
			throw new HubException("Name was null");

		}
		this.store.create(nodeData);

		return new String[]{nodeData.get_id(), nodeData.get_rev()};

	}

	static {
        try {
            instance = new NodeStore();

        } catch (Exception e) {
            logger.error(">>>>>>>>>>>>> Catastrophic Error", e);

        }
    }

	public static NodeStore getInstance() {
		return instance;

	}

}