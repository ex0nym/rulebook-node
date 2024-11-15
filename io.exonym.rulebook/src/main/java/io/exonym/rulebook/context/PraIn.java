package io.exonym.rulebook.context;

import com.ibm.zurich.idmx.jaxb.JaxbHelperClass;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.xml.PresentationPolicy;
import eu.abc4trust.xml.RevocationInformation;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.actions.*;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.parallel.ModelCommandProcessor;
import io.exonym.lite.parallel.ModelSingleSequence;
import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.ExoNotify;
import io.exonym.lite.pojo.NetworkMapItem;
import io.exonym.lite.pojo.XKey;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.Const;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.WhiteList;
import io.exonym.utils.storage.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBIntrospector;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ArrayBlockingQueue;

public class PraIn extends ModelCommandProcessor {
    
    private static final Logger logger = LogManager.getLogger(PraIn.class);
    private final int maxScheduledEventTimeSeconds = 10;

    private final AbstractNetworkMap networkMap;
    private final CacheContainer cache;

    private final NetworkPublicKeyManager publicKeyManager;

    protected PraIn() {
        super(Const.FLUX_CAPACITY, "PraIn", 60000);
        PkiExternalResourceContainer pki = PkiExternalResourceContainer.getInstance();
        this.networkMap = pki.getNetworkMap();
        this.cache = pki.getCache();
        this.publicKeyManager = NetworkPublicKeyManager.getInstance();

    }

    @Override
    protected ArrayBlockingQueue<Msg> getPipe() {
        return super.getPipe();
    }

    @Override
    protected void receivedMessage(Msg msg) {
        if (msg instanceof ExoNotify){
            try {
                ExoNotify notify = (ExoNotify) msg;
                Path pathToLocalFolder = computeLocalFolderPath(notify);
                logger.info("Path to local folder @ PRAIN= " + pathToLocalFolder);

                if (isUpdateToExistingStaticData(pathToLocalFolder)){
                    String xml = deserializeB64(notify);
                    Object obj = checkSignature(xml, notify);

                    if (obj instanceof PresentationPolicy){
                        logger.info("Updating PresentationPolicy @ Subscribe");
                        updateLocalLeadData((PresentationPolicy)obj, xml,
                                pathToLocalFolder, notify.getPpSigB64());
                        cache.store(obj);

                    } else if (obj instanceof RevocationInformation){
                        logger.info("Updating RevocationInformation @ Subscribe");
                        RevocationInformation rai = (RevocationInformation)obj;
                        updateModLeadData(rai, xml,
                                pathToLocalFolder, notify.getRaiSigB64());
                        updateKeyManager(rai);
                        cache.store(obj);

                    } else {
                        logger.info("Ignoring object (if null, could be that sig failed.) " + obj);

                    }
                } else {
                    logger.info("Scheduling new static data");
                    scheduleAddNewStaticData(notify.getNodeUid(), pathToLocalFolder);

                }
            } catch (Exception e) {
                logger.debug("Ignoring object " + e.getMessage());

            }
        } else {
            logger.debug("Ignoring object" + msg);

        }
    }

    private String deserializeB64(ExoNotify notify) {
        try {
            if (notify.getRaiB64()!=null){
                return JaxbHelper.b64XmlToString(notify.getRaiB64());

            } else if (notify.getPpB64()!=null) {
                return JaxbHelper.b64XmlToString(notify.getPpB64());

            } else {
                logger.info("Empty notify");
                return null;

            }
        } catch (Exception e) {
            logger.info("Error", e);
            return null;

        }
    }

    private Object checkSignature(String xml, ExoNotify notify) {
        try {
            AsymStoreKey key = this.publicKeyManager.getKey(notify.getNodeUid());
            boolean isRai = notify.getRaiB64()!=null;
            String sig = notify.getPpSigB64()!=null ? notify.getPpSigB64() : notify.getRaiSigB64();
            boolean verified = checkSignature(key, xml, sig);

            if (verified){
                if (isRai){
                    JAXBElement<?> jb = JaxbHelperClass.deserialize(xml, true);
                    return JAXBIntrospector.getValue(jb);

                } else {
                    JAXBElement<?> jb = JaxbHelperClass.deserialize(xml, true);
                    return JAXBIntrospector.getValue(jb);

                }
            } else {
                logger.info("Unverified static data");
                return null;

            }
        } catch (Exception e) {
            logger.info("Error at deserialise", e);
            return null;

        }
    }

    private boolean checkSignature(AsymStoreKey key, String xml, String sigB64) throws Exception {
        byte[] signed = NodeVerifier.stripStringToSign(xml).getBytes();
        byte[] sig = Base64.decodeBase64(sigB64);
        return key.verifySignature(signed, sig);

    }

