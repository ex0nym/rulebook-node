package io.exonym.rulebook.context;

import com.google.gson.Gson;
import io.exonym.actor.actions.MyTrustNetworks;
import io.exonym.helpers.UIDHelper;
import io.exonym.lite.exceptions.ErrorMessages;
import io.exonym.lite.exceptions.UxException;
import io.exonym.lite.parallel.ModelCommandProcessor;
import io.exonym.lite.parallel.Msg;
import io.exonym.lite.pojo.ExoNotify;
import io.exonym.utils.storage.NodeInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

public class ExonymPublisher extends ModelCommandProcessor {

    private static final Logger logger = LogManager.getLogger(ExonymPublisher.class);
    private RulebookNodeProperties props = RulebookNodeProperties.instance();
    private static ExonymPublisher instance;
    private MqttClient mqttClient;
    private Gson gson = new Gson();

    public static final String TOPIC_RULEBOOK = "rb";

    private HashMap<String, String> uidToTopic = new HashMap<>();

    static {
        instance = new ExonymPublisher();
    }
    protected static ExonymPublisher getInstance(){
        return instance;

    }

    private ExonymPublisher()  {
        super(100, "ExonymPublisher", 60000);
        try {
            String uid = UUID.randomUUID().toString()
                    .replaceAll("-", "");

            logger.info(">>>>>>>>>>>>>> PUBLISHER");
            logger.info("> " );
            logger.info("> " + props.getMqttBroker());
            logger.info("> " + uid);
            logger.info("> " );
            logger.info(">>>>>>>>>>>>>> ");

            mqttClient = new MqttClient(props.getMqttBroker(), uid);


            MyTrustNetworks myTrustNetworks = new MyTrustNetworks();
            if (myTrustNetworks.isDefined()){
                mqttClient.connect();
                NodeInformation ni = myTrustNetworks.getOnePrioritizeModerator()
                        .getTrustNetwork().getNodeInformation();
                if (myTrustNetworks.isModerator()){
                    URI mod = ni.getNodeUid();
                    URI lead = ni.getLeadUid();
                    UIDHelper helper = new UIDHelper(mod);
                    uidToTopic.put(mod.toString(), helper.getRulebookModTopic());
                    uidToTopic.put(lead.toString(), helper.getRulebookLeadTopic());
                    uidToTopic.put(TOPIC_RULEBOOK, helper.getRulebookTopic());

                } else if (myTrustNetworks.isLeader()){
                    URI lead = ni.getLeadUid();
                    String topic = UIDHelper.computeRulebookTopicFromUid(lead);
                    uidToTopic.put(TOPIC_RULEBOOK, topic);

                }
            } else {
                throw new UxException(ErrorMessages.RULEBOOK_NODE_NOT_INITIALIZED);

            }
        } catch (UxException e) {
            logger.info(e.getMessage());

        } catch (Exception e) {
            logger.error("Error", e);

        }
    }

    @Override
    protected void receivedMessage(Msg msg) {
        // internal message on the pipe.
        if (msg instanceof ExoNotify){
            try {
                ExoNotify notify = (ExoNotify)msg;
                String json = gson.toJson(notify);
                byte[] toBroadcast = json.getBytes(StandardCharsets.UTF_8);
                int len = toBroadcast.length;
                json = len + json;
                MqttMessage message = new MqttMessage(
                        json.getBytes(StandardCharsets.UTF_8));

                message.setQos(1);
                String topic = null;

                if (notify.getType().equals(ExoNotify.TYPE_LEAD)){
                    topic = uidToTopic.get(notify.getNodeUID().toString());
                    logger.debug("broadcast topic=" + topic);

                }
                if (topic!=null){
                    mqttClient.publish(topic, message);

                } else {
                    logger.warn("Failed to send message, topic was null");

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
}
