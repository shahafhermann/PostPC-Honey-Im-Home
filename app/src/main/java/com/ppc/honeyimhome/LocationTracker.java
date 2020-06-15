package com.ppc.honeyimhome;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;

import static android.content.Context.LOCATION_SERVICE;

public class LocationTracker {

    private Context context;
    private boolean isTracking;
    private LocationInfo homeLocation;
    private LocationInfo curLocation;
    private LocationManager locationManager;

    private static SharedPreferences prefs;
    private static final String LOCATION_PREFS_NAME = "locationPreference";
    private static final String SP_HOME_LOCATION = "homeLocation";
    private static final String SP_CUR_LOCATION = "curLocation";
    private static final String SP_IS_TRACKING = "isTracking";

    private static final int LOCATION_REFRESH_TIME = 5000;
    private static final int LOCATION_REFRESH_DISTANCE = 10;
    public static final int REQUEST_CODE_PERMISSION_FINE_LOCATION = 111;
    public static final String LOCATION_CHANGED_ACTION = "locationChanged";
    public static final String STOPPED_TRACKING_ACTION = "stoppedTracking";
    public static final String SET_HOME_ACTION = "setHome";
    public static final String CLEAR_HOME_ACTION = "clearHome";
    public static final String GOOD_ACCURACY_ACTION = "goodAccuracy";

    private boolean isWorker;

    /**
     * Location Listener. When the location is changed, get it's info and broadcast.
     */
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            curLocation = new LocationInfo(location);
            if (isWorker) {
                if (location.getAccuracy() < 50) {
                    fireIntent(GOOD_ACCURACY_ACTION);
                }
            } else {
                fireIntent(LOCATION_CHANGED_ACTION);
            }
        }

        /* The following are deprecated */
        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }
    };

    /**
     * Constructor for the tracker
     * @param context The application context
     */
    public LocationTracker(Context context, boolean isWorker) {
        this.context = context;
        this.isTracking = false;
        this.isWorker = isWorker;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        retrieveData();

        locationManager = (LocationManager)
                context.getSystemService(LOCATION_SERVICE);
    }

    public SharedPreferences getPrefs() { return prefs; }

    /**
     * Get the most updated data from SharedPreference
     */
    public void retrieveData() {
        Gson gson = new Gson();
        String json = prefs.getString(SP_HOME_LOCATION, "");
        homeLocation = gson.fromJson(json, LocationInfo.class);

        json = prefs.getString(SP_CUR_LOCATION, "");
        curLocation = gson.fromJson(json, LocationInfo.class);
        if (this.curLocation == null) {
            this.curLocation = new LocationInfo();
        }

        isTracking = prefs.getBoolean(SP_IS_TRACKING, false);
    }

    private void fireIntent(String action) {
        Intent locationIntent = new Intent();
        locationIntent.setAction(action);
        context.sendBroadcast(locationIntent);
    }

    /**
     * Check if the tracker is currently tracking
     * @return
     */
    public boolean isTracking() {return this.isTracking; }

    /**
     * Get the current location
     */
    public LocationInfo getCurrentLocation() { return this.curLocation; }

    /**
     * Get the home location
     */
    public LocationInfo getHomeLocation() { return this.homeLocation; }

    /**
     * Clear the home location
     */
    public void clearHome() {
        homeLocation = null;
        prefs.edit()
                .remove(SP_HOME_LOCATION)
                .apply();
        fireIntent(CLEAR_HOME_ACTION);
    }

    /**
     * Set the home location to the current location
     */
    public void setHomeLocation() {
        this.homeLocation = this.curLocation;
        Gson gson = new Gson();
        prefs.edit()
                .putString(SP_HOME_LOCATION, gson.toJson(homeLocation))
                .apply();
        fireIntent(SET_HOME_ACTION);
    }

    /**
     * Start tracking fine-location
     */
    public void startTracking() {
        this.isTracking = true;
        prefs.edit()
                .putBoolean(SP_IS_TRACKING, isTracking)
                .apply();

        boolean hasLocationPermission = ActivityCompat  // todo WTF
                .checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;

        if (hasLocationPermission()) {
            if (locationManager != null) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        LOCATION_REFRESH_TIME,
                        LOCATION_REFRESH_DISTANCE, locationListener);
                Gson gson = new Gson();
                prefs.edit()
                        .putString(SP_CUR_LOCATION, gson.toJson(curLocation))
                        .apply();
            }
        }
    }

    /**
     * Stop tracking fine-location
     */
    public void stopTracking() {
        this.isTracking = false;
        prefs.edit()
                .putBoolean(SP_IS_TRACKING, isTracking)
                .apply();
        locationManager.removeUpdates(locationListener);
        fireIntent(STOPPED_TRACKING_ACTION);
    }

    /**
     * Check if the user has granted fine-location permissions.
     * @return true if permission was granted, false otherwise
     */
    public boolean hasLocationPermission() {
        return ActivityCompat
                .checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
    }
}