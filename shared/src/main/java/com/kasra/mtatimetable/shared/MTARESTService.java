package com.kasra.mtatimetable.shared;

import com.kasra.mtatimetable.shared.models.TimeTableResult;

import retrofit.http.GET;
import retrofit.http.Query;

/**
 * Retrofit connectors for https://github.com/mimouncadosch/MTA-API
 */
public interface MTARESTService {
    @GET("/api")
    TimeTableResult getScheduleForStop(@Query("id") String stationId);
}
