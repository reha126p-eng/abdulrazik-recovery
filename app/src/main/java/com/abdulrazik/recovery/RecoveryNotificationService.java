package com.abdulrazik.recovery;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.content.SharedPreferences;
import android.os.Bundle;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecoveryNotificationService extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();

        if (!pkg.equals("com.whatsapp") &&
            !pkg.equals("com.whatsapp.w4b")) return;

        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;

        if (extras == null) return;

        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);

        if (text == null || text.toString().trim().isEmpty()) return;

        String message =
            "فرستنده: " + (title == null ? "نامشخص" : title) +
            "\nپیام: " + text +
            "\nزمان: " + new SimpleDateFormat(
                "yyyy/MM/dd HH:mm:ss", Locale.getDefault()
            ).format(new Date());

        SharedPreferences prefs =
            getSharedPreferences("recovery_data", MODE_PRIVATE);

        String old = prefs.getString("whatsapp_messages", "");
        String updated = message + "\n--------------------\n" + old;

        if (updated.length() > 100000) {
            updated = updated.substring(0, 100000);
        }

        prefs.edit().putString("whatsapp_messages", updated).apply();
    }
}
