package com.kasra.mtatimetable;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.TextView;

import com.kasra.mtatimetable.shared.MTAIntentService;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends Activity {
    // Logic
    private Timer updateTimer;
    private ResponseReceiver mReceiver;
    // UI
    @InjectView(R.id.activity_main_textview) protected TextView mainTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        mainTextView.setText("Loading...");

        mReceiver = new ResponseReceiver();
        IntentFilter intentFilter = new IntentFilter(MTAIntentService.BROADCAST_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                startItUp();
            }
        }, 0, 30000);
        startItUp();
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateTimer.cancel();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    private void startItUp() {
        Intent serviceIntent = new Intent(this, MTAIntentService.class);
        startService(serviceIntent);
    }

    private void onFailure(String error) {
        mainTextView.setText(error);
    }

    private void onSuccess(String destination, int minutes) {
        mainTextView.setText(minutes + " min\n to " + destination);
    }

    private class ResponseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(MTAIntentService.EXTRA_DATA_FAILURE)) {
                onFailure(intent.getStringExtra(MTAIntentService.EXTRA_DATA_ERROR));
                return;
            }

            onSuccess(intent.getStringExtra(MTAIntentService.EXTRA_DATA_DESTINATION),
                      intent.getIntExtra(MTAIntentService.EXTRA_DATA_TIME, -2));
        }
    }
}
