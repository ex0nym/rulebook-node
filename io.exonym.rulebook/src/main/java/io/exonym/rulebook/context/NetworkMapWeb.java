package io.exonym.rulebook.context;

import com.cloudant.client.org.lightcouch.DocumentConflictException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.actions.*;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.couchdb.QueryOrGate;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.connect.UrlHelper;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.*;
import io.exonym.lite.standard.Const;
import io.exonym.utils.storage.CacheContainer;
import io.exonym.lite.standard.WhiteList;
import io.exonym.lite.time.DateHelper;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.rulebook.schema.CacheNodeContainer;
import io.exonym.utils.storage.*;
import io.exonym.lite.pojo.NodeData;
import io.exonym.rulebook.schema.IdContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;



public class NetworkMapWeb extends AbstractNetworkMap {

    private static final Logger logger = LogManager.getLogger(NetworkMapWeb.class);
    private final RulebookNodeProperties props = RulebookNodeProperties.instance();
    private NetworkMapNodeOverview allLeads = null;

    private MyTrustNetworks myTrustNetworks = new MyTrustNetworks();

    private URI myModeratorsLeadUID;
    private URI myModeratorUID;
    private URI myNodesLeadUID;


    protected NetworkMapWeb() throws Exception {}

    /**
     * @return the default path if it is a file system.  Null if you are using a database.
     */
    @Override
    protected Path defineRootPath() {
        return null;
    }

    /**
     * @return the appropriate Cache back for the environment.
     */
    @Override
    protected CacheContainer instantiateCache() throws Exception {
        return CacheNodeContainer.getInstance();
    }

    /**
     * Write the NMIS and the NMIAs to your chosen repository
     *
     * @param rulebookId
     * @param source
     * @param nmil
     * @param advocatesForLead
     * @throws Exception
     */
    @Override
    protected void writeVerifiedSource(String rulebookId, String source,
                                       NetworkMapItemLead nmil,
                                       ArrayList<NetworkMapItemModerator> advocatesForLead) throws Exception {
        throw new RuntimeException("spawn() was overridden and this shouldn't have been called");
    }



    protected void refresh(){
        spawn();
    }
    @Override
    public void spawn() {
        try {
            CouchRepository<NetworkMapNodeOverview> repo = CouchDbHelper.repoNetworkMapLeadOverview();
            try {
                QueryBasic q = QueryBasic.selectType(NetworkMapNodeOverview.TYPE_NETWORK_MAP_NODE_OVERVIEW);
                allLeads = repo.read(q).get(0);
                localStateDefinition(allLeads);
                globalStateDefinition(allLeads);
                defineListeners();
                repo.update(allLeads);

            } catch (NoDocumentException e) {
                logger.warn("CREATING NEW NETWORK_MAP_NODE_OVERVIEW()");
                allLeads = new NetworkMapNodeOverview();
                localStateDefinition(allLeads);
                globalStateDefinition(allLeads);
                defineListeners();
                repo.create(allLeads);

            }
        } catch (Exception e) {
            logger.error("Failed to Initialize Network Map ", e);

        }
    }

    private void defineListeners() throws UxException {
        ArrayList<URI> leads = new ArrayList<>(allLeads.getLeads().keySet());
        String rh = UIDHelper.computeRulebookHashUid(allLeads.getThisModeratorLeadUID());

        if (rh!=null){
            HashSet<URI> listenTo = allLeads.getListeningToLeads();
            for (URI uri : leads) {
                logger.info("Eval Lead " + uri);
                if (uri.toString().contains(rh)){
                    logger.info("Adding Lead " + rh);
                    listenTo.add(uri);

                } else if (Rulebook.isSybil(uri)){
                    logger.info("Adding Lead " + uri);
                    listenTo.add(uri);

                }
            }
        }
    }

    public NetworkMapNodeOverview getAllLeads() {
        return allLeads;
    }

    @Override
    public boolean networkMapExists() throws Exception {
        CouchRepository<NetworkMapNodeOverview> repo = CouchDbHelper.repoNetworkMapLeadOverview();
        try {
            QueryBasic q = QueryBasic.selectType(
                    NetworkMapNodeOverview.TYPE_NETWORK_MAP_NODE_OVERVIEW);

            allLeads = repo.read(q).get(0);
            return true;

        } catch (NoDocumentException e) {
            return false;

        }
    }

