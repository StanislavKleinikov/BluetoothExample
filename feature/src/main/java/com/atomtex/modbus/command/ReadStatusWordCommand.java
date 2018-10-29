package com.atomtex.feature.command;

import android.app.Service;

import com.atomtex.feature.domain.Modbus;

import java.util.concurrent.ScheduledExecutorService;

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
