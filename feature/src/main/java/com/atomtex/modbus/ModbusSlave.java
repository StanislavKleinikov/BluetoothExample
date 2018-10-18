package com.atomtex.modbus;

/**
 * @author stanislav.kleinikov@gmail.com
 */
public class ModbusSlave extends Modbus {

    public ModbusSlave(ModbusTransport transport) {
        super(transport);
    }

    public ModbusTransport getTransport() {
        return super.getTransport();
    }

    public void setTransport(ModbusTransport transport) {
        super.setTransport(transport);
    }

    public boolean connect() {
        return getTransport().connect();
    }

    @Override
    public boolean sendMessage(ModbusMessage message) {
        return getTransport().sendMessage(message.getBuffer());
    }

    @Override
    public ModbusMessage receiveMessage() {
        ModbusMessage message = new ModbusMessage(getTransport().receiveMessage());
        message.setIntegrity(CRC16.checkCRC(message.getBuffer()));
        return message;
    }

    @Override
    public void disconnect() {
        getTransport().close();

    }
}
