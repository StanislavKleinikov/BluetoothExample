package com.example.kleinikov_sd.exampleapp.feature;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.atomtex.modbus.command.Command;
import com.atomtex.modbus.util.BTD3Constant;
import com.atomtex.modbus.util.ByteUtil;
import com.atomtex.modbus.domain.Modbus;
import com.atomtex.modbus.domain.ModbusMessage;
import com.atomtex.modbus.domain.ModbusSlave;
import com.atomtex.modbus.transport.ModbusTransportFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.atomtex.modbus.util.BTD3Constant.*;

import static com.example.kleinikov_sd.exampleapp.feature.MainActivity.TAG;

/**
 * This class is the Service for communication with A device through the Bluetooth.
 * Was designed by using the local service pattern to communicate with the activity
 * which it bounded.
 * Contains methods to create a {@link BluetoothSocket} and to make reconnect with
 * a device in case the signal loss
 *
 * @author kleinikov.stanislav@gmail.com
 */
public class DeviceService extends Service {

    public static final String ACTION_UNABLE_CONNECT = "unableToConnect";
    public static final String ACTION_CONNECTION_ACTIVE = "connectionIsActive";
    public static final String ACTION_RECONNECT = "actionReconnect";
    public static final String ACTION_DISCONNECT = "actionDisconnect";
    public static final String ACTION_CANCEL = "actionCancel";
    // private static final int TIMEOUT = 100;

    private DeviceService.Callbacks mActivity;
    private BluetoothDevice mDevice;
    private ScheduledExecutorService mExecutor;
    private Intent mIntent;
    private Modbus modbus;
    private Command command;

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

    class LocalBinder extends Binder {
        DeviceService getServiceInstance() {
            return DeviceService.this;
        }
    }

    public void registerClient(Activity activity) {
        this.mActivity = (DeviceService.Callbacks) activity;
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

    private void sendData(ModbusMessage message) {
        Log.i(TAG, "Send data: " + (System.currentTimeMillis() - mTime) + Arrays.toString(message.getBuffer()));

        if (!modbus.sendMessage(message)) {
            stop();
            Log.e(TAG, "An error occurred while sending data");
            mIntent.setAction(ACTION_DISCONNECT);
            sendBroadcast(mIntent);
            restartConnection();
        }
        beginListenForData();
        mMessageNumber++;
        mActivity.updateMessageNumber(mMessageNumber, mErrorNumber);
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

    private void beginListenForData() {
        Log.i(TAG, "Start listening " + (System.currentTimeMillis() - mTime));
        ModbusMessage message = modbus.receiveMessage();
        if (message.getBuffer() == null) {
            Log.e(TAG, "Unable to read");
            mErrorNumber++;
            mActivity.updateMessageNumber(mMessageNumber, mErrorNumber);
        } else if (!message.isIntegrity()) {
            Log.e(TAG, "Buffer" + Arrays.toString(message.getBuffer()));
            mErrorNumber++;
            mActivity.updateMessageNumber(mMessageNumber, mErrorNumber);
        }
        Log.i(TAG, "Response time" + " " + (System.currentTimeMillis() - mTime)
                + " Answer text " + ByteUtil.getHexString(message.getBuffer()));
    }

    public void start() {
        Log.e(TAG, "Start");
        Intent intent = new Intent(getApplicationContext(), DeviceCommunicateActivity.class);
        intent.putExtra(DeviceCommunicateActivity.KEY_ACTIVATED, true);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
        intent.putExtra(DeviceCommunicateActivity.KEY_MESSAGE_NUMBER, mMessageNumber);
        intent.putExtra(DeviceCommunicateActivity.KEY_ERROR_NUMBER, mErrorNumber);
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

        builder.setSmallIcon(R.mipmap.ic_launcher)
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

        byte[] commandData = new byte[]{ADDRESS, READ_SW};
        command = modbus.getCommand(READ_SW);
        command.execute(modbus, commandData, this);

//        mExecutor = Executors.newScheduledThreadPool(1);
//        ModbusMessage message = new ModbusMessage(ModbusMessage.MESSAGE_07);
//        mExecutor.scheduleAtFixedRate(() -> sendData(message), 500, TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        Log.e(TAG, "Stop");
        stopForeground(true);
        if (command != null) {
            command.stop();
            command = null;
        }
//        if (mExecutor != null) {
//            mExecutor.shutdownNow();
//            mExecutor = null;
//        }
    }

    public Callbacks getBoundedActivity() {
        return mActivity;
    }

    public interface Callbacks {
        void updateMessageNumber(int messageNumber, int errorNumber);

    }

    @Override
    public void onDestroy() {
        stop();
        modbus.disconnect();
        Log.i(TAG, "Destroy service");
        super.onDestroy();
    }
}
