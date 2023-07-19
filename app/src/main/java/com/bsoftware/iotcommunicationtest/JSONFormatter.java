package com.bsoftware.iotcommunicationtest;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONFormatter {

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
}
