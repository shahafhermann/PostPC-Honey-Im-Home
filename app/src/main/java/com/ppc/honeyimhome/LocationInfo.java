package com.ppc.honeyimhome;

import android.location.Location;

public class LocationInfo {

    private double latitude;
    private double longitude;
    private float accuracy;

    LocationInfo() {
        this.latitude = 0;
        this.longitude = 0;
        this.accuracy = 0;
    }

    LocationInfo(Location location) {
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.accuracy = location.getAccuracy();
    }

    public double getLatitude() { return this.latitude; }

    public double getLongitude() { return this.longitude; }

    public float getAccuracy() { return this.accuracy; }
}
