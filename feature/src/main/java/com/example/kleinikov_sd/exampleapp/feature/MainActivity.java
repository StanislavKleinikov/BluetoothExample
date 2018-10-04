package com.example.kleinikov_sd.exampleapp.feature;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String BLE_PIN = "0000";
    private static final String TAG = "myTag";

    private Button switcherButton;
    private Button searchButton;

    private BluetoothAdapter mBluetoothAdapter;
    private SimpleAdapter simpleAdapter;
    private BroadcastReceiver broadcastReceiver;
    private ListView listViewDevices;
    private BluetoothDevice mDevice;

    private static ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switcherButton = findViewById(R.id.switch_button);
        searchButton = findViewById(R.id.find_devices);
        listViewDevices = findViewById(R.id.list_devices);
        listViewDevices.setOnItemClickListener((parent, view, position, id) -> {
            Map map = (Map) simpleAdapter.getItem(position);
            String address = (String) map.get("Address");
            mDevice = mBluetoothAdapter.getRemoteDevice(address);
            if (mDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
               mDevice.createBond();
            } else {
                Intent intent = new Intent(this, DeviceCommunicateActivity.class);
                intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
                mBluetoothAdapter.cancelDiscovery();
                startActivity(intent);
            }
        });

        switcherButton.setOnClickListener(this::switchState);
        searchButton.setOnClickListener(v -> findDevices());

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        simpleAdapter = new SimpleAdapter(this, arrayList, android.R.layout.simple_list_item_2, new String[]{"Name", "Address"},
                new int[]{android.R.id.text1, android.R.id.text2});
        listViewDevices.setAdapter(simpleAdapter);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        broadcastReceiver = new SingleBroadCastReceiver();
        registerReceiver(broadcastReceiver, filter);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    private void switchState(View view) {
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
            Toast.makeText(getApplicationContext(), "Turned off", Toast.LENGTH_SHORT).show();
        } else {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on", Toast.LENGTH_SHORT).show();
        }
    }

    private void findDevices() {
        arrayList.clear();
        simpleAdapter.notifyDataSetChanged();

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            switch (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                case PackageManager.PERMISSION_DENIED:
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            0);

                    break;
                case PackageManager.PERMISSION_GRANTED:
                    mBluetoothAdapter.startDiscovery();
                    break;
            }
        }
    }

    private class SingleBroadCastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                HashMap<String, String> map = new HashMap<>();
                map.put("Name", device.getName());
                map.put("Address", device.getAddress());
                if (!arrayList.contains(map) && device.getName() != null) {
                    arrayList.add(map);
                }
                simpleAdapter.notifyDataSetChanged();
            } else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                bluetoothDevice.setPin(BLE_PIN.getBytes());
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (bluetoothDevice.getBondState()) {
                    case BluetoothDevice.BOND_BONDED:
                        intent = new Intent(MainActivity.this, DeviceCommunicateActivity.class);
                        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
                        mBluetoothAdapter.cancelDiscovery();
                        startActivity(intent);
                }
            }

        }
    }

    @Override
    public void onBackPressed() {
        Log.e(TAG,"on Back pressed");
        DeviceCommunicateActivity.cancel();
        super.onBackPressed();
    }
}
