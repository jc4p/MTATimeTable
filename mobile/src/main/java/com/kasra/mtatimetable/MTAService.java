package com.kasra.mtatimetable;

import com.kasra.mtatimetable.models.TimeTableResult;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Query;

/**
 * Retrofit connectors for https://github.com/mimouncadosch/MTA-API
 */
public interface MTAService {
    @GET("/api")
    void getScheduleForStop(@Query("id") String stationId, Callback<TimeTableResult> callback);
}
