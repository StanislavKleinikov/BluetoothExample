package com.atomtex.modbus;

public interface ModbusTransport {

    boolean sendMessage(byte[] message);

    byte[] receiveMessage();

    boolean isConnected();

}
