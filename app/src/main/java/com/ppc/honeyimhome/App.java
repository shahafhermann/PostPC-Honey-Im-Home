package com.ppc.honeyimhome;

import android.app.Application;
import android.os.Build;

public class App extends Application {

    private LocationTracker locationTracker;
    private MessageManager messageManager;

    @Override
    public void onCreate() {
        super.onCreate();

        locationTracker = new LocationTracker(this);
        messageManager = new MessageManager(this);
    }

    public LocationTracker getLocationTracker() {
        return this.locationTracker;
    }

    public MessageManager getMessageManager() {
        return this.messageManager;
    }
}
