package com.atomtex.modbus.command;

import android.app.Service;

import com.atomtex.modbus.domain.Modbus;
import com.atomtex.modbus.service.LocalService;

public interface Command {

    void execute(Modbus modbus, byte[] data, LocalService service);

    void stop();
}
