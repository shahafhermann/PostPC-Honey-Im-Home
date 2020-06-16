package com.ppc.honeyimhome;

import android.app.Application;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class App extends Application {

    public final String NAME = "Honey I'm Home!";

    private LocationTracker locationTracker;
    private MessageManager messageManager;
    private App app;

    private LocalBroadcastManager localBroadcastManager;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;

        locationTracker = new LocationTracker(this, false);
        messageManager = new MessageManager(this);

        setSmsBroadcastReceiver();
        setRepeatedWork();
    }

    /**
     * Get the app's location tracker
     */
    public LocationTracker getLocationTracker() {
        return this.locationTracker;
    }

    /**
     * Get the app's message manager
     */
    public MessageManager getMessageManager() {
        return this.messageManager;
    }

    /**
     * Get the app's localBroadcastManager
     */
    public LocalBroadcastManager getLocalBroadcastManager() { return localBroadcastManager; }

    /**
     * Set a SMS broadcast receiver
     */
    private void setSmsBroadcastReceiver() {
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter filter = new IntentFilter(MessageManager.SEND_SMS_ACTION);
        localBroadcastManager.registerReceiver(new LocalSendSmsBroadcastReceiver(), filter);
    }

    /**
     * Set a repeated work request via WorkManager
     */
    private void setRepeatedWork() {
        // Define a periodic work
        PeriodicWorkRequest locationWorkRequest =
                new PeriodicWorkRequest
                        .Builder(RepeatedLocationWork.class, 15, TimeUnit.MINUTES)
                        .build();
        // Init the work manager with above work
        final Operation locationWork = WorkManager
                .getInstance(this)
                .enqueueUniquePeriodicWork("locationWork",
                        ExistingPeriodicWorkPolicy.REPLACE,
                        locationWorkRequest);
    }
}