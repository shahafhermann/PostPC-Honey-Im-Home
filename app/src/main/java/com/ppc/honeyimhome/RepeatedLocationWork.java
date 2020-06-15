package com.ppc.honeyimhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.ListenableWorker;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;

/**
 * Repeated work for location tracking
 */
public class RepeatedLocationWork extends ListenableWorker {

    private CallbackToFutureAdapter.Completer<Result> callback = null;
    private BroadcastReceiver broadcastReceiver;
    private App app;
    private Context context;
    private LocationInfo prevLocation;
    private LocationTracker newTracker;

    private static final String SP_PREV_LOCATION = "prevLocation";
    private static final String smsContent = "Honey I'm Home!";

    public RepeatedLocationWork(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.app = (App) context.getApplicationContext();
        this.context = context;
        newTracker = new LocationTracker(context, true);
    }

    @Override
    public ListenableFuture<Result> startWork() {
        Log.e("start", "worker started");

        ListenableFuture<Result> future = CallbackToFutureAdapter
                .getFuture(new CallbackToFutureAdapter.Resolver<Result>() {
            @Nullable
            @Override
            public Object attachCompleter(@NonNull CallbackToFutureAdapter
                    .Completer<Result> completer) throws Exception {
                callback = completer;
                return null;
            }
        });

        MessageManager messageManager = app.getMessageManager();
        LocationTracker locationTracker = app.getLocationTracker();

        // Check send-SMS and fine-location permissions
        if (!messageManager.hasSmsPermission() || ! locationTracker.hasLocationPermission()) {
            callback.set(Result.success());
            return future;
        }

        // Check if we have a phone number and home location saved in SP
        messageManager.retrieveData();
        locationTracker.retrieveData();
        if (messageManager.getPhone().isEmpty() || messageManager.getPhone() == null ||
            locationTracker.getHomeLocation() == null) {
            callback.set(Result.success());
            return future;
        }

        // Start tracking (use a new location tracker to not interfere with the one that's
        // controlled by the main activity
        newTracker.startTracking();
        placeReceiver();

        return future;
    }

    /**
     * Place a broadcast receiver for GOOD_ACCURACY intent
     */
    private void placeReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onReceiveActions();
            }
        };
        IntentFilter filter = new IntentFilter(LocationTracker.GOOD_ACCURACY_ACTION);
        getApplicationContext().registerReceiver(broadcastReceiver, filter);
    }

    /**
     * Actions to perform on receiving the broadcast
     */
    private void onReceiveActions() {
        newTracker.stopTracking();
        LocationInfo curLocation = newTracker.getCurrentLocation();

        SharedPreferences prefs = newTracker.getPrefs();
        Gson gson = new Gson();
        String json = prefs.getString(SP_PREV_LOCATION, "");
        prevLocation = gson.fromJson(json, LocationInfo.class);
        boolean wasNull = false;
        if (prevLocation == null) {
            prevLocation = new LocationInfo();
            wasNull = true;
        }

        // If we get to the second condition then prevLocation is not null
        if (wasNull || distance(prevLocation, curLocation)[0] < 50) {
            // Save current as previous and return success
            updatePrevLocation(prefs, curLocation);
            callback.set(Result.success());
            return;
        }

        // If we get here then prevLocation is not null
        // AND the distance to curLocation is greater than or equal to 50.
        // Check proximity to home location
        if (distance(curLocation, app.getLocationTracker().getHomeLocation())[0] < 50) {
            Intent smsIntent = new Intent();
            smsIntent.setAction(MessageManager.SEND_SMS_ACTION);
            smsIntent.putExtra(MessageManager.PHONE_NUMBER_KEY,
                               app.getMessageManager().getPhone());
            smsIntent.putExtra(MessageManager.SMS_CONTENT_KEY, smsContent);
            app.getLocalBroadcastManager().sendBroadcast(smsIntent);
        }

        // Either way, update prevLocation as current and return success
        updatePrevLocation(prefs, curLocation);
        callback.set(Result.success());
    }

    /**
     * Calculate the distance between two LocationInfo objects
     * @param first First LocationInfo
     * @param second Second LocationInfo
     * @return
     */
    private float[] distance(LocationInfo first, LocationInfo second) {
        double fLat = first.getLatitude();
        double fLong = first.getLongitude();
        double sLat = second.getLatitude();
        double sLong = second.getLongitude();
        float[] result = new float[1];
        Location.distanceBetween(fLat, fLong, sLat, sLong, result);
        return result;
    }

    /**
     * Update the prevLocation
     * @param prefs The SharedPreference to save to
     * @param curLocation The current location to save
     */
    private void updatePrevLocation(SharedPreferences prefs, LocationInfo curLocation) {
        Gson gson = new Gson();
        prefs.edit()
                .putString(SP_PREV_LOCATION, gson.toJson(curLocation))
                .apply();
    }
}
