package io.exonym.rulebook.context;

import com.cloudant.client.org.lightcouch.DocumentConflictException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.actions.AbstractNetworkMap;
import io.exonym.actor.actions.MyStaticData;
import io.exonym.actor.actions.NodeVerifier;
import io.exonym.actor.actions.TrustNetworkWrapper;
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
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
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


    @Override
    protected NodeVerifier openNodeVerifier(URI staticNodeUrl0, URI
            staticNodeUrl1, boolean isTargetSource) throws Exception {
        return NodeVerifier.tryNode(staticNodeUrl0,
                staticNodeUrl1, true, false);
    }

    protected void refresh(){
        spawn();
    }
    @Override
    public void spawn() {
        try {
            CouchRepository<NetworkMapNodeOverview> repo = CouchDbHelper.repoNetworkMapSourceOverview();
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

    private void defineListeners() {
        ArrayList<URI> sources = new ArrayList<>(allLeads.getSources().keySet());
        String rh = UIDHelper.computeRulebookHashFromLeadUid(
                allLeads.getThisModeratorLeadUID());
        if (rh!=null){
            HashSet<URI> listenTo = allLeads.getListeningToLeads();
            for (URI uri : sources) {
                if (uri.toString().contains(rh)){
                    listenTo.add(uri);

                } else if (Rulebook.isSybil(uri)){
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
        CouchRepository<NetworkMapNodeOverview> repo = CouchDbHelper.repoNetworkMapSourceOverview();
        try {
            QueryBasic q = QueryBasic.selectType(
                    NetworkMapNodeOverview.TYPE_NETWORK_MAP_NODE_OVERVIEW);

            allLeads = repo.read(q).get(0);
            return true;

        } catch (NoDocumentException e) {
            return false;

        }
    }

    private void localStateDefinition(NetworkMapNodeOverview allSources) {
        try {
            CouchRepository<NodeData> repo = CouchDbHelper.repoNodeData();
            ArrayList<String> orGate = new ArrayList<>();
            orGate.add(NodeData.TYPE_MODERATOR);
            orGate.add(NodeData.TYPE_LEAD);
            QueryOrGate query = new QueryOrGate("type", orGate);
            List<NodeData> node = repo.read(query);
            HashMap<String, NodeData> local = new HashMap<>();
            for (NodeData n : node){
                local.put(n.getType(), n);

            }
            NodeData host = local.get(NodeData.TYPE_MODERATOR);
            NodeData source = local.get(NodeData.TYPE_LEAD);
            if (host!=null){
                allSources.setModeratorUID(host.getNodeUid());
                URI sourceUuid = UIDHelper.computeLeadUidFromModUid(host.getNodeUid());
                allSources.setThisModeratorLeadUID(sourceUuid);
                allSources.setLatestRevocationInformationHash(host.getLastRAIHash());

            } if (source!=null){
                allSources.setThisNodeLeadUID(source.getNodeUid());
                allSources.setLatestPresentationPolicyHash(source.getLastPPHash());

            }
            if (host!=null && source!=null){
                logger.debug(
                        NetworkMapNodeOverview.LOCAL_STATE_INDEPENDENT_LEAD_AND_MODERATOR + " : "
                                + source.getNodeUid() + " " + host.getSourceUid());

                if (source.getNodeUid().equals(host.getSourceUid())){
                    allSources.setCurrentLocalState(
                            NetworkMapNodeOverview.LOCAL_STATE_LEAD_AND_MODERATOR);

                } else {
                    allSources.setCurrentLocalState(
                            NetworkMapNodeOverview.LOCAL_STATE_INDEPENDENT_LEAD_AND_MODERATOR);

                }
            } else if (host!=null){
                allSources.setCurrentLocalState(
                        NetworkMapNodeOverview.LOCAL_STATE_MODERATOR);

            } else if (source!=null){
                allSources.setCurrentLocalState(
                        NetworkMapNodeOverview.LOCAL_STATE_LEAD);

            } else {
                throw new Exception("Source and Host are both Null: should have received a NoDocumentException");

            }
        } catch (NoDocumentException e) {
            allSources.setCurrentLocalState(NetworkMapNodeOverview.LOCAL_STATE_UNDEFINED);

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
                return allLeads.getSources().get(i);

            }
        }
        throw new HubException("Failed to find target:" + target);

    }

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

                } else if (allLeads.getSources().isEmpty()){
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
            refreshNodesOnNecessarySource(allLeads);

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

    @Deprecated
    protected void refreshPresentationPoliciesIfNecessary() throws UxException {
        try {
            NetworkMapNodeOverview state = openState();
            ConcurrentHashMap<URI, NetworkMapItemLead> sources = state.getSources();
            if (state.isLeadRequiresUpdate()) {
                sourceCryptoUpdate(sources.get(state.getThisNodeLeadUID()));

            }
            if (state.isSybilRequiresUpdate()){
                sybilCryptoUpdate(sources.get(URI.create("sybil")));

            }
        } catch (Exception e) {
            throw new UxException(ErrorMessages.NETWORK_MAP_REFRESH_FAILURE);

        }
    }

    private NetworkMapNodeOverview openState() throws Exception {
        try {
            QueryBasic q = QueryBasic.selectType(NetworkMapNodeOverview.TYPE_NETWORK_MAP_NODE_OVERVIEW);
            return CouchDbHelper.repoNetworkMapSourceOverview().read(q).get(0);

        } catch (NoDocumentException e) {
            throw new HubException("Network Map Not Available");

        }
    }

    @Deprecated
    private void sourceCryptoUpdate(NetworkMapItem source) throws Exception {
        if (source!=null){
            NodeVerifier verifier = openNodeVerifier(source.getStaticURL0(),
                    source.getRulebookNodeURL().resolve("static"),
                    true);
            IAuthenticator auth = IAuthenticator.getInstance();
            IdContainer container = new IdContainer(auth.getContainerNameForNode());
            container.saveLocalResource(verifier.getPresentationPolicy(), true);
            container.saveLocalResource(verifier.getCredentialSpecification(), true);
            boolean conflicted = true;
            while (conflicted){
                try {
                    NetworkMapNodeOverview nm = openState();
                    nm.setLeadRequiresUpdate(false);
//                    mapSourceRepo.update(nm);
                    conflicted=false;

                } catch (DocumentConflictException e) {
                    logger.debug("Document Conflicted - trying again");

                }
            }
        } else {
            throw new HubException("Network Map Origin must listen to updates from Sybil");

        }

    }

    @Deprecated
    private void sybilCryptoUpdate(NetworkMapItem sybil) throws Exception {
        if (sybil!=null){
            NodeVerifier verifier = NodeVerifier.openNode(sybil.getStaticURL0(),
                    true, false);
            IAuthenticator auth = IAuthenticator.getInstance();
            IdContainer container = new IdContainer(auth.getContainerNameForNode());
            container.saveLocalResource(verifier.getCredentialSpecification(), true);
            container.saveLocalResource(verifier.getPresentationPolicy(), true);
            boolean conflicted = true;
            while (conflicted){
                try {
                    NetworkMapNodeOverview nm = openState();
                    nm.setSybilRequiresUpdate(false);
//                    mapSourceRepo.update(nm);
                    conflicted=false;

                } catch (DocumentConflictException e) {
                    logger.debug("Document Conflicted - trying again");

                }
            }
        } else {
            throw new HubException("Network Map Origin must listen to updates from Sybil");

        }
    }

    private void refreshLeads(TrustNetworkWrapper tnw, NetworkMapNodeOverview allLeads) {
        logger.debug("Refreshing Leads");
        ConcurrentHashMap<URI, NetworkMapItemLead> leads = allLeads.getSources();
        leads.clear();
        URI thisNodesLead = allLeads.getThisModeratorLeadUID();
        String thisModeratorUID = allLeads.getModeratorUID().toString();

        thisNodesLead = (thisNodesLead==null ?
                allLeads.getThisNodeLeadUID() : thisNodesLead);

        boolean isLeadAdded = false;
        boolean isModeratorAddedToLead = false;

        for (NetworkParticipant participant : tnw.getAllParticipants()) {
            try {
                NetworkMapItemLead nmis = (NetworkMapItemLead) updateItem(participant, true);
                leads.put(nmis.getLeadUID(), nmis);
                if (participant.getNodeUid().toString().equals(thisModeratorUID)){
                    isModeratorAddedToLead = true;
                }
                logger.debug(nmis.getNodeUID() + " " + thisNodesLead);
                if (nmis.getNodeUID().equals(thisNodesLead)){
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

    private void refreshNodesOnNecessarySource(NetworkMapNodeOverview allSources) throws Exception {

        ConcurrentHashMap<URI, NetworkMapItemLead> leads = allSources.getSources();

        logger.info("Refreshing Nodes on Necessary Sources " + leads.size());
        CouchRepository<NetworkMapItemLead> repo = CouchDbHelper.repoNetworkMapItemSource();

        MyStaticData mti = null;
        try {
            mti = new MyStaticData(true);

        } catch (Exception e) {
            try {
                mti = new MyStaticData(false);

            } catch (Exception ex) {
                logger.error("NODE NOTE YET ESTABLISHED");

            }
        }
        URI myNodeUrl = mti.getTrustNetworkWrapper()
                .getNodeInformation().getRulebookNodeUrl();

        for (URI sourceUID : leads.keySet()){
            NetworkMapItem lead = leads.get(sourceUID);
            logger.info("Working Source: " + lead.getNodeUID() + " " + lead);
            TrustNetwork tn = null;
            byte[] pkBytes = null;

            try {
                String url0 = lead.getStaticURL0() + Const.SIGNATURES_XML;
                URI niUid = Const.TRUST_NETWORK_UID;

                String fn = io.exonym.utils.storage.IdContainer.uidToXmlFileName(niUid);
                String urlNi0 = lead.getStaticURL0() + "/" + fn;
                String urlForNode = lead.getRulebookNodeURL().toString();
                logger.info("Computed local versus terget=" + urlForNode + " " + myNodeUrl);


                if (!urlForNode.toString().equals(myNodeUrl.toString())){
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
                    tn = mti.getTrustNetworkWrapper().getTrustNetwork();
                    pkBytes = mti.getKcw().getKey(
                            KeyContainerWrapper.TN_ROOT_KEY).getPublicKey();

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
                        refreshNodes(tn, pkBytes, allSources, q, repo);

                    }
                } catch (NoDocumentException e) {
                    logger.warn("CREATING NEW NetworkMapItemSource() " + sourceUID);
                    NetworkMapItemLead n = new NetworkMapItemLead();
                    n.setNodeUID(sourceUID);
                    repo.create(n);
                    refreshNodes(tn, pkBytes, allSources, q, repo);

                }

            } catch (Exception e) {
                logger.info("Failed to Find Source " + lead.getNodeUID());
                logger.error("Error", e);

            }
        }
    }

    private void refreshNodes(TrustNetwork tnSource, byte[] sourcePk,
                              NetworkMapNodeOverview allLeads,
                              QueryBasic queryForSource,
                              CouchRepository<NetworkMapItemLead> repoSource) throws Exception {


        URI sourceUid = tnSource.getNodeInformation().getNodeUid();
        NetworkMapItemLead nmis = repoSource.read(queryForSource).get(0);

        logger.debug("Refreshing Node " + sourceUid);
        TrustNetworkWrapper tnw = new TrustNetworkWrapper(tnSource);
        Collection<NetworkParticipant> moderators = tnw.getAllParticipants();

        ArrayList<URI> modsForLead = nmis.getModeratorsForLead();
        for (NetworkParticipant p : moderators){
            modsForLead.add(p.getNodeUid());
            logger.debug("Mod for Lead = " + p.getNodeUid());
        }


        for (URI uri : nmis.getModeratorsForLead()){
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
            updateSourceItem(i, tnSource, sourcePk, allLeads);
            repoSource.update(i);

        } catch (Exception e) {
            NetworkMapItemLead i = new NetworkMapItemLead();
            updateSourceItem(i, tnSource, sourcePk, allLeads);
            logger.warn("CREATING NEW NETWORK_MAP_ITEM_SOURCE() " + i.getNodeUID());
            repoSource.create(i);

        }
    }


    private void updateSourceItem(NetworkMapItem i, TrustNetwork tn, byte[] sourcePk,
                                  NetworkMapNodeOverview allSources) {
        NodeInformation ni = tn.getNodeInformation();
        i.setLastUpdated(tn.getLastUpdated());
        i.setNodeUID(ni.getNodeUid());
        i.setBroadcastAddress(ni.getBroadcastAddress());
        i.setStaticURL0(ni.getStaticNodeUrl0());
        i.setRulebookNodeURL(ni.getRulebookNodeUrl());

        String[] n = ni.getNodeUid().toString().split(":");
        boolean isHost = n.length==5;
        logger.debug(ni.getLeadUid() + " isHost=" + isHost + " " + allSources.getThisNodeLeadUID());

        if (ni.getLeadUid().equals(allSources.getThisModeratorLeadUID())){
            if (isHost){
                i.setType(NetworkMapItem.TYPE);

            }
        }
    }

    private NetworkMapItem updateItem(NetworkParticipant participant, boolean source) throws Exception {
        NetworkMapItem nmi = new NetworkMapItemModerator();
        if (source){
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
            CouchRepository<NetworkMapNodeOverview> repo = CouchDbHelper.repoNetworkMapSourceOverview();
            NetworkMapNodeOverview overview = repo.read(QueryBasic.selectType(NetworkMapNodeOverview.TYPE_NETWORK_MAP_NODE_OVERVIEW)).get(0);
            this.myModeratorUID = overview.getModeratorUID();
            this.myModeratorsLeadUID = overview.getThisModeratorLeadUID();
            this.myNodesLeadUID = overview.getThisNodeLeadUID();

        } catch (NoDocumentException e) {
            throw new UxException(ErrorMessages.RULEBOOK_NODE_NOT_INITIALIZED,
                    "It is likely that this node is not yet part of the Source it claims to be.");

        }
    }

    @Override
    public NetworkMapItemLead nmiForMyNodesSource() throws Exception {
        if (myNodesLeadUID ==null){
            populateNodeFields();

        } if (myNodesLeadUID ==null){
            throw new HubException("THIS_NODE_IS_NOT_A_SOURCE");

        }
        return (NetworkMapItemLead) nmiForNode(myNodesLeadUID);

    }

    public NetworkMapItemLead nmiForMyAdvocatesSource() throws Exception {
        if (myModeratorsLeadUID ==null){
            populateNodeFields();

        } if (myModeratorsLeadUID ==null){
            throw new HubException("THIS_NODE_IS_NOT_A_SOURCE");

        }
        return (NetworkMapItemLead) nmiForNode(myModeratorsLeadUID);

    }

    @Override
    public NetworkMapItemModerator nmiForMyNodesAdvocate() throws Exception {
        if (myModeratorUID ==null){
            populateNodeFields();

        } if (myModeratorUID ==null){
            throw new HubException("THIS_NODE_IS_NOT_AN_ADVOCATE");

        }
        return (NetworkMapItemModerator) nmiForNode(myModeratorUID);

    }

    public static void main(String[] args) throws Exception{
        String jso = "{\"type\":\"ACK\",\"advocateUID\":\"urn:rulebook:exosources:baseline:69bb840695e4fd79a00577de5f0071b311bbd8600430f6d0da8f865c5c459d44\",\"t\":\"2023-03-21T09:46:54Z\",\"sigB64\":\"owi6c3JdrhH6PfWMVKSnD3Tu+NojKhhrWb3MXom1hu6SJEOMGGemIPr2nhkek1diK3flBpudvgZUVOD9iWq/KKNnwZgaT9Ujo4VXXgfZMXKRm2lqiPPMh07FYhyLc0Y8mlx+JPQQB63i8Fs8iFV5vszFqbtMZ6ZhxiCUw4QGA2M9MyO8Dzx8eYVQIUkDqhIWpyR0XzSWUuG3XnzQUv/NgyBLM+lYEDAqwKjJbOa+HO60D4HyMoqDDNiYVi5LP0F7plemq391BD2ln/6FXHmh7JEp52EhFUOQgFLzPcXMlVbK4vKyuhXRYFY0eP9gMO7mC8LARmjkYUZNpSo0ObChlg\\u003d\\u003d\"}";
        ExoNotify notify = JaxbHelper.jsonToClass(jso, ExoNotify.class);
        byte[] sig = ExoNotify.signatureOnAckAndOrigin(notify);
        byte[] sigIn = Base64.decodeBase64(notify.getSigB64().getBytes(StandardCharsets.UTF_8));
        AsymStoreKey key = AsymStoreKey.blank();
        key.assembleKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjdFeH4v9lcyQCDgMDrk8AFyn9HUlriX6ucOlC3JrY1khkuSG2PspendK86+uEYFh0ApA0oeAZwJTCuqyrbRq8WAvGY0dBq0nHff65wXwCCd1LDiZb51A0gs7JfdnZUa2qddmBcXalTG5DvvzO+G003YhIQtANRfArSObIzstTDi6fB9Hcqic0xsm7EUUB1qnmRwNBWRRSaP+oTv0nBZaE0q7lo5WGz6VitMaKCn37Kn0OQi4YP0os51RoI+DkHnZu7PNaG4WCZrNxWafnHeUVrCPwOw4sWsAIwNi9NGbxo5eYTZw9m8A/sDfDY5WWgVc/7e5lrmVpr38g4Qw4abqIwIDAQAB");
        key.verifySignature(sig, sigIn);

//        NetworkMapWeb map = new NetworkMapWeb();
//        map.spawn();


    }
}
