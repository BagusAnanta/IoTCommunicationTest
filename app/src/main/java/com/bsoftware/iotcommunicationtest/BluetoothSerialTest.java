package com.bsoftware.iotcommunicationtest;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
            } else {
                Log.d("Permission Granted", "All permission Granted");
            }
        }
    });

    // For Bluetooth Communication
    // init bluetoohtAdapter
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private int REQUEST_ENABLE_BT = 1;

    TextView resultTextView;

    private String deviceName;
    private String deviceHardwareAddress;
    private StringBuilder dataBuffer = new StringBuilder();
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private static final int MESSAGE_READ = 0;

    private static final String BrokerURI = "tcp://test.mosquitto.org:1883";
    private static final String ClientID = "mqttv311";
    private static final String Topic = "ambulance/cabin";
    private MqttHandler mqttHandler;
    private JSONFormatter jsonFormatter = new JSONFormatter();

    GetLocationManager getLocationManager;

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            if (message.what == MESSAGE_READ) {
                String receiverMessage = message.obj.toString();
                String Finaldata = String.valueOf(dataBuffer.append(receiverMessage));

                // 5 means we publish a data if a data fully
                if(Finaldata.length() == 5){
                    // publishMessage(Topic, Finaldata);
                }
            }
            return true;
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_serial_test);
        // getLocationManager in here because can't call a context in up before
        getLocationManager = new GetLocationManager(this,BluetoothSerialTest.this);

        resultTextView = findViewById(R.id.testTextView);

        // permission
        permissionList.addAll(Arrays.asList(permission));
        tellPermission(permissionList);
        // initBluetooth();

        // Mqtt Test
        mqttHandler = new MqttHandler();
        mqttHandler.connect(BrokerURI,ClientID);
        publishMessage(Topic,jsonFormatter.Writedata(3.15f,3.13f,getLocationManager.getLongitudeData(),getLocationManager.getLatitudeData()));


    }

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
                scanBluetoothAddress();
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

                if (deviceName.equals("AMBULANCE POLSRI") && deviceHardwareAddress.equals("3C:61:05:3F:61:16")) {
                    connectFromAddressandName();
                }
            }
        }
    }

    private void connectFromAddressandName() {

        Log.d("DataIntent Name", deviceName);
        Log.d("DataIntent Address", deviceHardwareAddress);

        // we connect a device use Address
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceHardwareAddress);

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
              return;
            }
            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();

            // start read data
            startReadData();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("Connection Statue","Connecting into ESP32");
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

                while(true) {
                    try {
                        bytes = inputStream.read(buffer);
                        String receiverMessage = new String(buffer, 0,bytes);
                        Log.d("Message", receiverMessage);
                        Message readMsg = handler.obtainMessage(MESSAGE_READ, bytes, -1, receiverMessage);
                        readMsg.sendToTarget();
                        Log.d("FinalMessage", String.valueOf(readMsg));
                    } catch (IOException e) {
                        Log.e("InputStream Connection", "InputStream Connection fail", e);
                        break;
                    }
                }
            }
        }).start();
    }

    // function for test
    private void publishMessage(String topic, String JSONmessage){
        // we send a json data format
        MqttMessage messageJSON = new MqttMessage(JSONmessage.getBytes());
        Toast.makeText(this, "Publishing message: " + messageJSON, Toast.LENGTH_SHORT).show();
        mqttHandler.publish(topic, String.valueOf(messageJSON));
    }


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
        getLocationManager.stopLocationUpdate();
    }
}