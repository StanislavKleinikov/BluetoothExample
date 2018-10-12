package com.example.kleinikov_sd.exampleapp.feature;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.example.kleinikov_sd.exampleapp.feature.MainActivity.TAG;

public class ConnectionDeviceService extends Service {

    public static final String ACTION_UNABLE_CONNECT = "unableToConnect";
    public static final String ACTION_CONNECTION_ACTIVE = "connectionIsActive";
    public static final String ACTION_RECONNECT = "actionReconnect";
    public static final String ACTION_DISCONNECT = "actionDisconnect";
    public static final String ACTION_CANCEL = "actionCancel";
    private static final byte[] MESSAGE_07 = new byte[]{0x01, 0x07, (byte) 0xe2, 0x41};
    private static final int TIMEOUT = 100;

    private Callbacks mActivity;
    private BluetoothDevice mDevice;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private BluetoothSocket mSocket;
    private ScheduledExecutorService mExecutor;
    private Intent mIntent;

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
        mIntent = new Intent();
        if (!openBT(mDevice)) {
            mIntent.setAction(ACTION_CANCEL);
            sendBroadcast(mIntent);
        }
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

    public boolean openBT(BluetoothDevice device) {

        try {
            ParcelUuid[] idArray = device.getUuids();
            UUID uuid = UUID.fromString(idArray[0].toString());
            mSocket = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        try {
            mSocket.connect();
            mInputStream = mSocket.getInputStream();
            mOutputStream = mSocket.getOutputStream();
            Intent connectionActiveIntent = new Intent(ACTION_CONNECTION_ACTIVE);
            sendBroadcast(connectionActiveIntent);
            return true;
        } catch (IOException connectException) {
            Log.w(TAG, "Unable to connect");
            mIntent.setAction(ACTION_UNABLE_CONNECT);
            sendBroadcast(mIntent);
            mSocket = null;
            return false;
        }
    }

    private void sendData(byte[] message) {
        Log.i(TAG, "Send data: " + (System.currentTimeMillis() - mTime));
        try {
            mOutputStream.write(message);
            beginListenForData();
            mMessageNumber++;
            mActivity.updateMessageNumber(mMessageNumber, mErrorNumber);
        } catch (IOException e) {
            stop();
            Log.e(TAG, "An error occurred while sending data");
            mIntent.setAction(ACTION_DISCONNECT);
            sendBroadcast(mIntent);
            restartConnection();
            e.printStackTrace();
        }
    }

    private void restartConnection() {
            boolean isConnected;
            Log.e(TAG, "Start thread");
            do {
                Log.e(TAG, "openBT");
                sendBroadcast(mIntent.setAction(ACTION_RECONNECT));
                isConnected = openBT(mDevice);
            } while (!isConnected);
            mIntent.setAction(ACTION_CONNECTION_ACTIVE);
            sendBroadcast(mIntent);
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
                Log.e(TAG, "Unable to read");
                mErrorNumber++;
                mActivity.updateMessageNumber(mMessageNumber, mErrorNumber);
            }
        }
        try {
            if (!CRC.getInstance().checkCRC(buffer)) {
                mErrorNumber++;
                mActivity.updateMessageNumber(mMessageNumber, mErrorNumber);
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
        Log.e(TAG, "Start");
        Intent intent = new Intent(getApplicationContext(), DeviceCommunicateActivity.class);
        intent.putExtra(DeviceCommunicateActivity.KEY_ACTIVATED, true);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
        intent.putExtra(DeviceCommunicateActivity.KEY_MESSAGE_NUMBER, mMessageNumber);
        intent.putExtra(DeviceCommunicateActivity.KEY_ERROR_NUMBER, mErrorNumber);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, "myChanelId")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(mDevice.getName())
                        .setContentText("Executing...")
                        .setOngoing(true)
                        .setWhen(new Date().getTime())
                        .setUsesChronometer(true)
                        .setContentIntent(resultPendingIntent);

        Notification notification = builder.build();
        startForeground(1, notification);

        mTime = System.currentTimeMillis();
        Log.i(TAG, "Start time " + (System.currentTimeMillis() - mTime));
        mExecutor = Executors.newScheduledThreadPool(1);
        mExecutor.scheduleAtFixedRate(() -> sendData(MESSAGE_07), 300, TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        stopForeground(true);
        if (mExecutor != null) {
            mExecutor.shutdownNow();
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
        void updateMessageNumber(int messageNumber, int errorNumber);
    }

    @Override
    public void onDestroy() {
        resetConnection();
        Log.i(TAG, "Destroy service");
        super.onDestroy();
    }
}
