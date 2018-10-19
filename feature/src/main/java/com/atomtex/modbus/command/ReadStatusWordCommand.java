package com.atomtex.modbus.command;

import android.app.Service;

import com.atomtex.modbus.domain.Modbus;
import com.atomtex.modbus.domain.ModbusMessage;
import com.atomtex.modbus.util.ByteUtil;
import com.example.kleinikov_sd.exampleapp.feature.DeviceService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReadStatusWordCommand implements Command {

    private static final int TIMEOUT = 100;
    private ScheduledExecutorService executor;
    private Service service;
    private int messageNumber;
    private int errorNumber;


    @Override
    public void execute(Modbus modbus, byte[] data, Service service) {
    /*    byte[] messageBytes = ByteUtil.getMessageWithCRC16(data);
        ModbusMessage message = new ModbusMessage(messageBytes);

        DeviceService deviceService = (DeviceService) service;

        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> {
            if (!modbus.sendMessage(message)) {
                deviceService.stop();
                deviceService.getBoundedActivity().updateMessageNumber(messageNumber, errorNumber);
            }


        }, 500, TIMEOUT, TimeUnit.MILLISECONDS);*/
    }

    @Override
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

}
