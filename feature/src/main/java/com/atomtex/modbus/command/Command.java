package com.atomtex.feature.command;

import android.app.Service;

import com.atomtex.feature.domain.Modbus;

public interface Command {

    void execute(Modbus modbus, byte[] data, Service service);

    void stop();
}
