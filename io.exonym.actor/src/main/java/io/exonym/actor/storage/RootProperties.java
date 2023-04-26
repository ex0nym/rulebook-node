package io.exonym.actor.storage;

import io.exonym.lite.authenticators.RootPropertyFeatures;
import io.exonym.lite.exceptions.UxException;

import java.net.URL;

public class RootProperties extends RootPropertyFeatures {
	
	private final String broadcastUrl;
	private final SFTPLogonData primarySftpCredentials;
	private final SFTPLogonData secondarySftpCredentials;

	private final SFTPLogonData tokenTransfer;

	private final String primaryDomain;
	private final String primaryStaticDataFolder;

	private final String failoverDomain;
	private final String failoverStaticDataFolder;

	private final String nodeSupportNumber;
	private final String spawnWiderNetworkFrom;
	private final String rulebookNodeURL;

	private final String isoCountryCode;

	private final String authorizedDomain;

	private final boolean openSubscription;
	private final boolean openSourcePublication;

	/*
	 * 
	 */
	protected RootProperties() throws Exception {
		super();
		try {
			this.primaryDomain =mandatory("ROOT_DOMAIN");
			this.primaryStaticDataFolder =optional("STATIC_DATA_FOLDER", "");
			this.broadcastUrl =mandatory("BROADCAST_URL");
			this.rulebookNodeURL = mandatory("RULEBOOK_NODE_URL");

			String sftp = "SFTP data is needed to publish static data.";

			String sftpHost=messageOnFail("SFTP_HOST", sftp);
			int sftpPort = Integer.parseInt(mandatory("SFTP_PORT"));
			String sftpUsername = mandatory("SFTP_USERNAME");
			String sftpPassword=mandatory("SFTP_PASSWORD");
			String knownHost0=mandatory("KNOWN_HOST0");
			String knownHost1=mandatory("KNOWN_HOST1");
			String knownHost2=mandatory("KNOWN_HOST2");

			this.primarySftpCredentials = new SFTPLogonData();
			this.primarySftpCredentials.setPort(sftpPort);
			this.primarySftpCredentials.setHost(sftpHost);

			this.primarySftpCredentials.setUsernameAndPassword(sftpUsername, sftpPassword);
			this.primarySftpCredentials.setKnownHosts(knownHost0, knownHost1, knownHost2);

			String failSftpHost=optional("FAILOVER_SFTP_HOST", null);
			this.authorizedDomain = optional("AUTHORIZED_DOMAIN", null);

			if (failSftpHost!=null){
				int failSftpPort = Integer.parseInt(mandatory("FAILOVER_SFTP_PORT"));
				String failSftpUsername = mandatory("FAILOVER_SFTP_USERNAME");
				String failSftpPassword=mandatory("FAILOVER_SFTP_PASSWORD");
				String failKnownHost0=mandatory("FAILOVER_KNOWN_HOST0");
				String failKnownHost1=mandatory("FAILOVER_KNOWN_HOST1");
				String failKnownHost2=mandatory("FAILOVER_KNOWN_HOST2");
				this.secondarySftpCredentials = new SFTPLogonData();
				this.secondarySftpCredentials.setPort(failSftpPort);
				this.secondarySftpCredentials.setHost(failSftpHost);
				this.secondarySftpCredentials.setKnownHosts(failKnownHost0, failKnownHost1, failKnownHost2);
				this.secondarySftpCredentials.setUsernameAndPassword(failSftpUsername, failSftpPassword);

				this.failoverDomain = mandatory("FAILOVER_DOMAIN");
				this.failoverStaticDataFolder = optional("FAILOVER_STATIC_DATA_FOLDER", "");

			} else {
				this.secondarySftpCredentials = null;
				this.failoverDomain = null;
				this.failoverStaticDataFolder = null;

			}
			String tokenHost=optional("TOKEN_TRANSFER_HOST", null);
			if (tokenHost!=null){
				int tSftpPort = Integer.parseInt(mandatory("TOKEN_TRANSFER_PORT"));
				String tSftpUsername = mandatory("TOKEN_TRANSFER_USERNAME");
				String tSftpPassword=mandatory("TOKEN_TRANSFER_PASSWORD");
				String tKnownHost0=optional("TOKEN_TRANSFER_KNOWN_HOST0", null);
				String tKnownHost1=optional("TOKEN_TRANSFER_KNOWN_HOST1", null);
				String tKnownHost2=optional("TOKEN_TRANSFER_KNOWN_HOST2", null);
				this.tokenTransfer = new SFTPLogonData();
				this.tokenTransfer.setPort(tSftpPort);
				this.tokenTransfer.setHost(tokenHost);
				this.tokenTransfer.setUsernameAndPassword(tSftpUsername, tSftpPassword);
				this.tokenTransfer.setKnownHosts(tKnownHost0, tKnownHost1, tKnownHost2);

			} else {
				this.tokenTransfer=null;

			}

			this.spawnWiderNetworkFrom=optional("SPAWN_WIDER_NETWORK_FROM", "https://exonym.io/trust");
			this.nodeSupportNumber=optional("NODE_SUPPORT_NUMBER", "No contact number for this Host");
			this.isoCountryCode =  mandatory("ISO_COUNTRY_CODE");
			this.openSubscription = Boolean.parseBoolean(optional("OPEN_SUBSCRIPTION", "true"));
			this.openSourcePublication = Boolean.parseBoolean(optional("OPEN_SOURCE_PUBLICATION", "false"));

		} catch (NumberFormatException e) {
			throw new UxException("Fatal Error - Bad Configuration - Required Attribute Missing", e);

		} catch (NullPointerException e) {
			throw new UxException("Fatal Error - Bad Configuration - Required Attribute Missing", e);

		}
	}



	protected String getPrimaryDomain() {
		return primaryDomain;
	}

	protected String getPrimaryStaticDataFolder() {
		return primaryStaticDataFolder;
	}

	protected String getNodeSupportNumber() {
		return nodeSupportNumber;
	}

	protected String getSpawnWiderNetworkFrom() { return spawnWiderNetworkFrom; }

	protected String getIsoCountryCode() {
		return isoCountryCode;
	}

	protected String getFailoverDomain() {
		return failoverDomain;
	}

	protected String getFailoverStaticDataFolder() {
		return failoverStaticDataFolder;
	}

	protected SFTPLogonData getPrimarySftpCredentials() {
		return primarySftpCredentials;
	}

	protected SFTPLogonData getSecondarySftpCredentials() {
		return secondarySftpCredentials;
	}

	protected String getBroadcastUrl() {
		return broadcastUrl;
	}

	protected String getAuthorizedDomain() {
		return authorizedDomain;
	}

	@Override
	protected String getPrimaryAdminUsername() {
		return super.getPrimaryAdminUsername();
	}

	@Override
	protected String getNodeRoot() {
		return super.getNodeRoot();
	}

	@Override
	protected String getDbPrefix() {
		return super.getDbPrefix();
	}

	@Override
	protected URL getDbUrl() {
		return super.getDbUrl();
	}

	@Override
	protected String getDbUsername() {
		return super.getDbUsername();
	}

	@Override
	protected String getDbPassword() {
		return super.getDbPassword();
	}

	protected boolean isOpenSubscription() {
		return openSubscription;
	}

	public String getRulebookNodeURL() {
		return rulebookNodeURL;
	}

	protected SFTPLogonData getTokenTransfer() {
		return tokenTransfer;
	}

	public boolean isOpenSourcePublication() {
		return openSourcePublication;
	}
}
