package com.abdulrazik.recovery;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.Manifest;
import android.provider.Settings;
import android.provider.MediaStore;
import android.database.Cursor;
import android.net.Uri;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;

public class MainActivity extends Activity {
    LinearLayout root, mediaContainer;
    TextView output;
    int blue = Color.rgb(35, 105, 220);
    int pendingMediaKind = -1;

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
        sub.setText("بازیابی و نگهداری پیام‌ها و رسانه‌ها");
        sub.setTextSize(17);
        sub.setTextColor(Color.DKGRAY);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub);

        addSpace();
        addButton("فعال‌سازی ذخیره پیام‌های واتساپ", v ->
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        addButton("نمایش پیام‌های ذخیره‌شده واتساپ", v -> showWhatsApp());

        addSpace();
        TextView mediaTitle = new TextView(this);
        mediaTitle.setText("جست‌وجوی رسانه‌های گوشی");
        mediaTitle.setTextSize(20);
        mediaTitle.setTextColor(blue);
        mediaTitle.setTypeface(null, 1);
        root.addView(mediaTitle);

        addButton("جست‌وجوی عکس‌ها", v -> scanMedia(0));
        addButton("جست‌وجوی ویس و فایل‌های صوتی", v -> scanMedia(1));
        addButton("جست‌وجوی ویدیوها", v -> scanMedia(2));

        addButton("راهنمای بازیابی", v -> showHelp());

        output = new TextView(this);
        output.setText("وضعیت: برنامه آماده است");
        output.setTextColor(Color.rgb(35, 55, 80));
        output.setTextSize(15);
        output.setPadding(15, 20, 15, 20);
        root.addView(output);

        mediaContainer = new LinearLayout(this);
        mediaContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(mediaContainer);
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

    void scanMedia(int kind) {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            pendingMediaKind = kind;
            requestPermissions(
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
            return;
        }

        Uri baseUri;
        String mime;
        String label;

        if (kind == 0) {
            baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            mime = "image/*";
            label = "عکس‌ها";
        } else if (kind == 1) {
            baseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            mime = "audio/*";
            label = "فایل‌های صوتی و ویس‌ها";
        } else {
            baseUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            mime = "video/*";
            label = "ویدیوها";
        }

        mediaContainer.removeAllViews();
        output.setText("در حال جست‌وجوی " + label + " ...");

        String[] projection = {
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE
        };

        Cursor cursor = null;
        int count = 0;
        try {
            cursor = getContentResolver().query(
                baseUri, projection, null, null,
                MediaStore.MediaColumns.DATE_MODIFIED + " DESC");

            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.DISPLAY_NAME);
                int sizeColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.SIZE);

                while (cursor.moveToNext() && count < 200) {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    long size = cursor.getLong(sizeColumn);
                    Uri fileUri = ContentUris.withAppendedId(baseUri, id);

                    Button item = new Button(this);
                    item.setText((name == null ? "فایل بدون نام" : name)
                        + "\n" + (size / 1024) + " KB");
                    item.setTextSize(13);
                    item.setAllCaps(false);
                    mediaContainer.addView(item);

                    item.setOnClickListener(v -> {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(fileUri, mime);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(this,
                                "برنامه‌ای برای باز کردن این فایل پیدا نشد.",
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                    count++;
                }
            }
        } catch (Exception e) {
            output.setText("خطا در جست‌وجو: " + e.getMessage());
            return;
        } finally {
            if (cursor != null) cursor.close();
        }

        output.setText("تعداد " + label + " پیدا شده: " + count
            + "\nبرای باز کردن هر فایل روی آن بزن.");
        if (count == 0) {
            TextView empty = new TextView(this);
            empty.setText("فایلی پیدا نشد.");
            mediaContainer.addView(empty);
        }
    }

    void showHelp() {
        output.setText(
            "۱. دسترسی اعلان‌ها را فعال کن.\n\n" +
            "۲. اعلان‌های واتساپ را روشن نگه دار.\n\n" +
            "۴. برای جست‌وجوی رسانه‌ها مجوز حافظه را بده.\n\n" +
            "این بخش رسانه‌های موجود و قابل دسترسی را پیدا می‌کند. " +
            "فایل‌هایی که کاملاً پاک شده‌اند و نسخه‌ای از آن‌ها باقی نمانده، " +
            "ممکن است قابل بازیابی نباشند."
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);

        if (results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == 101 && pendingMediaKind >= 0) {
                int kind = pendingMediaKind;
                pendingMediaKind = -1;
                scanMedia(kind);
            }
        } else {
            output.setText("دسترسی لازم داده نشد.");
        }
    }
}
