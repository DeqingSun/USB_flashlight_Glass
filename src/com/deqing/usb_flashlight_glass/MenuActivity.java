package com.deqing.usb_flashlight_glass;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.glass.view.MenuUtils;

/**
 * Menu Activity used to stop the ongoing service
 */
public class MenuActivity extends Activity {
    private final Handler mHandler = new Handler();
    private boolean isFlashlightOn;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof FlashlightService.FlashlightBinder) {
                isFlashlightOn = ((FlashlightService.FlashlightBinder) service).isFlashlightOn();
                mFlashlightService=((FlashlightService.FlashlightBinder) service);
                openOptionsMenu();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        	mFlashlightService=null;
            // Nothing to do here.
        }
    };
    
    private FlashlightService.FlashlightBinder mFlashlightService;
    private boolean mAttachedToWindow;
    private boolean mOptionsMenuOpen;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindService(new Intent(this, FlashlightService.class), mConnection, 0);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;
        openOptionsMenu();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttachedToWindow = false;
    }

    @Override
    public void openOptionsMenu() {
        if (!mOptionsMenuOpen && mAttachedToWindow) {
            mOptionsMenuOpen = true;
            super.openOptionsMenu();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        MenuItem stop = menu.findItem(R.id.menu_stop);
        MenuItem toggle = menu.findItem(R.id.menu_toggle);
        if (isFlashlightOn || (mFlashlightService==null)) {
            MenuUtils.setDescription(stop, R.string.menu_stop_off_description);
            toggle.setTitle(R.string.menu_off);
            toggle.setIcon(R.drawable.ic_flashlight_50_off);
        } else {
            MenuUtils.setDescription(stop, R.string.menu_stop_description);
            toggle.setTitle(R.string.menu_on);
            toggle.setIcon(R.drawable.ic_flashlight_50_on);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection.
        switch (item.getItemId()) {
            case R.id.menu_stop:
                // Stop the service at the end of the message queue for proper options menu
                // animation. This is only needed when starting a new Activity or stopping a Service
                // that published a LiveCard.
                post(new Runnable() {
                    @Override
                    public void run() {
                        stopService(new Intent(MenuActivity.this, FlashlightService.class));
                    }
                });
                return true;
            case R.id.menu_toggle:
            	if (mFlashlightService!=null){
            		mFlashlightService.setToggleFlashlight();
            	}
            	//((FlashlightService.FlashlightBinder) service).isFlashlightOn();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        mOptionsMenuOpen = false;
        
        unbindService(mConnection);
        
        finish();
    }

    /**
     * Posts a {@link Runnable} at the end of the message loop, overridable for testing.
     */
    protected void post(Runnable runnable) {
        mHandler.post(runnable);
    }
}
