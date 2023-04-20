package io.exonym.x0basic;

import io.exonym.lite.couchdb.QueryStandard;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.parallel.ModelCommandProcessor;
import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.ExoNotify;
import io.exonym.lite.pojo.Namespace;
import io.exonym.lite.pojo.NetworkMapItem;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.Form;
import io.exonym.lite.time.DateHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BlobManager extends ModelCommandProcessor {

    private static final Logger logger = LogManager.getLogger(BlobManager.class);

    private X0Properties props = X0Properties.getInstance();
    private AsymStoreKey ownPublicKey = props.getHostPublicKey();
    private QueryStandard query = new QueryStandard();

    protected BlobManager() throws Exception {
        super(5, "BlobManager", 60000);
        this.query.addFieldSelector(NetworkMapItem.FIELD_PUBLIC_KEY);

    }

    private void write(ExoNotify notify) throws Exception {
        if (notify.getType().equals(ExoNotify.TYPE_JOIN)){
            join(notify);

        } else if (notify.getType().equals(ExoNotify.TYPE_VIOLATION)){
            violation(notify);

        } else {
            throw new UxException("Type not supported: " + notify.getType());

        }
    }

    private void join(ExoNotify notify) throws Exception {
        String tm0 = DateHelper.currentBareIsoUtcDate();
        String tm1 = DateHelper.yesterdayBareIsoUtcDate();
        directories();

        File yesterdayFile = new File(props.getOutputPath() + tm1 + "-exonyms.csv");
        File identifiers = new File(props.getOutputPath() + tm0 + "-exonyms.csv");

        if (yesterdayFile.exists()){
            logger.debug(yesterdayFile.getAbsolutePath());
            compressJoin(yesterdayFile);
            
        }
        if (!identifiers.exists()) {
            identifiers.createNewFile();

        }
        try (FileWriter writer = new FileWriter(identifiers, true)){
            logger.debug("Writing " + identifiers);
            writer.write(DateTime.now(DateTimeZone.UTC).getMillis()
                    + "," + notify.getNibble6() + "," + notify.getHashOfX0() + "\n");

        }
    }

    private void violation(ExoNotify notify) throws Exception {
        String tm0 = DateHelper.currentBareIsoUtcDate();
        String tm1 = DateHelper.yesterdayBareIsoUtcDate();
        directories();
        File violations = new File(props.getOutputPath() + "/" + tm0 + "-violations.csv");
        File yesterdays = new File(props.getOutputPath() + "/" + tm1 + "-violations.csv");
        if (yesterdays.exists()){
            logger.debug(yesterdays.getAbsolutePath());
            compressViolations(yesterdays);

        }
        if (!violations.exists()) {
            violations.createNewFile();

        }
        try (FileWriter writer = new FileWriter(violations, true)){
            writer.write(DateTime.now(DateTimeZone.UTC).getMillis() + "," +
                    notify.getNibble6() + "," + notify.getHashOfX0() + "," + notify.getT() + "\n");

        }
    }

    private void compressViolations(File yesterdays) throws Exception {
        String f = toFileNameRoot(this.props.getAdvocateUID());
        File zip = new File(props.getOutputPath() + "/" + f + ".vio.zip");
        if (zip.exists()){
            packIntoExistingZip(zip, yesterdays);

        } else {
            createNewZip(zip, yesterdays);

        }
        yesterdays.delete();

    }

    private void compressJoin(File yesterFile) throws Exception {
        String f = toFileNameRoot(props.getAdvocateUID());
        File zip = new File(props.getOutputPath() + "/" + f + ".join.zip");
        if (zip.exists()){
            packIntoExistingZip(zip, yesterFile);

        } else {
            createNewZip(zip, yesterFile);

        }
        yesterFile.delete();

    }

    private void packIntoExistingZip(File zip, File target) {
        HashMap<String, String> zipProps = new HashMap<>();
        zipProps.put("create", "false");
        zipProps.put("encoding", "UTF-8");
        logger.debug("hello: " +  zip.toURI());
        try (FileSystem zipFs = FileSystems.newFileSystem(URI.create("jar:" + zip.toURI()), zipProps)){
            Path zipFilePath = zipFs.getPath(target.getName());
            Path newFile = Paths.get(target.getPath());
            Files.copy(newFile, zipFilePath);

        } catch (Exception e){
            logger.error("Error", e);

        }
    }

    private void createNewZip(File zip, File target) throws Exception {
        logger.debug("Called Create New Zip");
        FileInputStream fis = new FileInputStream(target);
        FileOutputStream fos = new FileOutputStream(zip);
        ZipOutputStream zos = new ZipOutputStream(fos);
        ZipEntry item = new ZipEntry(target.getName());
        zos.putNextEntry(item);
        byte[] bytes = new byte[1024];
        int length;
        while((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);

        }
        zos.flush(); zos.close();
        fos.flush(); fos.close();
        fis.close();

    }

    private void directories() {
        File identifiers = new File(props.getOutputPath());
        if (!identifiers.exists()){
            logger.info("Making Directories " + props.getOutputPath());
            identifiers.mkdirs();

        }
    }

    @Override
    protected void receivedMessage(Msg msg) {
        try {
            if (msg instanceof ExoNotify){
                ExoNotify notify = (ExoNotify)msg;
                Authenticator.authenticateNotify(notify, ownPublicKey);
                write(notify);

            }
        } catch (Exception e) {
            logger.error("Error", e);

        }
    }

    @Override
    protected void periodOfInactivityProcesses() {}

    @Override
    protected ArrayBlockingQueue<Msg> getPipe() {
        return super.getPipe();
    }

    @Override
    protected void close() throws Exception {
        super.close();

    }

    public static String toFileNameRoot(URI hostUuid){
        String prefix = Namespace.URN_PREFIX_COLON;
        String result = hostUuid.toString().substring(prefix.length());
        return result.replaceAll(":", ".");

    }

    public static void main(String[] args) throws Exception {
        ExoNotify notify = new ExoNotify();
        notify.setType(ExoNotify.TYPE_JOIN);
        notify.setHashOfX0(Form.sha256AsHex("31".getBytes(StandardCharsets.UTF_8)));
        notify.setNibble6("234256");
        BlobManager blob = new BlobManager();
        blob.join(notify);

        notify.setHashOfX0(Form.sha256AsHex("54".getBytes(StandardCharsets.UTF_8)));
        notify.setNibble6("23238d");
        blob.join(notify);

        notify.setHashOfX0(Form.sha256AsHex("78".getBytes(StandardCharsets.UTF_8)));
        notify.setNibble6("a1257d");
        blob.join(notify);

    }
}