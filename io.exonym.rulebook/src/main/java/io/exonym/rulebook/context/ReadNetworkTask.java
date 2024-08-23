package io.exonym.rulebook.context;

import eu.abc4trust.xml.*;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.actions.NodeManager;
import io.exonym.actor.actions.NodeVerifier;
import io.exonym.actor.actions.TrustNetworkWrapper;
import io.exonym.actor.actions.XContainerJSON;
import io.exonym.actor.storage.SFTPClient;
import io.exonym.lite.exceptions.HubException;
import io.exonym.lite.connect.UrlHelper;
import io.exonym.lite.time.DateHelper;
import io.exonym.lite.standard.PassStore;
import io.exonym.utils.storage.KeyContainer;
import io.exonym.utils.storage.NetworkParticipant;
import io.exonym.utils.storage.NodeInformation;
import io.exonym.utils.storage.TrustNetwork;
import io.exonym.rulebook.exceptions.ItemNotFoundException;
import io.exonym.lite.pojo.NodeData;
import io.exonym.rulebook.schema.XNodeContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
@Deprecated
public class ReadNetworkTask implements Job {
    
    private static final Logger logger = LogManager.getLogger(ReadNetworkTask.class);

    private final RulebookNodeProperties props = RulebookNodeProperties.instance();
    private final String baseUrl = props.getPrimaryDomain() + "/" + props.getPrimaryStaticDataFolder();
    private String networkName = null;
    private NodeVerifier ownNode = null;
    private TrustNetworkWrapper ownTrustNetwork = null;
    private SFTPClient ftp = null;
    private boolean amISource = false;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.info("Running network redundancy chron job " + DateHelper.currentIsoUtcDateTime());
        try {
            Collection<NodeData> node = findNodeData();
            for (NodeData item : node){
                networkName = item.getNetworkName();
                boolean source = item.getType().equals(NodeData.TYPE_LEAD);
                ownNode = NodeVerifier.openNode(item.getNodeUrl(), source, source);
                ownTrustNetwork = new TrustNetworkWrapper(ownNode.getTargetTrustNetwork());
                processNode(item);

            }
        } catch (ItemNotFoundException e){
            logger.info("CHRONJOB: There are no sources or nodes on this XNode");

        } catch (Exception e){
            logger.error("Chronjob error", e);

        }
    }

    private void processNode(NodeData self) throws Exception {
        NodeVerifier source = rootToSource(self);
        TrustNetworkWrapper sourceTrustNetwork = new TrustNetworkWrapper(source.getTargetTrustNetwork());
        Collection<NetworkParticipant> participants = sourceTrustNetwork.getAllParticipants();
        for (NetworkParticipant participant : participants){
            updateNodeIfAndOnlyIfNecessary(self, source, participant);

        }
    }

    private void updateNodeIfAndOnlyIfNecessary(NodeData self, NodeVerifier source, NetworkParticipant participant) throws Exception {
        KeyContainer kc = openLocalSignatureData(source.getNodeName(), participant.getNodeUid());
        boolean amISource = self.getType().equals(NodeData.TYPE_LEAD);

        if (kc!=null){ // Local Data Found
            String t = kc.getLastUpdateTime();
            try {
                URI known = NodeVerifier.ping(participant.getStaticNodeUrl0(),
                        participant.getRulebookNodeUrl().resolve("static"),
                        t, false, amISource);
                if (known!=null){ // established which URL worked and there is an update available
                    NodeVerifier node = NodeVerifier.openNode(known, false, amISource);
                    participant.setLastUpdateTime(DateHelper.currentIsoUtcDateTime());
                    participant.setAvailableOnMostRecentRequest(true);
                    writeLocalNodeData(node);

                } else { // local data was up-to-date
                    participant.setAvailableOnMostRecentRequest(true);
                    participant.setLastUpdateTime(DateHelper.currentIsoUtcDateTime());

                }
            } catch (Exception e){
                logger.error("The node " + participant.getNodeUid() + " was unavailable - next node. This has been Logged in the .ni file");
                participant.setLastUpdateTime(DateHelper.currentIsoUtcDateTime());
                participant.setAvailableOnMostRecentRequest(false);

            }
        } else { // Local Data did not exist
            try {
                NodeVerifier node = NodeVerifier.tryNode(participant.getStaticNodeUrl0(),
                        participant.getRulebookNodeUrl().resolve("static"),
                        false, amISource);
                participant.setLastUpdateTime(DateHelper.currentIsoUtcDateTime());
                participant.setAvailableOnMostRecentRequest(true);
                writeLocalNodeData(node);

            } catch (Exception e){
                participant.setLastUpdateTime(DateHelper.currentIsoUtcDateTime());
                participant.setAvailableOnMostRecentRequest(false);

            }
        }
    }

    private NodeVerifier rootToSource(NodeData self) throws Exception {
        if (self.getType().equals(NodeData.TYPE_MODERATOR)){
            NodeVerifier node = NodeVerifier.openNode(self.getNodeUrl(), false, false);
            NodeInformation nodeInformation = node.getTargetTrustNetwork().getNodeInformation();
            URI sUrl = nodeInformation.getStaticLeadUrl0();
            URI fUrl = nodeInformation.getRulebookNodeUrl();
            URI sUid = nodeInformation.getLeadUid();
            KeyContainer localSourceSig = openLocalSignatureData(self.getNetworkName(), sUid);
            NetworkParticipant participant = ownTrustNetwork.getParticipantWithError(sUid);

            // Check for local data - if it does not exist then try - else - ping
            if (localSourceSig!=null){
                URI known = NodeVerifier.ping(sUrl, fUrl,
                        localSourceSig.getLastUpdateTime(), true, false);

                if (known!=null){
                    logger.info("There were updates since the last check " + sUid);
                    NodeVerifier source = NodeVerifier.openNode(known, true, false);
                    writeLocalSourceData(source);
                    participant.setAvailableOnMostRecentRequest(true);
                    return source;

                } else {
                    URL url = new URL(baseUrl + "/" + node.getNodeName() + "/network/");
                    logger.info("No updates since last check " + sUid);
                    participant.setAvailableOnMostRecentRequest(true);
                    return NodeVerifier.openLocal(url, localSourceSig, false);

                }
            } else {
                try {
                    NodeVerifier source = NodeVerifier.tryNode(sUrl, fUrl, true, false);
                    writeLocalSourceData(source);
                    participant.setAvailableOnMostRecentRequest(true);
                    participant.setLastUpdateTime(DateHelper.currentIsoUtcDateTime());
                    return source;

                } catch (Exception e){
                    participant.setAvailableOnMostRecentRequest(false);
                    writeTrustNetwork(ownTrustNetwork);
                    throw e;

                }
            }
        } else if (self.getType().equals(NodeData.TYPE_LEAD)){
            logger.info("CHRONJOB: I am source and therefore returning own NodeVerifier object");
            return ownNode;

        } else {
            throw new HubException("CHRONJOB ERROR - Bad NodeData Type Programming Error " + self.getType());

        }
    }

    private void writeTrustNetwork(TrustNetworkWrapper ownTrustNetwork) throws Exception {
        TrustNetwork tn = ownTrustNetwork.finalizeTrustNetwork();
        NodeManager m = new NodeManager(tn.getNodeInformation().getNodeName());
        PassStore p = new PassStore(RulebookNodeProperties.instance().getNodeRoot(), false);
        if (p!=null){
            m.publishTrustNetwork(tn, ownNode.getRawKeys(), p);

        } else {
            throw new HubException("Failed to Synchronize Network because administrator has not signed on since the last restart");

        }
    }

    private void writeLocalSourceData(NodeVerifier source) throws Exception {
        String url = baseUrl + "/" + source.getNodeName() + "/network/";
        try {
            TrustNetwork tn = source.getTargetTrustNetwork();
            PresentationPolicy pp = source.getPresentationPolicy();
            CredentialSpecification cs = source.getCredentialSpecification();
            KeyContainer kc = source.getRawKeys();
            String tnString = JaxbHelper.serializeToXml(tn, TrustNetwork.class);
            String ppString = JaxbHelper.serializeToXml(pp, PresentationPolicy.class);
            String csString = JaxbHelper.serializeToXml(cs, CredentialSpecification.class);
            String kcString = JaxbHelper.serializeToXml(kc, KeyContainer.class);

            String tnFileName = XContainerJSON.uidToXmlFileName(tn.getNodeInformationUid());
            String ppFileName = XContainerJSON.uidToXmlFileName(pp.getPolicyUID());
            String csFileName = XContainerJSON.uidToXmlFileName(cs.getSpecificationUID());
            String kcFileName = XContainerJSON.uidToFileName(
                    tn.getNodeInformation().getNodeUid()) + "-sig.xml";

            save(tnString, tnFileName);
            save(ppString, ppFileName);
            save(csString, csFileName);
            save(kcString, kcFileName);

        } catch (Exception e){
            throw e;

        }
    }

    private void writeLocalNodeData(NodeVerifier node) throws Exception {
        String url = baseUrl + "/" + node.getNodeName() + "/network/";
        try {
            HashMap<String, String> fileNameToXml = new HashMap<>();
            TrustNetwork tn = node.getTargetTrustNetwork();
            String tnXml = JaxbHelper.serializeToXml(tn, TrustNetwork.class);
            String tnF = XNodeContainer.uidToXmlFileName(tn.getNodeInformationUid());
            fileNameToXml.put(tnF, tnXml);

            InspectorPublicKey ins = node.getInspectorPublicKey();
            String insF = XNodeContainer.uidToXmlFileName(ins.getPublicKeyUID());
            String insXml = XNodeContainer.convertObjectToXml(ins);
            fileNameToXml.put(insF, insXml);

            Set<String> raps = node.getAllRevocationAuthorityFileNames();
            for (String f : raps){
                RevocationAuthorityParameters rap = node.getRevocationAuthorityParameters(f);
                String xml = XNodeContainer.convertObjectToXml(rap);
                fileNameToXml.put(f, xml);

            }

            Set<String> rais = node.getAllRevocationInformationFileNames();
            for (String f : rais){
                RevocationInformation rai = node.getRevocationInformation(f);
                String xml = XNodeContainer.convertObjectToXml(rai);
                fileNameToXml.put(f, xml);

            }

            Set<String> is = node.getIssuerParameterFileNames();
            for (String f : is){
                IssuerParameters i = node.getIssuerParameters(f);
                String xml = XNodeContainer.convertObjectToXml(i);
                fileNameToXml.put(f, xml);

            }

            KeyContainer kc = node.getRawKeys();
            String kcXml = JaxbHelper.serializeToXml(kc, KeyContainer.class);
            String kcFileName = XContainerJSON.uidToFileName(
                    tn.getNodeInformation().getNodeUid()) + "-sig.xml";
            fileNameToXml.put(kcFileName, kcXml);

            for (String fileName : fileNameToXml.keySet()){
                save(fileNameToXml.get(fileName), fileName);

            }
        } catch (Exception e){
            throw e;

        }
    }

    private void save(String content, String fileName) throws Exception {
        if (ftp==null){
            ftp = new SFTPClient(props.getPrimarySftpCredentials());
            ftp.connect();

        }
        try {
            ftp.overwrite(fileName, content, false);

        } catch (Exception e) {
            ftp.overwrite(fileName, content, true);

        }
    }

    private KeyContainer openLocalSignatureData(String networkName, URI nodeUid) throws Exception {
        try {
            String url = baseUrl + "/" + networkName +
                    "/network/" + XContainerJSON.uidToFileName(nodeUid) + "-sig.xml";

            byte[] b = UrlHelper.readXml(new URL(url));
            return JaxbHelper.xmlToClass(new String(b, "UTF8"), KeyContainer.class);

        } catch (SecurityException e){
            throw e;

        } catch (FileNotFoundException e){
            return null;

        } catch (Exception e){
            throw e;

        }
    }

    private Collection<NodeData> findNodeData() throws Exception {
        NodeStore store = NodeStore.getInstance();
        ArrayList<NodeData> nodes = store.findAllNetworkNodes();

        HashMap<String, NodeData> collect = new HashMap<>();
        // Favour the source of a network if it runs on this node.
        for (NodeData item : nodes){
            if (item.getType().equals(NodeData.TYPE_MODERATOR)){
                if (!collect.containsKey(item.getNetworkName())){
                    collect.put(item.getNetworkName(), item);

                }
            } else if (item.getType().equals(NodeData.TYPE_LEAD)){
                if (collect.containsKey(item.getNetworkName())){
                    collect.remove(item.getNetworkName());

                }
                collect.put(item.getNetworkName(), item);

            }
        }
        return collect.values();

    }
}
