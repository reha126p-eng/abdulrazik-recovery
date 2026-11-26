package com.abdulrazik.recovery;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;

public class MainActivity extends Activity {

    LinearLayout root;
    TextView output;
    int blue = Color.rgb(35, 105, 220);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
    }

    void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 35, 24, 30);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(241, 246, 255));
        scroll.addView(root);
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("ABDULRAZIK RECOVERY");
        title.setTextSize(25);
        title.setTextColor(blue);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, 1);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("بازیابی و نگهداری پیام‌ها");
        sub.setTextSize(17);
        sub.setTextColor(Color.DKGRAY);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub);

        addSpace();

        addButton("فعال‌سازی ذخیره پیام‌های واتساپ", v ->
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));

        addButton("نمایش پیام‌های ذخیره‌شده واتساپ", v ->
            showWhatsApp());

        addButton("دسترسی و پشتیبان‌گیری پیامک‌ها", v ->
            readSMS());

        addButton("راهنمای بازیابی پیام‌ها", v -> showHelp());

        output = new TextView(this);
        output.setText("وضعیت: برنامه آماده است");
        output.setTextColor(Color.rgb(35, 55, 80));
        output.setTextSize(15);
        output.setPadding(15, 20, 15, 20);
        root.addView(output);
    }

    void addSpace() {
        Space space = new Space(this);
        root.addView(space, new LinearLayout.LayoutParams(1, 22));
    }

    void addButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(blue));
        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 8, 0, 8);
        root.addView(button, params);
        button.setOnClickListener(listener);
    }

    void showWhatsApp() {
        SharedPreferences prefs =
            getSharedPreferences("recovery_data", MODE_PRIVATE);
        String data = prefs.getString("whatsapp_messages",
            "هنوز پیامی ذخیره نشده است.\nابتدا دسترسی اعلان‌ها را فعال کن.");
        output.setText("پیام‌های ذخیره‌شده واتساپ:\n\n" + data);
    }

    void readSMS() {
        if (checkSelfPermission(Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.READ_SMS}, 100);
            return;
        }

        StringBuilder result = new StringBuilder();
        Cursor cursor = null;

        try {
            cursor = getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                new String[]{"address", "body", "date"},
                null, null, "date DESC");

            if (cursor != null) {
                int count = 0;
                while (cursor.moveToNext() && count < 100) {
                    String address = cursor.getString(0);
                    String body = cursor.getString(1);
                    long date = cursor.getLong(2);

                    result.append("فرستنده: ").append(address)
                        .append("\nپیام: ").append(body)
                        .append("\n--------------------\n");
                    count++;
                }
            }
        } catch (Exception e) {
            result.append("خطا: ").append(e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }

        if (result.length() == 0) {
            result.append("پیامکی پیدا نشد.");
        }

        SharedPreferences prefs =
            getSharedPreferences("recovery_data", MODE_PRIVATE);
        prefs.edit().putString("sms_backup", result.toString()).apply();
        output.setText("پیامک‌های موجود و ذخیره‌شده:\n\n" + result);
    }

    void showHelp() {
        output.setText(
            "۱. دسترسی اعلان‌ها را فعال کن.\n\n" +
            "۲. اعلان‌های واتساپ را روشن نگه دار.\n\n" +
            "۳. برای پیامک‌ها دسترسی خواندن را بده.\n\n" +
            "توجه: فقط پیام‌هایی که پس از فعال‌سازی " +
            "از طریق اعلان دریافت شوند ذخیره خواهند شد. " +
            "پیام‌های حذف‌شده قدیمی بدون نسخه پشتیبان قابل تضمین نیستند."
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 100 && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            readSMS();
        } else {
            output.setText("مجوز خواندن پیامک داده نشد.");
        }
    }
}
