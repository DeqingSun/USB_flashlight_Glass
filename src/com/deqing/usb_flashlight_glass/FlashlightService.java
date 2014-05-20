package com.deqing.usb_flashlight_glass;

import android.app.PendingIntent;
import android.app.Service;
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
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.glass.timeline.LiveCard;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Service which controls the Live Card which shows status on the flashlight. The flashlight is toggled on when the
 * live card is created and disabled when it is removed (and this service is subsequently destroyed)
 */
public class FlashlightService extends Service {
    private static final String TAG = FlashlightService.class.getSimpleName();
    private static final String LIVE_CARD_TAG = "flashlight";
    private static final String ACTION_USB_PERMISSION = "com.deqing.usb_flashlight_glass.USB_PERMISSION";
    private FlashlightBinder mFlashlightBinder = new FlashlightBinder();
    private LiveCard mLiveCard;
    private RemoteViews mLiveCardView;
    private UsbManager mUsbManager;
    private UsbDevice mFlashlight;
    private boolean mIsFlashlightOn = false;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.d(TAG, "GOT Permission!");
                        toggle_flashlight();
                    } else {
                        mIsFlashlightOn = false;
                        updateStatus(R.string.connection_failed);
                        Log.e(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };
    private PendingIntent mPermissionIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        mUsbManager = (UsbManager) getSystemService(USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (mLiveCard == null) {
            mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
            mLiveCardView = new RemoteViews(getPackageName(), R.layout.flashlight);
            mLiveCard.setViews(mLiveCardView);
            // Create the required menu activity intent
            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
            // Publish the live card
            mLiveCard.publish(LiveCard.PublishMode.REVEAL);
            // Start the search for already attached devices
            search_usb_device();
        } else {
            mLiveCard.navigate();
        }
        return START_STICKY;
    }

    public void search_usb_device() {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        Log.d(TAG, deviceList.toString());
        Log.d(TAG, Integer.toString(deviceList.size()));
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            Log.d(TAG, String.format("VID %04X PID %04X NAME %s", device.getVendorId(),
                    device.getProductId(), device.getDeviceName()));
            if (device.getVendorId() == 0x4207 && device.getProductId() == 0x20A0) {
                mFlashlight = device;
                break;
            }
        }
        if (mFlashlight == null) {
            mIsFlashlightOn = false;
            Log.w(TAG, "NOT GOT DEVICE!!");
            updateStatus(R.string.device_not_found);
        }
        Log.d(TAG, "GOT DEVICE!");
        if (mUsbManager.hasPermission(mFlashlight)) {
            toggle_flashlight();
        } else {
            mUsbManager.requestPermission(mFlashlight, mPermissionIntent);
        }
    }

    public void toggle_flashlight() {
        if (!mUsbManager.hasPermission(mFlashlight)) {
            Log.e(TAG, "No permission to toggle flashlight");
            updateStatus(R.string.connection_failed);
            return;
        }
        Log.d(TAG, "There are " + mFlashlight.getInterfaceCount() + " interfaces.");
        UsbInterface intf = mFlashlight.getInterface(0);
        Log.d(TAG, "There are " + intf.getEndpointCount() + " endpoints.");
        UsbDeviceConnection usb_connection = mUsbManager.openDevice(mFlashlight);
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
            mIsFlashlightOn = !mIsFlashlightOn;
            Log.d(TAG, "Flashlight toggled");
            updateStatus(R.string.flashlight_enabled);
        } else {
            mIsFlashlightOn = false;
            Log.e(TAG, "Out endpoint not found");
            updateStatus(R.string.connection_failed);
        }
        usb_connection.close();
        usb_connection.releaseInterface(intf);
    }

    private void updateStatus(int statusResId) {
        if (mLiveCardView != null && mLiveCard != null && mLiveCard.isPublished()) {
            mLiveCardView.setTextViewText(R.id.status, getString(statusResId));
            mLiveCard.setViews(mLiveCardView);
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        if (mIsFlashlightOn) {
            toggle_flashlight();
        }
        if (mLiveCard != null && mLiveCard.isPublished()) {
            mLiveCard.unpublish();
            mLiveCard = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return new FlashlightBinder();
    }

    /**
     * Binder giving access whether the flashlight is on
     */
    public class FlashlightBinder extends Binder {
        public FlashlightBinder() {
        }

        public boolean isFlashlightOn() {
            return mIsFlashlightOn;
        }
    }
}
