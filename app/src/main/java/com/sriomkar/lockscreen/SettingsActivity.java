package com.sriomkar.lockscreen;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Settings screen (the app's launcher): paste a lockscreen link and the
 *  wallpaper uses it next time the lock screen shows — no rebuild needed. */
public class SettingsActivity extends Activity {
    static final String PREFS = "lockscreen_prefs";
    static final String KEY_URL = "wallpaper_url";
    static final String DEFAULT_URL =
        "https://jobs.sriomkar.com/lockscreen?job_id=li-4474501423&deadline=2026-11-30T23:59:00-05:00";

    /** The link the wallpaper should load: the saved one, or the default. */
    static String loadUrl(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_URL, DEFAULT_URL);
    }

    private EditText urlField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView label = new TextView(this);
        label.setText("Lock screen link (job + deadline):");
        root.addView(label);

        urlField = new EditText(this);
        urlField.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        urlField.setText(loadUrl(this));
        urlField.setSelectAllOnFocus(true);
        root.addView(urlField, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = new TextView(this);
        hint.setText("Paste a lockscreen link, e.g.\n"
                + "https://jobs.sriomkar.com/lockscreen?job_id=...&deadline=2026-12-15T23:59:00-05:00\n"
                + "The wallpaper picks it up the next time your lock screen shows.");
        root.addView(hint);

        Button save = new Button(this);
        save.setText("Save");
        save.setOnClickListener(v -> saveUrl());
        root.addView(save);

        Button reset = new Button(this);
        reset.setText("Reset to Pax8");
        reset.setOnClickListener(v -> {
            urlField.setText(DEFAULT_URL);
            saveUrl();
        });
        root.addView(reset);

        setContentView(root);
    }

    private void saveUrl() {
        String url = urlField.getText().toString().trim();
        if (!url.startsWith("https://") || !url.contains("lockscreen")) {
            Toast.makeText(this,
                    "That doesn't look like a lockscreen link — it must start "
                    + "with https:// and contain 'lockscreen'.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putString(KEY_URL, url).apply();
        Toast.makeText(this,
                "Saved — your lock screen will use the new link next time it shows.",
                Toast.LENGTH_LONG).show();
    }
}
