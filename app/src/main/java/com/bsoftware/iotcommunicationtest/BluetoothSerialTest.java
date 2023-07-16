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
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.S)
public class BluetoothSerialTest extends AppCompatActivity {

    ArrayList<String> permissionList = new ArrayList<>();
    String[] permission = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_serial_test);

        // permission
        permissionList.addAll(Arrays.asList(permission));
        tellPermission(permissionList);
        initBluetooth();
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
            }
        }
    }


}