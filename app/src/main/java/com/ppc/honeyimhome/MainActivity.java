package com.ppc.honeyimhome;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String PERMISSION_REQ_MSG
            = "Thip app can't do much without location permissions...";

    private App app;
    private LocationTracker locationTracker;
    private MessageManager messageManager;
    private Activity mainActivity = this;
    BroadcastReceiver locationBroadcastReceiver;
    BroadcastReceiver smsBroadcastReceiver;

    private Button trackButton;
    private Button setHomeButton;
    private Button clearHomeButton;
    private TextView curLatitudeContent;
    private TextView curLongitudeContent;
    private TextView accuracyContent;
    private TextView homeLatitudeContent;
    private TextView homeLongitudeContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = (App) getApplicationContext();
        locationTracker = app.getLocationTracker();
        messageManager = app.getMessageManager();

        setViews();
        setLocationBroadcastReceiver();
        setSmsBroadcastReceiver();
    }

    private void setViews() {
        trackButton = findViewById(R.id.trackButton);
        trackButton.setOnClickListener(new OnTrackButtonClick());

        setHomeButton = findViewById(R.id.setButton);
        setHomeButton.setOnClickListener(new OnSetHomeButtonClick());

        clearHomeButton = findViewById(R.id.clearButton);
        clearHomeButton.setOnClickListener(new OnClearHomeButtonClick());

        curLatitudeContent = findViewById(R.id.curLatitudeContent);
        curLongitudeContent = findViewById(R.id.curLongitudeContent);
        accuracyContent = findViewById(R.id.accuracyContent);
        homeLatitudeContent = findViewById(R.id.homeLatitudeContent);
        homeLongitudeContent = findViewById(R.id.homeLongitudeContent);
    }

    /**
     * Set a location broadcast receiver
     */
    private void setLocationBroadcastReceiver() {
        locationBroadcastReceiver = new locationBroadcastReceiver();
        IntentFilter filter = new IntentFilter(LocationTracker.LOCATION_CHANGED_ACTION);
        filter.addAction(LocationTracker.STOPPED_TRACKING_ACTION);
        filter.addAction(LocationTracker.SET_HOME_ACTION);
        filter.addAction(LocationTracker.CLEAR_HOME_ACTION);
        this.registerReceiver(locationBroadcastReceiver, filter);
    }

    /**
     * A location broadcast receiver
     */
    private class locationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action == null){
                return;
            }

            switch (action) {
                case LocationTracker.LOCATION_CHANGED_ACTION:
                    toggleTrackButton(true);
                    updateLocationInfoLabels();
                    if (locationTracker.getCurrentLocation().getAccuracy() < 50) {
                        toggleSetHomeButton(true);
                    }
                    toggleLocationInfoLabels(true);
                    break;
                case LocationTracker.STOPPED_TRACKING_ACTION:
                    toggleTrackButton(false);
                    toggleSetHomeButton(false);
                    toggleLocationInfoLabels(false);
                    break;
                case LocationTracker.SET_HOME_ACTION:
                    toggleClearButton(true);
                    updateHomeLocationLabels(false);
                    break;
                case LocationTracker.CLEAR_HOME_ACTION:
                    toggleClearButton(false);
                    updateHomeLocationLabels(true);
                    break;
            }
        }
    }

    /**
     * Set a SMS broadcast receiver
     */
    private void setSmsBroadcastReceiver() {
        smsBroadcastReceiver = new LocalSendSmsBroadcastReceiver();
        IntentFilter filter = new IntentFilter(MessageManager.SEND_SMS_ACTION);
        this.registerReceiver(smsBroadcastReceiver, filter);
    }

    /**
     * A SMS broadcast receiver
     */
    private class LocalSendSmsBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityCompat
                    .checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                    PackageManager.PERMISSION_GRANTED) {

                String phone = intent.getStringExtra(MessageManager.PHONE_NUMBER_KEY);
                String content = intent.getStringExtra(MessageManager.SMS_CONTENT_KEY);
                if (phone == null || phone.isEmpty() || content == null || content.isEmpty()) {
                    Log.e(MessageManager.ERROR_TAG, "Phone or content invalid");
                }

                messageManager.sendText(intent);

                messageManager.createNotificationChannel(context, intent);

                Intent intentToOpen = new Intent(app, MainActivity.class);
                intentToOpen
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent
                        .getActivity(app, 0, intentToOpen, 0);

                NotificationCompat.Builder builder = new NotificationCompat
                        .Builder(app, MessageManager.NTFC_CHANNEL_ID)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle("My notification")
                        .setContentText("Hello World!")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        // Set the intent that will fire when the user taps the notification
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(app);

                // notificationId is a unique int for each notification that you must define
                notificationManager.notify(MessageManager.NTFC_ID, builder.build());

            } else {  // No permission
                Log.e(MessageManager.ERROR_TAG, "No SMS permission granted");
            }
        }
    }

    /**
     * Changes the track button appearance.
     * @param startTracking indicates if "Start Tracking" or "Stop Tracking" was clicked.
     */
    private void toggleTrackButton(boolean startTracking) {
        if (startTracking) {
            trackButton.setText(R.string.stop_tracking);
            trackButton.setBackgroundColor(getResources()
                    .getColor(R.color.stop_tracking_button));
            trackButton.setTextColor(getResources()
                    .getColor(R.color.stop_tracking_button_text));
        } else {
            trackButton.setText(R.string.start_tracking);
            trackButton.setBackgroundColor(getResources()
                    .getColor(R.color.start_tracking_button));
            trackButton.setTextColor(getResources()
                    .getColor(R.color.start_tracking_button_text));
        }
    }

    /**
     * Toggle the clear home location button
     * @param enable Indicates if it should be enabled or not
     */
    private void toggleClearButton(boolean enable) {
        if (enable) {
//            clearHomeButton.setVisibility(View.VISIBLE);
            clearHomeButton.setEnabled(true);
        } else {
//            clearHomeButton.setVisibility(View.INVISIBLE);
            clearHomeButton.setEnabled(false);
        }
    }

    /**
     * Toggle the clear home location button
     * @param enable Indicates if it should be enabled or not
     */
    private void toggleSetHomeButton(boolean enable) {
        if (enable) {
//            setHomeButton.setVisibility(View.VISIBLE);
            setHomeButton.setEnabled(true);
        } else {
//            setHomeButton.setVisibility(View.INVISIBLE);
            setHomeButton.setEnabled(false);
        }
    }

    /**
     * Update UI for current location
     */
    private void updateLocationInfoLabels() {
        LocationInfo curLocation = locationTracker.getCurrentLocation();
        curLatitudeContent.setText(String.valueOf(curLocation.getLatitude()));
        curLongitudeContent.setText(String.valueOf(curLocation.getLongitude()));
        accuracyContent.setText(String.valueOf(curLocation.getAccuracy()));
    }

    /**
     * Update UI for current location
     */
    private void toggleLocationInfoLabels(boolean show) {
        TextView title = findViewById(R.id.curLocationTitle);
        View divider = findViewById(R.id.currentTitleDivider);
        TextView latTitle = findViewById(R.id.curLatitudeTitle);
        TextView longTitle = findViewById(R.id.curLongitudeTitle);
        TextView accuracyTitle = findViewById(R.id.accuracyTitle);
        if (show) {
            setHomeButton.setVisibility(View.VISIBLE);
            clearHomeButton.setVisibility(View.VISIBLE);

            title.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);
            latTitle.setVisibility(View.VISIBLE);
            longTitle.setVisibility(View.VISIBLE);
            accuracyTitle.setVisibility(View.VISIBLE);
            curLatitudeContent.setVisibility(View.VISIBLE);
            curLongitudeContent.setVisibility(View.VISIBLE);
            accuracyContent.setVisibility(View.VISIBLE);
        } else {
            setHomeButton.setVisibility(View.INVISIBLE);
            clearHomeButton.setVisibility(View.INVISIBLE);

            title.setVisibility(View.INVISIBLE);
            divider.setVisibility(View.INVISIBLE);
            latTitle.setVisibility(View.INVISIBLE);
            longTitle.setVisibility(View.INVISIBLE);
            accuracyTitle.setVisibility(View.INVISIBLE);
            curLatitudeContent.setVisibility(View.INVISIBLE);
            curLongitudeContent.setVisibility(View.INVISIBLE);
            accuracyContent.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Update UI for home location
     */
    private void updateHomeLocationLabels(boolean reset) {
        if (reset) {
            homeLatitudeContent.setText(app.getResources().getString(R.string.info_default));
            homeLongitudeContent.setText(app.getResources().getString(R.string.info_default));
        } else {
            LocationInfo homeLocation = locationTracker.getHomeLocation();
            homeLatitudeContent.setText(String.valueOf(homeLocation.getLatitude()));
            homeLongitudeContent.setText(String.valueOf(homeLocation.getLongitude()));
        }
    }

    /**
     * Listener for the Tracking button click action
     */
    private class OnTrackButtonClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (locationTracker.isTracking()) {  // Turn off
                locationTracker.stopTracking();
            } else {  // It's turned off so attempt turning on
                if (locationTracker.hasLocationPermission()) {
                    locationTracker.startTracking();
                } else {
                    ActivityCompat.requestPermissions(
                            mainActivity,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LocationTracker.REQUEST_CODE_PERMISSION_FINE_LOCATION);
                }
            }

        }
    }

    /**
     * Listener for the ClearHomeLocation button click action
     */
    private class OnClearHomeButtonClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            locationTracker.clearHome();
        }
    }

    /**
     * Listener for the ClearHomeLocation button click action
     */
    private class OnSetHomeButtonClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            locationTracker.setHomeLocation();
        }
    }

    /**
     * What should we do on receiving an answer to the permission request
     * @param requestCode The request code
     * @param permissions The permissions requested
     * @param grantResults The results, indices correspond to 'permissions'
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        // we know we asked for only 1 permission, so we will surely get exactly 1 result
        // (grantResults.size == 1)
        // depending on your use case, if you get only SOME of your permissions
        // (but not all of them), you can act accordingly

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) { // cool
            if (requestCode == LocationTracker.REQUEST_CODE_PERMISSION_FINE_LOCATION) {
                locationTracker.startTracking();
            } else if (requestCode == MessageManager.REQUEST_CODE_PERMISSION_SEND_TEXT) {

            }

        } else {
            // the user has denied our request! =-O
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show a UI to explain the necessity of the permission
                Toast toast = Toast.makeText(app, PERMISSION_REQ_MSG, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (locationTracker.isTracking()) {
            locationTracker.stopTracking();
        }
        unregisterReceiver(locationBroadcastReceiver);
        unregisterReceiver(smsBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Resume MessageManager
        messageManager.retrieveData();

        // Resume LocationTracker
        if (locationTracker.isTracking()) {
            locationTracker.startTracking();
        } else {
            toggleLocationInfoLabels(false);
        }
        updateLocationInfoLabels();
        LocationInfo home = locationTracker.retrieveData();
        if (home == null) {
            toggleClearButton(false);
        } else {
            toggleClearButton(true);
            updateHomeLocationLabels(false);
        }
        if (locationTracker.getCurrentLocation().getAccuracy() < 50) {
            toggleSetHomeButton(true);
        } else {
            toggleSetHomeButton(false);
        }
    }
}
