package com.atomtex.modbus.transport;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

/**
 * @author stanislav.kleinikov@gmail.com
 */
public class ModbusRFCOMMTransport implements ModbusTransport, Closeable {

    private BluetoothDevice device;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    private ModbusRFCOMMTransport() {
    }

    private static class ModbusRFCOMMTransportHolder {
        private static final ModbusRFCOMMTransport instance = new ModbusRFCOMMTransport();
    }

    public static ModbusRFCOMMTransport getInstance(BluetoothDevice device) {
        ModbusRFCOMMTransport instance = ModbusRFCOMMTransportHolder.instance;
        instance.device = device;
        return instance;
    }

    @Override
    public boolean connect() {
        try {
            ParcelUuid[] idArray = device.getUuids();
            UUID uuid = UUID.fromString(idArray[0].toString());
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            socket = device.createRfcommSocketToServiceRecord(uuid);
            socket.connect();
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean sendMessage(byte[] message) {
        try {
            outputStream.write(message);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public byte[] receiveMessage() {
        long startTime = System.currentTimeMillis();
        byte[] buffer = new byte[0];
        int currentPosition = 0;
        while ((System.currentTimeMillis() - startTime) < 100) {
            try {
                int bytesAvailable = inputStream.available();
                if (bytesAvailable > 0) {
                    byte[] packetBytes = new byte[bytesAvailable];
                    buffer = Arrays.copyOf(buffer, buffer.length + packetBytes.length);
                    inputStream.read(packetBytes);
                    for (int i = 0; i < bytesAvailable; i++, currentPosition++) {
                        byte b = packetBytes[i];
                        buffer[currentPosition] = b;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return buffer;
    }

    @Override
    public boolean isConnected() {
        if (socket != null) {
            return socket.isConnected();
        } else
            return false;
    }


    public void close() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = null;
        }
        if (outputStream != null) {
            try {
                outputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStream = null;
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
        }
    }

}
