package com.atomtex.modbus;

/**
 * @author stanislav.kleinikov@gmail.com
 */
public class ModbusMessage {

    public static final byte[] MESSAGE_07 = new byte[]{0x01, 0x07, (byte) 0xe2, 0x41};

    private byte[] buffer;
    private int length;
    private boolean integrity;

    public ModbusMessage(byte[] buffer) {
        this.buffer = buffer;
        length = buffer.length;
        integrity = true;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
        length = buffer.length;
    }

    public int getLength() {
        return length;
    }

    public boolean isIntegrity() {
        return integrity;
    }

    public void setIntegrity(boolean integrity) {
        this.integrity = integrity;
    }
}
