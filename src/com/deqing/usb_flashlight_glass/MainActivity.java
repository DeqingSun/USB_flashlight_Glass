package com.deqing.usb_flashlight_glass;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.google.android.glass.app.Card;

import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                            Log.d(TAG, "GOT Permission!");
                            toggle_flashlight();
                        }
                    } else {
                        Log.e(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };
    private final BroadcastReceiver mUsbAttachedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null) {
                        Log.d(TAG, "GOT DEVICE!");
                    }
                }
            }
        }
    };
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final Handler handler = new Handler();
    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private UsbDevice target_device = null;
    private Card message_card;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        //IntentFilter attachedFilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        //registerReceiver(mUsbAttachedReceiver, attachedFilter);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        message_card = new Card(this);
        message_card.setText("Trying to connect to usb flashlight");
        message_card.setFootnote("");
        updateView();
        search_usb_device();
    }

    @Override
    protected void onResume() {
        super.onResume();
        count_down_exit();
    }

    void count_down_exit() {
        handler.removeCallbacksAndMessages(null);
        message_card.setFootnote("Will exit in " + 3 + " seconds.");
        updateView();
        int time_limit;
        if (target_device == null || mUsbManager.hasPermission(target_device)) {
            time_limit = 3;
        } else {
            time_limit = 10;
        }
        for (int count = 0; count < time_limit; count++) {
            final int count1 = time_limit - 1 - count;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    message_card.setFootnote("Will exit in " + count1 + " seconds.");
                    updateView();
                    if (count1 == 0) {
                        finish();
                    }
                }
            }, 1000 * (count + 1));
        }
    }

    public boolean search_usb_device() {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        Log.d(TAG, deviceList.toString());
        Log.d(TAG, Integer.toString(deviceList.size()));
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            Log.d(TAG, String.format("VID %04X PID %04X NAME %s", device.getVendorId(),
                    device.getProductId(), device.getDeviceName()));
            if (device.getVendorId() == 0x4207 && device.getProductId() == 0x20A0) {
                target_device = device;
                break;
            }
        }
        if (target_device != null) {
            Log.d(TAG, "GOT DEVICE!");
            message_card.setText("Flashlight found!");
            updateView();
            mUsbManager.requestPermission(target_device, mPermissionIntent);
            return true;
        }
        Log.w(TAG, "NOT GOT DEVICE!!");
        message_card.setText("Not found!");
        updateView();
        return false;
    }

    public void toggle_flashlight() {
        if (mUsbManager.hasPermission(target_device)) {
            UsbDeviceConnection usb_connection;
            Log.d(TAG, "There are " + target_device.getInterfaceCount() + " interfaces.");
            UsbInterface intf = target_device.getInterface(0);
            Log.d(TAG, "There are " + intf.getEndpointCount() + " endpoints.");
            usb_connection = mUsbManager.openDevice(target_device);
            usb_connection.claimInterface(intf, true);
            UsbEndpoint out_endpoint = null;
            for (int i = 0; i < intf.getEndpointCount(); i++) {
                UsbEndpoint endpoint = intf.getEndpoint(i);
                if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                    out_endpoint = endpoint;
                }
            }
            if (out_endpoint != null) {
                byte[] bytes = {(byte) 0x80};
                int TIMEOUT = 0;
                usb_connection.bulkTransfer(out_endpoint, bytes, 1, TIMEOUT);
                message_card.setText("Flashlight Toggled");
                updateView();
            }
        } else {
            message_card.setText("NO USB permission");
            updateView();
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void updateView() {
        View card_view = message_card.getView();
        setContentView(card_view);
    }
}
