package io.exonym.rulebook.context;

import com.google.gson.Gson;
import io.exonym.actor.actions.MyTrustNetworks;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.parallel.ModelCommandProcessor;
import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.ExoNotify;
import io.exonym.lite.time.Timing;
import io.exonym.utils.storage.NodeInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

public class NotificationPublisher extends ModelCommandProcessor {

    private static final Logger logger = LogManager.getLogger(NotificationPublisher.class);
    private RulebookNodeProperties props = RulebookNodeProperties.instance();
    private static NotificationPublisher instance;
    private MqttClient mqttClient;
    private Gson gson = new Gson();

    public static final String TOPIC_RULEBOOK = "rb";

    private HashMap<String, String> uidToTopic = new HashMap<>();

    static {
        instance = new NotificationPublisher();
    }
    protected static NotificationPublisher getInstance(){
        return instance;

    }

    private NotificationPublisher()  {
        super(15, "ExonymPublisher", 60000);
        try{
            connectToMosquitto();

        } catch (UxException e) {
            logger.info(e.getMessage());

        } catch (Exception e) {
            logger.error("Error", e);

        }
    }

    private void connectToMosquitto() throws Exception {
        MyTrustNetworks myTrustNetworks = new MyTrustNetworks();
        if (myTrustNetworks.isDefined()){
            String id = myTrustNetworks.mosquittoId(false);
            String mqt = props.getMqttBroker() + ":9001";
            logger.info(">>>>>>>>>>>>>> PUBLISHER");
            logger.info("> " );
            logger.info("> " + mqt);
            logger.info("> " + id);
            logger.info("> " );
            logger.info("> pwSet=" + (props.getMqttPassword()!=null) );
            logger.info("> " );
            logger.info(">>>>>>>>>>>>>> ");

            mqttClient = new MqttClient(mqt, id);
            mqttClient.setCallback(new PublisherCallback());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(false);
            options.setUserName(id);
            options.setPassword(props.getMqttPassword().toCharArray());

            mqttClient.connect(options);


            NodeInformation ni = myTrustNetworks.getOnePrioritizeModerator()
                    .getTrustNetwork().getNodeInformation();

            if (myTrustNetworks.isModerator()){
                URI mod = ni.getNodeUid();
                URI lead = ni.getLeadUid();
                UIDHelper helper = new UIDHelper(mod);
                uidToTopic.put(mod.toString(), helper.getRulebookModTopic());
                uidToTopic.put(lead.toString(), helper.getRulebookLeadTopic());
                uidToTopic.put(TOPIC_RULEBOOK, helper.getRulebookTopic());
                outputTopics();

            } else if (myTrustNetworks.isLeader()){
                URI lead = ni.getLeadUid();
                String topic = UIDHelper.computeRulebookTopicFromUid(lead);
                uidToTopic.put(TOPIC_RULEBOOK, topic);
                outputTopics();

            }
        } else {
            logger.info(">>>>>>>>>>>>>> PUBLISHER");
            logger.info("> " );
            logger.info("> " + props.getMqttBroker());
            logger.info(">  -- not defined --" );
            logger.info("> " );
            logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>  ");
            throw new UxException(ErrorMessages.RULEBOOK_NODE_NOT_INITIALIZED);

        }
    }

    private void outputTopics() {
        for (String uid : uidToTopic.keySet()){
            logger.info(uid + "-->" + uidToTopic.get(uid));

        }
    }

    @Override
    protected void receivedMessage(Msg msg) {
        // internal message on the pipe.
        if (msg instanceof ExoNotify){
            try {
                ExoNotify notify = (ExoNotify)msg;
                if (notify.getType()!=null){
                    String json = gson.toJson(notify);
                    logger.debug("Sending message to network=" + json);

                    byte[] toBroadcast = json.getBytes(StandardCharsets.UTF_8);
                    int len = toBroadcast.length;
                    json = len + json;
                    MqttMessage message = new MqttMessage(
                            json.getBytes(StandardCharsets.UTF_8));

                    message.setQos(1);
                    message.setRetained(true);

                    String topic = uidToTopic.get(notify.getNodeUid().toString());

                    if (topic!=null){
                        topic += notify.getType();
                        logger.debug(topic);
                        mqttClient.publish(topic, message);

                    } else {
                        throw new UxException("Failed to send message, topic was null");

                    }
                } else {
                    throw new UxException("TYPE_NOT_SET");

                }
            } catch (Exception e) {
                logger.info("error", e);

            }
        } else {
            logger.info("Unknown Object Type " + msg);

        }
    }

    @Override
    protected void periodOfInactivityProcesses() {

    }

    @Override
    protected synchronized boolean isBusy() {
        return super.isBusy();
    }

    @Override
    protected ArrayBlockingQueue<Msg> getPipe() {
        return super.getPipe();
    }

    @Override
    protected void close() throws Exception {
        this.mqttClient.disconnect();
        super.close();

    }

    private class PublisherCallback implements MqttCallback {

        @Override
        public void connectionLost(Throwable throwable) {
            long coeff = 2;
            long max = 30000;
            long wait = Timing.randomWait(1000);
            int count = 1;

            while (!mqttClient.isConnected()){
                try {
                    wait = wait < max ? wait * (coeff * (long)count) : wait;
                    logger.info(">>>>>>>>>> ");
                    logger.info("> ");
                    logger.info("> Connection to Mosquitto lost");
                    logger.info("> ");
                    logger.info("> ");
                    logger.info("> " + throwable.getMessage());
                    logger.info("> ");
                    logger.info("> Attempting to reconnect in " + wait + "ms");
                    logger.info("> ");
                    Thread.sleep(wait);
                    connectToMosquitto();
                    count++;

                } catch (Exception e) {
                    logger.info("Error", e);

                }

            }
        }

        @Override
        public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }
}
