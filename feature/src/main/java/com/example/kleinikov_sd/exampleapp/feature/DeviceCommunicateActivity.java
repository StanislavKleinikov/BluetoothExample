package com.example.kleinikov_sd.exampleapp.feature;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import static com.example.kleinikov_sd.exampleapp.feature.MainActivity.TAG;

/**
 * @author kleinikov.stanislav@gmail.com
 */
public class DeviceCommunicateActivity extends AppCompatActivity implements DeviceService.Callbacks {

    public static final String KEY_RESPONSE_TEXT = "responseText";
    public static final String KEY_MESSAGE_NUMBER = "messageNumber";
    public static final String KEY_ERROR_NUMBER = "errorNumber";
    public static final String KEY_ACTIVATED = "activated";
    public static final String KEY_SERVICE_INTENT = "serviceIntent";
    public static final String KEY_DEVICE = "device";
    public static final String KEY_TOGGLE_CLICKABLE = "clickable";

    private TextView deviceNameText;
    private TextView responseText;
    private TextView messageNumberView;
    private TextView errorNumberView;
    private Button changeDeviceButton;
    private ToggleButton toggleButton;
    private BluetoothDevice mDevice;
    private BroadcastReceiver mReceiver;

    private DeviceService mService;
    private Intent serviceIntent;
    private ServiceConnection mConnection;
    private ProgressDialog dialog;

    private int mMessageNumber;
    private int mErrorNumber;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communicate_device);

        dialog = new ProgressDialog(DeviceCommunicateActivity.this);
        dialog.setTitle("Connecting to device");
        dialog.setMessage("Please wait..");
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);

        deviceNameText = findViewById(R.id.device_name);
        responseText = findViewById(R.id.response_text);
        messageNumberView = findViewById(R.id.message_number);
        errorNumberView = findViewById(R.id.error_number);
        changeDeviceButton = findViewById(R.id.change_device);
        changeDeviceButton.setOnClickListener(v -> {
            cancel(getString(R.string.toast_change_device));
        });
        toggleButton = findViewById(R.id.toggle_button);
        toggleButton.setOnClickListener((v) -> {
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
            toggleButton.setClickable(savedInstanceState.getBoolean(KEY_TOGGLE_CLICKABLE));
            serviceIntent = savedInstanceState.getParcelable(KEY_SERVICE_INTENT);
            mDevice = savedInstanceState.getParcelable(KEY_DEVICE);
        } else {
            mDevice = getIntent().getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            toggleButton.setChecked(getIntent().getBooleanExtra(KEY_ACTIVATED, false));
            mMessageNumber = getIntent().getIntExtra(KEY_MESSAGE_NUMBER, 0);
            mErrorNumber = getIntent().getIntExtra(KEY_ERROR_NUMBER, 0);
        }

        messageNumberView.setText(String.valueOf(mMessageNumber));
        errorNumberView.setText(String.valueOf(mErrorNumber));
        deviceNameText.setText(mDevice.getName());
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                DeviceService.LocalBinder binder = (DeviceService.LocalBinder) service;
                mService = binder.getServiceInstance(); //Get instance of your service!
                mService.registerClient(DeviceCommunicateActivity.this); //Activity register in the service as client for callbacks!
                Log.i(TAG, "Service connected ");
            }

            @Override
            public void onServiceDisconnected(ComponentName arg) {
                Log.i(TAG, "service disconnected ");
            }
        };
        IntentFilter filter = new IntentFilter(DeviceService.ACTION_UNABLE_CONNECT);
        filter.addAction(DeviceService.ACTION_CONNECTION_ACTIVE);
        filter.addAction(DeviceService.ACTION_RECONNECT);
        filter.addAction(DeviceService.ACTION_DISCONNECT);
        filter.addAction(DeviceService.ACTION_CANCEL);
        mReceiver = new ConnectionBroadCastReceiver();
        registerReceiver(mReceiver, filter);

        if (serviceIntent == null) {
            dialog.show();
            serviceIntent = new Intent(this, DeviceService.class);
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
        outState.putBoolean(KEY_TOGGLE_CLICKABLE, toggleButton.isClickable());
        outState.putParcelable(KEY_SERVICE_INTENT, serviceIntent);
        outState.putParcelable(KEY_DEVICE, mDevice);
    }

    private void cancel(String message) {
        Log.e(TAG, "cancel");
        setResult(RESULT_CANCELED);
        stopService(serviceIntent);
        getIntent().putExtra(MainActivity.EXTRA_MESSAGE, message);
        finish();
    }

    private void makeToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        mService.onDestroy();
        setResult(RESULT_OK);
        super.onBackPressed();
    }

    @Override
    public void updateMessageNumber(int messageNumber, int errorNumber) {
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
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private class ConnectionBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DeviceService.ACTION_CONNECTION_ACTIVE.equals(action)) {
                responseText.setText(getString(R.string.status_connected));
                makeToast(getString(R.string.toast_connection_active));
                toggleButton.setClickable(true);
                if (dialog.isShowing()) {
                    dialog.cancel();
                }
            } else if (DeviceService.ACTION_UNABLE_CONNECT.equals(action)) {
                responseText.setText(getString(R.string.status_unable_connect));
                makeToast(getString(R.string.toast_connection_failed));
                toggleButton.setClickable(false);
            } else if (DeviceService.ACTION_RECONNECT.equals(action)) {
                responseText.setText(getString(R.string.status_reconnect));
                makeToast(getString(R.string.toast_reconnection));
                toggleButton.setClickable(false);
            } else if (DeviceService.ACTION_DISCONNECT.equals(action)) {
                responseText.setText(getString(R.string.status_disconnect));
                makeToast(getString(R.string.toast_connection_failed));
                toggleButton.setClickable(false);
            } else if (DeviceService.ACTION_CANCEL.equals(action)) {
                cancel(getString(R.string.status_unable_connect));
            }
        }
    }
}
