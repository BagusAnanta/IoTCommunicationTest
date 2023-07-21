package com.bsoftware.iotcommunicationtest;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONFormatter {

    private float heartrate;
    private float spo2;

    public String Writedata(Float heartRate, Float spo2, Double Longitude, Double Latitude){
        JSONObject object = new JSONObject();
        String finaldata = "Hello";
        try{
            object.put("HeartRate",heartRate);
            object.put("SPO2",spo2);
            object.put("Longitude",Longitude);
            object.put("Latitude",Latitude);

            finaldata = object.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("JSONFormatter","Fail to create a data",e);
        }
        return finaldata;
    }

    public void ParsingData(String datajson){
        try {
            JSONObject jsondata = new JSONObject(datajson);

            float heartRate = (float) jsondata.getDouble("HeartRate");
            float spo = (float) jsondata.getDouble("SPO2");

            // set a value in here
            setHeartrate(heartRate);
            setSpo2(spo);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public float getHeartrate() {
        return heartrate;
    }

    public void setHeartrate(float heartrate) {
        this.heartrate = heartrate;
    }

    public float getSpo2() {
        return spo2;
    }

    public void setSpo2(float spo2) {
        this.spo2 = spo2;
    }



}
