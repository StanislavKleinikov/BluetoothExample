package com.atomtex.modbus.service;

import android.app.Service;
import android.os.Binder;

import com.atomtex.modbus.activity.Callback;

public abstract class LocalService extends Service {

    public abstract Callback getBoundedActivity();

    public abstract void registerClient(Callback activity);

    public abstract void start();

    public abstract void stop();

}
