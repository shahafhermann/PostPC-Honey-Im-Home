package com.ppc.honeyimhome;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;

public class MessageManager {

    private Context context;
    private SmsManager smsManager;
    private String curPhone;

    private static SharedPreferences prefs;
    private static final String LOCATION_PREFS_NAME = "smsPreference";
    private static final String SP_PHONE = "phoneNumber";

    public static final String NTFC_CHANNEL_NAME = "HoneyImHome!";
    public static final String NTFC_CHANNEL_ID = "smsNtfc";
    public static final int NTFC_ID = 5;

    public static final String ERROR_TAG = "SmsBroadcastReceiver";
    public static final int REQUEST_CODE_PERMISSION_SEND_TEXT = 222;
    public static final String SEND_SMS_ACTION = "POST_PC.ACTION_SEND_SMS";

    public static final String PHONE_NUMBER_KEY = "phoneNumber";
    public static final String SMS_CONTENT_KEY = "smsContent";

    /**
     * Constructor for the tracker
     * @param context The application context
     */
    public MessageManager(Context context) {
        this.context = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        retrieveData();

        smsManager = SmsManager.getDefault();
    }

    /**
     * Get the most updated data from SharedPreference
     */
    public void retrieveData() {
        this.curPhone = prefs.getString(SP_PHONE, "");
    }

    public void sendText(Intent intent) {
        String content = intent.getStringExtra(SMS_CONTENT_KEY);
        PendingIntent sentIntent = PendingIntent
                .getActivity(context, REQUEST_CODE_PERMISSION_SEND_TEXT, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        smsManager.sendTextMessage(curPhone, null, content, sentIntent, null);
    }

    public void createNotificationChannel(Context context, Intent intent) {
        String phone = intent.getStringExtra(PHONE_NUMBER_KEY);
        String content = intent.getStringExtra(SMS_CONTENT_KEY);

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String description = String.format("sending sms to %s: %s", phone, content);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NTFC_CHANNEL_ID,
                    NTFC_CHANNEL_NAME, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context
                    .getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            } else {
                Log.e(ERROR_TAG, "NTFC Channel Creation Error");
            }
        }
    }
}