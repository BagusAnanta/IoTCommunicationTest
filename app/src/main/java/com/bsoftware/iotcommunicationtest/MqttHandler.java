package com.bsoftware.iotcommunicationtest;

import  org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttHandler {

    private MqttClient client;

    public void connect(String brokerUrl, String clientId){
        try{
            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(brokerUrl,clientId,persistence);

            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);

            client.connect(connectOptions);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void disconnected(){
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic,String message){
        try{
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            client.publish(topic,mqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
