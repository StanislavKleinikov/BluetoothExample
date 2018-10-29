package com.atomtex.modbus.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.atomtex.modbus.activity.Callback;
import com.atomtex.modbus.activity.DeviceCommunicateActivity;
import com.atomtex.modbus.command.Command;
import com.atomtex.modbus.domain.Modbus;
import com.atomtex.modbus.domain.ModbusSlave;
import com.atomtex.modbus.transport.ModbusTransportFactory;

import java.util.Date;

import static com.atomtex.modbus.util.BTD3Constant.*;

import static com.atomtex.modbus.activity.MainActivity.TAG;

/**
 * This class is the Service for communication with A device through the Bluetooth.
 * Was designed by using the local service pattern to communicate with the activity
 * which it bounded.
 * Contains methods to create a {@link BluetoothSocket} and to make reconnect with
 * a device in case the signal loss
 *
 * @author kleinikov.stanislav@gmail.com
 */
public class DeviceService extends LocalService {

    public static final String ACTION_UNABLE_CONNECT = "unableToConnect";
    public static final String ACTION_CONNECTION_ACTIVE = "connectionIsActive";
    public static final String ACTION_RECONNECT = "actionReconnect";
    public static final String ACTION_DISCONNECT = "actionDisconnect";
    public static final String ACTION_CANCEL = "actionCancel";

    private Callback mActivity;
    private BluetoothDevice mDevice;
    private Intent mIntent;
    private Modbus modbus;
    private Command command;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Start service");
        mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        mIntent = new Intent();
        new Thread(() -> {
            modbus = new ModbusSlave(ModbusTransportFactory.getTransport(mDevice));
            if (!connect()) {
                mIntent.setAction(ACTION_CANCEL);
                sendBroadcast(mIntent);
            } else {
                Intent connectionActiveIntent = new Intent(ACTION_CONNECTION_ACTIVE);
                sendBroadcast(connectionActiveIntent);
            }
        }).start();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new DeviceService.LocalBinder();
    }

    public class LocalBinder extends Binder {
        public DeviceService getServiceInstance() {
            return DeviceService.this;
        }
    }

    @Override
    public void registerClient(Callback activity) {
        this.mActivity = activity;
    }

    public boolean connect() {
        if (modbus.connect()) {
            return true;
        } else {
            Log.w(TAG, "Unable to connect");
            mIntent.setAction(ACTION_UNABLE_CONNECT);
            sendBroadcast(mIntent);
            return false;
        }
    }

    public void restartConnection() {
        Log.e(TAG, "Restart connection " + Thread.currentThread().getId());
        new Thread(() -> {
            boolean isConnected = false;
            while (!isConnected) {
                modbus.disconnect();
                mIntent.setAction(ACTION_RECONNECT);
                sendBroadcast(mIntent);
                isConnected = connect();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            isConnected = false;
            while (!isConnected) {
                modbus.disconnect();
                isConnected = connect();
            }
            Intent connectionActiveIntent = new Intent(ACTION_CONNECTION_ACTIVE);
            sendBroadcast(connectionActiveIntent);
            start();
        }).start();
    }


    @SuppressWarnings("deprecation")
    public void start() {
        Log.e(TAG, "Start");
        Intent intent = new Intent(getApplicationContext(), DeviceCommunicateActivity.class);
        intent.putExtra(DeviceCommunicateActivity.KEY_ACTIVATED, true);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel =
                    new NotificationChannel("ID", "Notification", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(notificationChannel);
            builder = new NotificationCompat.Builder(getApplicationContext(), notificationChannel.getId());
        } else {
            builder = new NotificationCompat.Builder(getApplicationContext());
        }

        builder.setSmallIcon(com.example.kleinikov_sd.exampleapp.R.drawable.ic_launcher_background)
                .setContentTitle(mDevice.getName())
                .setContentText("Executing...")
                .setOngoing(true)
                .setWhen(new Date().getTime())
                .setUsesChronometer(true)
                .setContentIntent(resultPendingIntent);

        Notification notification = builder.build();
        startForeground(1, notification);

        byte[] commandData = new byte[]{ADDRESS, READ_SW};
        command = modbus.getCommand(READ_SW);
        command.execute(modbus, commandData, this);
    }

    public void stop() {
        stopForeground(true);
        if (command != null) {
            command.stop();
        }
        Log.e(TAG, "Stop");
    }

    public Callback getBoundedActivity() {
        return mActivity;
    }


    @Override
    public void onDestroy() {
        stop();
        modbus.disconnect();
        Log.i(TAG, "Destroy service");
        super.onDestroy();
    }
}
