package io.exonym.actor.actions;

import io.exonym.abc.util.FileType;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.storage.SFTPClient;
import io.exonym.lite.connect.UrlHelper;
import io.exonym.lite.standard.*;
import io.exonym.uri.NamespaceMngt;
import io.exonym.utils.storage.NetworkParticipant;
import io.exonym.utils.storage.NodeInformation;
import io.exonym.utils.storage.TrustNetwork;
import io.exonym.lite.pojo.XKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WiderTrustNetworkManagement {
    
    private static final Logger logger = LogManager.getLogger(WiderTrustNetworkManagement.class);

    private AsymStoreKey key;
    private SFTPClient sftp = null;
    private TrustNetworkWrapper tnw = null;

    private final RulebookNodeProperties props = RulebookNodeProperties.instance();
    private final String path = props.getPrimaryStaticDataFolder() + "/sources.xml";

    public WiderTrustNetworkManagement() throws Exception {
        TrustNetwork wtn = openWiderTrustNetwork();
        tnw = new TrustNetworkWrapper(wtn);

    }

    @Deprecated
    protected void unlock(File xkey, String password) throws Exception {
        XKey xk = JaxbHelper.xmlFileToClass(
                JaxbHelper.fileToPath(xkey),
                XKey.class);
        this.key = AsymStoreKey.blank();
        Cipher cipher = CryptoUtils.generatePasswordCipher(Cipher.DECRYPT_MODE, password, null);
        this.key.assembleKey(xk.getPublicKey(), xk.getPrivateKey(), cipher);

    }

    public TrustNetwork openWiderTrustNetwork() throws Exception {
        try {
            byte[] raw = UrlHelper.read(URI.create(props.getSpawnWiderNetworkFrom()).toURL());
            String xml = new String(raw, StandardCharsets.UTF_8);
            return JaxbHelper.xmlToClass(xml, TrustNetwork.class);

        } catch (FileNotFoundException e) {
            return new TrustNetwork();

        }
    }

    public TrustNetwork updateWiderTrustNetwork() throws Exception {
        byte[] raw = UrlHelper.read(URI.create(props.getSpawnWiderNetworkFrom()).toURL());
        TrustNetwork tn = JaxbHelper.xmlToClass(new String(raw, StandardCharsets.UTF_8), TrustNetwork.class);
        TrustNetworkWrapper tnw = new TrustNetworkWrapper(tn);
        tn = tnw.finalizeTrustNetwork();
        byte[] trust = JaxbHelper.serializeToXml(tn, TrustNetwork.class).getBytes();
        publish(URI.create(props.getPrimaryStaticDataFolder()), trust, "sources.xml");
        return tn;

    }

    protected void open() throws Exception {

    }

    protected void publish() throws Exception {
        TrustNetwork wtn = tnw.finalizeTrustNetwork();
        byte[] trust = JaxbHelper.serializeToXml(wtn, TrustNetwork.class).getBytes();
        publish(URI.create(props.getPrimaryStaticDataFolder()), trust, "sources.xml");

    }

    protected void addSource(URI sourceUrl) throws Exception {
        NodeVerifier v = NodeVerifier.openNode(sourceUrl.toURL(), true, false);
        TrustNetworkWrapper tnw = new TrustNetworkWrapper(v.getTargetTrustNetwork());
        NodeInformation source = tnw.getNodeInformation();

        NetworkParticipant participant = this.tnw.addParticipant(
                source.getNodeUid(), source.getStaticSourceUrl0(),
                source.getStaticSourceUrl1(), source.getRulebookNodeUrl(), source.getBroadcastAddress(),
                v.getPublicKey(), source.getRegion(), tnw.getMostRecentIssuerParameters());

        participant.setRulebookNodeUrl(source.getRulebookNodeUrl());
        participant.setNetworkName(v.getNodeName());

    }

    @Deprecated
    protected XKey newKey(String password) throws Exception {
        PassStore p = new PassStore(password, true);
        AsymStoreKey k = new AsymStoreKey();
        XKey x = new XKey();
        x.setHash("2048-RSA-Unsalted Password Encrypted Private Key-PKCS5S2(128bit)-AES/ECB/PKCS5Padding-io.exonym.lite.standard.AsymStoreKey");
        x.setKeyUid(URI.create(NamespaceMngt.URN_PREFIX_COLON + "root-key"));
        x.setPrivateKey(k.getEncryptedEncodedForm(password));
        x.setPublicKey(k.getPublicKey().getEncoded());
        return x;

    }

    protected TrustNetwork setupWiderTrustNetwork() throws Exception {
        TrustNetwork tn = new TrustNetwork();
        NodeInformation ni = new NodeInformation();
        String url = props.getPrimaryDomain() + "/" + props.getPrimaryStaticDataFolder();
        ni.setStaticNodeUrl0(new URL(url));
        ni.setStaticNodeUrl1(new URL("https://failover.io"));
        tn.setNodeInformation(ni);

        TrustNetworkWrapper tnw = new TrustNetworkWrapper(tn);
        tn = tnw.finalizeTrustNetwork();

        byte[] trust = JaxbHelper.serializeToXml(tn, TrustNetwork.class).getBytes();
        publish(URI.create(props.getPrimaryStaticDataFolder()), trust, "sources.xml");

        return tn;

    }

    private void publish(URI url, byte[] xml, String xmlFileName) throws Exception {
        if (UrlHelper.isXml(xml)){
            if (FileType.isXmlDocument(xmlFileName)){
                if (sftp ==null || !sftp.isActive()) {
                    sftp = new SFTPClient(props.getPrimarySftpCredentials());
                    sftp.connect();

                }
                String xml0 = new String(xml);
                try {
                    sftp.overwrite(url.toString() + "/" + xmlFileName, xml0, false);

                } catch (Exception e) {
                    sftp.overwrite(url.toString() + "/" + xmlFileName, xml0, true);

                }
            } else {
                throw new Exception("The file name was not an xml file name " + xmlFileName);

            }
        } else {
            throw new SecurityException("The value passed was not XML");

        }
    }

    public static void main(String[] args) throws Exception {
        WiderTrustNetworkManagement wtn = new WiderTrustNetworkManagement();
        wtn.setupWiderTrustNetwork();
        wtn.addSource(URI.create("https://trust.exonym.io/sybil/x-source")); //*/
        wtn.addSource(URI.create("https://trust.exonym.io/nu0/exonym/x-source")); //*/
//        wtn.addSource(URI.create("https://trust.exonym.io/nu0/exosources/x-source")); //*/
//
////        wtn.addSource(URI.create("https://spectra.plus/spectra/x-source")); //*/
////        wtn.addSource(URI.create("https://original.spectra.plus/original/x-source")); //*/
        wtn.publish();


    }
}
