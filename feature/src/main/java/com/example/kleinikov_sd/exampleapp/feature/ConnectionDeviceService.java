package com.example.kleinikov_sd.exampleapp.feature;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConnectionDeviceService extends Service {

    private static final int TIMEOUT = 100;
    public static final String TAG = "myTag";

    private Callbacks mActivity;
    private BluetoothDevice mDevice;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private BluetoothSocket mSocket;
    private ScheduledExecutorService mExecutor;

    private int mMessageNumber;
    private int mErrorNumber;
    private long mTime;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Start service");
        mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        openBT();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    class LocalBinder extends Binder {
        ConnectionDeviceService getServiceInstance() {
            return ConnectionDeviceService.this;
        }
    }

    public void registerClient(Activity activity) {
        this.mActivity = (Callbacks) activity;
    }

    private void openBT() {
        if (mSocket != null) {
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
            Toast.makeText(getApplicationContext(), "Connection is active", Toast.LENGTH_SHORT).show();
        } catch (IOException connectException) {
            Log.w(TAG, "Unable to connect");
            mActivity.onBackPressed();
            Toast.makeText(getApplicationContext(), "Unable to connect. Please, try again", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendData() {
        Log.i(TAG, "Send data: " + (System.currentTimeMillis() - mTime));
        try {
            byte[] message = new byte[]{0x01, 0x07, (byte) 0xe2, 0x41};
            mOutputStream.write(message);
            beginListenForData();
            mMessageNumber++;
            try {
                mActivity.updateClient(mMessageNumber, mErrorNumber);
            } catch (Throwable e) {
                Log.e(TAG, "Error ", e);
            }
        } catch (IOException e) {
            Log.e(TAG, "An error occurred while sending data");
            stop();
            e.printStackTrace();
        }
    }

    private void beginListenForData() {
        long startTime = System.currentTimeMillis();
        Log.i(TAG, "Start listening " + (System.currentTimeMillis() - mTime));
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
                mErrorNumber++;
                mActivity.updateClient(mMessageNumber, mErrorNumber);
            }
        }
        try {
            if (!CRC.getInstance().checkCRC(buffer)) {
                mErrorNumber++;
                mActivity.updateClient(mMessageNumber, mErrorNumber);
                Log.e(TAG, "Buffer" + Arrays.toString(buffer));
            }
        } catch (Throwable e) {
            Log.e(TAG, " error", e);
        }
        Log.i(TAG, "Response time" + " " + (System.currentTimeMillis() - mTime) + " Answer text " + outText);
    }

    private String getHexString(Byte number) {
        String x = Integer.toHexString(number & 255);
        if (x.length() < 2) {
            x = "0" + x;
        }
        return x;
    }

    public void start() {
        Intent notificationIntent = ((Activity)mActivity).getIntent();

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), "myChanelId")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(mDevice.getName())
                .setContentText("executing")
                .setContentIntent(pendingIntent).build();
        startForeground(1, notification);


        mTime = System.currentTimeMillis();
        Log.i(TAG, "Start time " + (System.currentTimeMillis() - mTime));
        mExecutor = Executors.newScheduledThreadPool(1);
        mExecutor.scheduleAtFixedRate(this::sendData, 300, TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        stopForeground(true);
        if (mExecutor != null) {
            mExecutor.shutdown();
            mExecutor = null;
        }
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

    public interface Callbacks {
        void updateClient(int messageNumber, int errorNumber);
        void onBackPressed();
    }

    @Override
    public void onDestroy() {
        resetConnection();
        Log.i(TAG, "Destroy service");
        super.onDestroy();
    }
}
