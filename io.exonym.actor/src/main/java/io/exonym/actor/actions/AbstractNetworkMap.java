package io.exonym.actor.actions;

import io.exonym.abc.util.JaxbHelper;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.connect.UrlHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.*;
import io.exonym.utils.storage.CacheContainer;
import io.exonym.utils.storage.NetworkParticipant;
import io.exonym.utils.storage.TrustNetwork;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractNetworkMap {

    private Path rootPath = null;
    private final CacheContainer cache;

    public AbstractNetworkMap() throws Exception {
        this.rootPath = defineRootPath();
        this.cache = instantiateCache();

    }

    public void spawnIfDoesNotExist() throws Exception {
        if (!this.networkMapExists()){
            this.spawn();

        }
    }

    /**
     *
     * @return the default path if it is a file system.  Null if you are using a database.
     */
    protected abstract Path defineRootPath();

    /**
     *
     * @return the appropriate Cache back for the environment.
     */
    protected abstract CacheContainer instantiateCache() throws Exception;

    /**
     * Write the NMIS and the NMIAs to your chosen repository
     *
     * @param rulebookId
     * @param source
     * @param nmis
     * @param advocatesForSource
     * @throws Exception
     */
    protected abstract void writeVerifiedSource(String rulebookId, String source, NetworkMapItemLead nmis,
                                       ArrayList<NetworkMapItemModerator> advocatesForSource) throws Exception;


    protected Path pathToRootPath() {
        if (rootPath==null){
            throw new RuntimeException("You have not defined a root path in the implementation");
        }
        return rootPath;

    }

    protected Path pathToSourcePath(String rulebook, String source) {
        return rootPath.resolve(rulebook).resolve(source);
    }

    protected Path pathToRulebookPath(String rulebook) {
        return rootPath.resolve(rulebook);
    }

    public void spawn() throws Exception {
        cleanupExisting();
        TrustNetworkWrapper tnw = new TrustNetworkWrapper(openSourceSet());
        Collection<NetworkParticipant> allSources = tnw.getAllParticipants();
        for (NetworkParticipant source : allSources){
            buildMapForSource(source);

        }
    }

    private void buildMapForSource(NetworkParticipant source) throws Exception {
        NetworkMapItemLead nmis = new NetworkMapItemLead();
        buildBasisNMI(nmis, source);
        nmis.setLeadUID(source.getNodeUid());
        ArrayList<URI> advocateListForSource = new ArrayList<>();
        ArrayList<NetworkMapItemModerator> advocatesForSource = verifySource(advocateListForSource, source);
        nmis.setModeratorsForLead(advocateListForSource);
        String[] parts = source.getNodeUid().toString().split(":");
        writeVerifiedSource(parts[3], parts[2], nmis, advocatesForSource);

    }


    public String toNmiFilename(URI advocate) {
        return advocate.toString()
//                .replaceAll(":" + rulebookId, "") // overly complex to recompute UID
                .replaceAll(":", ".") + ".nmi";

    }

    public URI fromNmiFilename(String filename) {
        if (filename==null){
            throw new NullPointerException();
        }
        return URI.create(filename.replaceAll(".nmi", "").replaceAll("\\.", ":"));
    }


    public abstract boolean networkMapExists() throws Exception;

    protected void cleanupExisting() {
    }

    private ArrayList<NetworkMapItemModerator> verifySource(ArrayList<URI> advocateListForSource, NetworkParticipant source) throws Exception {
        NodeVerifier verifier = openNodeVerifier(source.getStaticNodeUrl0(),
                source.getStaticNodeUrl1(), true);

        Rulebook rulebook = verifier.getRulebook();
        cache.store(rulebook);
        cache.store(verifier.getPresentationPolicy());
        cache.store(verifier.getCredentialSpecification());

        TrustNetworkWrapper tnw = new TrustNetworkWrapper(verifier.getTargetTrustNetwork());
        Collection<NetworkParticipant> allAdvocates = tnw.getAllParticipants();
        ArrayList<NetworkMapItemModerator> advocatesForSource = new ArrayList<>();
        for (NetworkParticipant advocate : allAdvocates){
            URI sourceUid = source.getNodeUid();
            advocateListForSource.add(advocate.getNodeUid());
            advocatesForSource.add(buildAdvocateNMIA(sourceUid, advocate));

        }
        return advocatesForSource;

    }

    protected abstract NodeVerifier openNodeVerifier(URI staticNodeUrl0,
                                                     URI staticNodeUrl1,
                                                     boolean isTargetSource) throws Exception;

    private NetworkMapItemModerator buildAdvocateNMIA(URI sourceUid, NetworkParticipant participant) throws Exception {
        NetworkMapItemModerator nmia = new NetworkMapItemModerator();
        buildBasisNMI(nmia, participant);
        nmia.setLeadUID(sourceUid);
        return nmia;

    }

    private void buildBasisNMI(NetworkMapItem nmi, NetworkParticipant participant) throws Exception {
        nmi.setNodeUID(participant.getNodeUid());
        nmi.setPublicKeyB64(participant.getPublicKey().getPublicKey());
        nmi.setStaticURL0(participant.getStaticNodeUrl0());
        nmi.setLastUpdated(participant.getLastUpdateTime());
        nmi.setBroadcastAddress(participant.getBroadcastAddress());
        nmi.setRulebookNodeURL(participant.getRulebookNodeUrl());
        nmi.setRegion(participant.getRegion());
        URI lastUid = participant.getLastIssuerUID();
        if (lastUid!=null){
            UIDHelper helper = new UIDHelper(lastUid);
            nmi.setLastIssuerUID(lastUid);
            nmi.setLeadName(helper.getLeadName());
            nmi.setModeratorName(helper.getModeratorName());

        }
    }

    private TrustNetwork openSourceSet() throws Exception {
        try {
            String sources = "https://trust.exonym.io/leads.xml";
            byte[] s = UrlHelper.readXml(new URL(sources));
            return JaxbHelper.xmlToClass(new String(s, StandardCharsets.UTF_8), TrustNetwork.class);

        } catch (Exception e) {
            throw e;

        }
    }


    public NetworkMapItem findNetworkMapItem(URI sourceOrAdvocate) throws Exception {
        if (sourceOrAdvocate==null){
            throw new HubException("Null URL - Programming Error");
        }
        URI sourceUid = UIDHelper.computeLeadUidFromModUid(sourceOrAdvocate);
        String sourceName = UIDHelper.computeLeadNameFromModOrLeadUid(sourceUid);
        String rulebookId = UIDHelper.computeRulebookHashFromLeadUid(sourceUid);
        Path path = null;
        if (UIDHelper.isAdvocateUid(sourceOrAdvocate)){
            path = pathToSourcePath(rulebookId, sourceName)
                    .resolve(toNmiFilename(sourceOrAdvocate));

            if (Files.exists(path)){
                return JaxbHelper.jsonFileToClass(path, NetworkMapItemModerator.class);

            } else {
                throw new UxException(ErrorMessages.FILE_NOT_FOUND,
                        path.toAbsolutePath().toString());

            }
        } else if (UIDHelper.isSourceUid(sourceOrAdvocate)){
            path = pathToRulebookPath(rulebookId).resolve(toNmiFilename(sourceOrAdvocate));

            if (Files.exists(path)){
                return JaxbHelper.jsonFileToClass(path, NetworkMapItemLead.class);

            } else {
                throw new UxException(ErrorMessages.FILE_NOT_FOUND,
                        path.toAbsolutePath().toString());

            }
        } else {
            throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, sourceOrAdvocate.toString());

        }
    }

    public List<String> getSourceFilenamesForRulebook(String rulebookId) throws UxException {
        if (rulebookId==null){
            throw new NullPointerException();

        } if (rulebookId.startsWith(Namespace.URN_PREFIX_COLON)){
            rulebookId = rulebookId.replaceAll(Namespace.URN_PREFIX_COLON, "");

        }
        Path path = pathToRulebookPath(rulebookId);
        if (Files.exists(path)){
            return Stream.of(new File(path.toString()).listFiles())
                    .filter(file -> !file.isDirectory())
                    .map(File::getName)
                    .collect(Collectors.toList());

        } else {
            throw new UxException(ErrorMessages.FILE_NOT_FOUND, "No such rulebook");

        }
    }

    public NetworkMapItem nmiForNode(URI uid) throws Exception {
        if (uid==null){
            throw new HubException("Null Node UID - Programming Error");

        }
        String fileName = toNmiFilename(uid);

        if (UIDHelper.isSourceUid(uid)){
            String rulebookId = UIDHelper.computeRulebookHashFromLeadUid(uid);
            Path nmiPath = pathToRulebookPath(rulebookId).resolve(fileName);
            return JaxbHelper.jsonFileToClass(nmiPath, NetworkMapItemLead.class);

        } else if (UIDHelper.isAdvocateUid(uid)){
            String rulebookId = UIDHelper.computeRulebookIdFromAdvocateUid(uid);
            String sourceName = UIDHelper.computeLeadNameFromModOrLeadUid(uid);
            Path nmiPath = pathToSourcePath(rulebookId, sourceName).resolve(fileName);
            return JaxbHelper.jsonFileToClass(nmiPath, NetworkMapItemModerator.class);

        } else {
            throw new UxException(ErrorMessages.FILE_NOT_FOUND + ":" + uid.toString());

        }
    }

    public NetworkMapItemLead nmiForSybilSource() throws Exception {
        return (NetworkMapItemLead) nmiForNode(Rulebook.SYBIL_SOURCE_UID);
    }

    public NetworkMapItemModerator nmiForSybilTestNet() throws Exception {
        return (NetworkMapItemModerator) nmiForNode(Rulebook.SYBIL_TEST_NET_UID);
    }

    public NetworkMapItemModerator nmiForSybilMainNet() throws Exception {
        return (NetworkMapItemModerator) nmiForNode(Rulebook.SYBIL_MAIN_NET_UID);
    }

    public NetworkMapItemLead nmiForMyNodesSource() throws Exception{
        throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, "Wallets do not have sources");
    }

    public NetworkMapItemModerator nmiForMyNodesAdvocate() throws Exception{
        throw new UxException(ErrorMessages.INCORRECT_PARAMETERS, "Wallets do not have advocates");
    }

    protected NetworkMapItem findRandomAdvocateForSource(String source) throws Exception {
        List<NetworkMapItem> hosts = findAdvocatesForSource(source);
        int size = hosts.size();
        int target = (int)(Math.random() * 1000000) % size;
        return hosts.get(target);

    }

    protected List<NetworkMapItem> findAdvocatesForSource(String source) throws Exception {
        return null;

    }
}
