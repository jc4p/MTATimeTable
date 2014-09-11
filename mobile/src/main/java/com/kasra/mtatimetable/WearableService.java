package com.kasra.mtatimetable;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.kasra.mtatimetable.shared.MTAIntentService;

import java.util.concurrent.TimeUnit;

/**
 * This class does *nothing* but wait for a message then immediately tell MTAIntentService to fire.
 *
 * I don't know if this is actually required our not.. we'll look into that later.
 */
public class WearableService extends WearableListenerService {
    private final static String TAG = "WearableService";

    public final static String PATH_REQUEST = "/request";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (!messageEvent.getPath().equals(PATH_REQUEST)) {
            return;
        }

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        ConnectionResult connectionResult = googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess()) {
            Log.e(TAG, "Failed to connect to GoogleApiClient.");
            return;
        }

        Intent serviceIntent = new Intent(this, MTAIntentService.class);
        startService(serviceIntent);
    }

}
