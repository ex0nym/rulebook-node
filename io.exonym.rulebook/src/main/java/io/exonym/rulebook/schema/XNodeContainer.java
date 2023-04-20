package io.exonym.rulebook.schema;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.actor.actions.XContainerJSON;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.utils.storage.XContainerSchema;
import io.exonym.rulebook.context.XNodeContainerStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class XNodeContainer extends XContainerJSON {
	
	private static final Logger logger = LogManager.getLogger(XNodeContainer.class);
	
	private XNodeContainerStore store = XNodeContainerStore.getInstance();

	public XNodeContainer(String username, boolean create) throws Exception {
		super(username, create);
		
	}

	public XNodeContainer(String username) throws Exception {
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
	protected XContainerSchema init(boolean create) throws Exception {
		if (this.store==null) {
			store = XNodeContainerStore.getInstance();
			
		}
		if (create) {
			try {
				logger.debug("Store is " + store);
				logger.debug("Username is " + getUsername());

				store.getContainer(this.getUsername());
				
				throw new Exception("The container already exists");
				
			} catch (NoDocumentException e) {
				XNodeContainerSchema schema0 = new XNodeContainerSchema();
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
				
				XNodeContainerSchema schema0 = store.getContainer(this.getUsername());
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
		store.update((XNodeContainerSchema) getSchema());
		updateLists();
		
	}

	@Override
	public void delete() {
		try {
			store.delete((XNodeContainerSchema)getSchema());

		} catch (Exception e) {
			logger.error("Error", e);

		}
	}
}
