package io.exonym.rulebook.context;

import com.cloudant.client.org.lightcouch.DocumentConflictException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.actions.AbstractNetworkMap;
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
import io.exonym.utils.storage.CacheContainer;
import io.exonym.lite.standard.WhiteList;
import io.exonym.lite.time.DateHelper;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.rulebook.schema.CacheNodeContainer;
import io.exonym.utils.storage.*;
import io.exonym.lite.pojo.NodeData;
import io.exonym.rulebook.schema.XNodeContainer;
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
    private NetworkMapNodeOverview allSources = null;

    private URI myAdvocatesSourceUID;
    private URI myAdvocateUID;
    private URI myNodesSourceUID;


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
     * @param nmis
     * @param advocatesForSource
     * @throws Exception
     */
    @Override
    protected void writeVerifiedSource(String rulebookId, String source,
                                       NetworkMapItemSource nmis,
                                       ArrayList<NetworkMapItemAdvocate> advocatesForSource) throws Exception {
        throw new RuntimeException("spawn() was overridden and this shouldn't have been called");
    }


    @Override
    protected NodeVerifier openNodeVerifier(URL staticNodeUrl0, URL staticNodeUrl1, boolean isTargetSource) throws Exception {
        return NodeVerifier.tryNode(staticNodeUrl0, staticNodeUrl1, true, false);
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
                allSources = repo.read(q).get(0);
                localStateDefinition(allSources);
                globalStateDefinition(allSources);
                defineListeners();
                repo.update(allSources);

            } catch (NoDocumentException e) {
                logger.warn("CREATING NEW NETWORK_MAP_NODE_OVERVIEW()");
                allSources = new NetworkMapNodeOverview();
                localStateDefinition(allSources);
                globalStateDefinition(allSources);
                defineListeners();
                repo.create(allSources);

            }
        } catch (Exception e) {
            logger.error("Failed to Initialize Network Map ", e);

        }
    }

    private void defineListeners() {
        ArrayList<URI> sources = new ArrayList<>(allSources.getSources().keySet());
        String rh = UIDHelper.computeRulebookHashFromSourceUid(
                allSources.getThisAdvocateSourceUID());
        if (rh!=null){
            HashSet<URI> listenTo = allSources.getListeningToSources();
            for (URI uri : sources) {
                if (uri.toString().contains(rh)){
                    listenTo.add(uri);

                } else if (Rulebook.isSybil(uri)){
                    listenTo.add(uri);

                }
            }
        }

    }

    public NetworkMapNodeOverview getAllSources() {
        return allSources;
    }

    @Override
    public boolean networkMapExists() throws Exception {
        CouchRepository<NetworkMapNodeOverview> repo = CouchDbHelper.repoNetworkMapSourceOverview();
        try {
            QueryBasic q = QueryBasic.selectType(NetworkMapNodeOverview.TYPE_NETWORK_MAP_NODE_OVERVIEW);
            allSources = repo.read(q).get(0);
            return true;

        } catch (NoDocumentException e) {
            return false;

        }
    }

    private void localStateDefinition(NetworkMapNodeOverview allSources) {
        try {
            CouchRepository<NodeData> repo = CouchDbHelper.repoNodeData();
            ArrayList<String> orGate = new ArrayList<>();
            orGate.add(NodeData.TYPE_NODE);
            orGate.add(NodeData.TYPE_SOURCE);
            QueryOrGate query = new QueryOrGate("type", orGate);
            List<NodeData> node = repo.read(query);
            HashMap<String, NodeData> local = new HashMap<>();
            for (NodeData n : node){
                local.put(n.getType(), n);

            }
            NodeData host = local.get(NodeData.TYPE_NODE);
            NodeData source = local.get(NodeData.TYPE_SOURCE);
            if (host!=null){
                allSources.setAdvocateUID(host.getNodeUid());
                URI sourceUuid = UIDHelper.computeSourceUidFromNodeUid(host.getNodeUid());
                allSources.setThisAdvocateSourceUID(sourceUuid);
                allSources.setLatestRevocationInformationHash(host.getLastRAIHash());

            } if (source!=null){
                allSources.setThisNodeSourceUID(source.getNodeUid());
                allSources.setLatestPresentationPolicyHash(source.getLastPPHash());

            }
            if (host!=null && source!=null){
                logger.debug(
                        NetworkMapNodeOverview.LOCAL_STATE_INDEPENDENT_SOURCE_AND_HOST + " : "
                                + source.getNodeUid() + " " + host.getSourceUid());

                if (source.getNodeUid().equals(host.getSourceUid())){
                    allSources.setCurrentLocalState(
                            NetworkMapNodeOverview.LOCAL_STATE_SOURCE_AND_HOST);

                } else {
                    allSources.setCurrentLocalState(
                            NetworkMapNodeOverview.LOCAL_STATE_INDEPENDENT_SOURCE_AND_HOST);

                }
            } else if (host!=null){
                allSources.setCurrentLocalState(
                        NetworkMapNodeOverview.LOCAL_STATE_HOST);

            } else if (source!=null){
                allSources.setCurrentLocalState(
                        NetworkMapNodeOverview.LOCAL_STATE_SOURCE);

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

        if (WhiteList.isAdvocateUid(uid)){
            CouchRepository<NetworkMapItemAdvocate> repo = CouchDbHelper.repoNetworkMapItemAdvocate();
            return repo.read(q).get(0);

        } else {
            CouchRepository<NetworkMapItemSource> repo = CouchDbHelper.repoNetworkMapItemSource();
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
        logger.debug("all source = " + allSources);
        logger.debug("all sourceslistening " + allSources.getListeningToSources());
        for (URI i : allSources.getListeningToSources()){
            if (i.toString().contains(target.toString())){
                return allSources.getSources().get(i);

            }
        }
        throw new HubException("Failed to find target:" + target);

    }

    protected void globalStateDefinition(NetworkMapNodeOverview allSources) {
        logger.info("Refreshing NetworkMap");
        try {
            TrustNetwork t = openSourceSet(allSources);
            TrustNetworkWrapper tnw = new TrustNetworkWrapper(t);

            DateTime remote = new DateTime(t.getLastUpdated());
            String lastFullCycle = allSources.getLastRefresh();

            if (lastFullCycle!=null){
                DateTime local = new DateTime(lastFullCycle);

                if (remote.isAfter(local)) {
                    logger.debug("Local Source Set Required Refreshing (local/remote):\n\t\t "
                            + DateHelper.isoUtcDateTime(local) + " / "
                            + DateHelper.isoUtcDateTime(remote));
                    allSources.setLastRefresh(t.getLastUpdated());
                    refreshSources(tnw, allSources);

                } else if (allSources.getSources().isEmpty()){
                    allSources.setLastRefresh(t.getLastUpdated());
                    refreshSources(tnw, allSources);

                } else {
                    logger.debug("Sources are up to date.");

                }
            } else {
                logger.debug("Last Full Cycle was not set - Refreshing Sources");
                allSources.setLastRefresh(t.getLastUpdated());
                refreshSources(tnw, allSources);

            }
            refreshNodesOnNecessarySource(allSources);

        } catch (FileNotFoundException e) {
            logger.debug("Handled exception thrown");
            allSources.setLastRefresh(null);
            allSources.setCurrentGlobalState(
                    NetworkMapNodeOverview.GLOBAL_STATE_SOURCE_SET_UNAVAILABLE);

        } catch (Exception e) {
            logger.error("Unsure what to set the global state as here.", e);

        }
    }

    private TrustNetwork openSourceSet(NetworkMapNodeOverview allSources) throws Exception {
        try {
            String sources = props.getSpawnWiderNetworkFrom();
            byte[] s = UrlHelper.readXml(new URL(sources));
            return JaxbHelper.xmlToClass(new String(s, StandardCharsets.UTF_8), TrustNetwork.class);

        } catch (Exception e) {
            allSources.setCurrentGlobalState(
                    NetworkMapNodeOverview.GLOBAL_STATE_SOURCE_SET_UNAVAILABLE);
            throw e;

        }
    }

    @Deprecated
    protected void refreshPresentationPoliciesIfNecessary() throws UxException {
        try {
            NetworkMapNodeOverview state = openState();
            ConcurrentHashMap<URI, NetworkMapItemSource> sources = state.getSources();
            if (state.isSourceRequiresUpdate()) {
                sourceCryptoUpdate(sources.get(state.getThisNodeSourceUID()));

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
                    source.getStaticURL1(), true);
            IAuthenticator auth = IAuthenticator.getInstance();
            XNodeContainer container = new XNodeContainer(auth.getContainerNameForNode());
            container.saveLocalResource(verifier.getPresentationPolicy(), true);
            container.saveLocalResource(verifier.getCredentialSpecification(), true);
            boolean conflicted = true;
            while (conflicted){
                try {
                    NetworkMapNodeOverview nm = openState();
                    nm.setSourceRequiresUpdate(false);
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
            XNodeContainer container = new XNodeContainer(auth.getContainerNameForNode());
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

    private void refreshSources(TrustNetworkWrapper tnw, NetworkMapNodeOverview allSources) {
        logger.debug("Refreshing Sources");
        ConcurrentHashMap<URI, NetworkMapItemSource> sources = allSources.getSources();
        sources.clear();
        URI thisNodesSource = allSources.getThisAdvocateSourceUID();
        thisNodesSource = (thisNodesSource==null ?
                allSources.getThisNodeSourceUID() : thisNodesSource);

        boolean isSourceAdded = false;

        for (NetworkParticipant participant : tnw.getAllParticipants()) {
            try {
                NetworkMapItemSource nmis = (NetworkMapItemSource) updateItem(participant, true);
                sources.put(nmis.getSourceUID(), nmis);

                logger.debug(nmis.getNodeUID() + " " + thisNodesSource);
                if (nmis.getNodeUID().equals(thisNodesSource)){
                    isSourceAdded = true;

                }
            } catch (MalformedURLException e) {
                logger.error("Bad URL", e);

            } catch (Exception e) {
                logger.error("Unexpected Error", e);

            }
        }
        logger.debug("Sources Detected from WiderWrapper=" + sources.size());
        allSources.setSources(sources);

        if (isSourceAdded){
            allSources.setCurrentGlobalState(
                    NetworkMapNodeOverview.GLOBAL_STATE_THIS_SOURCE_LISTED_HOST_INDETERMINATE);

        } else {
            allSources.setCurrentGlobalState(
                    NetworkMapNodeOverview.GLOBAL_STATE_SOURCE_SET_AVAILABLE_THIS_SOURCE_UNLISTED);

        }
    }

    private void refreshNodesOnNecessarySource(NetworkMapNodeOverview allSources) throws Exception {

        ConcurrentHashMap<URI, NetworkMapItemSource> sources = allSources.getSources();

        logger.info("Refreshing Nodes on Necessary Sources " + sources.size());
        CouchRepository<NetworkMapItemSource> repo = CouchDbHelper.repoNetworkMapItemSource();
        for (URI sourceUID : sources.keySet()){
            NetworkMapItem source = sources.get(sourceUID);
            logger.info("Working Source: " + source.getNodeUID() + " " + source);

            try {
                String url0 = source.getStaticURL0() + "/signatures.xml";
//                String url1 = source.getUrl1() + "/signatures.xml";
                URI niUid = URI.create(source.getNodeUID() + ":ni");
                String fn = XContainer.uidToXmlFileName(niUid);
                String urlNi0 = source.getStaticURL0() + "/" + fn;
//                String urlNi1 = source.getUrl1() + "/" + fn;

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
                byte[] pkBytes = source.getPublicKeyB64();
                key.assembleKey(source.getPublicKeyB64());
                key.verifySignature(pkBytes, pkSig.getSignature());
                key.verifySignature(niString.getBytes(StandardCharsets.UTF_8), niSig.getSignature());

                TrustNetwork tn = JaxbHelper.xmlToClass(ni, TrustNetwork.class);
                QueryBasic q = new QueryBasic();
                q.getSelector().put(NetworkMapItem.FIELD_NODE_UID, source.getNodeUID().toString());


                try {
                    NetworkMapItemSource s0 = repo.read(q).get(0);
                    DateTime lastLocalUpdate = new DateTime(s0.getLastUpdated());
                    logger.debug("Local Date Time " + DateHelper.isoUtcDateTime(lastLocalUpdate));
                    DateTime lastRemoteUpdate = new DateTime(tn.getLastUpdated());
                    logger.debug("Remote Date Time " + DateHelper.isoUtcDateTime(lastLocalUpdate));
                    if (lastRemoteUpdate.isAfter(lastLocalUpdate)){
                        refreshNodes(tn, pkBytes, allSources, q, repo);

                    }
                } catch (NoDocumentException e) {
                    logger.warn("CREATING NEW NetworkMapItemSource() " + sourceUID);
                    NetworkMapItemSource n = new NetworkMapItemSource();
                    n.setNodeUID(sourceUID);
                    repo.create(n);
                    refreshNodes(tn, pkBytes, allSources, q, repo);

                }
            } catch (Exception e) {
                logger.info("Failed to Find Source " + source.getNodeUID());
                logger.error("Error", e);

            }
        }
    }

    private void refreshNodes(TrustNetwork tnSource, byte[] sourcePk,
                              NetworkMapNodeOverview allSources,
                              QueryBasic queryForSource,
                              CouchRepository<NetworkMapItemSource> repoSource) throws Exception {


        URI sourceUid = tnSource.getNodeInformation().getNodeUid();
        NetworkMapItemSource nmis = repoSource.read(queryForSource).get(0);


        logger.debug("Refreshing Node " + sourceUid);
        TrustNetworkWrapper tnw = new TrustNetworkWrapper(tnSource);
        Collection<NetworkParticipant> advocates = tnw.getAllParticipants();

        ArrayList<URI> advocatesForSource = nmis.getAdvocatesForSource();
        for (NetworkParticipant p : advocates){
            advocatesForSource.add(p.getNodeUid());
            logger.debug("Advocate for Source = " + p.getNodeUid());
        }


        for (URI uri : nmis.getAdvocatesForSource()){
            logger.debug("Check After Update  = " + uri);

        }
        CouchRepository<NetworkMapItemAdvocate> repoAdvocate = CouchDbHelper.repoNetworkMapItemAdvocate();
        boolean isHostListed = false;

        URI thisHostSourceUuid = allSources.getThisAdvocateSourceUID();
        URI targetNodeSourceUid = tnSource.getNodeInformation().getSourceUid();
        logger.debug("isSourceWithinScope==" + thisHostSourceUuid + " " + targetNodeSourceUid);

        boolean isSourceWithinScope = (thisHostSourceUuid!=null &&
                thisHostSourceUuid.equals(targetNodeSourceUid));

        logger.debug("isSourceWithinScope=" + isSourceWithinScope );

        for (NetworkParticipant advocate : advocates){
            QueryBasic q = new QueryBasic();
            q.getSelector().put(NetworkMapItem.FIELD_NODE_UID, advocate.getNodeUid().toString());

            try {
                NetworkMapItemAdvocate i = repoAdvocate.read(q).get(0);
                NetworkMapItem j = updateItem(advocate, false);
                j.set_id(i.get_id());
                j.set_rev(i.get_rev());

                URI hostUuid = allSources.getAdvocateUID();
                if (isSourceWithinScope && hostUuid!=null){
                    if (hostUuid.equals(i.getNodeUID())){
                        isHostListed = true;

                    }
                }
                repoAdvocate.update(j);

            } catch (NoDocumentException e) {
                NetworkMapItem i = updateItem(advocate, false);
                URI advocateUID = allSources.getAdvocateUID();
                if (isSourceWithinScope && advocateUID!=null){
                    if (advocateUID.equals(i.getNodeUID())){
                        isHostListed = true;

                    }
                }
                logger.warn("CREATING NEW NetworkMapItemAdvocate() " + i.getNodeUID());
                repoAdvocate.create(i);

            }
        }
        if (isSourceWithinScope && isHostListed){
            allSources.setCurrentGlobalState(
                    NetworkMapNodeOverview.GLOBAL_STATE_THIS_NODE_LISTED);

        } else if (isSourceWithinScope){
            allSources.setCurrentGlobalState(
                    NetworkMapNodeOverview.GLOBAL_STATE_DEFINED_SOURCE_LISTED__THIS_HOST_UNLISTED);

        }
        QueryBasic q = new QueryBasic();
        q.getSelector().put(NetworkMapItem.FIELD_NODE_UID,
                tnw.getNodeInformation().getNodeUid().toString());
        try {
            NetworkMapItemSource i = repoSource.read(q).get(0);
            i.setAdvocatesForSource(advocatesForSource);
            updateSourceItem(i, tnSource, sourcePk, allSources);
            repoSource.update(i);

        } catch (Exception e) {
            NetworkMapItemSource i = new NetworkMapItemSource();
            updateSourceItem(i, tnSource, sourcePk, allSources);
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
        i.setStaticURL1(ni.getStaticNodeUrl1());
        i.setRulebookNodeURL(ni.getRulebookNodeUrl());

        String[] n = ni.getNodeUid().toString().split(":");
        boolean isHost = n.length==5;
        logger.debug(ni.getSourceUid() + " isHost=" + isHost + " " + allSources.getThisNodeSourceUID());

        if (ni.getSourceUid().equals(allSources.getThisAdvocateSourceUID())){
            if (isHost){
                i.setType(NetworkMapItem.TYPE);

            }
        }
    }

    private NetworkMapItem updateItem(NetworkParticipant participant, boolean source) throws Exception {
        NetworkMapItem nmi = new NetworkMapItemAdvocate();
        if (source){
            nmi = new NetworkMapItemSource();
        }
        URI sourceUuid = UIDHelper.computeSourceUidFromNodeUid(participant.getNodeUid());
        nmi.setType(NetworkMapItem.TYPE);
        nmi.setLastUpdated(participant.getLastUpdateTime());
        nmi.setSourceUID(sourceUuid);
        nmi.setNodeUID(participant.getNodeUid());
        nmi.setBroadcastAddress(participant.getBroadcastAddress());
        nmi.setStaticURL0(participant.getStaticNodeUrl0());
        nmi.setStaticURL1(participant.getStaticNodeUrl1());
        nmi.setPublicKeyB64(participant.getPublicKey().getPublicKey());
        nmi.setRulebookNodeURL(participant.getRulebookNodeUrl());
        nmi.setRegion(participant.getRegion());
        URI lastUid = participant.getLastIssuerUID();
        if (lastUid!=null){
            UIDHelper helper = new UIDHelper(lastUid);
            nmi.setLastIssuerUID(lastUid);
            nmi.setSourceName(helper.getSourceName());
            nmi.setAdvocateName(helper.getAdvocateName());

        }
        return nmi;

    }

    private void populateNodeFields() throws Exception {
        try {
            CouchRepository<NetworkMapNodeOverview> repo = CouchDbHelper.repoNetworkMapSourceOverview();
            NetworkMapNodeOverview overview = repo.read(QueryBasic.selectType(NetworkMapNodeOverview.TYPE_NETWORK_MAP_NODE_OVERVIEW)).get(0);
            this.myAdvocateUID = overview.getAdvocateUID();
            this.myAdvocatesSourceUID = overview.getThisAdvocateSourceUID();
            this.myNodesSourceUID = overview.getThisNodeSourceUID();

        } catch (NoDocumentException e) {
            throw new UxException(ErrorMessages.RULEBOOK_NODE_NOT_INITIALIZED,
                    "It is likely that this node is not yet part of the Source it claims to be.");

        }
    }

    @Override
    public NetworkMapItemSource nmiForMyNodesSource() throws Exception {
        if (myNodesSourceUID ==null){
            populateNodeFields();

        } if (myNodesSourceUID==null){
            throw new HubException("THIS_NODE_IS_NOT_A_SOURCE");

        }
        return (NetworkMapItemSource) nmiForNode(myNodesSourceUID);

    }

    public NetworkMapItemSource nmiForMyAdvocatesSource() throws Exception {
        if (myAdvocatesSourceUID ==null){
            populateNodeFields();

        } if (myAdvocatesSourceUID==null){
            throw new HubException("THIS_NODE_IS_NOT_A_SOURCE");

        }
        return (NetworkMapItemSource) nmiForNode(myAdvocatesSourceUID);

    }

    @Override
    public NetworkMapItemAdvocate nmiForMyNodesAdvocate() throws Exception {
        if (myAdvocateUID ==null){
            populateNodeFields();

        } if (myAdvocateUID==null){
            throw new HubException("THIS_NODE_IS_NOT_AN_ADVOCATE");

        }
        return (NetworkMapItemAdvocate) nmiForNode(myAdvocateUID);

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
