package com.atomtex.modbus.transport;

import android.bluetooth.BluetoothDevice;

import com.atomtex.modbus.transport.ModbusRFCOMMTransport;
import com.atomtex.modbus.transport.ModbusTransport;

/**
 * @author stanislav.kleinikov@gmail.com
 */
public class ModbusTransportFactory {

    public static ModbusTransport getTransport(BluetoothDevice device) {
        return ModbusRFCOMMTransport.getInstance(device);
    }
}
