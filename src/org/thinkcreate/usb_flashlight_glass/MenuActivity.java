package org.thinkcreate.usb_flashlight_glass;

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
import android.view.View;
import android.view.Window;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.view.MenuUtils;
import com.google.android.glass.view.WindowUtils;

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
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        	mFlashlightService=null;
            // Nothing to do here.
        }
    };

    private boolean mFromLiveCardVoice;
    private FlashlightService.FlashlightBinder mFlashlightService;
    private boolean mDoToggle;
    private boolean mDoStop;
    private boolean mDoFinish;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFromLiveCardVoice = getIntent().getBooleanExtra(LiveCard.EXTRA_FROM_LIVECARD_VOICE, false);
        if (mFromLiveCardVoice) {
            getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
        }
        bindService(new Intent(this, FlashlightService.class), mConnection, 0);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mFromLiveCardVoice) {
            openOptionsMenu();
        }
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (isMyMenu(featureId)) {
            getMenuInflater().inflate(R.menu.menu, menu);

            MenuItem stop = menu.findItem(R.id.menu_stop);
            MenuItem toggle = menu.findItem(R.id.menu_toggle);
            if (mFromLiveCardVoice){
                toggle.setTitle(R.string.menu_off_voice_command);
                stop.setTitle(R.string.menu_stop_voice_command);
            }

            if (!isFlashlightOn || (mFlashlightService==null)) {
                if (mFromLiveCardVoice){
                    toggle.setTitle(R.string.menu_on_voice_command);
                    stop.setTitle(R.string.menu_stop_voice_command);
                }else {
                    MenuUtils.setDescription(stop, R.string.menu_stop_off_description);
                    toggle.setTitle(R.string.menu_on);
                    stop.setTitle(R.string.menu_stop);
                    toggle.setIcon(R.drawable.ic_flashlight_50_on);
                }
            } else {
                if (mFromLiveCardVoice){
                    toggle.setTitle(R.string.menu_off_voice_command);
                    stop.setTitle(R.string.menu_stop_voice_command);
                }else {
                    MenuUtils.setDescription(stop, R.string.menu_stop_off_description);
                    toggle.setTitle(R.string.menu_off);
                    stop.setTitle(R.string.menu_stop);
                    toggle.setIcon(R.drawable.ic_flashlight_50_off);
                }
            }
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if (isMyMenu(featureId)) {
            // Don't reopen menu once we are finishing. This is necessary
            // since voice menus reopen themselves while in focus.
            return !mDoFinish;
        }
        return super.onPreparePanel(featureId, view, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (isMyMenu(featureId)) {
            switch (item.getItemId()) {
            case R.id.menu_stop:
                mDoStop=true;
                return true;
            case R.id.menu_toggle:
                mDoToggle=true;
                return true;
            }
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onPanelClosed(int featureId, Menu menu) {
        super.onPanelClosed(featureId, menu);
        if (isMyMenu(featureId)) {
            // When the menu panel closes, either an item is selected from the menu or the
            // menu is dismissed by swiping down. Either way, we end the activity.
            mDoFinish = true;
            performActionsIfConnected();
        }
    }

    private void performActionsIfConnected() {
        if (mFlashlightService != null) {
            if (mDoToggle) {
                mDoToggle = false;
                if (mFlashlightService!=null){
                    mFlashlightService.setToggleFlashlight();
                }
                //((FlashlightService.FlashlightBinder) service).isFlashlightOn();
            }
            if (mDoStop){
                mDoStop = false;
                // Stop the service at the end of the message queue for proper options menu
                // animation. This is only needed when starting a new Activity or stopping a Service
                // that published a LiveCard.
                post(new Runnable() {
                    @Override
                    public void run() {
                        stopService(new Intent(MenuActivity.this, FlashlightService.class));
                    }
                });
            }
            if (mDoFinish) {
                mFlashlightService = null;
                unbindService(mConnection);
                finish();
            }
        }
    }

    /**
     * Posts a {@link Runnable} at the end of the message loop, overridable for testing.
     */
    protected void post(Runnable runnable) {
        mHandler.post(runnable);
    }

    private boolean isMyMenu(int featureId) {
        return featureId == Window.FEATURE_OPTIONS_PANEL ||
                featureId == WindowUtils.FEATURE_VOICE_COMMANDS;
    }
}
