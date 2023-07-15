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


    public MQTTClient(Context context, String BrokerURI, String ClientID){
        this.context = context;
        this.BrokerURI = BrokerURI;
        this.ClientID = ClientID;
    }

    private MqttAndroidClient mqttClient = new MqttAndroidClient(context,BrokerURI,ClientID);

    private void connection(IMqttActionListener callbackConnection){
        MqttConnectOptions option = new MqttConnectOptions();
        option.setCleanSession(true);

        try{
            mqttClient.connect(option);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void publish(String topic,String messageData, int Qos,IMqttActionListener callbackPublish){
        try{
           mqttClient.publish(topic,messageData.getBytes(),Qos,false);
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
        connection(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d("onConnectionSuccess", "Mqtt Success");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e("onConnectionFail","Mqtt Fail because exception" + exception);
            }
        });

        // publish
        publish(
                topic,
                messageData,
                Qos,
                new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d("Publish Success","Message Publish Successfully");
                        Toast.makeText(context, "Published Successfully", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e("Publish Fail", "Message Publisher Failed");
                        Toast.makeText(context, "Publish Fail", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        return null;
    }
}
