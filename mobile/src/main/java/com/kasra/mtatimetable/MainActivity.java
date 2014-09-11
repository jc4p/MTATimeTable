package com.kasra.mtatimetable;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.kasra.mtatimetable.models.POI;
import com.kasra.mtatimetable.models.Station;
import com.kasra.mtatimetable.models.TimeTableResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class MainActivity extends Activity {
    private static POI Home = new POI(40.682782, -73.964506);
    private static POI Work = new POI(40.708876, -74.006689);

    private ArrayList<String> fromHomeTimes;
    private ArrayList<String> fromWorkTimes;

    private Timer updateTimer;

    @InjectView(R.id.activity_main_textview) protected TextView mainTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        mainTextView.setText("Loading...");
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                mainTextView.post(new Runnable() {
                    @Override
                    public void run() {
                        startItUp();
                    }
                });
            }
        }, 0, 30000);
        startItUp();
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateTimer.cancel();
    }

    private void startItUp() {
        Log.i("Test", "startItUp");
        if (fromHomeTimes == null || fromWorkTimes == null) {
            loadTimeTable(Station.CLINTON_WASHINGTON);
        } else {
            getUserLocation();
        }
    }

    private SharedPreferences getSharedPrefs() {
        return getSharedPreferences("timeTable", Context.MODE_PRIVATE);
    }

    private void loadTimeTable(String stationId) {
        Set<String> timeTable = getTimeTableFromCache(stationId);
        if (timeTable != null) {
            onTimeTableLoaded(stationId, timeTable);
            return;
        }

        getTimeTableFromNetwork(stationId);
    }

    private void onTimeTableLoaded(String stationId, Set<String> timeTable) {
        Log.i("Test", "onTimeTableLoaded " + stationId);
        if(stationId.equals(Station.CLINTON_WASHINGTON)) {
            fromHomeTimes = new ArrayList<String>(timeTable);
            Collections.sort(fromHomeTimes);
            loadTimeTable(Station.FULTON);
        }
        else {
            fromWorkTimes = new ArrayList<String>(timeTable);
            Collections.sort(fromWorkTimes);
            getUserLocation();
        }
    }

    private void getUserLocation() {
        final LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (lastKnownLocation != null) {
            Log.i("Test", "Calling findNextTrainFrom lastKnownLocation");
            findNextTrainFrom(lastKnownLocation);
            return;
        }

        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                Log.i("Test", "Calling findNextTrainFrom onLocationChanged");
                findNextTrainFrom(location);
                locationManager.removeUpdates(this);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    private void findNextTrainFrom(Location location) {
        float[] distanceToHome = new float[1];
        float[] distanceToWork = new float[1];

        Location.distanceBetween(location.getLatitude(), location.getLongitude(), Home.getLatitude(), Home.getLongitude(), distanceToHome);
        Location.distanceBetween(location.getLatitude(), location.getLongitude(), Work.getLatitude(), Work.getLongitude(), distanceToWork);

        // I don't even want to fucking talk about how stupid this datatype is.
        if (distanceToHome[0] < distanceToWork[0]) {
            // We're closer to home than to work, therefore we should go to work!
            // ^ That makes it sound like I have a really bad work/life balance.
            // #TODO: Fix work/life balance.

            getNextTrainTo(Station.FULTON);
        } else {
            // We're going home!
            getNextTrainTo(Station.CLINTON_WASHINGTON);
        }
    }

    private void getNextTrainTo(String location) {
        Calendar cal = Calendar.getInstance();

        // #TODO: For some reason the timetables start at 03 an go up to 26... so uh... figure out that edge case eventually I guess.
        int thisHour = cal.get(Calendar.HOUR_OF_DAY);
        int thisMinute = cal.get(Calendar.MINUTE);

        if (thisHour < 3) {
            // The timetable has a huge issue where results between 12 AM and 3 AM show up as
            // 24:00 --> 26:59... I don't know why.
            thisHour = 24 + thisHour;
        }

        // #TODO: See if this handles "It's 10:59 right now and the next train leaves at 11:01"
        for(String nextDeparture : (location.equals(Station.FULTON) ? fromHomeTimes : fromWorkTimes)) {
            // shut up, I don't want to do a .split()
            int departureHour = Integer.parseInt(new String(new char[] {nextDeparture.charAt(0), nextDeparture.charAt(1)}));

            if (departureHour < thisHour) {
                Log.i("Test", "Skipping " + nextDeparture + " because " + departureHour + " < " + thisHour);
                continue;
            }

            int departureMinute = Integer.parseInt(new String(new char[] { nextDeparture.charAt(3), nextDeparture.charAt(4)}));

            if (departureMinute < thisMinute) {
                Log.i("Test", "Skipping " + nextDeparture + " because " + departureMinute + " < " + thisMinute);
                continue;
            }

            int minutesUntilTrain;

            if (thisHour == departureHour) {
                minutesUntilTrain = departureMinute - thisMinute;
            } else {
                minutesUntilTrain = 60 - thisMinute + departureMinute;
            }

            if (minutesUntilTrain <= 1) {
                Log.i("Test", "Skipping " + nextDeparture + " because it's too soon");
                continue;
            }

            Log.i("Test", "Going with: " + nextDeparture);

            showResults(minutesUntilTrain);
            break;
        }
    }

    private void showResults(int minutes) {
        mainTextView.setText(minutes + " min");
    }

    private void onTimeTableLoadFailure(String stationId) {
        // #TODO: Nicer error state man
        Crouton.showText(MainActivity.this, "Unable to load time table for " + stationId, Style.ALERT);
    }

    public Set<String> getTimeTableFromCache(String stationId) {
        Set<String> results = getSharedPrefs().getStringSet(stationId, new HashSet<String>());

        if (results.size() != 0)
            return results;

        return null;
    }

    public void getTimeTableFromNetwork(String stationId) {
        // #TODO: This should be a singleton please.
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint("http://mtaapi.herokuapp.com/")
                .build();

        MTAService service = adapter.create(MTAService.class);
        service.getScheduleForStop(Station.CLINTON_WASHINGTON, new OnLoadedFromNetwork(stationId));
    }

    private class OnLoadedFromNetwork implements Callback<TimeTableResult> {
        private String stationId;

        public OnLoadedFromNetwork(String stationId) {
            this.stationId = stationId;
        }

        @Override
        public void success(TimeTableResult apiResult, Response response) {
            if (apiResult.getArrivalTimes() == null || apiResult.getArrivalTimes().size() == 0) {
                onTimeTableLoadFailure(stationId);
            }

            getSharedPrefs().edit()
                    .putStringSet(stationId, apiResult.getArrivalTimes())
                    .apply();

            onTimeTableLoaded(stationId, apiResult.getArrivalTimes());
        }

        @Override
        public void failure(RetrofitError error) {
            onTimeTableLoadFailure(stationId);
        }
    }

}
