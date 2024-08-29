package io.exonym.rulebook.context;

import io.exonym.actor.actions.NodeManager;
import io.exonym.actor.actions.IdContainerJSON;
import io.exonym.lite.exceptions.ProgrammingException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.standard.WhiteList;
import io.exonym.lite.standard.PassStore;
import io.exonym.lite.pojo.XKey;
import io.exonym.rulebook.schema.IdContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;

public final class NodeManagerWeb extends NodeManager {
	
	private static final Logger logger = LogManager.getLogger(NodeManagerWeb.class);


	public NodeManagerWeb(String sourceName) throws UxException {
		super(sourceName);
	}


	@Override
	protected IdContainerJSON establishNewContainer(String name, PassStore store) throws Exception {
		try {
			if (WhiteList.username(name)){
				return new IdContainer(name, true);
				
			} else {
				throw new UxException("A valid name is between 3 and 32 characters with underscores replacing spaces (" +  name + ")");	
				
			}
		} catch (UxException e) {
			throw e;

		} catch (Exception e) {
			throw new UxException("There is either a Network Source or a Node with this name on this hosting (" +  name  + ")" , e);
			
		}
	}

	@Override
	protected IdContainerJSON openContainer(String name, PassStore store) throws Exception {
		try {
			return new IdContainer(name);
				
		} catch (Exception e) {
			throw new UxException("The container '" + name + "' does not exist", e);
			
		}
	}

	@Override
	protected void saveKey(XKey xk) {
		try {
			CouchRepository<XKey> repo = CouchDbHelper.repoRootKey();
			repo.create(xk);

		} catch (Exception e) {
			logger.error("Failed to save Store Key", e);

		}
	}

	public static String issuerUidToNodeUid(URI issuerUid) throws ProgrammingException {
		if (issuerUid!=null) {
			if (issuerUid.toString().length() > 11) {
				String uid = issuerUid.toString();
				return uid.substring(0, uid.length()-11);
				
			} else {
				throw new ProgrammingException("IssuerUid was invalid " + issuerUid);
				
			}
		} else {
			throw new ProgrammingException("IssuerUid was null");
			
		}
	}


}
