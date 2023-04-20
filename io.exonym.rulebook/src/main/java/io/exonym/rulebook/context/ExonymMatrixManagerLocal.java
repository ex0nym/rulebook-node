package io.exonym.rulebook.context;

import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.google.gson.Gson;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.actions.ExonymMatrix;
import io.exonym.actor.storage.Poke;
import io.exonym.actor.storage.SFTPClient;
import io.exonym.actor.storage.SFTPLogonData;
import io.exonym.actor.storage.SignatureOn;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.*;
import io.exonym.lite.time.DateHelper;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.PassStore;
import io.exonym.utils.storage.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class ExonymMatrixManagerLocal extends ExonymMatrixManagerAbstract {

    /*

    /x-node/exox/
    /x-node/exoy/


    <00-2-hex>.xml = signatures on all 00, 01, etc...
    <0000-4-hex>.json = exonyms on all 0000, 0001, etc...
    poke.json = {t:utcTimestamp, s:signature}
    read-me.txt // warning about editing

    aaaa.json example
    {
        rules:["r0", "r1", "r2"],
        aaaa0..x0:{exonyms: [x0, x1, x2], violations: [{rule:"r0", t:"2021-09-01T13:00:00", settled:true}] },
        aaaa1..x0:{exonyms: [null, null, x2]},
    }

    fields are null when this file is on the y-list and those rules are controlled by someone else

    //*/


    private static final Logger logger = LogManager.getLogger(ExonymMatrixManagerLocal.class);
    private final NetworkMapItemAdvocate myNmia;
    private final ArrayList<String> ruleUrns;
    private AsymStoreKey signatureKey = null;

    private SFTPClient sftp0 = null;
    // private SFTPClient sftp1 = null;

    private boolean createdPoke = false;
    private boolean createdSignature = false;
    private boolean createdMatrix = false;

    private SFTPLogonData sftpLogonData;

    private final String root;

    private final AbstractXContainer advocateContainer;
    private final String staticFolder;
    private final String staticFailoverFolder;

    public ExonymMatrixManagerLocal(AbstractXContainer advocateContainer,
                                    ArrayList<String> ruleUrns, NetworkMapItemAdvocate myNmia,
                                    SFTPLogonData logonData, String root,
                                    String staticFolder, String staticFailoverFolder) throws Exception {
        try {
            this.advocateContainer =advocateContainer;
            this.ruleUrns = ruleUrns;
            this.myNmia = myNmia;
            this.sftpLogonData = logonData;
            this.root = root;
            this.staticFolder=staticFolder;
            this.staticFailoverFolder = staticFailoverFolder;

            logger.debug("Initializing-----------------------");
            logger.debug("static0 \t\t" + myNmia.getStaticURL0());
            logger.debug("static1 \t" + myNmia.getStaticURL1());
            logger.debug("advocateUID \t" + myNmia.getNodeUID());
            logger.debug("rulebookNodeURL \t\t" + myNmia.getRulebookNodeURL());
            logger.debug("-----------------------------------");

        } catch (NoDocumentException e) {
            throw new UxException("No Node Defined", e);

        } catch (Exception e){
            throw e;

        }
    }

    /**
     * A user has joined the node and exonyms need to be published
     */
    protected void addExonymRow(ExonymMatrixRow controlled) throws Exception {
        addExonymRow(controlled, null);

    }
    /**
     * A user has joined the node and exonyms need to be published
     */
    protected void addExonymRow(ExonymMatrixRow controlled, ExonymMatrixRow uncontrolled) throws Exception {
        String nodeUrl = this.myNmia.getStaticURL0().toString();
        if (uncontrolled==null){
            String x0 = verifyOnlyControlledRow(controlled);
            openExonymMatrix(nodeUrl, x0, this.xList, this.root);
            matrix.addExonymRow(x0, controlled);
            publish(this.xList);

        } else {
            String x0 = verifyControlledVsUncontrolled(controlled, uncontrolled);
            openExonymMatrix(nodeUrl, x0, this.xList, this.root);
            matrix.addExonymRow(x0, controlled);
            publish(this.xList);

            openExonymMatrix(nodeUrl, x0, this.yList, this.root);
            matrix.addExonymRow(x0, uncontrolled);
            publish(this.yList);

        }
    }

    private String verifyOnlyControlledRow(ExonymMatrixRow controlled) throws UxException {
        ArrayList<String> cont = controlled.getExonyms();
        String x0 = cont.get(0);
        for (String c : cont){
            if (c.equals("null")){
                throw new UxException("An exonym was null");

            }
        }
        return x0;
    }

    private String verifyControlledVsUncontrolled(ExonymMatrixRow controlled, ExonymMatrixRow uncontrolled) throws UxException {
        ArrayList<String> c = controlled.getExonyms();
        ArrayList<String> u = uncontrolled.getExonyms();
        if (c.size()==u.size()){
            int index = 0;
            String x0 = null;
            for (String c0 : c){
                String u0 = u.get(index);
                if (x0==null){
                    x0 = (c0.equals("null") ? u0 : c0);

                }
                logger.debug("Got Root >>>> " + x0);
                int len = u0.getBytes(StandardCharsets.UTF_8).length;
                if (c0.equals(u0)){
                    throw new UxException("Exonyms must be published to either X or Y lists not both" +
                            ". index:" + index);

                } if (c0.equals("null") && len!=NYM_LENGTH){
                    throw new UxException("If you do not control a rule " +
                            "then it must be added to the uncontrolled list. index:" + index + " length:" + len);

                }
                index++;
            }
            return x0;

        } else {
            throw new UxException("Controlled and Uncontrolled Rows were not the same size");

        }
    }

    @Override
    protected Poke handlePokeNotFound(Exception e) throws Exception {
        logger.info("Creating new Poke");
        this.createdPoke = true;
        return new Poke();

    }

    @Override
    protected KeyContainerWrapper handleKeyContainerNotFound(Exception e) throws Exception {
        logger.debug("Got an expected exception with message:" + e.getMessage());
        this.createdSignature = true;
        return new KeyContainerWrapper(new KeyContainer());

    }

    @Override
    protected ExonymMatrix handleMatrixNotFound(Exception e, String nibble6) throws Exception {
        this.createdMatrix =true;
        return ExonymMatrix.init(ruleUrns, nibble6);

    }

    @Override
    protected void authenticate(String root, Poke poke, KeyContainerWrapper kcw, ExonymMatrix matrix,
                              String nibble3, String nibble6) throws Exception {
        initKey();
        if (!createdPoke){
            authPoke(poke, this.signatureKey);
            createdPoke = false;

        } if (!createdSignature){
            authSigs(poke, nibble3, this.signatureKey);
            signatureByteData = null;
            createdSignature = false;

        } if (!createdMatrix){
            authMatrix(kcw, nibble6, this.signatureKey);
            matrixByteData = null;
            createdMatrix = false;

        }
        this.poke = poke;
        this.kcw = kcw;
        this.matrix = matrix;

    }

    /**
     * An existing user has been revoked and a violation needs adding
     */
    protected void addViolation(String x0, Violation violation) throws Exception {
        String nodeUrl = this.myNmia.getStaticURL0().toString();
        openExonymMatrix(nodeUrl, x0, this.xList, this.root);
        ExonymMatrixRow row = this.matrix.findExonymRow(x0);
        if (row!=null){
            String rN = violation.getRuleUrn();
            int rule = matrix.getRuleUrns().indexOf(rN);
            if (rule!=-1){
                String xN = row.getExonyms().get(rule);

                if (!xN.equals("null")){
                    row.getViolations().add(violation);
                    publish(this.xList);

                } else {
                    throw new UxException("The rule " + rN + " is not controlled locally.  " +
                            "Search for the controlling host.");
                }
            } else {
                throw new UxException("There is no such RuleDid on this rulebook");

            }
        } else {
            throw new UxException("No such exonym.  Are you using the root? n6="
                    + matrix.getNibble6() + "");

        }
    }

    private void publish(String xOrYList) throws Exception {
        initKey();
        // Sign matrix -> s0
        SignatureAndSerializedFile matrixPub = signMatrix();

        // Update kcw with s0
        XKey sig = new XKey();
        sig.setKeyUid(URI.create(matrix.getNibble6()));
        sig.setSignature(matrixPub.getSig());
        kcw.updateKey(sig);

        // Sign kc ->  s1
        SignatureAndSerializedFile n6Sigs = signKeyContainer();
        // Update poke with s1
        HashMap<String, String> n3Sigs = poke.getSignatures();
        n3Sigs.put(matrix.getNibble3(), Base64.encodeBase64String(n6Sigs.getSig()));

        // SignatureOn.poke() -> s2
        poke.setT(DateHelper.currentIsoUtcDateTime());
        byte[] toSign = SignatureOn.poke(poke);
        byte[] pokeSig = this.signatureKey.sign(toSign);

        // Insert s2 into poke.signature
        n3Sigs.put(Poke.SIGNATURE_ON_POKE, Base64.encodeBase64String(pokeSig));
        Gson g = new Gson();
        String pokeString = g.toJson(poke, Poke.class);

        write(xOrYList, pokeString, n6Sigs.getFile(), matrixPub.getFile());

    }

    private SignatureAndSerializedFile signMatrix() {
        String m = matrix.toJson();
        byte[] s = signatureKey.sign(m.getBytes(StandardCharsets.UTF_8));
        return new SignatureAndSerializedFile(s, m);
    }

    private SignatureAndSerializedFile signKeyContainer() throws Exception {
        KeyContainer kc = kcw.getKeyContainer();
        String f = JaxbHelper.serializeToXml(kc, KeyContainer.class);
        byte[] s = signatureKey.sign(f.getBytes(StandardCharsets.UTF_8));
        return new SignatureAndSerializedFile(s, f);

    }

    private void write(String xOrYList, String pokeString, String n3, String n6) throws Exception {
        activateSftpOut();
        String r0 = staticFolder;
        String r1 = staticFailoverFolder;
        String name = "/" + myNmia.getSourceName() + "/x-node";

        String pokePath0 = r0 + name + computePokePathToFile(xOrYList);
        String pokePath1 = r1 + name + computePokePathToFile(xOrYList);
        String n3Path0 = r0 + name + computeN3PathToFile(matrix.getNibble3(), xOrYList);
        String n3Path1 = r1 + name + computeN3PathToFile(matrix.getNibble3(), xOrYList);
        String n6Path0 = r0 + name + computeN6PathToFile(matrix.getNibble3(), matrix.getNibble6(), xOrYList);
        String n6Path1 = r1 + name + computeN6PathToFile(matrix.getNibble3(), matrix.getNibble6(), xOrYList);

        logger.debug(pokePath0);
        logger.debug(pokePath1);
        logger.debug(n3Path0);
        logger.debug(n3Path1);
        logger.debug(n6Path0);
        logger.debug(n6Path1);

//        String testFile = "<html><body>identity is control</body></html>";
//        put(sftp0, r0 + name + xOrYList + "/" + matrix.getNibble3() + "/index.html", testFile);
//        logger.debug("finished test primary");
//        put(sftp1, r1 + name + xOrYList + "/" + matrix.getNibble3() + "/index.html", testFile);
//        logger.debug("finished test failover");

        put(sftp0, pokePath0, pokeString);
//        put(sftp1, pokePath1, pokeString);
        put(sftp0, n3Path0, n3);
//        put(sftp1, n3Path1, n3);
        put(sftp0, n6Path0, n6);
//        put(sftp1, n6Path1, n6);

    }

    private void put(SFTPClient sftp, String path, String file) throws Exception {
        try {
            logger.debug("Trying Root as /");
            sftp.overwrite(path, file, true);

        } catch (Exception e) {
            throw e;
            //logger.debug("Trying Root as /uploads/", e);
            //sftp.overwrite(path, file, true);

        }
    }

    private void activateSftpOut() throws Exception {
        if (sftp0 ==null || !sftp0.isActive()) {
            sftp0 = new SFTPClient(this.sftpLogonData);
            sftp0.connect();

        }
        /* if (sftp1 ==null || !sftp1.isActive()) {
            sftp1 = new SFTPClient(props.getSecondarySftpCredentials());
            sftp1.connect();

        } //*/
    }

    private void initKey() throws Exception {
        if (this.signatureKey==null){
            PassStore store = new PassStore(this.root, false);
            KeyContainer kc = this.advocateContainer.openResource("keys.xml");
            KeyContainerWrapper kcw = new KeyContainerWrapper(kc);
            XKey key = kcw.getKey(KeyContainerWrapper.TN_ROOT_KEY);
            this.signatureKey = AsymStoreKey.blank();
            signatureKey.assembleKey(key.getPublicKey(),
                    key.getPrivateKey(),
                    store.getDecipher());

        }
    }
}