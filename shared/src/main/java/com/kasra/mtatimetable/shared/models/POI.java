package com.kasra.mtatimetable.shared.models;

public class POI {
    private double latitude;
    private double longitude;

    public POI(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
