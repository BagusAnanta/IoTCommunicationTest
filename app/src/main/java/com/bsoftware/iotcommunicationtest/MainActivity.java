package com.bsoftware.iotcommunicationtest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.bsoftware.iotcommunicationtest.MqttClient.MQTTClient;

import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.S)
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_CONNECTION = 3;
    private static final int REQUEST_FINE_LOCATION = 2;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private TextView valueTextView;

    private final String DEVICE_NAME = "AMBULANCE POLSRI";
    private boolean isScanning = false;
    private final ScanSettings scanSettings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build();

    private int currentConnectionAttemp = 1;
    private final int MAXIMUM_CONNECTION_ATTEMPTS = 5;

    // scan bluetooth
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if (result.getDevice().getName().equals(DEVICE_NAME)) {
                runOnUiThread(() -> {
                    Log.d("onScanResult", "Connection into BLE");
                    Toast.makeText(MainActivity.this, "Connecting into BLE", Toast.LENGTH_SHORT).show();
                });
            }

            if (isScanning) {
                result.getDevice().connectGatt(MainActivity.this, false, gattcallback);
                isScanning = false;
                bluetoothAdapter.getBluetoothLeScanner().stopScan(this);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            // on fail scanning
            Log.d("onScanFailed", "Connection fail");
            Toast.makeText(MainActivity.this, "Scan connection fail", Toast.LENGTH_SHORT).show();
        }
    };

    private void startReceiving() {
        runOnUiThread(() -> {
            Log.d("onScanConnection", "Scan BLE Connection");
            Toast.makeText(MainActivity.this, "Scan BLE Connection", Toast.LENGTH_SHORT).show();
        });

        isScanning = true;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {return;}
        bluetoothAdapter.getBluetoothLeScanner().startScan(null, scanSettings, scanCallback);

    }

    private final BluetoothGattCallback gattcallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("State Connected", "Connect Into Gatt Server");
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                    // ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.BLUETOOTH},REQUEST_BLUETOOTH_CONNECTION);
                    return;
                }
                gatt.discoverServices();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connection from BLE Device", Toast.LENGTH_SHORT).show());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("State Disconnected", "Disconnected From Gatt Server");
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Disconnected from BLE device", Toast.LENGTH_SHORT).show();
                    valueTextView.setText("State Disconnected");
                });
            } else {
                gatt.close();
                currentConnectionAttemp+=1;
                runOnUiThread(() -> {
                    Log.w("Attemp connection", "Attemp to Connection" + currentConnectionAttemp/MAXIMUM_CONNECTION_ATTEMPTS);
                });
            }

            if(currentConnectionAttemp <= MAXIMUM_CONNECTION_ATTEMPTS){
                startReceiving();
            } else {
                runOnUiThread(() -> {
                    Log.e("ConnectionStatus","Could not connection into ble device");
                    Toast.makeText(MainActivity.this,"Could connect into ble",Toast.LENGTH_SHORT).show();
                });
            }


        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService service = gatt.getService(UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b"));
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8"));
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH}, REQUEST_BLUETOOTH_CONNECTION);
            }
            gatt.setCharacteristicNotification(characteristic, true);

            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);

        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);

            byte[] data = characteristic.getValue();
            int test = (
                    (data[3] & 0xFF) << 24) |
                    ((data[2] & 0xFF) << 16) |
                    ((data[1] & 0xFF) << 8) |
                    (data[0] & 0xFF);

            runOnUiThread(() -> {
                valueTextView.setText(String.valueOf(test));
                Toast.makeText(MainActivity.this, "Data = " + test, Toast.LENGTH_SHORT).show();
            });
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        valueTextView = findViewById(R.id.testTextView);
        String BrokerURI = "tcp://test.mosquitto.org:1883";
        String Topic = "ambulance/cabin";
        String Message = "Halo Disana !";
        String ClientID = "mqttv311";
        int Qos = 0;

        // Mqtt Tester
        MQTTClient mqttClient = new MQTTClient(this,BrokerURI,ClientID).onMqttStartedAndPublisher(Topic,Message,Qos);

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.BLUETOOTH_CONNECT},REQUEST_BLUETOOTH_CONNECTION);
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth LE not support", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
        } else {
            // Enable bluetooth in here
            enableBluetooth();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // disconnectFromDevice in here
        disconnectFromDevice();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_FINE_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // EnableBluetooth in here
            enableBluetooth();
        } else {
            Toast.makeText(this, "Location Permission required for Bluetooth LE", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void enableBluetooth() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH}, REQUEST_BLUETOOTH_CONNECTION);
            }
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            // connectionIntoDevice
            connectionToDevice();
            startReceiving();
        }
    }

    private void connectionToDevice() {
       BluetoothDevice device = bluetoothAdapter.getRemoteDevice("3C:61:05:3F:61:16");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.BLUETOOTH},REQUEST_BLUETOOTH_CONNECTION);
        }
        bluetoothGatt = device.connectGatt(this, false, gattcallback);
    }

    private void disconnectFromDevice() {
        if (bluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.BLUETOOTH},REQUEST_BLUETOOTH_CONNECTION);
            }
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
    }
}