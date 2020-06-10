package com.ppc.honeyimhome;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String LOCATION_PERMISSION_REQ_MSG
            = "Thip app can't do much without location permissions...";
    private static final String SMS_PERMISSION_REQ_MSG
            = "Without SMS permissions your honey will never know that you're home!";

    private App app;
    private LocationTracker locationTracker;
    private MessageManager messageManager;
    private Activity mainActivity = this;
    BroadcastReceiver locationBroadcastReceiver;
    BroadcastReceiver smsBroadcastReceiver;
    LocalBroadcastManager localBroadcastManager;

    private Button smsButton;
    private Button deletePhoneButton;
    private Button trackButton;
    private Button setHomeButton;
    private Button clearHomeButton;
    private TextView curLatitudeContent;
    private TextView curLongitudeContent;
    private TextView accuracyContent;
    private TextView homeLatitudeContent;
    private TextView homeLongitudeContent;

    private String smsContent = "Honey I'm Sending a Test Message!";

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
        deletePhoneButton = findViewById(R.id.deletePhoneButton);
        deletePhoneButton.setOnClickListener(new OnDeletePhoneButtonClick());

        smsButton = findViewById(R.id.setPhoneButton);
        smsButton.setOnClickListener(new OnSmsButtonClick());

        trackButton = findViewById(R.id.trackButton);
        trackButton.setOnClickListener(new OnTrackButtonClick());

        setHomeButton = findViewById(R.id.setHomeButton);
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
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        smsBroadcastReceiver = new LocalSendSmsBroadcastReceiver();
        IntentFilter filter = new IntentFilter(MessageManager.SEND_SMS_ACTION);
        localBroadcastManager.registerReceiver(smsBroadcastReceiver, filter);
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

                // Notification

                messageManager.createNotificationChannel(context);

                Intent intentToOpen = new Intent(app, MainActivity.class);
                intentToOpen
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pendingIntent = PendingIntent
                        .getActivity(app, 0, intentToOpen, 0);

                String ntfcBody = String.format("sending sms to %s: %s", phone, content);
                NotificationCompat.Builder builder = new NotificationCompat
                        .Builder(app, MessageManager.NTFC_CHANNEL_ID)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle(app.NAME)
                        .setContentText(ntfcBody)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        // Make sure it shows the full text
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(ntfcBody))
                        // Set the intent that will fire when the user taps the notification
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(app);

                // notificationId is a unique int for each notification that you must define
                notificationManager.notify(MessageManager.NTFC_ID, builder.build());

                // Actually send
                messageManager.sendText(intent);

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
            trackButton.setBackgroundTintList(ColorStateList
                    .valueOf(getResources().getColor(R.color.stop_tracking_button)));
            trackButton.setTextColor(getResources()
                    .getColor(R.color.stop_tracking_button_text));
        } else {
            trackButton.setText(R.string.start_tracking);
            trackButton.setBackgroundTintList(ColorStateList
                    .valueOf(getResources().getColor(R.color.start_tracking_button)));
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
     * Toggle the clear home location button
     * @param hasPhone Indicates if it should be enabled or not
     */
    private void toggleSmsButtons(boolean hasPhone) {
        if (hasPhone) {
            smsButton.setText(app.getResources().getString(R.string.test_sms));
            deletePhoneButton.setEnabled(true);
        } else {
            smsButton.setText(app.getResources().getString(R.string.set_phone));
            deletePhoneButton.setEnabled(false);
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
     * Show a dialog and request a phone number input, then save it (or delete).
     */
    private void setPhoneNumber() {
        final EditText input = new EditText(MainActivity.this);
        input.setPadding(50, 100, 50, 15);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Set a Phone Number")
                .setView(input)
                .setPositiveButton("Confirm",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Save to SP
                                messageManager.setPhone(input.getText().toString());
                                // Edit the button
                                toggleSmsButtons(true);
                            }
                        })
                .setNegativeButton("Delete",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                // Save to SP
                                messageManager.setPhone("");
                                toggleSmsButtons(false);
                            }
                        })
                .show();
    }

    /**
     * Listener for the Tracking button click action
     */
    private class OnDeletePhoneButtonClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            messageManager.setPhone("");
            toggleSmsButtons(false);
        }
    }

    /**
     * Listener for the Tracking button click action
     */
    private class OnSmsButtonClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            String phone = messageManager.getPhone();
            if ((phone != null) && !phone.isEmpty()) {
                // The phone number is set
                Intent smsIntent = new Intent();
                smsIntent.setAction(MessageManager.SEND_SMS_ACTION);
                smsIntent.putExtra(MessageManager.PHONE_NUMBER_KEY, phone);
                smsIntent.putExtra(MessageManager.SMS_CONTENT_KEY, smsContent);
                localBroadcastManager.sendBroadcast(smsIntent);
            } else {
                if (messageManager.hasSmsPermission()) {
                    // Set the phone number
                    setPhoneNumber();

                } else {
                    ActivityCompat.requestPermissions(
                            mainActivity,
                            new String[]{Manifest.permission.SEND_SMS},
                            MessageManager.REQUEST_CODE_PERMISSION_SEND_TEXT);
                }
            }
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
                // This was a location request
                locationTracker.startTracking();
            } else if (requestCode == MessageManager.REQUEST_CODE_PERMISSION_SEND_TEXT) {
                // This was a SMS request
                setPhoneNumber();
            }
        } else {
            // the user has denied our request! =-O
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show a UI to explain the necessity of the permission
                Toast toast = Toast.makeText(app, LOCATION_PERMISSION_REQ_MSG, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.SEND_SMS)) {
                // Show a UI to explain the necessity of the permission
                Toast toast = Toast.makeText(app, SMS_PERMISSION_REQ_MSG, Toast.LENGTH_LONG);
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
        localBroadcastManager.unregisterReceiver(smsBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Resume MessageManager
        messageManager.retrieveData();
        if (messageManager.getPhone() != null && !messageManager.getPhone().isEmpty()) {
            toggleSmsButtons(true);
        }

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
