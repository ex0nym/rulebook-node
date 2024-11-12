package io.exonym.rulebook.context;

import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.google.gson.Gson;
import io.exonym.abc.util.JaxbHelper;
import io.exonym.actor.actions.ExonymMatrix;
import io.exonym.actor.storage.Poke;
import io.exonym.actor.storage.SFTPClient;
import io.exonym.actor.storage.SignatureOn;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.pojo.*;
import io.exonym.lite.standard.Const;
import io.exonym.lite.time.DateHelper;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.PassStore;
import io.exonym.utils.storage.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;

public class ExonymMatrixManagerLocal extends ExonymMatrixManagerAbstract {

    /*
    /moderator/exox/
    /moderator/exoy/

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

    private String fileSystemPath = Const.PATH_OF_STATIC;
    private final NetworkMapItemModerator myNmim;
    private final ArrayList<URI> allRules;
    private AsymStoreKey signatureKey = null;

    private boolean createdPoke = false;
    private boolean createdSignature = false;
    private boolean createdMatrix = false;

    private final String root;

    private final AbstractIdContainer modContainer;


    public ExonymMatrixManagerLocal(AbstractIdContainer modContainer,
                                    ArrayList<URI> ruleUrns, NetworkMapItemModerator myNmim,
                                    String root) throws Exception {
        try {
            this.modContainer =modContainer;
            this.allRules = ruleUrns;
            this.myNmim = myNmim;
            this.root = root;

            logger.debug("Initializing-----------------------");
            logger.debug("static0 \t\t" + myNmim.getStaticURL0());
            logger.debug("modUID \t" + myNmim.getNodeUID());
            logger.debug("rulebookNodeURL \t\t" + myNmim.getRulebookNodeURL());
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
    protected void addExonymRow(Vio vio, ExonymMatrixRow controlled) throws Exception {
        addExonymRow(vio, controlled, null);

    }
    /**
     * A user has joined the node and exonyms need to be published
     */
    protected void addExonymRow(Vio vio, ExonymMatrixRow controlled, ExonymMatrixRow uncontrolled) throws Exception {
        if (vio==null){
            handleJoin(controlled, uncontrolled);

        } else { // it's a rejoin
            handleRejoin(vio, controlled, uncontrolled);

        }
    }

    public ArrayList<URI> getAllRules() {
        return allRules;
    }