    @Override
    protected NodeVerifier openNodeVerifier(URI staticNodeUrl0, boolean isTargetSource) throws Exception {
        throw new HubException("DEPRECATED");
    }

    private void localStateDefinition(NetworkMapNodeOverview allLeads) {
        try {
            NodeInformation leadNi = null;
            NodeInformation modNi = null;
            if (myTrustNetworks.isModerator()){
                TrustNetwork tn = myTrustNetworks.getModerator().getTrustNetwork();
                leadNi = tn.getNodeInformation();
                allLeads.setModeratorUID(leadNi.getNodeUid());
                allLeads.setThisModeratorLeadUID(leadNi.getLeadUid());

            }
            if (myTrustNetworks.isLeader()){
                TrustNetwork tn = myTrustNetworks.getLead().getTrustNetwork();
                modNi = tn.getNodeInformation();
                allLeads.setThisNodeLeadUID(modNi.getNodeUid());

            }

            if (myTrustNetworks.isLeader() && myTrustNetworks.isModerator()){
                if (leadNi.getNodeUid().equals(modNi.getLeadUid())){
                    allLeads.setCurrentLocalState(
                            NetworkMapNodeOverview.LOCAL_STATE_LEAD_AND_MODERATOR);

                } else {
                    allLeads.setCurrentLocalState(
                            NetworkMapNodeOverview.LOCAL_STATE_INDEPENDENT_LEAD_AND_MODERATOR);

                }
            } else if (myTrustNetworks.isModerator()){
                allLeads.setCurrentLocalState(
                        NetworkMapNodeOverview.LOCAL_STATE_MODERATOR);

            } else if (myTrustNetworks.isLeader()){
                allLeads.setCurrentLocalState(
                        NetworkMapNodeOverview.LOCAL_STATE_LEAD);

            } else {
                allLeads.setCurrentLocalState(NetworkMapNodeOverview.LOCAL_STATE_UNDEFINED);

            }
        } catch (Exception e) {
            logger.error("Unexpected Error - Failed to Define Local State", e);

        }
    }

    @Override
    public NetworkMapItem nmiForNode(URI uid) throws Exception {
        if (uid==null){
            throw new HubException("Null Node UID - Programming Error");

        }
        return nmiForNode(uid.toString());

    }

    public NetworkMapItem nmiForNode(String uid) throws Exception {
        if (uid==null){
            throw new HubException("Null Node UID - Programming Error");

        }
        QueryBasic q = new QueryBasic();
        q.getSelector().put(NetworkMapItem.FIELD_NODE_UID, uid);

        if (WhiteList.isModeratorUid(uid)){
            CouchRepository<NetworkMapItemModerator> repo = CouchDbHelper.repoNetworkMapItemAdvocate();
            return repo.read(q).get(0);

        } else {
            CouchRepository<NetworkMapItemLead> repo = CouchDbHelper.repoNetworkMapItemSource();
            return repo.read(q).get(0);

        }
    }

    protected List<NetworkMapItem> findHostsForSource(URI rulebook) throws Exception {
        throw new RuntimeException("Not implemented");

    }


    protected NetworkMapItem getSourceContainingString(URI target) throws Exception {
        if (target==null){
            throw new NullPointerException();

        }
        logger.debug("all source = " + allLeads);
        logger.debug("all sourceslistening " + allLeads.getListeningToLeads());
        for (URI i : allLeads.getListeningToLeads()){
            if (i.toString().contains(target.toString())){
                return allLeads.getLeads().get(i);

            }
        }
        throw new HubException("Failed to find target:" + target);

    }

