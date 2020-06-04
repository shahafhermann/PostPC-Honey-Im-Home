package com.ppc.honeyimhome;

import android.app.Application;

public class App extends Application {

    private LocationTracker locationTracker;

    @Override
    public void onCreate() {
        super.onCreate();

        locationTracker = new LocationTracker(this);
    }

    public LocationTracker getLocationTracker() {
        return this.locationTracker;
    }
}
