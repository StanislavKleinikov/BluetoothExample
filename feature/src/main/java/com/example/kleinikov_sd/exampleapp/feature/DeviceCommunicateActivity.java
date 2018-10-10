package com.example.kleinikov_sd.exampleapp.feature;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class DeviceCommunicateActivity extends AppCompatActivity implements ConnectionDeviceService.Callbacks {

    private static final String KEY_RESPONSE_TEXT = "responseText";
    private static final String KEY_MESSAGE_NUMBER = "messageNumber";
    private static final String KEY_ERROR_NUMBER = "errorNumber";
    private static final String KEY_ACTIVATED = "activated";
    private static final String KEY_SERVICE_INTENT = "serviceIntent";
    private static final String KEY_DEVICE = "device";
    public static final String TAG = "myTag";

    private TextView deviceNameText;
    private TextView responseText;
    private TextView messageNumberView;
    private TextView errorNumberView;
    private ToggleButton toggleButton;
    private BluetoothDevice mDevice;

    private ConnectionDeviceService mService;
    private Intent serviceIntent;
    private ServiceConnection mConnection;

    private int mMessageNumber;
    private int mErrorNumber;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communicate_device);

        deviceNameText = findViewById(R.id.device_name);
        responseText = findViewById(R.id.response_text);
        messageNumberView = findViewById(R.id.message_number);
        errorNumberView = findViewById(R.id.error_number);
        toggleButton = findViewById(R.id.toggle_button);
        toggleButton.setOnClickListener(v -> {
            if (toggleButton.isChecked()) {
                mService.start();
            } else {
                mService.stop();
            }
        });

        if (savedInstanceState != null) {
            responseText.setText(savedInstanceState.getCharSequence(KEY_RESPONSE_TEXT));
            mMessageNumber = savedInstanceState.getInt(KEY_MESSAGE_NUMBER);
            mErrorNumber = savedInstanceState.getInt(KEY_ERROR_NUMBER);
            toggleButton.setChecked(savedInstanceState.getBoolean(KEY_ACTIVATED));
            serviceIntent = savedInstanceState.getParcelable(KEY_SERVICE_INTENT);
            mDevice = savedInstanceState.getParcelable(KEY_DEVICE);
            Log.e(TAG, "mDevice " + (mDevice==null));
        } else {
            mDevice = getIntent().getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        }

        messageNumberView.setText(String.valueOf(mMessageNumber));
        errorNumberView.setText(String.valueOf(mErrorNumber));
        deviceNameText.setText(mDevice.getName());
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                ConnectionDeviceService.LocalBinder binder = (ConnectionDeviceService.LocalBinder) service;
                mService = binder.getServiceInstance(); //Get instance of your service!
                mService.registerClient(DeviceCommunicateActivity.this); //Activity register in the service as client for callbacks!
            }

            @Override
            public void onServiceDisconnected(ComponentName arg) {
                Toast.makeText(DeviceCommunicateActivity.this, "Service has been disconnected", Toast.LENGTH_SHORT).show();
            }
        };

        if (serviceIntent == null) {
            serviceIntent = new Intent(this, ConnectionDeviceService.class);
            serviceIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
            startService(serviceIntent);
        }
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE); //Binding to the service!
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(KEY_RESPONSE_TEXT, responseText.getText());
        outState.putInt(KEY_MESSAGE_NUMBER, mMessageNumber);
        outState.putInt(KEY_ERROR_NUMBER, mErrorNumber);
        outState.putBoolean(KEY_ACTIVATED, toggleButton.isChecked());
        outState.putParcelable(KEY_SERVICE_INTENT, serviceIntent);
        outState.putParcelable(KEY_DEVICE, mDevice);
    }

    @Override
    public void onBackPressed() {
        stopService(serviceIntent);
        super.onBackPressed();
    }

    @Override
    public void updateClient(int messageNumber, int errorNumber) {
        runOnUiThread(() -> {
            mMessageNumber = messageNumber;
            mErrorNumber = errorNumber;
            messageNumberView.setText(String.valueOf(messageNumber));
            errorNumberView.setText(String.valueOf(errorNumber));
        });
    }

    @Override
    protected void onDestroy() {
        unbindService(mConnection);
        super.onDestroy();
    }
}
