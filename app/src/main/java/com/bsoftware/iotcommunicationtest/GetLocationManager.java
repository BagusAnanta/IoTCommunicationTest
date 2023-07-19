package com.bsoftware.iotcommunicationtest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class GetLocationManager {
    // we make for android 10 and higher
    private Context context;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private int REQUEST_PERMISSION = 1;
    private Activity activity;
    private double longitude;
    private double latitude;

    public GetLocationManager(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        // create location request in here
        createLocationRequest();
        // check permission and turn on GPS if disable
        checkLocationPermission();
        // call locationCallback in here
        locationCallback = new GetLocationCallback();
    }

    private void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION);
        } else {
            // we can turn on a GPS in here
            turnOnGPS();
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
        // null in here, we can try catch in here
        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (NullPointerException e){
            e.printStackTrace();
        }

    }

    /*I think we can call this function onDestroy maybe because in onDestroy state we must stop a
    * gps tracker too*/
    public void stopLocationUpdate(){
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
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

    private class GetLocationCallback extends LocationCallback{
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            Location location = locationResult.getLastLocation();

            if(location != null){
                // we set logitude and latitude in here
                setLongitudeData(location.getLongitude());
                setLatitudeData(location.getLatitude());
            }
        }
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
