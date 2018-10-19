package com.atomtex.modbus.command;

import android.app.Service;

import com.atomtex.modbus.domain.Modbus;

public interface Command {

    void execute(Modbus modbus, byte[] data, Service service);

    void stop();
}