    // Defines the nodes status on the large network.
    protected void globalStateDefinition(NetworkMapNodeOverview allLeads) {
        logger.info("Refreshing NetworkMap");

        try {
            TrustNetwork t = openLeadSet(allLeads);
            TrustNetworkWrapper tnw = new TrustNetworkWrapper(t);

            DateTime remote = new DateTime(t.getLastUpdated());
            String lastFullCycle = allLeads.getLastRefresh();

            if (lastFullCycle!=null){
                DateTime local = new DateTime(lastFullCycle);

                if (remote.isAfter(local)) {
                    logger.debug("Local Lead Set Required Refreshing (local/remote):\n\t\t "
                            + DateHelper.isoUtcDateTime(local) + " / "
                            + DateHelper.isoUtcDateTime(remote));
                    allLeads.setLastRefresh(t.getLastUpdated());
                    refreshLeads(tnw, allLeads);

                } else if (allLeads.getLeads().isEmpty()){
                    allLeads.setLastRefresh(t.getLastUpdated());
                    refreshLeads(tnw, allLeads);

                } else {
                    logger.debug("Leads are up to date.");

                }
            } else {
                logger.debug("Last Full Cycle was not set - Refreshing Leads");
                allLeads.setLastRefresh(t.getLastUpdated());
                refreshLeads(tnw, allLeads);

            }
            refreshModsOnNecessaryLeads(allLeads);

        } catch (FileNotFoundException e) {
            logger.debug("Handled exception thrown");
            allLeads.setLastRefresh(null);
            allLeads.setCurrentGlobalState(
                    NetworkMapNodeOverview.GLOBAL_STATE_LEAD_SET_UNAVAILABLE);

        } catch (Exception e) {
            logger.error("Unsure what to set the global state as here.", e);

        }
    }

    private TrustNetwork openLeadSet(NetworkMapNodeOverview allLeads) throws Exception {
        try {
            String leads = props.getSpawnWiderNetworkFrom();
            byte[] s = UrlHelper.readXml(new URL(leads));
            return JaxbHelper.xmlToClass(new String(s, StandardCharsets.UTF_8), TrustNetwork.class);

        } catch (Exception e) {
            allLeads.setCurrentGlobalState(
                    NetworkMapNodeOverview.GLOBAL_STATE_LEAD_SET_UNAVAILABLE);
            throw e;

        }
    }

    private NetworkMapNodeOverview openState() throws Exception {
        try {
            QueryBasic q = QueryBasic.selectType(NetworkMapNodeOverview.TYPE_NETWORK_MAP_NODE_OVERVIEW);
            return CouchDbHelper.repoNetworkMapLeadOverview().read(q).get(0);

        } catch (NoDocumentException e) {
            throw new HubException("Network Map Not Available");

        }
    }


    private void refreshLeads(TrustNetworkWrapper tnw, NetworkMapNodeOverview allLeads) {
        logger.debug("Refreshing Leads");
        ConcurrentHashMap<URI, NetworkMapItemLead> leads = allLeads.getLeads();
        leads.clear();
        URI thisNodesLead = allLeads.getThisModeratorLeadUID();
        URI thisModeratorUID = allLeads.getModeratorUID();

        thisNodesLead = (thisNodesLead==null ?
                allLeads.getThisNodeLeadUID() : thisNodesLead);

        boolean isLeadAdded = false;
        boolean isModeratorAddedToLead = false;

        for (NetworkParticipant participant : tnw.getAllParticipants()) {
            try {
                NetworkMapItemLead nmil = (NetworkMapItemLead) updateItem(participant, true);
                leads.put(nmil.getLeadUID(), nmil);
                if (participant.getNodeUid().toString().equals(thisModeratorUID)){
                    isModeratorAddedToLead = true;
                }
                logger.debug(nmil.getNodeUID() + " " + thisNodesLead);
                if (nmil.getNodeUID().equals(thisNodesLead)){
                    isLeadAdded = true;

                }
            } catch (MalformedURLException e) {
                logger.error("Bad URL", e);

            } catch (Exception e) {
                logger.error("Unexpected Error", e);

            }
        }
        logger.debug("Leads Detected from Wider Wrapper=" + leads.size());
        allLeads.setLeads(leads);

        if (isLeadAdded && isModeratorAddedToLead) {
            allLeads.setCurrentGlobalState(
                    NetworkMapNodeOverview.GLOBAL_STATE_THIS_MODERATOR_LISTED);

        } else if (isLeadAdded){
            allLeads.setCurrentGlobalState(
                    NetworkMapNodeOverview.GLOBAL_STATE_THIS_LEAD_LISTED_MOD_INDETERMINATE);

        } else {
            allLeads.setCurrentGlobalState(
                    NetworkMapNodeOverview.GLOBAL_STATE_LEAD_SET_AVAILABLE_THIS_LEAD_UNLISTED);

        }
    }

