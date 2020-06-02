package com.ppc.honeyimhome;

public class LocationInfo {

    private double latitude;
    private double longitude;
    private int accuracy;

    LocationInfo(double latitude, double longitude, int accuracy) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
    }
}
