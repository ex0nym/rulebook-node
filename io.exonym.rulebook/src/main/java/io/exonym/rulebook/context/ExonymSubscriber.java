package io.exonym.rulebook.context;

import io.exonym.actor.actions.MyTrustNetworks;
import io.exonym.actor.actions.PkiExternalResourceContainer;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.NetworkMapItem;
import io.exonym.lite.standard.CryptoUtils;
import io.exonym.utils.storage.NodeInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

public class ExonymSubscriber {
    
    private static final Logger logger = LogManager.getLogger(ExonymSubscriber.class);
    private static ExonymSubscriber instance;
    private NetworkPublicKeyManager publicKeyManager;
    private ConflictResolver conflictResolver;
    private ExoMatrixWriter writer;
    private JoinIn joinIn0;
    private JoinIn joinIn1;
    private ViolationIn violationIn;
    private PraIn praIn;
    private BlobManager blobManager;
    private UpdateQueue updateQueue;

    private ArrayBlockingQueue<Msg> pipeOut;

    private String[] topics = new String[2];

    static {
        instance = new ExonymSubscriber();

    }

    private ExonymSubscriber(){
        try {
            subscribeToMosquittoTopics();
            buildListenerFramework();

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
        conflictResolver = new ConflictResolver();

        writer = new ExoMatrixWriter(conflictResolver.getPipe());


        joinIn0 = new JoinIn(0, writer.getPipe(), publicKeyManager);
        joinIn1 = new JoinIn(1, writer.getPipe(), publicKeyManager);

        ArrayList<ArrayBlockingQueue<Msg>> pipesToJoin = new ArrayList<>();
        pipesToJoin.add(joinIn0.getPipe());
        pipesToJoin.add(joinIn1.getPipe());

        praIn = new PraIn();
        violationIn = new ViolationIn(publicKeyManager);
        blobManager = new BlobManager();
        updateQueue = new UpdateQueue(pipesToJoin,
                violationIn.getPipe(),
                blobManager.getPipe(),
                praIn.getPipe());

        pipeOut = updateQueue.getPipe();

    }

    private void subscribeToMosquittoTopics() throws Exception {
        NetworkMapItem nmi = PkiExternalResourceContainer.getInstance()
                .getNetworkMap().nmiForSybilTestNet();
        UIDHelper helper = new UIDHelper(nmi.getNodeUID());
        topics[0] = helper.getRulebookTopic() + UIDHelper.MQTT_WILDCARD;
        MyTrustNetworks mine = new MyTrustNetworks();

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
            MqttClient client = new MqttClient(props.getMqttBroker(), id);
            client.setCallback(new SubscriberCallback());
            client.connect();
            client.subscribe(topics[0]);
            if (topics[1]!=null){
                client.subscribe(topics[1]);
            }
            logger.info(">>>>>>>>>>>>>>>>>>>>");
            logger.info("> SUBSCRIBER ");
            logger.info("> ");
            logger.info("> " + topics[0]);
            logger.info("> " + topics[1]);
            logger.info("> ");
            logger.info(">>>>>>>>>>>>>>>>>>>>");

        } else {
            logger.info(">>>>>>>>>>>>>>>>>>>>");
            logger.info("> NOT SUBSCRIBED");
            logger.info("> ");
            logger.info("> No error, but the node needs to be defined to subscribe.");
            logger.info("> ");
            logger.info("> Restart after node is a moderator or lead.");
            logger.info("> ");
            logger.info(">>>>>>>>>>>>>>>>>>>>");

        }
    }

    protected static ExonymSubscriber getInstance(){
        return instance;
    }

    public void close() {

    }

    private class SubscriberCallback implements MqttCallback {

        @Override
        public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
            byte[] received = mqttMessage.getPayload();
            String json = new String(received, StandardCharsets.UTF_8);
            BroadcastStringIn msg = new BroadcastStringIn(json);
            logger.info(json);
            pipeOut.put(msg);

        }

        @Override
        public void connectionLost(Throwable cause) {
            logger.info("Connection Lost: " + cause.getMessage());

        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }

}
