package com.bsoftware.iotcommunicationtest;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


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

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.S)
public class BluetoothSerialTest extends AppCompatActivity {

    ArrayList<String> permissionList = new ArrayList<>();
    String[] permission = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET
    };
    int permission_count = 0;

    ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
        @Override
        public void onActivityResult(Map<String, Boolean> result) {
            ArrayList<Boolean> list = new ArrayList<>(result.values());
            permissionList = new ArrayList<>();
            permission_count = 0;

            for (int i = 0; i < list.size(); i++) {
                if (shouldShowRequestPermissionRationale(permission[i])) {
                    permissionList.add(permission[i]);
                } else if (!hasPermission(BluetoothSerialTest.this, permission[i])) {
                    permission_count++;
                }
            }

            if (permissionList.size() > 0) {
                tellPermission(permissionList);
            } else if (permission_count > 0) {
                showPermissionDialog();
            } else if(permissionList.size() == 0 && permission_count == 0) {
                Log.d("Permission Granted", "All permission Granted");
            }
        }
    });

    // For Bluetooth Communication
    // init bluetoohtAdapter
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private int REQUEST_ENABLE_BT = 1;
    private String deviceName;
    private String deviceHardwareAddress;
    private StringBuilder dataBuffer = new StringBuilder();
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private static final int MESSAGE_READ = 0;

    private static final String BrokerURI = "tcp://192.3.113.195:1883";
    private static final String ClientID = "mqttv311";
    private static final String Topic_status = "BG1003AM/status";
    private static final String Topic_patient = "BG1003AM/patient";
    private MqttHandler mqttHandler;
    private JSONFormatter jsonFormatter = new JSONFormatter();
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private LocationSettingsRequest locationSettingsRequest;
    private Location lastlocation;
    private SettingsClient settingsClient;
    private int REQUEST_PERMISSION = 1;
    private double longitude;
    private double latitude;

    private float longitudeStr;
    private float latitudeStr;

    private TextView status,heartRate,spo2,latitude_text,longitude_text;

    String fetched_address = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_serial_test);

        status = findViewById(R.id.status_text);
        heartRate = findViewById(R.id.heartrate_number);
        spo2 = findViewById(R.id.spo_number);
        latitude_text = findViewById(R.id.lat_number);
        longitude_text = findViewById(R.id.log_number);

        // Check connection
        checkInternetConnection(this);

        // set up webview
       /* WebView webviewInterface = (WebView) findViewById(R.id.webviewint);
        // for test, if a web done, dont forget change this
        webviewInterface.loadUrl("https://teknikkomputer.polsri.ac.id/");*/
        //webviewInterface.loadUrl("192.168.43.31:80");


        // permission
        permissionList.addAll(Arrays.asList(permission));
        tellPermission(permissionList);

        // Mqtt Test
        mqttHandler = new MqttHandler();
        mqttHandler.connect(BrokerURI,ClientID,Topic_status);
        // and we init a bluetooh too

        if(!bluetoothAdapter.isEnabled()){
            initBluetooth();
        } else {
            scanBluetoothAddress();
        }

        //GPS
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        // check permission and turn on GPS if disable
        checkLocationPermission();
        // we init GPS in here lah
        init();

    }


    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            if (message.what == MESSAGE_READ) {
                String receiverMessage = message.obj.toString();
                jsonFormatter.ParsingData(receiverMessage);
                // and set data in here
                publishMessage(Topic_patient,jsonFormatter.Writedata("BG1003AM",jsonFormatter.getHeartrate(),jsonFormatter.getSpo2(),getLatitudeStr(),getLongitudeStr()));
                // set text in here
                heartRate.setText(String.valueOf(jsonFormatter.getHeartrate()));
                spo2.setText(String.valueOf(jsonFormatter.getSpo2()));
                latitude_text.setText(String.valueOf(getLatitudeStr()));
                longitude_text.setText(String.valueOf(getLongitudeStr()));
            }
            return true;
        }
    });

    /*---------------------------------------------------------------------------------------------------------------------------
     * ------------------------------------------------PERMISSION FUNCTION------------------------------------------------------*/

    private void tellPermission(@NonNull ArrayList<String> permissionList) {
        try {
            String[] permissionStr = new String[permissionList.size()];
            for (int i = 0; i < permissionStr.length; i++) {
                if (permissionStr.length > 0) {
                    permissionLauncher.launch(permission);
                } else {
                    showPermissionDialog();
                }
            }
        } catch (NullPointerException E) {
            Log.e("PermissionErrorException","NullPointerException At :",E);
        }
    }

    AlertDialog alertDialog;

    private void showPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Need Permission")
                .setMessage("Need permission for this app for functional this app")
                .setPositiveButton(
                        "Settings", (dialog, which) ->
                        {
                            dialog.dismiss();
                        });
        if (alertDialog == null) {
            alertDialog = builder.create();
            if (!alertDialog.isShowing()) {
                alertDialog.show();
            }
        }
    }

    private boolean hasPermission(Context context, String permissionStr) {
        return ContextCompat.checkSelfPermission(context, permissionStr) == PackageManager.PERMISSION_GRANTED;
    }

    /*---------------------------------------------------------------------------------------------------------------------------
     * ------------------------------------------------END PERMISSION FUNCTION------------------------------------------------------*/

    /*---------------------------------------------------------------------------------------------------------------------------
     * ------------------------------------------------BLUETOOH CODE CONNECTION---------------------------------------------------*/


    private void initBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "You Device not Support a Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            // turn on bluetooth
            if (!bluetoothAdapter.isEnabled()) {
                // if a bluetooth disable, must turn on bluetooth
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e("BLUETOOTH_ENABLE", "BLUETOOTH_ENABLE NOT GRANTED");
                    return;
                }
                startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
                if(bluetoothAdapter.isEnabled()){
                    scanBluetoothAddress();
                }
            } else {
                // if bluetooth enable
                scanBluetoothAddress();
            }
        }

    }

    private void scanBluetoothAddress() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Set<BluetoothDevice> pairDevice = bluetoothAdapter.getBondedDevices();
        if (pairDevice.size() > 0) {
            for (BluetoothDevice device : pairDevice) {
                deviceName = device.getName();
                deviceHardwareAddress = device.getAddress();

                Log.d("Bluetooth Name :", deviceName);
                Log.d("Bluetooth Address :", deviceHardwareAddress);

                // if you use another device like more esp32 you must change a Bluetooth MAC Address
                // old address : 3C:61:05:3F:61:16
                // new esp32 address : A8:42:E3:49:AC:1A
                if (deviceName.equals("AMBULANCE POLSRI") && deviceHardwareAddress.equals("A8:42:E3:49:AC:1A")) {
                    connectFromAddressandName();
                }
            }
        }
    }

    private void connectFromAddressandName() {

        // we connect a device use MAC Address
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceHardwareAddress);

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
              return;
            }
            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
            /* I think before connect we gonna check a bluetooth socket connecting if a connection is available we gonna check and scan again*/
            try{
                bluetoothSocket.connect();
            } catch (IOException e){
                // check this bluetooth connect, if not connect we reconnecting again and give a time
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        while(!bluetoothSocket.isConnected()){
                            // we try a thread in 10 second and if a not connect we finish
                            // reconnecting in 10 second
                            Log.d("BluetoothSocket","Reconnection condition");
                            // reconnecting again
                            /*But if you use a recursive function you must handle a stack overflow exception*/
                            try{
                                connectFromAddressandName();
                                Thread.sleep(5000); // 5 second thread
                            } catch (StackOverflowError stackOverflowError){
                                // if a stackoverflow found we must reset a app of finish a program in here
                                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                                   restartApp();
                                } else {
                                    // if below android Q we finish a program
                                    finish();
                                }

                            } catch (InterruptedException ex) {
                                throw new RuntimeException(ex);
                            }

                            // if a bluetooth socket not connection to... we close app and finish
                            if(!bluetoothSocket.isConnected()){
                                Toast.makeText(BluetoothSerialTest.this, "Bluetooth Device not connecting app shutdown", Toast.LENGTH_SHORT).show();
                                try {
                                    bluetoothSocket.close();
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                                finish();
                                break;
                            }
                        }
                    }
                });
            }

            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();

            // start read data
            startReadData();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("Connection Statue","Connecting into ESP32");
                    if(bluetoothSocket.isConnected()){
                        Toast.makeText(BluetoothSerialTest.this, "Connected", Toast.LENGTH_SHORT).show();
                        status.setText("Connecting");
                    } else {
                        Toast.makeText(BluetoothSerialTest.this, "Disconencted", Toast.LENGTH_SHORT).show();
                        status.setText("Disconnecting");
                    }
                }
            });
        } catch (IOException e) {
            Log.e("Connection Statue","Fail to connection",e);
            finish();
        }
    }

    private void startReadData(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                int bytes;
                String receiverMessage;

                while(true) {
                    try {
                        try {
                            // nullpointerexception pottentially
                            bytes = inputStream.read(buffer);
                            receiverMessage = new String(buffer, 0, bytes);
                            Log.d("Message", receiverMessage);
                            Message readMsg = handler.obtainMessage(MESSAGE_READ, bytes, -1, receiverMessage);
                            readMsg.sendToTarget();
                            Log.d("FinalMessage", String.valueOf(readMsg));
                        } catch (IOException e) {
                            Log.e("InputStream Connection", "InputStream Connection fail", e);
                            // try to rescan a bluetooth and we can give connection time in here in 10 second
                            scanBluetoothAddress();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!bluetoothSocket.isConnected()) {
                                        Toast.makeText(BluetoothSerialTest.this, "Reconnecting", Toast.LENGTH_SHORT).show();
                                        status.setText("Reconnecting");
                                    } else {
                                        Toast.makeText(BluetoothSerialTest.this, "Connected", Toast.LENGTH_SHORT).show();
                                        status.setText("Connecting");
                                    }
                                }
                            });
                            // break;
                        }
                    } catch (NullPointerException e){
                        // if a app null we restart a app
                       restartApp();
                    }
                }
            }
        }).start();
    }

    @Override
    public void startActivityForResult(@NonNull Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
        if(requestCode == REQUEST_ENABLE_BT){
            // if a request code equals EQUALS_ENABLE_BT
           if(bluetoothAdapter.isEnabled()) {
               scanBluetoothAddress();
           }
        }
    }

    /*---------------------------------------------------------------------------------------------------------------------------
     * ------------------------------------------------END BLUETOOH CODE CONNECTION---------------------------------------------------*/

    /*---------------------------------------------------------------------------------------------------------------------------
    * ------------------------------------------------CHECK NETWORK CONNECTION---------------------------------------------------*/

    private void checkInternetConnection(Context context){
        ConnectivityManager internetContext = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            NetworkCapabilities networkCapatibility = internetContext.getNetworkCapabilities(internetContext.getActiveNetwork());

            // check condition
            if(networkCapatibility == null){
                Toast.makeText(context, "Network Disconnect, please check connection", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Network Connected", Toast.LENGTH_SHORT).show();
            }

        } else {
            // if a android below tiramisu
            NetworkInfo networkInfo = internetContext.getActiveNetworkInfo();
            if(networkInfo.isConnectedOrConnecting() && networkInfo.isAvailable()){
                Toast.makeText(context, "Network Connected", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Network Disconnect, please check a connection", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*---------------------------------------------------------------------------------------------------------------------------
     * ------------------------------------------------END CHECK NETWORK CONNECTION---------------------------------------------------*/


    /*---------------------------------------------------------------------------------------------------------
    * -------------------------------------------MQTT PUBLISHER FUNCTION---------------------------------------*/
    private void publishMessage(String topic, String JSONmessage){
        // we send a json data format
        MqttMessage messageJSON = new MqttMessage(JSONmessage.getBytes());
        mqttHandler.publish(topic, String.valueOf(messageJSON));
    }
    /*---------------------------------------------------------------------------------------------------------
     * -------------------------------------------END MQTT PUBLISHER FUNCTION---------------------------------------*/

    /*--------------------------------------------------------------------------------------------------------------------------
    * ------------------------------------------------- GPS AREA ---------------------------------------------------------------*/

    public void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION);
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
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                })
                .addOnFailureListener(e ->{
                    int statusCode = ((ApiException) e).getStatusCode();
                    Log.d("startlocationupdate","Contain error" + statusCode);
                });
    }

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
        assert lastlocation != null;
        Log.d("Location" ,"latitude"+ lastlocation.getLatitude());
        Log.d("Location","longitude"+ lastlocation.getLongitude());
        Log.d("Location","altitude"+ lastlocation.getAltitude());

        String s_lat = String.format(Locale.ROOT,"%.6f",lastlocation.getLatitude());
        String s_log = String.format(Locale.ROOT,"%.6f",lastlocation.getLongitude());

        latitude = lastlocation.getLatitude();
        longitude = lastlocation.getLongitude();

        if(lastlocation.getLatitude() == 0 && lastlocation.getLongitude() == 0){
            // set default start  data
            setLatitudeStr("-2.983316");// -> must s_lat
            setLongitudeStr("104.73318");// -> must s_log
        }


        setLatitudeStr(s_lat);// -> must s_lat
        setLongitudeStr(s_log);// -> must s_log

        try{
            // for Address (if a need, and dont delete this code)
            Geocoder geocoder = new Geocoder(this,Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude,longitude,1);

            fetched_address = addresses.get(0).getAddressLine(0);
            Log.d("Location","LocationdataAddress"+fetched_address);


        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void turnOnGPS() {
        // check a gps enable
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // if a GPS Active enable we get a log and lat data
            // we request a location
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            // you may scan a bluetooth in here
        } else {
            // if GPS disable
            requestEnableGPS();

        }
    }

    public void init(){
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                receiveLocation(locationResult);
            }
        };

        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,5000) // -> 5000
                .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                .setMinUpdateIntervalMillis(500) // default -> 500
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Turn On A GPS Needed")
                .setMessage("Need Turn on a GPS for Application Function, click Turn On")
                .setPositiveButton("Turn On", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    this.startActivity(intent);
                    dialog.dismiss();
                });
        builder.create();
        builder.show();
    }

    public Float getLongitudeStr() {
        return longitudeStr;
    }

    public void setLongitudeStr(String longitudeStr) {
        this.longitudeStr = Float.parseFloat(longitudeStr);
    }

    public Float getLatitudeStr() {
        return latitudeStr;
    }

    public void setLatitudeStr(String latitudeStr) {
        this.latitudeStr = Float.parseFloat(latitudeStr);
    }

    /*------------------------------------------------------------------------------------------------------
     * ------------------------------------------END GPS AREA ------------------------------------------------*/

    /*------------------------------------------------------------------------------------------------------
     * ------------------------------------------RESTART APP ------------------------------------------------*/

    private void restartApp(){
        Toast.makeText(BluetoothSerialTest.this, "Application close restart app", Toast.LENGTH_SHORT).show();
        // for restart a app
        Context context = getApplicationContext();
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        Intent mainIntent = Intent.makeRestartActivityTask(intent.getComponent());
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

    /*------------------------------------------------------------------------------------------------------
     * ------------------------------------------END RESTART APP ------------------------------------------------*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(bluetoothSocket != null){
            try{
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e("Bluetooth Destroy","Fail close a socket",e);
            }
        }
        mqttHandler.disconnected();
        stopLocationUpdate();
    }
}