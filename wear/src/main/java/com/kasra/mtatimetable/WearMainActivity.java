package com.kasra.mtatimetable;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Timer;
import java.util.TimerTask;

public class WearMainActivity extends Activity implements MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Timer updateTimer;
    private GoogleApiClient mGoogleApiClient;

    private TextView mTextView;

    private final static String TAG = "WearMainActivity";

    public final static String PATH_REQUEST = "/request";
    public final static String DATA_ITEM_FAILED_PATH = "/data-item-failed";
    public final static String DATA_ITEM_SUCCESS_PATH = "/data-item-success";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();

        WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.activity_main_textview);
                mTextView.setText("Loading...");
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (updateTimer != null) {
            updateTimer.cancel();
        }
    }

    private void startItUp() {
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                for (Node node : getConnectedNodesResult.getNodes()) {
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), PATH_REQUEST, null);
                }
            }
        });
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());

        String message = "";
        if (messageEvent.getPath().equals(DATA_ITEM_FAILED_PATH)) {
            message = new String(messageEvent.getData());
        } else if (messageEvent.getPath().equals(DATA_ITEM_SUCCESS_PATH)) {
            message = new String(messageEvent.getData());
        }

        final String finalMessage = message;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mTextView.setText(finalMessage);
            }
        };

        mainHandler.post(runnable);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected to Google Api Service");
        Wearable.MessageApi.addListener(mGoogleApiClient, this);

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
    public void onConnectionSuspended(int i) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mTextView.setText("Unable to connect to device.");
    }
}
