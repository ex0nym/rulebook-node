package io.exonym.rulebook.schema;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.actor.actions.IdContainerJSON;
import io.exonym.lite.pojo.IdContainerSchema;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.rulebook.context.NodeContainerStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IdContainer extends IdContainerJSON {
	
	private static final Logger logger = LogManager.getLogger(IdContainer.class);
	
	private NodeContainerStore store = NodeContainerStore.getInstance();

	public IdContainer(String username, boolean create) throws Exception {
		super(username, create);
		
	}

	public IdContainer(String username) throws Exception {
		super(username, false);
		
	}

	public void setRegisteredSSIDevice(String deviceId, AsymStoreKey key) throws Exception {
		this.getSchema().setDeviceId(deviceId);
		this.getSchema().setAppPublicKey(key.getPublicKey().getEncoded());
		this.commitSchema();

	}

	public void nullifyDeviceId() throws Exception {
		this.getSchema().setDeviceId(null);
		this.commitSchema();
	}

	public String getDeviceId() {
		return this.getSchema().getDeviceId();
	}


	public AsymStoreKey getAppPublicKeyQ() throws Exception {
		AsymStoreKey key = AsymStoreKey.blank();
		key.assembleKey(this.getSchema().getAppPublicKey());
		return key;
	}

	@Override
	protected IdContainerSchema init(boolean create) throws Exception {
		if (this.store==null) {
			store = NodeContainerStore.getInstance();
			
		}
		if (create) {
			try {
				logger.debug("Store is " + store);
				logger.debug("Username is " + getUsername());

				store.getContainer(this.getUsername());
				
				throw new Exception("The container already exists");
				
			} catch (NoDocumentException e) {
				IdContainerSchema schema0 = new IdContainerSchema();
				schema0.setUsername(this.getUsername());
				logger.debug("Store is " + store);
				logger.debug("Username is " + getUsername());
				
				store.add(schema0);
				return schema0;
				
			}
		} else {
			try {
				logger.debug("Store is " + store);
				logger.debug("Username is " + getUsername());
				
				io.exonym.lite.pojo.IdContainerSchema schema0 = store.getContainer(this.getUsername());
				return schema0;
				
			} catch (NoDocumentException e) {
				throw new Exception("The container does not exist", e);
				
			}
		}
	}

	@Override
	protected void commitSchema() throws Exception {
		logger.debug("Store is " + store);
		logger.debug("Schema is " + getSchema());
		store.update( getSchema());
		updateLists();
		
	}

	@Override
	public void delete() {
		try {
			store.delete(getSchema());

		} catch (Exception e) {
			logger.error("Error", e);

		}
	}
}
