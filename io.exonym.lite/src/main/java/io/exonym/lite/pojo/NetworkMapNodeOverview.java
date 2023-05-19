package io.exonym.lite.pojo;

import io.exonym.lite.couchdb.AbstractCouchDbObject;

import java.net.URI;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkMapNodeOverview extends AbstractCouchDbObject {


    public static final String TYPE_NETWORK_MAP_NODE_OVERVIEW = "network-map";

    public static final String GLOBAL_STATE_SOURCE_SET_UNAVAILABLE = "GLOBAL_STATE_SOURCE_SET_UNAVAILABLE";
    public static final String GLOBAL_STATE_SOURCE_SET_AVAILABLE_THIS_SOURCE_UNLISTED = "GLOBAL_STATE_SOURCE_SET_AVAILABLE_THIS_SOURCE_UNLISTED";
    public static final String GLOBAL_STATE_DEFINED_SOURCE_LISTED__THIS_HOST_UNLISTED = "GLOBAL_STATE_DEFINED_SOURCE_LISTED__THIS_HOST_UNLISTED";
    public static final String GLOBAL_STATE_THIS_SOURCE_LISTED_HOST_INDETERMINATE = "GLOBAL_STATE_THIS_SOURCE_LISTED_HOST_INDETERMINATE";
    public static final String GLOBAL_STATE_THIS_NODE_LISTED = "GLOBAL_STATE_THIS_NODE_LISTED";

    public static final String LOCAL_STATE_UNDEFINED = "LOCAL_STATE_UNDEFINED";
    public static final String LOCAL_STATE_HOST = "LOCAL_STATE_HOST";
    public static final String LOCAL_STATE_SOURCE = "LOCAL_STATE_SOURCE";
    public static final String LOCAL_STATE_SOURCE_AND_HOST = "LOCAL_STATE_SOURCE_AND_HOST";
    public static final String LOCAL_STATE_INDEPENDENT_SOURCE_AND_HOST = "LOCAL_STATE_INDEPENDENT_SOURCE_AND_HOST";

    private ConcurrentHashMap<URI, NetworkMapItemSource> sources = new ConcurrentHashMap<>();
    private String lastRefresh = null;
    private String currentLocalState = "UNKNOWN";
    private String currentGlobalState = "UNKNOWN";
    private URI advocateUID;
    private URI thisAdvocateSourceUID;

    private URI thisNodeSourceUID;

    private HashSet<URI> listeningToSources = new HashSet<>();

    private boolean sybilRequiresUpdate = false;
    private boolean sourceRequiresUpdate = false;

    private String latestRevocationInformationHash = null;
    private String latestPresentationPolicyHash = null;

    public NetworkMapNodeOverview(){
        this.setType("network-map");

    }

    public ConcurrentHashMap<URI, NetworkMapItemSource> getSources() {
        return sources;

    }

    public void setSources(ConcurrentHashMap<URI, NetworkMapItemSource> sources) {
        this.sources = sources;

    }

    public String getLastRefresh() {
        return lastRefresh;
    }

    public void setLastRefresh(String lastRefresh) {
        this.lastRefresh = lastRefresh;
    }

    public String getCurrentLocalState() {
        return currentLocalState;
    }

    public void setCurrentLocalState(String currentLocalState) {
        this.currentLocalState = currentLocalState;
    }

    public String getCurrentGlobalState() {
        return currentGlobalState;
    }

    public void setCurrentGlobalState(String currentGlobalState) {
        this.currentGlobalState = currentGlobalState;
    }

    public URI getAdvocateUID() {
        return advocateUID;
    }

    public void setAdvocateUID(URI advocateUID) {
        this.advocateUID = advocateUID;
    }

    public URI getThisAdvocateSourceUID() {
        return thisAdvocateSourceUID;
    }

    public void setThisAdvocateSourceUID(URI thisAdvocateSourceUID) {
        this.thisAdvocateSourceUID = thisAdvocateSourceUID;
    }


    public HashSet<URI> getListeningToSources() {
        return listeningToSources;
    }

    public void setListeningToSources(HashSet<URI> listeningToSources) {
        this.listeningToSources = listeningToSources;
    }

    public boolean isSybilRequiresUpdate() {
        return sybilRequiresUpdate;
    }

    public void setSybilRequiresUpdate(boolean sybilRequiresUpdate) {
        this.sybilRequiresUpdate = sybilRequiresUpdate;
    }

    public boolean isSourceRequiresUpdate() {
        return sourceRequiresUpdate;
    }

    public void setSourceRequiresUpdate(boolean sourceRequiresUpdate) {
        this.sourceRequiresUpdate = sourceRequiresUpdate;
    }

    public String getLatestRevocationInformationHash() {
        return latestRevocationInformationHash;
    }

    public void setLatestRevocationInformationHash(String latestRevocationInformationHash) {
        this.latestRevocationInformationHash = latestRevocationInformationHash;
    }

    public String getLatestPresentationPolicyHash() {
        return latestPresentationPolicyHash;
    }

    public void setLatestPresentationPolicyHash(String latestPresentationPolicyHash) {
        this.latestPresentationPolicyHash = latestPresentationPolicyHash;
    }

    public URI getThisNodeSourceUID() {
        return thisNodeSourceUID;
    }

    public void setThisNodeSourceUID(URI thisNodeSourceUID) {
        this.thisNodeSourceUID = thisNodeSourceUID;
    }
}
