package com.atomtex.feature.domain;

import com.atomtex.feature.command.Command;
import com.atomtex.feature.transport.ModbusTransport;

/**
 * @author stanislav.kleinikov@gmail.com
 */
public abstract class Modbus {

    private ModbusTransport transport;

    Modbus(ModbusTransport transport) {
        this.transport = transport;
    }

    public ModbusTransport getTransport() {
        return transport;
    }

    public void setTransport(ModbusTransport transport) {
        this.transport = transport;
    }

    public abstract boolean connect();

    public abstract boolean sendMessage(ModbusMessage message);

    public abstract Command getCommand(Byte commandId);

    public abstract ModbusMessage receiveMessage();

    public void disconnect() {
        transport.close();
    }

    public boolean isConnected() {
        return transport.isConnected();
    }
}