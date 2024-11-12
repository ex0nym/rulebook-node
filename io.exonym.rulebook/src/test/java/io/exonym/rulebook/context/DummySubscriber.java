package io.exonym.rulebook.context;

import io.exonym.lite.parallel.ModelCommandProcessor;
import io.exonym.lite.parallel.Msg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;

public class DummySubscriber extends ModelCommandProcessor {
    
    private static final Logger logger = LogManager.getLogger(DummySubscriber.class);
    private String topic;
    private String userId;
    private String broker;
    private MqttClient mqttClient;

    protected DummySubscriber(String topic, String broker, String userId) {
        super(1, "DummySub", 5000);
        this.topic=topic;
        this.userId = userId;
        this.broker = broker;



    }

    @Override
    protected void onStart() {
        try {
            mqttClient = new MqttClient(broker, userId);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(false);
//            options.setUserName("testuser");
//            options.setPassword("helloworld".toCharArray());

            mqttClient.setCallback(new SubscriberCallback());
            mqttClient.connect(options);
            if (mqttClient.isConnected()){
                logger.info("Connected successfully");
                mqttClient.subscribe(topic, 1);

            }

        } catch (MqttException e) {
            logger.info("Error", e);
        }


    }

    @Override
    protected void periodOfInactivityProcesses() {

    }

    @Override
    protected void receivedMessage(Msg msg) {

    }

    private class SubscriberCallback implements MqttCallback{

        @Override
        public void connectionLost(Throwable throwable) {
            logger.info("Lost:" + throwable.getMessage());

        }

        @Override
        public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
            logger.info("Message received on topic: " + topic);
            byte[] received = mqttMessage.getPayload();
            String json = new String(received, StandardCharsets.UTF_8);
            logger.info(json);


        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }
}
