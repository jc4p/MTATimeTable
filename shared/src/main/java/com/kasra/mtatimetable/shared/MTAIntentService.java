package com.kasra.mtatimetable.shared;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.kasra.mtatimetable.shared.models.POI;
import com.kasra.mtatimetable.shared.models.Station;
import com.kasra.mtatimetable.shared.models.TimeTableResult;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import retrofit.RestAdapter;

public class MTAIntentService extends IntentService {
    private static POI Home = new POI(40.682782, -73.964506);
    private static POI Work = new POI(40.708876, -74.006689);

    private ArrayList<String> fromHomeTimes;
    private ArrayList<String> fromWorkTimes;

    public static final String BROADCAST_ACTION = "com.kasra.mtatimetable.BROADCAST";
    public static final String EXTRA_DATA_SUCCESS = "com.kasra.mtatimetable.SUCCESS";
    public static final String EXTRA_DATA_FAILURE = "com.kasra.mtatimetable.FAILURE";
    public static final String EXTRA_DATA_ERROR = "com.kasra.mtatimetable.ERROR";
    public static final String EXTRA_DATA_TIME = "com.kasra.mtatimetable.TIME";
    public static final String EXTRA_DATA_DESTINATION = "com.kasra.mtatimetable.DESTINATION";

    public final static String DATA_ITEM_FAILED_PATH = "/data-item-failed";
    public final static String DATA_ITEM_SUCCESS_PATH = "/data-item-success";

    public MTAIntentService() {
        super("MTAIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        fromHomeTimes = new ArrayList<String>(loadTimeTable(Station.CLINTON_WASHINGTON));
        Collections.sort(fromHomeTimes);

        fromWorkTimes = new ArrayList<String>(loadTimeTable(Station.FULTON));
        Collections.sort(fromWorkTimes);

        Location userLocation = getUserLocation();
        if (userLocation == null) {
            fail("LOCATION");
        }

        String destination = getDestination(userLocation);
        int minutes = getTimeUntilNextTrainTo(destination);

        success(destination.equals(Station.FULTON) ? "work" : "home", minutes);
    }

    private void fail(String error) {
        Intent localIntent = new Intent(BROADCAST_ACTION)
                .putExtra(EXTRA_DATA_FAILURE, true)
                .putExtra(EXTRA_DATA_ERROR, error);

        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        // Also send it to any Android Wear devices that are listening
        GoogleApiClient googleApiClient = getApiClient();

        if (googleApiClient == null) {
            return;
        }

        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
        for (Node node : nodes.getNodes()) {
            Wearable.MessageApi
                    .sendMessage(googleApiClient, node.getId(), DATA_ITEM_FAILED_PATH, error.getBytes())
                    .await();
        }
    }

    private void success(String destination, int minutes) {
        Intent localIntent = new Intent(BROADCAST_ACTION)
                .putExtra(EXTRA_DATA_SUCCESS, true)
                .putExtra(EXTRA_DATA_DESTINATION, destination)
                .putExtra(EXTRA_DATA_TIME, minutes);

        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        // Also send it to any Android Wear devices that are listening
        GoogleApiClient googleApiClient = getApiClient();

        if (googleApiClient == null) {
            return;
        }

        String payload = minutes + " min\n to " + destination;

        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
        for (Node node : nodes.getNodes()) {
            Wearable.MessageApi
                    .sendMessage(googleApiClient, node.getId(), DATA_ITEM_SUCCESS_PATH, payload.getBytes())
                    .await();
        }
    }

    private GoogleApiClient getApiClient() {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        ConnectionResult connectionResult = googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess()) {
            return null;
        }

        return googleApiClient;
    }

    private SharedPreferences getSharedPrefs() {
        return getSharedPreferences("timeTable", Context.MODE_PRIVATE);
    }

    private Set<String> loadTimeTable(String stationId) {
        Set<String> timeTable = getTimeTableFromCache(stationId);
        if (timeTable != null) {
            return timeTable;
        }

        return getTimeTableFromNetwork(stationId);
    }

    public Set<String> getTimeTableFromCache(String stationId) {
        Set<String> results = getSharedPrefs().getStringSet(stationId, new HashSet<String>());

        if (results.size() != 0)
            return results;

        return null;
    }

    public Set<String> getTimeTableFromNetwork(String stationId) {
        // #TODO: This should be a singleton please.
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint("http://mtaapi.herokuapp.com/")
                .build();

        MTARESTService service = adapter.create(MTARESTService.class);
        TimeTableResult result = service.getScheduleForStop(stationId);

        if (result == null || result.getArrivalTimes() == null || result.getArrivalTimes().size() == 0)
            return null;

        return result.getArrivalTimes();
    }

    private Location getUserLocation() {
        final LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        // #TODO: See if our previous way of "no location found" works in a Service.
        if (lastKnownLocation != null) {
            return lastKnownLocation;
        }

        return null;
    }

    private String getDestination(Location location) {
        float[] distanceToHome = new float[1];
        float[] distanceToWork = new float[1];

        Location.distanceBetween(location.getLatitude(), location.getLongitude(), Home.getLatitude(), Home.getLongitude(), distanceToHome);
        Location.distanceBetween(location.getLatitude(), location.getLongitude(), Work.getLatitude(), Work.getLongitude(), distanceToWork);

        // I don't even want to fucking talk about how stupid this datatype is.
        if (distanceToHome[0] < distanceToWork[0]) {
            // We're closer to home than to work, therefore we should go to work!
            // ^ That makes it sound like I have a really bad work/life balance.
            // #TODO: Fix work/life balance.

            return Station.FULTON;
        } else {
            // We're going home!
            return Station.CLINTON_WASHINGTON;
        }
    }

    private int getTimeUntilNextTrainTo(String location) {
        Calendar cal = Calendar.getInstance();

        int thisHour = cal.get(Calendar.HOUR_OF_DAY);
        int thisMinute = cal.get(Calendar.MINUTE);

        if (thisHour < 3) {
            // The timetable has a huge issue where results between 12 AM and 3 AM show up as
            // 24:00 --> 26:59... I don't know why.
            thisHour = 24 + thisHour;
        }

        for (String nextDeparture : (location.equals(Station.FULTON) ? fromHomeTimes : fromWorkTimes)) {
            // shut up, I don't want to do a .split()
            int departureHour = Integer.parseInt(new String(new char[]{nextDeparture.charAt(0), nextDeparture.charAt(1)}));

            if (departureHour < thisHour) {
                continue;
            }

            int departureMinute = Integer.parseInt(new String(new char[]{nextDeparture.charAt(3), nextDeparture.charAt(4)}));

            if (departureHour == thisHour && departureMinute < thisMinute) {
                continue;
            }

            int minutesUntilTrain;

            if (thisHour == departureHour) {
                minutesUntilTrain = departureMinute - thisMinute;
            } else {
                minutesUntilTrain = 60 - thisMinute + departureMinute;
            }

            if (minutesUntilTrain <= 1) {
                continue;
            }

            return minutesUntilTrain;
        }

        return -1;
    }
}
