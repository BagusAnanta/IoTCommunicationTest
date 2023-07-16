package com.bsoftware.iotcommunicationtest.BluetoothConnection;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.UUID;

public class ConnectionThread extends Thread {
    private BluetoothSocket bluetoothSocket;
    private final BluetoothDevice bluetoothDevice;

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    // Default SPP UUID
    private final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private Context context;
    private BluetoothDevice getBluetoothDevice;

    public ConnectionThread(BluetoothDevice device, Context context) {
        BluetoothSocket temp = null;
        bluetoothDevice = this.getBluetoothDevice = device;

        try {
            if (ActivityCompat.checkSelfPermission(this.context = context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            temp = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));

        } catch (IOException e) {
            Log.e("Connection Thread", e.toString());
        }

        bluetoothSocket = temp;
    }

    public void run() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothAdapter.cancelDiscovery();

        try{
            bluetoothSocket.connect();
        } catch (IOException e) {
            try{
                bluetoothSocket.close();
                Log.e("ConnectionStateClose","Close Condition");
            } catch (IOException ex) {
                Log.e("CloseException",ex.toString());
            }
            return;
        }

        new BluetoothService.ConnectedThread(bluetoothSocket).start();
    }

    public void cancel(){
        try{
            bluetoothSocket.close();
        } catch (IOException e) {
            Log.e("ConnectionClose","Can't close a client socket");
        }
    }

}
