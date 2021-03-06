package org.thinkcreate.usb_flashlight_glass;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.RemoteViews;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.widget.CardBuilder;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Service which controls the Live Card which shows status on the flashlight. The flashlight is toggled on when the
 * live card is created and disabled when it is removed (and this service is subsequently destroyed)
 */
public class FlashlightService extends Service {
    private static final String TAG = FlashlightService.class.getSimpleName();
    private static final String LIVE_CARD_TAG = "flashlight";
    private static final String ACTION_USB_PERMISSION = "org.thinkcreate.usb_flashlight_glass.USB_PERMISSION";
    private LiveCard mLiveCard;
    private RemoteViews mLiveCardView, mLiveCardLoadingView;
    private UsbManager mUsbManager;
    private UsbDevice mFlashlight;
    private boolean mIsFlashlightOn = false;
    private SoundPool mSoundPool;
    private Intent mMenuIntent;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            System.out.println(action);
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.d(TAG, "GOT Permission!");
                        toggle_flashlight(true, true);    //turn on flashlight
                    } else {
                        mIsFlashlightOn = false;
                        updateFlashlightMenu();
                        updateStatus(R.string.connection_failed, R.drawable.ic_flashlight_150_err);
                        Log.e(TAG, "permission denied for device " + device);
                    }
                }
            }else if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) || UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                search_usb_device();
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
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (mLiveCard == null) {
            mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
            mLiveCardView = new RemoteViews(getPackageName(), R.layout.flashlight);
            mLiveCardLoadingView = new RemoteViews(getPackageName(), R.layout.loadinglayout);
            mLiveCard.setViews(mLiveCardLoadingView);
            // Create the required menu activity intent
            mMenuIntent = new Intent(this, MenuActivity.class);
            mMenuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            updateFlashlightMenu();
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
        mFlashlight = null;
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
        updateFlashlightMenu();//update if flashlight is connected
        if (mFlashlight == null) {
            mIsFlashlightOn = false;
            updateFlashlightMenu();
            Log.w(TAG, "NOT GOT DEVICE!!");
            updateStatus(R.string.device_not_found, R.drawable.ic_flashlight_150_err);
            return;
        }
        Log.d(TAG, "GOT DEVICE!");
        if (mUsbManager.hasPermission(mFlashlight)) {
            toggle_flashlight(true, true);    //turn on flashlight
        } else {
            mUsbManager.requestPermission(mFlashlight, mPermissionIntent);
        }
    }

    public void toggle_flashlight() {    //toggle flashlight
        if (mIsFlashlightOn) {
            toggle_flashlight(true, false);
        } else {
            toggle_flashlight(true, true);
        }
    }

    public void toggle_flashlight(boolean cmd_specified, boolean cmd_on) {
        if (mFlashlight == null) {
            updateStatus(R.string.device_not_found, R.drawable.ic_flashlight_150_err);
            return;
        }
        if (!mUsbManager.hasPermission(mFlashlight)) {
            Log.e(TAG, "No permission to toggle flashlight");
            updateStatus(R.string.connection_failed, R.drawable.ic_flashlight_150_err);
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
            int TIMEOUT = 0;
            byte[] bytes = {(byte) 'F', (byte) 'T'};
            if (cmd_specified) {
                if (cmd_on) {
                    bytes[1] = (byte) 'O';
                    mIsFlashlightOn = true;
                    updateFlashlightMenu();
                } else {
                    bytes[1] = (byte) 'F';
                    mIsFlashlightOn = false;
                    updateFlashlightMenu();
                }
            } else {
                mIsFlashlightOn = !mIsFlashlightOn;
                updateFlashlightMenu();
            }
            usb_connection.bulkTransfer(out_endpoint, bytes, 2, TIMEOUT);
            Log.d(TAG, "Flashlight toggled");
            if (mIsFlashlightOn) {
                updateStatus(R.string.flashlight_enabled, R.drawable.ic_flashlight_150_on);
            } else {
                updateStatus(R.string.flashlight_disabled, R.drawable.ic_flashlight_150_off);
            }
            mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
            mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    soundPool.play(sampleId, 1.0f, 1.0f, 0, 0, 1.0f);
                }
            });
            int soundResId = mIsFlashlightOn ? R.raw.sound_don : R.raw.sound_doff;
            mSoundPool.load(this, soundResId, 0);
        } else {
            mIsFlashlightOn = false;
            updateFlashlightMenu();
            Log.e(TAG, "Out endpoint not found");
            updateStatus(R.string.connection_failed, R.drawable.ic_flashlight_150_err);
        }
        usb_connection.close();
        usb_connection.releaseInterface(intf);
    }

    private void updateStatus(int statusResId, int imageResId) {
        if (mLiveCardView != null && mLiveCard != null && mLiveCard.isPublished()) {
            if (statusResId == R.string.device_not_found) {
                mLiveCard.setVoiceActionEnabled(false);
                Intent alertIntent = new Intent(this, AlertActivity.class);
                alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                this.startActivity(alertIntent);
                mLiveCardView.setTextViewText(R.id.status, getString(R.string.device_not_found_state));
                mLiveCardView.setImageViewResource(R.id.imageStatus, imageResId);
                mLiveCard.setViews(mLiveCardView);
            } else {
                mLiveCardView.setTextViewText(R.id.status, getString(statusResId));
                mLiveCardView.setImageViewResource(R.id.imageStatus, imageResId);
                mLiveCard.setViews(mLiveCardView);
                mLiveCard.setVoiceActionEnabled(true);
            }
        }
    }

    private void updateFlashlightMenu(){
        mMenuIntent.putExtra("flashlightIsOn", mIsFlashlightOn);
        mMenuIntent.putExtra("flashlightIsConnected", (mFlashlight != null));
        mLiveCard.setAction(PendingIntent.getActivity(this, 0, mMenuIntent, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        if (mIsFlashlightOn) {
            toggle_flashlight(true, false);    //turn off flashlight
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

        public void setToggleFlashlight() {
            toggle_flashlight();
        }
    }

    private final DialogInterface.OnClickListener mOnClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int button) {
            // TODO: Take action to respond to the tap.
        }
    };


}