    private void updateLocalLeadData(PresentationPolicy obj, String xml,
                                     Path pathToLocalFolder, String sig) {

        try {
            // todo add new NMI for node

            // todo schedule add new node for an added moderator

            URI uid = obj.getPolicyUID();
            Path sigXmlToUpdate = pathToLocalFolder.resolve(
                    Const.SIGNATURES_XML);

            Path ppXmlToUpdate = pathToLocalFolder.resolve(
                    IdContainer.uidToXmlFileName(uid));

            String sigs = Files.readString(sigXmlToUpdate);
            KeyContainer keyContainer = JaxbHelper.xmlToClass(sigs, KeyContainer.class);
            KeyContainerWrapper kcw = new KeyContainerWrapper(keyContainer);
            XKey k = new XKey();
            k.setKeyUid(uid);
            k.setSignature(Base64.decodeBase64(sig));
            kcw.updateKey(k);

            String sigsToWrite = JaxbHelper.serializeToXml(kcw.getKeyContainer(), KeyContainer.class);

            writeLocalFile(ppXmlToUpdate, xml);
            writeLocalFile(sigXmlToUpdate, sigsToWrite);

        } catch (Exception e) {
            logger.info("Error", e);

        }
    }


    private void updateModLeadData(RevocationInformation rai, String xml,
                                   Path pathToLocalFolder, String sig) {
        try {
            logger.info("Attempting to write to path=" + pathToLocalFolder);
            UIDHelper helper = new UIDHelper(rai.getRevocationInformationUID());
            URI uid = helper.getRevocationAuthorityInfo();

            Path raiXmlToUpdate = pathToLocalFolder.resolve(
                    IdContainer.uidToXmlFileName(uid));

            Path sigXmlToUpdate = pathToLocalFolder.resolve(
                    Const.SIGNATURES_XML);

            String sigs = Files.readString(sigXmlToUpdate);
            KeyContainer keyContainer = JaxbHelper.xmlToClass(sigs, KeyContainer.class);
            KeyContainerWrapper kcw = new KeyContainerWrapper(keyContainer);
            XKey k = new XKey();
            k.setKeyUid(uid);
            k.setSignature(Base64.decodeBase64(sig));
            kcw.updateKey(k);

            String sigsToWrite = JaxbHelper.serializeToXml(kcw.getKeyContainer(), KeyContainer.class);

            writeLocalFile(raiXmlToUpdate, xml);
            writeLocalFile(sigXmlToUpdate, sigsToWrite);

        } catch (Exception e) {
            logger.info("Error", e);

        }
    }

    private void writeLocalFile(Path path, String contents) throws IOException {
        Files.write(path, contents.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

    }

    private static void updateKeyManager(RevocationInformation rai) throws Exception {
        URI raiUid = rai.getRevocationInformationUID();
        ExonymOwner owner = VerifySupportSingleton.getInstance().getOwner();
        URI rapUid =rai.getRevocationAuthorityParametersUID();
        logger.info("Received UID for Revocation Infomation=" + raiUid);
        owner.addRevocationInformation(rapUid, rai);
        ExonymIssuer.logRevocationInformationDetails(rai);

    }

    private void scheduleAddNewStaticData(URI nodeUID, Path pathToLocalFolder) {
        double rnd = Math.random() * 1000;
        int seconds = ((int)rnd) % maxScheduledEventTimeSeconds;
        long ms = seconds * 1000l;

        logger.debug("Pausing for " + seconds + " " + ms + " SEARCHING FOR "
                + nodeUID + " in path " + pathToLocalFolder.toString());

        try {
            new VerifyNodeAndStore(ms, nodeUID, pathToLocalFolder);

        } catch (Exception e) {
            logger.info("Something went wrong", e);

        }
    }


    private boolean isUpdateToExistingStaticData(Path pathToLocalFolder) {
        return Files.exists(pathToLocalFolder);

    }

    private Path computeLocalFolderPath(ExoNotify notify) {
        String localFolder = CryptoUtils.computeSha256HashAsHex(
                notify.getNodeUid().toString());

        URI nodeUID = notify.getNodeUid();
        boolean isLead = WhiteList.isLeadUid(nodeUID);
        return Path.of(Const.PATH_OF_NETWORK, localFolder, (isLead ? Const.LEAD : Const.MODERATOR));

    }


    @Override
    protected void periodOfInactivityProcesses() {

    }

    private class VerifyNodeAndStore extends ModelSingleSequence {

        private URI nodeUid;
        private Path folder;


        public VerifyNodeAndStore(long pause,URI nodeUID, Path pathToLocalFolder) throws Exception {
            super("New Node To Local Data", pause);
            this.nodeUid=nodeUID;
            this.folder = pathToLocalFolder;
            this.start();
        }

        @Override
        protected void process() {
            try {
                logger.info("Writing new node static data to " + folder.toString());
                NetworkMapItem item = openNmi();
                NodeVerifier v = new NodeVerifier(item.getNodeUID(), true);
                URI issuer = new TrustNetworkWrapper(v.getTargetTrustNetwork())
                        .getMostRecentIssuerParameters();
                UIDHelper helper = new UIDHelper(issuer);
                RevocationInformation rai = v.getRevocationInformation(helper.getRevocationInfoFileName());
                updateKeyManager(rai);

            } catch (Exception e) {
                logger.info("Failed to add new node", e);

            }
        }


        // TODO replace with
        private NetworkMapItem openNmi() throws Exception {
            try {
                return networkMap.nmiForNode(nodeUid);

            } catch (Exception e) {
                try {
                    networkMap.spawn();
                    return networkMap.nmiForNode(nodeUid);

                } catch (Exception ex) {
                    throw ex;
                }
            }
        }
    }
}