    private void refreshModsOnNecessaryLeads(NetworkMapNodeOverview allLeads) throws Exception {

        ConcurrentHashMap<URI, NetworkMapItemLead> leads = allLeads.getLeads();

        logger.info("Refreshing Nodes on Necessary Sources " + leads.size());
        CouchRepository<NetworkMapItemLead> repo = CouchDbHelper.repoNetworkMapItemSource();


        URI myLeadUrl = myTrustNetworks.isLeader() ? myTrustNetworks.getLead()
                .getTrustNetwork().getNodeInformation().getStaticLeadUrl0() : null;


        for (URI leadUID : leads.keySet()){
            NetworkMapItem lead = leads.get(leadUID);
            logger.info("Working Source: " + lead.getNodeUID() + " " + lead);
            TrustNetwork tn = null;
            byte[] pkBytes = null;

            try {
                String url0 = lead.getStaticURL0() + Const.SIGNATURES_XML;
                URI niUid = Const.TRUST_NETWORK_UID;

                String fn = io.exonym.utils.storage.IdContainer.uidToXmlFileName(niUid);
                String urlNi0 = lead.getStaticURL0() + "/" + fn;
                String urlForStaticData = lead.getStaticURL0().toString();
                logger.info("Computed local versus terget=" + urlForStaticData + " " + myLeadUrl);

                if (myLeadUrl==null || !urlForStaticData.toString().equals(myLeadUrl.toString())){

                    byte[] signature = UrlHelper.read(new URL(url0)); // , new URL(url1));
                    KeyContainer kc = JaxbHelper.xmlToClass(signature, KeyContainer.class);
                    KeyContainerWrapper kcw = new KeyContainerWrapper(kc);
                    logger.debug("URL:" +  urlNi0);

                    byte[] ni = UrlHelper.read(new URL(urlNi0)); //, new URL(urlNi1));
                    String niString = new String(ni, StandardCharsets.UTF_8);
                    niString = NodeVerifier.stripStringToSign(niString);

                    XKey niSig = kcw.getKey(niUid);
                    XKey pkSig = kcw.getKey(KeyContainerWrapper.TN_ROOT_KEY);

                    AsymStoreKey key = AsymStoreKey.blank();
                    pkBytes = lead.getPublicKeyB64();
                    key.assembleKey(lead.getPublicKeyB64());
                    key.verifySignature(pkBytes, pkSig.getSignature());
                    key.verifySignature(niString.getBytes(StandardCharsets.UTF_8), niSig.getSignature());

                    tn = JaxbHelper.xmlToClass(ni, TrustNetwork.class);

                } else {
                    if (myTrustNetworks.isDefined()){
                        tn = myTrustNetworks.getLead().getTrustNetwork();
                        logger.info("Number of participants=" + tn.getParticipants().size());
                        pkBytes = myTrustNetworks.getLead()
                                .getKcw().getKey(KeyContainerWrapper.TN_ROOT_KEY)
                                .getPublicKey();

                    } else {
                        logger.info(ErrorMessages.RULEBOOK_NODE_NOT_INITIALIZED, "My trust networks not defined.");

                    }
                }

                QueryBasic q = new QueryBasic();
                q.getSelector().put(NetworkMapItem.FIELD_NODE_UID, lead.getNodeUID().toString());

                try {
                    NetworkMapItemLead s0 = repo.read(q).get(0);
                    DateTime lastLocalUpdate = new DateTime(s0.getLastUpdated());
                    logger.debug("Local Date Time " + DateHelper.isoUtcDateTime(lastLocalUpdate));
                    DateTime lastRemoteUpdate = new DateTime(tn.getLastUpdated());
                    logger.debug("Remote Date Time " + DateHelper.isoUtcDateTime(lastLocalUpdate));
                    if (lastRemoteUpdate.isAfter(lastLocalUpdate)){
                        refreshNodes(tn, pkBytes, allLeads, q, repo);

                    }
                } catch (NoDocumentException e) {
                    logger.warn("CREATING NEW NetworkMapItemSource() " + leadUID);
                    NetworkMapItemLead n = new NetworkMapItemLead();
                    n.setNodeUID(leadUID);
                    n.setPublicKeyB64(pkBytes);
                    repo.create(n);
                    refreshNodes(tn, pkBytes, allLeads, q, repo);

                }

            } catch (SocketTimeoutException e) {
                logger.info("Lead temporarily unavailable " + lead.getNodeUID());

            } catch (Exception e) {
                logger.info("Lead Unavailable: " + lead.getNodeUID());
                logger.error("Error", e);

            }
        }
    }

