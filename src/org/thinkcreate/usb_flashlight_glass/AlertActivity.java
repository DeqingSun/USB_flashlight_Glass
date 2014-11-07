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
        mAlertCard.setIcon(R.drawable.ic_warning_150);
        mAlertCard.setText(R.string.device_not_found);
        mAlertCard.setFootnote(R.string.device_not_found_foot_note);
        View card_view=mAlertCard.getView ();

        setContentView(card_view);
    }
}
