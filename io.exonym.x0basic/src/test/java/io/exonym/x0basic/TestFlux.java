package io.exonym.x0basic;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import io.exonym.lite.connect.BroadcasterBasic;
import io.exonym.lite.couchdb.QueryBasic;
import io.exonym.lite.pojo.ExoNotify;
import io.exonym.lite.pojo.XKey;
import io.exonym.lite.standard.AsymStoreKey;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.standard.PassStore;
import io.exonym.lite.standard.TestUtils;
import io.exonym.lite.time.DateHelper;
import io.exonym.lite.time.Timing;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;


public class TestFlux {

    // join -> ack, violation -> ack
    // [ join || violation ] (own broadcast -> localExoUpdate && exoMap update) !ack
    // [ join || violation ] (3rd party -> exoMap update) -> ack

    // valid join
    // invalid join
    // valid violation
    // invalid violation
    private static final Logger logger = LogManager.getLogger(TestFlux.class);

    private final X0Properties props = X0Properties.getInstance();
    private final ArrayList<String> dbPrefixes = new ArrayList<>();
    private final ArrayList<InetSocketAddress> addresses = new ArrayList<>();
    private final HashMap<URI, AsymStoreKey> keys = new HashMap<>();
    private final HashMap<String, URI> dbNameToUuid = new HashMap<>();

    private final URI sxUuid = URI.create("urn:exonym:hello:4f1092c4-3dd9-401a-b6d2-987b6368caad:world-68b0");
    private final URI xUuid = URI.create("urn:exonym:hello:4f1092c4-3dd9-401a-b6d2-987b6368caad:lone-fdd0");

    public TestFlux() {
        dbPrefixes.add("vanilla-sx_");
        dbPrefixes.add("vanilla-x_");
        dbNameToUuid.put(dbPrefixes.get(0), sxUuid);
        dbNameToUuid.put(dbPrefixes.get(1), xUuid);
        addresses.add(new InetSocketAddress("exonym-x-02", 9090));
        addresses.add(new InetSocketAddress( "exonym-x-02", 9091));

        try {
            String rootKey = "users";
            PassStore store = new PassStore(props.getNodeRoot(), false);
            QueryBasic q = QueryBasic.selectType("host");

            for (String dbName : dbPrefixes){
                logger.debug(Timing.currentTime());
                AsymStoreKey key = AsymStoreKey.blank();
                CloudantClient client = CouchDbClient.instance();
                Database db = client.database(dbName + rootKey, false);
                CouchRepository<XKey> repo = new CouchRepository<>(db, XKey.class);
                XKey xk = repo.read(q).get(0);
                key.assembleKey(xk.getPublicKey(), xk.getPrivateKey(), store.getDecipher());
                keys.put(dbNameToUuid.get(dbName), key);

            }
        } catch (Exception e) {
            logger.debug("Error", e);

        }
    }

    @Test
    void generateTestData() {
        try {
            long t = Timing.currentTime();
            for (int i = 0; i<1; i++){
                ArrayList<String> nyms = TestUtils.fakePseudonyms(1, null);
                ExoNotify notify = buildInvalidNotify(nyms.get(0), xUuid);

                BroadcasterBasic basic = new BroadcasterBasic(notify);
                basic.send(addresses.get(0));
//                for (InetSocketAddress address : addresses){
//                    logger.debug(address);
//
//
//                }
                basic.close();
                /*
                synchronized (this){
                    this.wait(1000);

                } //*/
            }
            logger.debug("Has been " + Timing.hasBeenMs(t));

        } catch (Exception e) {
            logger.debug("Error", e);

        }
    }

    private ExoNotify buildValidNotify(String x0, URI hostUuid){
        ExoNotify notify = new ExoNotify();
        String n6 = x0.substring(0,6);
        String x0Hash = CryptoUtils.computeSha256HashAsHex(x0);

        notify.setType(ExoNotify.TYPE_JOIN);
        notify.setNibble6(n6);
        notify.setHashOfX0(x0Hash);
        notify.setNodeUID(hostUuid);
        notify.setT(DateHelper.currentIsoUtcDateTime());

        AsymStoreKey key = this.keys.get(hostUuid);
        byte[] toSign = ExoNotify.signatureOn(notify);
        byte[] sigBytes = key.sign(toSign);
        String sig = Base64.encodeBase64String(sigBytes);
        notify.setSigB64(sig);
        return notify;

    }

    private ExoNotify buildInvalidNotify(String x0, URI hostUuid){
        ExoNotify notify = new ExoNotify();
        String n6 = x0.substring(0,6);
        String x0Hash = CryptoUtils.computeSha256HashAsHex(x0);

        notify.setType(ExoNotify.TYPE_JOIN);
        notify.setNibble6(n6);
        notify.setHashOfX0(x0Hash);
        notify.setNodeUID(hostUuid);
//        notify.setT(DateHelper.currentIsoUtcDateTime());

        AsymStoreKey key = this.keys.get(hostUuid);
        byte[] toSign = ExoNotify.signatureOn(notify);
        byte[] sigBytes = key.sign(toSign);
        String sig = Base64.encodeBase64String(sigBytes);
        notify.setSigB64(sig);
        return notify;

    }

    private ExoNotify buildValidViolation(String x0, URI hostUuid){
        ExoNotify notify = new ExoNotify();
        String n6 = x0.substring(0,6);
        String x0Hash = CryptoUtils.computeSha256HashAsHex(x0);

        notify.setType(ExoNotify.TYPE_VIOLATION);
        notify.setNibble6(n6);
        notify.setHashOfX0(x0Hash);
        notify.setNodeUID(hostUuid);
        notify.setT(DateHelper.currentIsoUtcDateTime());

        AsymStoreKey key = this.keys.get(hostUuid);
        byte[] toSign = ExoNotify.signatureOn(notify);
        byte[] sigBytes = key.sign(toSign);
        String sig = Base64.encodeBase64String(sigBytes);
        notify.setSigB64(sig);
        return notify;

    }

    private void print(String ln){
        System.out.println(ln);

    }

}
