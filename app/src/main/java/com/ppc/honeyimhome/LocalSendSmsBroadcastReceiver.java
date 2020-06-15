package com.ppc.honeyimhome;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

class LocalSendSmsBroadcastReceiver extends BroadcastReceiver {
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

            App app = (App) context.getApplicationContext();
            app.getMessageManager().createNotificationChannel(context);

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
            app.getMessageManager().sendText(intent);

        } else {  // No permission
            Log.e(MessageManager.ERROR_TAG, "No SMS permission granted");
        }
    }
}