    private void refreshNodes(TrustNetwork tnSource, byte[] sourcePk,
                              NetworkMapNodeOverview allLeads,
                              QueryBasic queryForSource,
                              CouchRepository<NetworkMapItemLead> repoSource) throws Exception {


        URI sourceUid = tnSource.getNodeInformation().getNodeUid();
        NetworkMapItemLead nmil = repoSource.read(queryForSource).get(0);

        logger.debug("Refreshing Node " + sourceUid);
        TrustNetworkWrapper tnw = new TrustNetworkWrapper(tnSource);
        Collection<NetworkParticipant> moderators = tnw.getAllParticipants();

        HashSet<URI> modsForLead = nmil.getModeratorsForLead();
        for (NetworkParticipant p : moderators){
            modsForLead.add(p.getNodeUid());
            logger.debug("Mod for Lead = " + p.getNodeUid());
        }


        for (URI uri : nmil.getModeratorsForLead()){
            logger.debug("Check After Update  = " + uri);

        }
        CouchRepository<NetworkMapItemModerator> repoAdvocate = CouchDbHelper.repoNetworkMapItemAdvocate();
        boolean isModListed = false;

        URI thisModLeadUid = allLeads.getThisModeratorLeadUID();
        URI targetNodeLeadUid = tnSource.getNodeInformation().getLeadUid();
        logger.debug("isSourceWithinScope==" + thisModLeadUid + " " + targetNodeLeadUid);

        boolean isLeadWithinScope = (thisModLeadUid!=null &&
                thisModLeadUid.equals(targetNodeLeadUid));

        logger.debug("isLeadWithinScope=" + isLeadWithinScope );

        for (NetworkParticipant advocate : moderators){
            QueryBasic q = new QueryBasic();
            q.getSelector().put(NetworkMapItem.FIELD_NODE_UID, advocate.getNodeUid().toString());

            try {
                NetworkMapItemModerator i = repoAdvocate.read(q).get(0);
                NetworkMapItem j = updateItem(advocate, false);
                j.set_id(i.get_id());
                j.set_rev(i.get_rev());

                URI hostUuid = allLeads.getModeratorUID();
                if (isLeadWithinScope && hostUuid!=null){
                    if (hostUuid.equals(i.getNodeUID())){
                        isModListed = true;

                    }
                }
                repoAdvocate.update(j);

            } catch (NoDocumentException e) {
                NetworkMapItem i = updateItem(advocate, false);
                URI advocateUID = allLeads.getModeratorUID();
                if (isLeadWithinScope && advocateUID!=null){
                    if (advocateUID.equals(i.getNodeUID())){
                        isModListed = true;

                    }
                }
                logger.warn("CREATING NEW NetworkMapItemAdvocate() " + i.getNodeUID());
                repoAdvocate.create(i);

            }
        }
        if (isLeadWithinScope && isModListed){
            allLeads.setCurrentGlobalState(
                    NetworkMapNodeOverview.GLOBAL_STATE_THIS_MODERATOR_LISTED);

        } else if (isLeadWithinScope){
            allLeads.setCurrentGlobalState(
                    NetworkMapNodeOverview.GLOBAL_STATE_DEFINED_LEAD_LISTED__THIS_MODERATOR_UNLISTED);

        }
        QueryBasic q = new QueryBasic();
        q.getSelector().put(NetworkMapItem.FIELD_NODE_UID,
                tnw.getNodeInformation().getNodeUid().toString());
        try {
            NetworkMapItemLead i = repoSource.read(q).get(0);
            i.setModeratorsForLead(modsForLead);
            updateLeadItem(i, tnSource, sourcePk, allLeads);
            repoSource.update(i);

        } catch (Exception e) {
            NetworkMapItemLead i = new NetworkMapItemLead();
            updateLeadItem(i, tnSource, sourcePk, allLeads);
            logger.warn("CREATING NEW NETWORK_MAP_ITEM_SOURCE() " + i.getNodeUID());
            repoSource.create(i);

        }
    }


