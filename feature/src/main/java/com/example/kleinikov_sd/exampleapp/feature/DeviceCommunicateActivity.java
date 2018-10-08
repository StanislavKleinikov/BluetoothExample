package com.example.kleinikov_sd.exampleapp.feature;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DeviceCommunicateActivity extends AppCompatActivity {

    public static final int SUCCESS = 1;
    public static final int FAIL = 2;


    private static final String KEY_RESPONSE_TEXT = "responseText";
    private static final String KEY_MESSAGE_NUMBER = "messageNumber";
    private static final String KEY_ERROR_NUMBER = "errorNumber";
    private static final String KEY_ACTIVATED = "activated";
    private static final int TIMEOUT = 100;
    private static final int HASH_07 = Arrays.hashCode(new byte[]{0x01, 0x07, 0x00, 0x30, 0x22});
    private static final String TAG = "myTag";

    private TextView deviceNameText;
    private TextView responseText;
    private TextView messageNumberView;
    private TextView errorNumberView;
    private ToggleButton toggleButton;
    private BluetoothDevice mDevice;
    private static InputStream mInputStream;
    private static OutputStream mOutputStream;
    private static BluetoothSocket mSocket;
    private static ScheduledExecutorService mExecutor;
    private static Handler mHandler;

    private static int messageNumber;
    private static int errorNumber;
    private long time;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communicate_device);

        deviceNameText = findViewById(R.id.device_name);
        responseText = findViewById(R.id.response_text);
        messageNumberView = findViewById(R.id.message_number);
        errorNumberView = findViewById(R.id.error_number);
        toggleButton = findViewById(R.id.toggle_button);
        toggleButton.setOnClickListener(v -> {
            if (toggleButton.isChecked()) {
                start();
            } else {
                stop();
            }
        });

        if (savedInstanceState != null) {
            responseText.setText(savedInstanceState.getCharSequence(KEY_RESPONSE_TEXT));
            messageNumber = savedInstanceState.getInt(KEY_MESSAGE_NUMBER);
            errorNumber = savedInstanceState.getInt(KEY_ERROR_NUMBER);
            toggleButton.setChecked(savedInstanceState.getBoolean(KEY_ACTIVATED));
        }

        messageNumberView.setText(String.valueOf(messageNumber));
        errorNumberView.setText(String.valueOf(errorNumber));
        mDevice = getIntent().getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        deviceNameText.setText(mDevice.getName());
        mHandler = new Handler(msg -> {
            switch (msg.what) {
                case SUCCESS:
                    messageNumber++;
                    messageNumberView.setText(String.valueOf(messageNumber));
                    break;
                case FAIL:
                    errorNumber++;
                    errorNumberView.setText(String.valueOf(errorNumber));
            }
            return false;
        });

        openBT();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(KEY_RESPONSE_TEXT, responseText.getText());
        outState.putInt(KEY_MESSAGE_NUMBER, messageNumber);
        outState.putInt(KEY_ERROR_NUMBER, errorNumber);
        outState.putBoolean(KEY_ACTIVATED, toggleButton.isChecked());
    }

    private void openBT() {

        if (mSocket != null) {
            return;
        }
        try {
            ParcelUuid[] idArray = mDevice.getUuids();
            UUID uuid = UUID.fromString(idArray[0].toString());
            mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
            resetMessageNumber();
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        try {
            mSocket.connect();
            mInputStream = mSocket.getInputStream();
            mOutputStream = mSocket.getOutputStream();
            Toast.makeText(this, "Connection is active", Toast.LENGTH_SHORT).show();
        } catch (IOException connectException) {
            Log.w(TAG, "Unable to connect");
            onBackPressed();
            Toast.makeText(this, "Unable to connect. Please, try again", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendData() {
        Log.i(TAG, "Send data: " + (System.currentTimeMillis() - time));
        try {
            byte[] message = new byte[]{0x01, 0x07, (byte) 0xe2, 0x41};
            mOutputStream.write(message);
            beginListenForData();
            mHandler.sendEmptyMessage(SUCCESS);
        } catch (IOException e) {
            Log.e(TAG, "An error occurred while sending data");
            stop();
            e.printStackTrace();
        }
    }

    private void beginListenForData() {
        long startTime = System.currentTimeMillis();
        Log.i(TAG, "Start listening " + (System.currentTimeMillis() - time));
        StringBuilder outText = new StringBuilder();
        byte[] buffer = new byte[0];
        int currentPosition = 0;
        while ((System.currentTimeMillis() - startTime) < TIMEOUT) {
            try {
                int bytesAvailable = mInputStream.available();
                if (bytesAvailable > 0) {
                    byte[] packetBytes = new byte[bytesAvailable];
                    buffer = Arrays.copyOf(buffer, buffer.length + packetBytes.length);
                    mInputStream.read(packetBytes);
                    for (int i = 0; i < bytesAvailable; i++, currentPosition++) {
                        byte b = packetBytes[i];
                        buffer[currentPosition] = b;
                        outText.append(getHexString(b)).append(" ");
                    }
                }
            } catch (IOException ex) {
                Log.e(TAG, "Unable to read", ex);
                mHandler.sendEmptyMessage(FAIL);
            }
        }
        if (HASH_07 != Arrays.hashCode(buffer)) {
            mHandler.sendEmptyMessage(FAIL);
            Log.e(TAG, "Buffer" + Arrays.toString(buffer));
        }

        Log.i(TAG, "Response time" + " " + (System.currentTimeMillis() - time) + " Answer text " + outText);
        runOnUiThread(() -> responseText.setText(outText));
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

    private void resetMessageNumber() {
        messageNumber = 0;
        errorNumber = 0;
        messageNumberView.setText(String.valueOf(messageNumber));
        errorNumberView.setText(String.valueOf(errorNumber));
    }

    private String getHexString(Byte number) {
        String x = Integer.toHexString(number & 255);
        if (x.length() < 2) {
            x = "0" + x;
        }
        return x;
    }

    private void start() {
        time = System.currentTimeMillis();
        Log.i(TAG, "Start time " + (System.currentTimeMillis() - time));
        mExecutor = Executors.newScheduledThreadPool(1);
        mExecutor.scheduleAtFixedRate(this::sendData, 300, TIMEOUT, TimeUnit.MILLISECONDS);
    }

    private void stop() {
        if (mExecutor != null) {
            mExecutor.shutdown();
            mExecutor = null;
        }
    }

    @Override
    public void onBackPressed() {
        stop();
        resetConnection();
        super.onBackPressed();
    }
}
