package com.atomtex.modbus;

/**
 * @author stanislav.kleinikov@gmail.com
 */
public interface ModbusTransport {

    boolean sendMessage(byte[] message);

    byte[] receiveMessage();

    boolean connect();

    boolean isConnected();

    void close();

}
