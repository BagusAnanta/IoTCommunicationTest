package com.bsoftware.iotcommunicationtest.BluetoothConnection;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.bsoftware.iotcommunicationtest.BluetoothSerialTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothService {
    private static final String TAG = "DEBUG_APP_BLUETOOTH";
    private static Handler handler;

    private interface MessageConstants{
        public static final int MESSAGE_READ = 0;
    }

    public static class ConnectedThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private byte[] buffer;

        Intent dataIntent;

        public ConnectedThread(BluetoothSocket socket){
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try{
                tempIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG,"Error occured when creating InputStream");
            }

            try{
                tempOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG,"Error occured when creating Output");
            }

            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run(){
            buffer = new byte[1024];
            int numByte;

            while (true){
                try{
                    numByte = inputStream.read(buffer);
                    Message readmessage = handler.obtainMessage(
                            MessageConstants.MESSAGE_READ,
                            numByte,
                            -1,
                            buffer
                    );
                    readmessage.sendToTarget();
                    Log.d("Message Receiver :", String.valueOf(readmessage));
                    dataIntent.putExtra("MessageReceiver",numByte);
                } catch (IOException e) {
                    Log.d(TAG,"Input Stream was Disconnected",e);
                    break;
                }
            }
        }
    }




}
