package com.ppc.honeyimhome;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class LocalSendSmsBroadcastReceiver extends BroadcastReceiver {

    private static final String ERROR_TAG = "SmsBroadcastReceiver";

    public static final String PHONE_NUMBER_KEY = "phoneNumber";
    public static final String SMS_CONTENT_KEY = "smsContent";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ActivityCompat
                .checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED) {

            String phone = intent.getStringExtra(PHONE_NUMBER_KEY);
            String content = intent.getStringExtra(SMS_CONTENT_KEY);
            if (phone == null || phone.isEmpty() || content == null || content.isEmpty()) {
                Log.e(ERROR_TAG, "Phone or content invalid");
            }



        } else {  // No permission
            Log.e(ERROR_TAG, "No SMS permission granted");
        }


    }
}