    private void updateLeadItem(NetworkMapItem i, TrustNetwork tn, byte[] sourcePk,
                                NetworkMapNodeOverview allLeads) {
        NodeInformation ni = tn.getNodeInformation();
        i.setLastUpdated(tn.getLastUpdated());
        i.setNodeUID(ni.getNodeUid());
        i.setBroadcastAddress(ni.getBroadcastAddress());
        i.setStaticURL0(ni.getStaticNodeUrl0());
        i.setPublicKeyB64(sourcePk);
        i.setRulebookNodeURL(ni.getRulebookNodeUrl());

        String[] n = ni.getNodeUid().toString().split(":");
        boolean isHost = n.length==5;
        logger.debug(ni.getLeadUid() + " isHost=" + isHost + " " + allLeads.getThisNodeLeadUID());

        if (ni.getLeadUid().equals(allLeads.getThisModeratorLeadUID())){
            if (isHost){
                i.setType(NetworkMapItem.TYPE);

            }
        }
    }

    private NetworkMapItem updateItem(NetworkParticipant participant, boolean lead) throws Exception {
        NetworkMapItem nmi = new NetworkMapItemModerator();
        if (lead){
            nmi = new NetworkMapItemLead();
        }
        URI sourceUuid = UIDHelper.computeLeadUidFromModUid(participant.getNodeUid());
        nmi.setType(NetworkMapItem.TYPE);
        nmi.setLastUpdated(participant.getLastUpdateTime());
        nmi.setLeadUID(sourceUuid);
        nmi.setNodeUID(participant.getNodeUid());
        nmi.setBroadcastAddress(participant.getBroadcastAddress());
        nmi.setStaticURL0(participant.getStaticNodeUrl0());
        nmi.setPublicKeyB64(participant.getPublicKey().getPublicKey());
        nmi.setRulebookNodeURL(participant.getRulebookNodeUrl());
        nmi.setRegion(participant.getRegion());
        URI lastUid = participant.getLastIssuerUID();
        if (lastUid!=null){
            UIDHelper helper = new UIDHelper(lastUid);
            nmi.setLastIssuerUID(lastUid);
            nmi.setLeadName(helper.getLeadName());
            nmi.setModeratorName(helper.getModeratorName());

        }
        return nmi;

    }

    private void populateNodeFields() throws Exception {
        try {
            CouchRepository<NetworkMapNodeOverview> repo = CouchDbHelper.repoNetworkMapLeadOverview();
            NetworkMapNodeOverview overview = repo.read(QueryBasic.selectType(NetworkMapNodeOverview.TYPE_NETWORK_MAP_NODE_OVERVIEW)).get(0);
            this.myModeratorUID = overview.getModeratorUID();
            this.myModeratorsLeadUID = overview.getThisModeratorLeadUID();
            this.myNodesLeadUID = overview.getThisNodeLeadUID();

        } catch (NoDocumentException e) {
            String name = RulebookNodeProperties.instance().getDbPrefix() + "_network";
            UxException e0 = new UxException(ErrorMessages.MOD_NOT_FOUND_ON_NETWORK_MAP, name);
            throw new UxException(ErrorMessages.RULEBOOK_NODE_NOT_INITIALIZED, e0,
                    "It is could be that this node is not yet part of the Trust Network it wants to join.");

        }
    }

    @Override
    public NetworkMapItemLead nmiForMyNodesLead() throws Exception {
        if (myNodesLeadUID ==null){
            populateNodeFields();

        } if (myNodesLeadUID ==null){
            throw new HubException("THIS_NODE_IS_NOT_A_SOURCE");

        }
        return (NetworkMapItemLead) nmiForNode(myNodesLeadUID);

    }

    public NetworkMapItemLead nmiForMyModeratorsLead() throws Exception {
        if (myModeratorsLeadUID ==null){
            populateNodeFields();

        } if (myModeratorsLeadUID ==null){
            throw new HubException("THIS_NODE_IS_NOT_A_SOURCE");

        }
        return (NetworkMapItemLead) nmiForNode(myModeratorsLeadUID);

    }

    @Override
    public NetworkMapItemModerator nmiForMyNodesModerator() throws Exception {
        if (myModeratorUID ==null){
            populateNodeFields();

        } if (myModeratorUID ==null){
            throw new HubException("THIS_NODE_IS_NOT_AN_ADVOCATE");

        }
        return (NetworkMapItemModerator) nmiForNode(myModeratorUID);

    }

    public static void main(String[] args) {
        logger.info(UIDHelper.transformMaterialUid(URI.create("urn:rulebook:asdasds:asdasd:asda:asda:i"), "rai"));

    }

}
