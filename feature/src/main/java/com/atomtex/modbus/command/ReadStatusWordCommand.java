package com.atomtex.modbus.command;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.atomtex.modbus.activity.DeviceCommunicateActivity;
import com.atomtex.modbus.domain.Modbus;
import com.atomtex.modbus.domain.ModbusMessage;
import com.atomtex.modbus.service.LocalService;
import com.atomtex.modbus.util.ByteUtil;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.atomtex.modbus.activity.MainActivity.TAG;
import static com.atomtex.modbus.service.DeviceService.ACTION_DISCONNECT;
import static com.atomtex.modbus.service.DeviceService.ACTION_CONNECTION_ACTIVE;
import static com.atomtex.modbus.service.DeviceService.ACTION_RECONNECT;
import static com.atomtex.modbus.service.DeviceService.ACTION_UNABLE_CONNECT;

public class ReadStatusWordCommand implements Command {

    private static final int TIMEOUT = 100;
    private Modbus modbus;
    private ScheduledExecutorService executor;
    private LocalService service;
    private Bundle bundle;
    private ModbusMessage message;
    private Intent intent;
    private int messageNumber;
    private int errorNumber;
    private long time;


    @Override
    public void execute(Modbus modbus, byte[] data, LocalService service) {
        this.modbus = modbus;
        this.service = service;
        byte[] messageBytes = ByteUtil.getMessageWithCRC16(data);
        message = new ModbusMessage(messageBytes);
        bundle = new Bundle();
        intent = new Intent();
        start();
    }

    public void start() {
        time = System.currentTimeMillis();
        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(this::sendMessage, 500, TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public Bundle getBundle() {
        bundle.putInt(DeviceCommunicateActivity.KEY_MESSAGE_NUMBER, messageNumber);
        bundle.putInt(DeviceCommunicateActivity.KEY_ERROR_NUMBER, errorNumber);
        return bundle;
    }

    private void sendMessage() {
        Log.i(TAG, "Send data: " + (System.currentTimeMillis() - time) + Arrays.toString(message.getBuffer()));
        if (!modbus.sendMessage(message)) {
            Log.e(TAG, "An error occurred while sending data");
            service.stop();
            intent.setAction(ACTION_DISCONNECT);
            service.sendBroadcast(intent);
            restartConnection();
        }
        receiveMessage();
        messageNumber++;
        service.getBoundedActivity().updateUI(getBundle());
    }

    private void receiveMessage() {
        Log.i(TAG, "Start listening " + (System.currentTimeMillis() - time));
        ModbusMessage message = modbus.receiveMessage();
        if (message.getBuffer() == null) {
            Log.e(TAG, "Unable to read");
            errorNumber++;
            service.getBoundedActivity().updateUI(getBundle());
        } else if (!message.isIntegrity()) {
            Log.e(TAG, "Buffer" + Arrays.toString(message.getBuffer()));
            errorNumber++;
            service.getBoundedActivity().updateUI(getBundle());
        }
        Log.i(TAG, "Response time" + " " + (System.currentTimeMillis() - time)
                + " Answer text " + ByteUtil.getHexString(message.getBuffer()));
    }

    public void restartConnection() {
        Log.e(TAG, "Restart connection " + Thread.currentThread().getId());
        new Thread(() -> {
            boolean isConnected = false;
            while (!isConnected) {
                modbus.disconnect();
                intent.setAction(ACTION_RECONNECT);
                service.sendBroadcast(intent);
                isConnected = modbus.connect();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.w(TAG, "Unable to connect");
                intent.setAction(ACTION_UNABLE_CONNECT);
                service.sendBroadcast(intent);
            }
            isConnected = false;
            while (!isConnected) {
                modbus.disconnect();
                isConnected = modbus.connect();
            }
            intent.setAction(ACTION_CONNECTION_ACTIVE);
            service.sendBroadcast(intent);
            start();
        }).start();
    }
}
