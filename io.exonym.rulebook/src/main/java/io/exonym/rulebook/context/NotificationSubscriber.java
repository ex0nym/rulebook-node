package io.exonym.rulebook.context;

import com.cloudant.client.org.lightcouch.NoDocumentException;
import io.exonym.actor.actions.MyTrustNetworks;
import io.exonym.actor.actions.PkiExternalResourceContainer;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.NetworkMapItem;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.lite.time.Timing;
import io.exonym.rulebook.schema.BroadcastStringIn;
import io.exonym.utils.storage.NodeInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class NotificationSubscriber {
    
    private static final Logger logger = LogManager.getLogger(NotificationSubscriber.class);
    private static NotificationSubscriber instance;

    private MqttClient mqttClient;
    private NetworkPublicKeyManager publicKeyManager;
    private OverrideResolver overrideResolver;
    private ExoMatrixWriter writer;
    private JoinIn joinIn0;
//    private JoinIn joinIn1;
    private ViolationIn violationIn;
    private PraIn praIn;
    private BlobManager blobManager;
    private NotificationQueue updateQueue;

    private ArrayBlockingQueue<Msg> pipeOut;

    private String[] topics = new String[2];

    static {
        instance = new NotificationSubscriber();

    }

    private NotificationSubscriber(){
        try {
            buildListenerFramework();
            subscribeToMosquittoTopics();

        } catch (UxException e) {

            logger.info(">>>>>>>>>>>>>>>>>>>>", e);
            logger.info("> NOT SUBSCRIBED");
            logger.info("> ");
            logger.info("> No error, but the node needs to be defined to subscribe.");
            logger.info("> ");
            logger.info("> Restart after node is a moderator or lead.");
            logger.info("> ");
            logger.info(">>>>>>>>>>>>>>>>>>>>");

        } catch (Exception e) {
            logger.info(">>>>>>>>>>>>>>>>>>>>");
            logger.info("> FAILED TO SUBSCRIBE");
            logger.info("> ");
            logger.info("> 1. Restart after Rulebook Node is defined.");
            logger.info("> 2. Check following error to resolve.");
            logger.info("> ");
            logger.info(">>>>>>>>>>>>>>>>>>>>");
            logger.error("MUST FIX: Subscriber failed to initialise.", e);
        }
    }

    private void buildListenerFramework() throws Exception {
        publicKeyManager = NetworkPublicKeyManager.getInstance();
        MyTrustNetworks mtn = publicKeyManager.getMyTrustNetworks();

        overrideResolver = new OverrideResolver(mtn);

        writer = new ExoMatrixWriter(mtn);

        joinIn0 = new JoinIn(0, writer.getPipe(), publicKeyManager);

        ArrayList<ArrayBlockingQueue<Msg>> pipesToJoin = new ArrayList<>();
        pipesToJoin.add(joinIn0.getPipe());
        // joinIn1 = new JoinIn(1, writer.getPipe(), publicKeyManager);
        // pipesToJoin.add(joinIn1.getPipe());

        praIn = new PraIn();
        violationIn = new ViolationIn(publicKeyManager, mtn);
        blobManager = new BlobManager();
        updateQueue = new NotificationQueue(pipesToJoin,
                overrideResolver.getPipe(),
                violationIn.getPipe(),
                blobManager.getPipe(),
                praIn.getPipe());

        pipeOut = updateQueue.getPipe();

    }

    private void subscribeToMosquittoTopics() throws Exception {
        try {
            MyTrustNetworks mine = new MyTrustNetworks();
            PkiExternalResourceContainer ext = PkiExternalResourceContainer.getInstance();
            NetworkMapItem nmi = null;

            if (mine.getRulebook().getDescription().isProduction()){
                nmi = ext.getNetworkMap().nmiForSybilLeadMainNet();
            } else {
                nmi = ext.getNetworkMap().nmiForSybilLeadTestNet();
            }
            UIDHelper helper = new UIDHelper(nmi.getNodeUID());

            topics[0] = helper.getRulebookTopic() + UIDHelper.MQTT_WILDCARD;

            if (mine.isModerator()){
                NodeInformation info = mine.getModerator()
                        .getTrustNetwork().getNodeInformation();
                helper = new UIDHelper(info.getNodeUid());

                topics[1] = helper.getRulebookTopic() + UIDHelper.MQTT_WILDCARD;
                if (topics[1].equals(topics[0])){
                    topics[1] = null;

                }
                RulebookNodeProperties props = RulebookNodeProperties.instance();
                String id = CryptoUtils.computeSha256HashAsHex(info.getNodeUid().toString());

                mqttClient = new MqttClient(props.getMqttBroker(), id);

                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(false);

                mqttClient.setCallback(new SubscriberCallback());
                mqttClient.connect(options);

                if (mqttClient.isConnected()){
                    mqttClient.subscribe(topics[0]);

                    if (topics[1]!=null){
                        mqttClient.subscribe(topics[1]);
                    }
                    logger.info(">>>>>>>>>>>>>>>>>>>>");
                    logger.info("> SUBSCRIBER ");
                    logger.info("> ");
                    logger.info("> " + topics[0]);
                    logger.info("> " + topics[1]);
                    logger.info("> ");
                    logger.info(">>>>>>>>>>>>>>>>>>>>");

                }
            } else {
                throw new UxException(ErrorMessages.RULEBOOK_NODE_NOT_INITIALIZED);

            }
        } catch (NoDocumentException e) {
            logger.debug("Still setting up node");
            throw new UxException(ErrorMessages.RULEBOOK_NODE_NOT_INITIALIZED, e);

        }
    }

    public static NotificationSubscriber getInstance(){
        return instance;
    }

    public void close() {

    }

    private class SubscriberCallback implements MqttCallback {

        LinkedBlockingDeque<String> dedup = new LinkedBlockingDeque<>(15);

        @Override
        public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
            byte[] received = mqttMessage.getPayload();
            String hash = CryptoUtils.computeMd5HashAsHex(received);

            if (!dedup.contains(hash)){
                String json = new String(received, StandardCharsets.UTF_8);
                BroadcastStringIn msg = new BroadcastStringIn(json);
                logger.info(json);
                pipeOut.put(msg);
                dedup.put(hash);

            } else {
                logger.debug("Filtered duplicate " + hash);

            }
        }

        @Override
        public void connectionLost(Throwable cause) {
            long coeff = 2;
            long max = 30000;
            long wait = Timing.randomWait(1000);
            int count = 1;

            while (!mqttClient.isConnected()){
                try {
                    wait = wait < max ? (wait * coeff * (long)count) : wait;
                    logger.info(">>>>>>>>>> ");
                    logger.info("> ");
                    logger.info("> Connection to Mosquitto lost");
                    logger.info("> ");
                    logger.info("> ");
                    logger.info("> " + cause.getMessage());
                    logger.info("> ");
                    logger.info("> Attempting to reconnect in " + wait + "ms");
                    logger.info("> ");
                    Thread.sleep(wait);
                    subscribeToMosquittoTopics();
                    count++;

                } catch (Exception e) {
                    logger.info("Error", e);

                }
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }
}
