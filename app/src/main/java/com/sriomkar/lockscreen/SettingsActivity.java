package com.sriomkar.lockscreen;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

/** Settings screen (the app's launcher): paste a lockscreen link and the
 *  wallpaper uses it next time the lock screen shows — no rebuild needed. */
public class SettingsActivity extends Activity {
    static final String PREFS = "lockscreen_prefs";
    static final String KEY_URL = "wallpaper_url";
    static final String KEY_BG = "lockscreen_bg";
    static final String KEY_BGURL = "lockscreen_bgurl";
    static final String DEFAULT_URL =
        "https://jobs.sriomkar.com/lockscreen?job_id=li-4474501423&deadline=2026-11-30T23:59:00-05:00";

    /** The link the wallpaper should load: the saved one, or the default. */
    static String loadUrl(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_URL, DEFAULT_URL);
    }

    /** The stored background choice ("default" when never set). */
    static String loadBackground(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);
        String bg = prefs.getString(KEY_BG, LockscreenBackground.BG_DEFAULT);
        return bg != null ? bg : LockscreenBackground.BG_DEFAULT;
    }

    /** The stored custom background image URL ("" when never set). */
    static String loadBackgroundUrl(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);
        String url = prefs.getString(KEY_BGURL, "");
        return url != null ? url : "";
    }

    /** The URL the wallpaper should load: the given link with the stored
     *  background choice applied (unchanged when "Default" is picked). */
    static String applyStoredBackground(Context ctx, String url) {
        return LockscreenBackground.applyBackground(
                url, loadBackground(ctx), loadBackgroundUrl(ctx));
    }

    private EditText urlField;
    private RadioGroup bgGroup;
    private EditText bgUrlField;

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

        TextView bgLabel = new TextView(this);
        bgLabel.setText("Background:");
        root.addView(bgLabel);

        // RadioGroup needs explicit ids on children added in code, or the
        // checked-change listener gets -1 instead of the picked button.
        bgGroup = new RadioGroup(this);
        bgGroup.setOrientation(LinearLayout.VERTICAL);
        String currentBg = loadBackground(this);
        for (int i = 0; i < LockscreenBackground.BG_CHOICES.length; i++) {
            RadioButton button = new RadioButton(this);
            button.setId(View.generateViewId());
            button.setText(LockscreenBackground.BG_LABELS[i]);
            button.setTag(LockscreenBackground.BG_CHOICES[i]);
            button.setChecked(
                    LockscreenBackground.BG_CHOICES[i].equals(currentBg));
            bgGroup.addView(button);
        }
        root.addView(bgGroup);

        TextView bgUrlLabel = new TextView(this);
        bgUrlLabel.setText("Custom image URL (only used when "
                + "'Custom image URL' is picked):");
        root.addView(bgUrlLabel);

        bgUrlField = new EditText(this);
        bgUrlField.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        bgUrlField.setText(loadBackgroundUrl(this));
        bgUrlField.setSelectAllOnFocus(true);
        root.addView(bgUrlField, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView bgUrlHint = new TextView(this);
        bgUrlHint.setText("Must start with https://, no spaces or "
                + "characters like quotes, brackets or backslashes, and "
                + "under 2048 characters. The server never fetches it — "
                + "your phone loads the image directly.");
        root.addView(bgUrlHint);

        // The custom URL field only matters when Custom is picked.
        bgUrlField.setEnabled(
                LockscreenBackground.BG_CUSTOM.equals(currentBg));
        bgGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton checked = group.findViewById(checkedId);
            boolean custom = checked != null
                    && LockscreenBackground.BG_CUSTOM.equals(checked.getTag());
            bgUrlField.setEnabled(custom);
        });

        Button save = new Button(this);
        save.setText("Save");
        save.setOnClickListener(v -> {
            if (saveUrl()) saveBackground();
        });
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

    private boolean saveUrl() {
        String url = urlField.getText().toString().trim();
        if (!CurrentJobUrl.isValidLockscreenUrl(url)) {
            Toast.makeText(this,
                    "That doesn't look like a lockscreen link — it must start "
                    + "with https:// and contain 'lockscreen'.",
                    Toast.LENGTH_LONG).show();
            return false;
        }
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putString(KEY_URL, url).apply();
        Toast.makeText(this,
                "Saved — your lock screen will use the new link next time it shows.",
                Toast.LENGTH_LONG).show();
        return true;
    }

    /**
     * Save the picked background. A custom choice with an invalid URL is
     * not saved — the toast says why, and the field keeps the bad value
     * so it can be fixed and saved again. The wallpaper then loads the
     * link as-is until a valid choice is saved.
     */
    private boolean saveBackground() {
        int checkedId = bgGroup.getCheckedRadioButtonId();
        RadioButton checked = checkedId != -1
                ? bgGroup.findViewById(checkedId) : null;
        String bg = checked != null && checked.getTag() instanceof String
                ? (String) checked.getTag() : LockscreenBackground.BG_DEFAULT;
        String bgUrl = bgUrlField.getText().toString().trim();
        if (LockscreenBackground.BG_CUSTOM.equals(bg)
                && !LockscreenBackground.isValidBgUrl(bgUrl)) {
            Toast.makeText(this,
                    "That custom image URL isn't valid — it must start with "
                    + "https://, have no spaces or characters like quotes, "
                    + "brackets or backslashes, and be under 2048 characters. "
                    + "The background was not saved.",
                    Toast.LENGTH_LONG).show();
            return false;
        }
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putString(KEY_BG, bg).putString(KEY_BGURL, bgUrl)
                .apply();
        return true;
    }
}
