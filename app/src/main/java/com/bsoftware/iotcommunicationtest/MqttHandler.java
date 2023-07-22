package com.bsoftware.iotcommunicationtest;

import  org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;

public class MqttHandler {

    private MqttClient client;

    public void connect(String brokerUrl, String clientId, String topicStatus){
        try{
            /*MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(brokerUrl,clientId,persistence);

            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);

            client.connect(connectOptions);*/

            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(brokerUrl,clientId,persistence);

            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            connectOptions.setAutomaticReconnect(true);
            connectOptions.setKeepAliveInterval(15);
            connectOptions.setWill(topicStatus,"offline".getBytes(),1,true);

            client.connect(connectOptions);
            client.publish(topicStatus,"online".getBytes(),1,true);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void connectStatus(String brokerUrl, String clientId, String topic){
        try{
            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(brokerUrl,clientId,persistence);

            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            connectOptions.setAutomaticReconnect(true);
            connectOptions.setWill(topic,"offline".getBytes(),0,true);

            client.connect(connectOptions);
            client.publish(topic,"online".getBytes(),0,true);
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

    private void testTopicpublish(){
        ArrayList<String> topic = new ArrayList<>();
        // fill a topic
        topic.add(0,"BG1001AM/patient");
        topic.add(1,"BG1001AM/patient");
        topic.add(2,"BG1003AM/patient");

        /*I don't know how get a police number from web, but we can try a get
        * and now i confuce how get a topic from a web */

    }
}