    private void handleJoin(ExonymMatrixRow controlled, ExonymMatrixRow uncontrolled) throws Exception {
        String nodeUrl = this.myNmim.getStaticURL0().toString();
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

    private void handleRejoin(Vio vio, ExonymMatrixRow controlled, ExonymMatrixRow uncontrolled) throws Exception {
        String nodeUrl = this.myNmim.getStaticURL0().toString();

        if (uncontrolled==null){
            String x0 = verifyOnlyControlledRow(controlled);
            openExonymMatrix(nodeUrl, x0, this.xList, this.root);
            ExonymMatrixRow row = matrix.findExonymRow(x0);
            if (row!=null) {
                updateViolation(row, vio);

            } else {
                matrix.addExonymRow(x0, controlled);

            }
            publish(this.xList);

        } else {
            // TODO Unsure whether the violation should/must be published to X and Y lists.
            String x0 = verifyControlledVsUncontrolled(controlled, uncontrolled);
            openExonymMatrix(nodeUrl, x0, this.xList, this.root);
            matrix.addExonymRow(x0, controlled);
            publish(this.xList);

            openExonymMatrix(nodeUrl, x0, this.yList, this.root);
            matrix.addExonymRow(x0, uncontrolled);
            publish(this.yList);

        }
    }

    private void updateViolation(ExonymMatrixRow row, Vio vio) {
        String timeOfVio = vio.getTimeOfViolation();
        ArrayList<Violation> violations = row.getViolations();
        for (Violation violation : violations){
            if (violation.getTimestamp().equals(timeOfVio)){
                violation.setSettled(true);
                break;

            }
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
        return ExonymMatrix.init(allRules, nibble6);

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
        logger.info("The matrix was authenticated and opened with rows: " + this.matrix.getMatrix().size());

    }

    /**
     * An existing user has been revoked and a violation needs adding
     */
    protected void resolveViolation(Vio vio, boolean isOverride) throws Exception {
        String nodeUrl = this.myNmim.getStaticURL0().toString();
        openExonymMatrix(nodeUrl, vio.getX0Hash(), vio.getNibble6(), this.xList, this.root);

        ExonymMatrixRow row = this.matrix.findExonymRow(vio.getX0Hash());

        if (row!=null){
            ArrayList<URI> rNs = vio.getRuleUids();

            if (!rNs.isEmpty()){
                URI rN = rNs.get(0);
                int rule = matrix.getRuleUrns().indexOf(rN);

                if (rule!=-1){
                    String xN = row.getExonyms().get(rule);

                    if (!xN.equals("null")){
                        ArrayList<Violation> violations = row.getViolations();

                        for (Violation violation : violations){
                            logger.info("Violation on settlement: override(" + isOverride + ")" + JaxbHelper.gson.toJson(violation));
                            if (violation.getTimestamp().equals(vio.getTimeOfViolation())){
                                if (isOverride){
                                    violation.setOverride(true);

                                } else {
                                    violation.setSettled(true);

                                }
                                break;

                            }
                        }
                        publish(this.xList);

                    } else {
                        throw new UxException("The rule " + rN + " is not controlled locally.  " +
                                "Search for the controlling host.");
                    }
                } else {
                    throw new UxException("There is no such RuleUID on this rulebook");

                }
            } else {
                throw new UxException("The RuleUID was not specified.");

            }
        } else {
            throw new UxException("No such exonym. Are you using the root? n6="
                    + matrix.getNibble6() + "");

        }
    }


    /**
     * An existing user has been revoked and a violation needs adding
     */
    protected void addViolation(String x0, Violation violation) throws Exception {
        logger.debug("Adding violation - for x0=" + x0);
        String nodeUrl = this.myNmim.getStaticURL0().toString();
        openExonymMatrix(nodeUrl, x0, this.xList, this.root);
        ExonymMatrixRow row = this.matrix.findExonymRow(x0);
        if (row!=null){
            ArrayList<URI> rNs = violation.getRuleUids();

            if (!rNs.isEmpty()){
                URI rN = rNs.get(0);
                int rule = matrix.getRuleUrns().indexOf(rN);

                if (rule!=-1){
                    String xN = row.getExonyms().get(rule);

                    if (!xN.equals("null")){
                        violation.blankForStorage();
                        row.getViolations().add(violation);

                        publish(this.xList);

                    } else {
                        throw new UxException("The rule " + rN + " is not controlled locally.  " +
                                "Search for the controlling host.");
                    }
                } else {
                    throw new UxException("There is no such RuleUID on this rulebook");

                }
            } else {
                throw new UxException("The RuleUID was not specified.");

            }
        } else {
            throw new UxException("No such exonym. Are you using the root? n6="
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
        Path pokePath0 = Path.of(fileSystemPath.toString(), Const.MODERATOR,
                computePokePathToFile(xOrYList));

        Path n3Path0 = Path.of(fileSystemPath.toString(), Const.MODERATOR,
                computeN3PathToFile(matrix.getNibble3(), xOrYList));

        Path n6Path0 = Path.of(fileSystemPath.toString(), Const.MODERATOR,
                computeN6PathToFile(matrix.getNibble3(), matrix.getNibble6(), xOrYList));

        logger.debug("writing exonym matrix file=" + pokePath0);
        logger.debug("writing exonym matrix file=" + n3Path0);
        logger.debug("writing exonym matrix file=" + n6Path0);

        writeLocal(pokePath0, pokeString);
        writeLocal(n3Path0, n3);
        writeLocal(n6Path0, n6);

    }

    private void writeLocal(Path path, String contents) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, contents.getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

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


    private void initKey() throws Exception {
        if (this.signatureKey==null){
            PassStore store = new PassStore(this.root, false);
            KeyContainer kc = this.modContainer.openResource("keys.xml");
            KeyContainerWrapper kcw = new KeyContainerWrapper(kc);
            XKey key = kcw.getKey(KeyContainerWrapper.TN_ROOT_KEY);
            this.signatureKey = AsymStoreKey.blank();
            signatureKey.assembleKey(key.getPublicKey(),
                    key.getPrivateKey(),
                    store.getDecipher());

        }
    }

    @Override
    protected Poke openPoke(String primaryUrl, String xOrYList) throws Exception {
        String poke = computePokePathToFile(xOrYList);
        Path pokePath = Path.of(fileSystemPath.toString(), Const.MODERATOR, poke).toAbsolutePath();

        logger.debug("Poke Path="  + pokePath);
        try {
            String pokeString = Files.readString(pokePath);
            Gson gson = new Gson();
            return gson.fromJson(pokeString, Poke.class);

        } catch (IOException e) {
            return handlePokeNotFound(e);

        }
    }

    @Override
    protected KeyContainerWrapper openSignatures(String primaryUrl, String nibble3, String xOrYList) throws Exception {
        String n3Part = computeN3PathToFile(nibble3, xOrYList);
        Path n3Path = Path.of(fileSystemPath.toString(), Const.MODERATOR, n3Part);
        try {
            String n3 = Files.readString(n3Path);
            this.signatureByteData = n3.getBytes(StandardCharsets.UTF_8);
            return new KeyContainerWrapper(JaxbHelper.xmlToClass(n3, KeyContainer.class));
        } catch (Exception e) {
            return handleKeyContainerNotFound(e);

        }
    }

    @Override
    protected ExonymMatrix openTargetMatrix(String primaryUrl, String nibble3, String nibble6, String xOrYList) throws Exception {
        if (this.matrixByteData==null){
            String path = computeN6PathToFile(nibble3, nibble6, xOrYList);
            Path matrixPath = Path.of(fileSystemPath, Const.MODERATOR, path);
            logger.debug(matrixPath);
            try {
                String m = Files.readString(matrixPath);
                this.matrixByteData = m.getBytes(StandardCharsets.UTF_8);
                Gson g = new Gson();
                return g.fromJson(m, ExonymMatrix.class);

            } catch (Exception e) {
                return handleMatrixNotFound(e, nibble6);

            }
        } else {
            String json = new String(matrixByteData, StandardCharsets.UTF_8);
            Gson g = new Gson();
            return g.fromJson(json, ExonymMatrix.class);

        }

    }
}