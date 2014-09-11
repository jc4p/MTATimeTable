package com.kasra.mtatimetable.shared.models;

import com.google.gson.annotations.SerializedName;

import java.util.Set;

/**
 * The API call to get the timetable is a bit weird so we can't
 * simply have Retrofit return it into a List of Strings, so we
 * make this super simple abstraction to ease it in.
 */
public class TimeTableResult {
    private class Result {
        @SerializedName("arrivals")
        protected Set<String> arrivalTimes;
    }

    private Result result;

    public Set<String> getArrivalTimes() {
        return result.arrivalTimes;
    }
}
