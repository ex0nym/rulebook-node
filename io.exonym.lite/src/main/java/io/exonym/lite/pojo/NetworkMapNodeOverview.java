package io.exonym.lite.pojo;

import io.exonym.lite.couchdb.AbstractCouchDbObject;

import java.net.URI;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkMapNodeOverview extends AbstractCouchDbObject {


    public static final String TYPE_NETWORK_MAP_NODE_OVERVIEW = "network-map";

    public static final String GLOBAL_STATE_LEAD_SET_UNAVAILABLE = "GLOBAL_STATE_LEAD_SET_UNAVAILABLE";
    public static final String GLOBAL_STATE_LEAD_SET_AVAILABLE_THIS_LEAD_UNLISTED = "GLOBAL_STATE_LEAD_SET_AVAILABLE_THIS_LEAD_UNLISTED";
    public static final String GLOBAL_STATE_DEFINED_LEAD_LISTED__THIS_MODERATOR_UNLISTED = "GLOBAL_STATE_DEFINED_LEAD_LISTED__THIS_MODERATOR_UNLISTED";
    public static final String GLOBAL_STATE_THIS_LEAD_LISTED_MOD_INDETERMINATE = "GLOBAL_STATE_THIS_LEAD_LISTED_MOD_INDETERMINATE";
    public static final String GLOBAL_STATE_THIS_MODERATOR_LISTED = "GLOBAL_STATE_THIS_MODERATOR_LISTED";

    public static final String LOCAL_STATE_UNDEFINED = "LOCAL_STATE_UNDEFINED";
    public static final String LOCAL_STATE_MODERATOR = "LOCAL_STATE_MODERATOR";
    public static final String LOCAL_STATE_LEAD = "LOCAL_STATE_LEAD";
    public static final String LOCAL_STATE_LEAD_AND_MODERATOR = "LOCAL_STATE_LEAD_AND_MODERATOR";
    public static final String LOCAL_STATE_INDEPENDENT_LEAD_AND_MODERATOR = "LOCAL_STATE_INDEPENDENT_LEAD_AND_MODERATOR";

    private ConcurrentHashMap<URI, NetworkMapItemLead> sources = new ConcurrentHashMap<>();
    private String lastRefresh = null;
    private String currentLocalState = "UNKNOWN";
    private String currentGlobalState = "UNKNOWN";
    private URI moderatorUID;
    private URI thisModeratorLeadUID;

    private URI thisNodeLeadUID;

    private HashSet<URI> listeningToLeads = new HashSet<>();

    private boolean sybilRequiresUpdate = false;
    private boolean leadRequiresUpdate = false;

    private String latestRevocationInformationHash = null;
    private String latestPresentationPolicyHash = null;

    public NetworkMapNodeOverview(){
        this.setType("network-map");

    }

    public ConcurrentHashMap<URI, NetworkMapItemLead> getSources() {
        return sources;

    }

    public void setLeads(ConcurrentHashMap<URI, NetworkMapItemLead> sources) {
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

    public URI getModeratorUID() {
        return moderatorUID;
    }

    public void setModeratorUID(URI moderatorUID) {
        this.moderatorUID = moderatorUID;
    }

    public URI getThisModeratorLeadUID() {
        return thisModeratorLeadUID;
    }

    public void setThisModeratorLeadUID(URI thisModeratorLeadUID) {
        this.thisModeratorLeadUID = thisModeratorLeadUID;
    }


    public HashSet<URI> getListeningToLeads() {
        return listeningToLeads;
    }

    public void setListeningToLeads(HashSet<URI> listeningToLeads) {
        this.listeningToLeads = listeningToLeads;
    }

    public boolean isSybilRequiresUpdate() {
        return sybilRequiresUpdate;
    }

    public void setSybilRequiresUpdate(boolean sybilRequiresUpdate) {
        this.sybilRequiresUpdate = sybilRequiresUpdate;
    }

    public boolean isLeadRequiresUpdate() {
        return leadRequiresUpdate;
    }

    public void setLeadRequiresUpdate(boolean leadRequiresUpdate) {
        this.leadRequiresUpdate = leadRequiresUpdate;
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

    public URI getThisNodeLeadUID() {
        return thisNodeLeadUID;
    }

    public void setThisNodeLeadUID(URI thisNodeLeadUID) {
        this.thisNodeLeadUID = thisNodeLeadUID;
    }
}
