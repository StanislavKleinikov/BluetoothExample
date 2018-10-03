package com.example.kleinikov_sd.exampleapp.feature;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
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
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Base64;
import java.util.UUID;

public class DeviceCommunicateActivity extends AppCompatActivity {

    private static final String MESSAGE = "01 07 e2 41";
    private static final String TAG = "myTag";
    private TextView requestText;
    private TextView responseText;
    private Button requestButtton;
    private BluetoothDevice mDevice;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private BluetoothSocket mSocket;
    volatile boolean stopWorker;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communicate_device);

        requestText = findViewById(R.id.request_text);
        responseText = findViewById(R.id.response_text);
        requestButtton = findViewById(R.id.request_button);
        mDevice = getIntent().getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        requestText.setText(mDevice.getName());
        requestButtton.setOnClickListener(this::sendData);

        openBT();
    }

    private void openBT() {
        BluetoothSocket tmp = null;
        try {
            ParcelUuid[] idArray = mDevice.getUuids();
            UUID uuid = UUID.fromString(idArray[0].toString());
            Log.w(TAG, "idArray " + idArray);
            tmp = mDevice.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mSocket = tmp;
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mSocket.connect();
            Log.w(TAG, "Run try...");
            mInputStream = mSocket.getInputStream();
            mOutputStream = mSocket.getOutputStream();
        } catch (IOException connectException) {
            Log.w(TAG, "Unable to connect", connectException);
            onDestroy();
        }
        Log.w(TAG, "connection " + mSocket.isConnected());

        beginListenForData();

        Toast.makeText(this, "Bluetooth opened", Toast.LENGTH_SHORT).show();

    }

    private void sendData(View view) {

        try {
            byte[] message = new byte[4];
            message[0] = 0x01;
            message[1] = 0x07;
            message[2] = -0x1e;
            message[3] = 0x41;

            byte signedByte = -0x1e;

            int unsignedByte = signedByte & 255;

            Log.i(TAG, "Signed: " + signedByte + " Unsigned: " + Integer.toHexString(unsignedByte));
            byte newByte = (byte) (unsignedByte - 256);
            Log.i(TAG, "New byte: " + Integer.toHexString(newByte));

            mOutputStream.write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10;


        Thread workerThread = new Thread(() -> {
            stopWorker = false;
            int readBufferPosition;
            byte[] readBuffer = new byte[1024];

            while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                try {
                    int bytesAvailable = mInputStream.available();
                    if (bytesAvailable > 0) {
                        byte[] packetBytes = new byte[bytesAvailable];
                        readBufferPosition = mInputStream.read(packetBytes);
                        for (int i = 0; i < bytesAvailable; i++) {
                            int unsignedByte = packetBytes[i];
                            byte b = (byte) (unsignedByte - 256);
                            Log.e(TAG, Integer.toHexString(b));
                            if (b == delimiter) {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "UTF-16");
                                readBufferPosition = 0;
                                Log.e(TAG, data);
                                handler.post(() -> responseText.setText(data));
                            } else {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                } catch (IOException ex) {
                    stopWorker = true;
                }
            }
        });

        workerThread.start();

    }

    private class ConnectThread extends Thread {

        private final BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mDevice = device;
            try {
                ParcelUuid[] idArray = mDevice.getUuids();
                UUID uuid = UUID.fromString(idArray[0].toString());
                Log.w(TAG, "idArray " + idArray);
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            Log.w(TAG, "Run Thread");
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mSocket.connect();
                Log.w(TAG, "Run try...");
                mInputStream = mSocket.getInputStream();
                mOutputStream = mSocket.getOutputStream();
            } catch (IOException connectException) {
                Log.w(TAG, "Unable to connect", connectException);
                onDestroy();
                return;
            }
            Log.w(TAG, "connection " + mSocket.isConnected());
        }


    }

    public void cancel() {
        try {
            mSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }

    @Override
    protected void onDestroy() {
        stopWorker = true;
        super.onDestroy();
    }
}
