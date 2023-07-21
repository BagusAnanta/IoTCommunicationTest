package com.bsoftware.iotcommunicationtest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;

import java.util.List;
import java.util.Locale;

public class GetLocationManager {
    // we make for android 10 and higher
    private Context context;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private LocationSettingsRequest locationSettingsRequest;
    private Location lastlocation;
    private SettingsClient settingsClient;
    private int REQUEST_PERMISSION = 1;
    private Activity activity;
    private double longitude;
    private double latitude;

    String fetched_address = "";

    public GetLocationManager(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        // check permission and turn on GPS if disable
        checkLocationPermission();
        // we init in here lah
        init();
    }


    public void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION);
            } else {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION);
            }
        } else {
            // we can turn on a GPS in here
            turnOnGPS();
        }
    }

    private void startLocationUpdate() {
        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(locationSettingsResponse -> {
                    Log.d("startlocationupdate", "Location settings ok");
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                })
                .addOnFailureListener(e ->{
                    int statusCode = ((ApiException) e).getStatusCode();
                    Log.d("startlocationupdate","Contain error" + statusCode);
                });
    }

    /*I think we can call this function onDestroy maybe because in onDestroy state we must stop a
     * gps tracker too*/
    public void stopLocationUpdate(){
      try{
          fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                  .addOnCompleteListener(task -> {Log.d("LocationUpdate","Stop location update");});
      } catch (NullPointerException e){
          Log.d("StopLocationUpdate","Error at :", e);
      }
    }

    private void receiveLocation(LocationResult locationResult){
        lastlocation = locationResult.getLastLocation();

        // test for now
        Log.d("Location" ,"latitude"+ lastlocation.getLatitude());
        Log.d("Location","longitude"+ lastlocation.getLongitude());
        Log.d("Location","altitude"+ lastlocation.getAltitude());

        Double s_lat = Double.valueOf(String.format(Locale.ROOT,"%.6f",lastlocation.getLatitude()));
        Double s_log = Double.valueOf(String.format(Locale.ROOT,"%.6f",lastlocation.getLongitude()));

        latitude = lastlocation.getLatitude();
        longitude = lastlocation.getLongitude();

        // we can set a data in here
        setLatitudeData(s_lat); // -> must s_lat
        setLongitudeData(s_log); // -> must s_log

        try{
            Geocoder geocoder = new Geocoder(context,Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude,longitude,1);

            fetched_address = addresses.get(0).getAddressLine(0);
            Log.d("Location","LocationdataAddress"+fetched_address);


        } catch (Exception e){
            e.printStackTrace();
        }

    }

    // Null pointer exception now
    private void turnOnGPS() {
        // check a gps enable
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // if a GPS Active enable we get a log and lat data
            // we request a location
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        } else {
            // if GPS disable
            requestEnableGPS();

        }
    }

    public void init(){
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        settingsClient = LocationServices.getSettingsClient(context);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                receiveLocation(locationResult);
            }
        };

       /* locationRequest = LocationRequest.create()
                .setInterval(5000)
                .setFastestInterval(500)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(100);*/

        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,5000)
                .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                .setMinUpdateIntervalMillis(500)
                .setMinUpdateDistanceMeters(1)
                .setWaitForAccurateLocation(true)
                .build();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
        startLocationUpdate();
    }



    private void requestEnableGPS(){
        // we make alert in here
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Turn On A GPS Needed")
                .setMessage("Need Turn on a GPS for Application Function, click Turn On")
                .setPositiveButton("Turn On", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(intent);
                });
        builder.create();
        builder.show();
    }



    public double getLongitudeData() {
        return longitude;
    }

    public void setLongitudeData(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitudeData() {
        return latitude;
    }

    public void setLatitudeData(double latitude) {
        this.latitude = latitude;
    }
}
