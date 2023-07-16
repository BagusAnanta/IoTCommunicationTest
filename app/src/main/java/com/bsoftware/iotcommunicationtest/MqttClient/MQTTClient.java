package com.bsoftware.iotcommunicationtest.MqttClient;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MQTTClient{
    private Context context;
    private String BrokerURI;
    private String ClientID;


    MqttAndroidClient mqttClient = new MqttAndroidClient(context,BrokerURI,ClientID);

    public MQTTClient(Context context, String BrokerURI, String ClientID){
        this.context = context;
        this.BrokerURI = BrokerURI;
        this.ClientID = ClientID;

    }

    private void connection(){
        MqttConnectOptions option = new MqttConnectOptions();
        option.setCleanSession(true);

        try{
            try {
                mqttClient.connect(option);
            } catch (NullPointerException e){
                Log.d("MqttClientNPE", e.toString());
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void publish(String topic,String messageData, int Qos){
        try{
          try{
              mqttClient.publish(topic,messageData.getBytes(),Qos,false);
          } catch (NullPointerException e){
              Log.d("MqttClientNPE", e.toString());
          }
        } catch (MqttException e){
            e.printStackTrace();
        }
    }

    public void disconnect(IMqttActionListener callbackDisconnect){
        try {
            mqttClient.disconnect(null,callbackDisconnect);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    public MQTTClient onMqttStartedAndPublisher(String topic, String messageData, int Qos){
        connection();

        // publish
        publish(topic, messageData, Qos);
        return null;
    }
}
