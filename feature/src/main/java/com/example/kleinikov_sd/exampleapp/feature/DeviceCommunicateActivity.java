package com.example.kleinikov_sd.exampleapp.feature;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.logging.SocketHandler;

public class DeviceCommunicateActivity extends AppCompatActivity {

    private static final String KEY_RESPONSE_TEXT = "responseText";
    private static final int TIMEOUT = 1000;


    private static final String TAG = "myTag";
    private TextView deviceNameText;
    private TextView responseText;
    private Button requestButton;
    private BluetoothDevice mDevice;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private static BluetoothSocket mSocket;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communicate_device);

        deviceNameText = findViewById(R.id.device_name);
        responseText = findViewById(R.id.response_text);
        requestButton = findViewById(R.id.request_button);
        mDevice = getIntent().getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        deviceNameText.setText(mDevice.getName());
        requestButton.setOnClickListener(this::sendData);

        if (savedInstanceState != null) {
            responseText.setText(savedInstanceState.getCharSequence(KEY_RESPONSE_TEXT));
        }

        openBT();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(KEY_RESPONSE_TEXT, responseText.getText());
    }

    private void openBT() {
        if (mSocket!=null && mSocket.isConnected()){
            try {
                mOutputStream = mSocket.getOutputStream();
                mInputStream = mSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            ParcelUuid[] idArray = mDevice.getUuids();
            UUID uuid = UUID.fromString(idArray[0].toString());
            mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
            Log.e(TAG, "New socket");
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        try {
            mSocket.connect();
            mInputStream = mSocket.getInputStream();
            mOutputStream = mSocket.getOutputStream();
        } catch (IOException connectException) {
            Log.w(TAG, "Unable to connect");
        }
        Toast.makeText(this, "Connection is active", Toast.LENGTH_SHORT).show();

    }

    private void sendData(View view) {
        if (requestButton.isActivated()) {
            return;
        }

        try {
            byte[] message = new byte[4];
            message[0] = 0x01;
            message[1] = 0x07;
            message[2] = (byte) 0xe2;
            message[3] = 0x41;

            Log.e(TAG, "write message");
            mOutputStream.write(message);
            requestButton.setActivated(true);

            beginListenForData();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void beginListenForData() {

        Thread workerThread = new Thread(() -> {
            StringBuilder outText = new StringBuilder();
            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < TIMEOUT) {
                try {
                    int bytesAvailable = mInputStream.available();
                    if (bytesAvailable > 0) {
                        byte[] packetBytes = new byte[bytesAvailable];
                        mInputStream.read(packetBytes);
                        for (int i = 0; i < bytesAvailable; i++) {
                            byte b = packetBytes[i];
                            outText.append(Integer.toHexString(b & 0xff)).append(" ");
                        }
                    }
                } catch (IOException ex) {
                    Log.e(TAG, "Unable to read", ex);
                }
            }
            runOnUiThread(() -> {
                Log.i(TAG,"Text " + outText);
                        responseText.setText(outText);
                        requestButton.setActivated(false);
                    }
            );
        });
        workerThread.start();

    }

    public static void cancel() {
        try {
            if (mSocket != null) {
                mSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        cancel();
        super.onBackPressed();
    }
}
