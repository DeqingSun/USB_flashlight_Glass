package org.thinkcreate.usb_flashlight_glass;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.android.glass.widget.CardBuilder;

/**
 * Created by seer on 11/6/2014.
 */
public class AlertActivity extends Activity {
    private CardBuilder mAlertCard;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAlertCard =  new CardBuilder(this, CardBuilder.Layout.ALERT);
        mAlertCard.setIcon(R.drawable.ic_cloud_sad_150);
        mAlertCard.setText(R.string.device_not_found);
        mAlertCard.setFootnote(R.string.device_not_found_foot_note);
        View card_view=mAlertCard.getView ();

        setContentView(card_view);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            openOptionsMenu();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.alertmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_dismiss:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
