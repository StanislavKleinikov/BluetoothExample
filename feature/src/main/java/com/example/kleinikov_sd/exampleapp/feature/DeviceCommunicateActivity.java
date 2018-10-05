package com.example.kleinikov_sd.exampleapp.feature;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DeviceCommunicateActivity extends AppCompatActivity {

    private static final String KEY_RESPONSE_TEXT = "responseText";
    private static final int TIMEOUT = 100;
    private static final int TIMEOUT_RESPONSE = 1000;

    private static final String TAG = "myTag";
    private TextView deviceNameText;
    private TextView responseText;
    private TextView messageNumberView;
    private TextView errorNumberView;
    private BluetoothDevice mDevice;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private static BluetoothSocket mSocket;
    private ScheduledExecutorService mExecutor;

    private int messageNumber;
    private int errorNumber;

    private long time;
    private static int threadId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communicate_device);

        deviceNameText = findViewById(R.id.device_name);
        responseText = findViewById(R.id.response_text);
        messageNumberView = findViewById(R.id.message_number);
        errorNumberView = findViewById(R.id.error_number);
        messageNumber = Integer.parseInt(messageNumberView.getText().toString());
        errorNumber = Integer.parseInt(errorNumberView.getText().toString());
        mDevice = getIntent().getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        deviceNameText.setText(mDevice.getName());

        openBT();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(KEY_RESPONSE_TEXT, responseText.getText());
    }

    private void openBT() {

        if (mSocket != null) {
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
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        try {
            mSocket.connect();
            mInputStream = mSocket.getInputStream();
            mOutputStream = mSocket.getOutputStream();
            Toast.makeText(this, "Connection is active", Toast.LENGTH_SHORT).show();
            mExecutor = Executors.newScheduledThreadPool(1);
            mExecutor.scheduleWithFixedDelay(this::sendData, 400, 1000, TimeUnit.MILLISECONDS);
        } catch (IOException connectException) {
            Log.w(TAG, "Unable to connect");
            onBackPressed();
            Toast.makeText(this, "Unable to connect. Please, try again", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendData() {
        beginListenForData();
       /* time = System.currentTimeMillis();
        Log.i(TAG, "Time: " + (System.currentTimeMillis() - time));
        try {
            byte[] message = new byte[]{0x01, 0x07, (byte) 0xe2, 0x41};
            mOutputStream.write(message);
            beginListenForData();
            messageNumber++;
            messageNumberView.setText(String.valueOf(messageNumber));
        } catch (IOException e) {
            e.printStackTrace();
            errorNumber++;
            errorNumberView.setText(String.valueOf(errorNumber));
        }*/
    }


    private void beginListenForData() {
        Log.i(TAG, "Done ");
        messageNumber++;
        runOnUiThread(() -> messageNumberView.setText(String.valueOf(messageNumber)));


       /* int x = threadId++;
        long startTime = System.currentTimeMillis();
        Log.e(TAG, "Start thread" + x);

        StringBuilder outText = new StringBuilder();
        try {
            int bytesAvailable = mInputStream.available();
            if (bytesAvailable > 0) {
                byte[] packetBytes = new byte[bytesAvailable];
                mInputStream.read(packetBytes);
                for (int i = 0; i < bytesAvailable; i++) {
                    byte b = packetBytes[i];
                    outText.append(getHexString(b)).append(" ");
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, "Unable to read", ex);
            errorNumber++;
            errorNumberView.setText(String.valueOf(errorNumber));
        }

        Log.i(TAG, "Response time from thread" + x + " " + (System.currentTimeMillis() - time) + " Answer text " + outText);
        runOnUiThread(() -> {
                    responseText.setText(outText);
                }
        );*/
    }

    private void resetConnection() {
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mInputStream = null;
        }
        if (mOutputStream != null) {
            try {
                mOutputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            mOutputStream = null;
        }
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSocket = null;
        }
    }

    private String getHexString(Byte number) {
        String x = Integer.toHexString(number & 255);
        if (x.length() < 2) {
            x = "0" + x;
        }
        return x;
    }

    @Override
    public void onBackPressed() {
        if (mExecutor != null) {
            mExecutor.shutdown();
        }
        resetConnection();
        super.onBackPressed();
    }
}
