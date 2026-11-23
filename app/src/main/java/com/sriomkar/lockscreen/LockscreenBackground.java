package com.sriomkar.lockscreen;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Background choice for the lock screen, picked in the app's Settings.
 *
 * The lockscreen page takes {@code ?bg=<name>} for its five built-in
 * backgrounds (glow, mist, fog, forest, rain) and {@code ?bgurl=<https
 * image URL>} for a custom image. The choice lives in SharedPreferences
 * and is applied by the wallpaper service to every URL it loads -- the
 * saved link and the server-provided one alike -- so it survives the
 * endpoint switching jobs. "Default" loads the link untouched, so a
 * background baked into the link itself keeps working.
 *
 * Pure Java (no Android imports) so the URL logic can be tested with a
 * plain javac harness.
 */
public final class LockscreenBackground {
    public static final String BG_DEFAULT = "default";
    public static final String BG_GLOW = "glow";
    public static final String BG_MIST = "mist";
    public static final String BG_FOG = "fog";
    public static final String BG_FOREST = "forest";
    public static final String BG_RAIN = "rain";
    public static final String BG_CUSTOM = "custom";

    /** Choices in Settings order. */
    public static final String[] BG_CHOICES = {
            BG_DEFAULT, BG_GLOW, BG_MIST, BG_FOG, BG_FOREST, BG_RAIN, BG_CUSTOM
    };

    /** Labels shown in Settings, in the same order as BG_CHOICES. */
    public static final String[] BG_LABELS = {
            "Default (from link)", "Glow", "Mist", "Fog", "Forest", "Rain",
            "Custom image URL"
    };

    // Same rule the server enforces on ?bgurl= (see the dashboard's
    // lockscreen route): https only, no quotes, parens, angle brackets,
    // backticks, backslashes or whitespace, max 2048 chars. The server
    // never fetches the URL -- the phone's browser loads the image
    // directly -- so the check here only stops a broken link from being
    // saved; a non-image link still shows the dark fallback colour.
    private static final int MAX_BG_URL_LENGTH = 2048;
    private static final Pattern BG_URL_PATTERN =
            Pattern.compile("^https://[A-Za-z0-9\\-._~:/?#@!$&*+,;=%]+$");

    private LockscreenBackground() {}

    /** True for one of the five named backgrounds (not default/custom). */
    public static boolean isNamedBackground(String bg) {
        return BG_GLOW.equals(bg) || BG_MIST.equals(bg) || BG_FOG.equals(bg)
                || BG_FOREST.equals(bg) || BG_RAIN.equals(bg);
    }

    /** The server's strict rule for a custom background image URL. */
    public static boolean isValidBgUrl(String url) {
        return url != null && url.length() <= MAX_BG_URL_LENGTH
                && BG_URL_PATTERN.matcher(url).matches();
    }

    /**
     * Apply the stored background choice to a lockscreen URL.
     *
     * "Default" (or null, or an unknown stored value) returns the URL
     * untouched. A named choice strips any bg=/bgurl= already on the URL
     * and appends bg=&lt;name&gt;. A custom choice appends the stored URL
     * as bgurl= (percent-encoded, so its own ? and &amp; can't split the
     * outer query); an invalid custom URL is not applied -- the URL loads
     * as-is. A null URL returns null.
     */
    public static String applyBackground(String url, String bg, String bgUrl) {
        if (url == null) return null;
        if (bg == null || BG_DEFAULT.equals(bg)) return url;
        String param;
        String value;
        if (BG_CUSTOM.equals(bg)) {
            if (!isValidBgUrl(bgUrl)) return url;
            param = "bgurl";
            value = urlEncode(bgUrl);
        } else if (isNamedBackground(bg)) {
            param = "bg";
            value = bg;
        } else {
            return url; // unknown stored value -- load as-is
        }
        return withQueryParam(stripBgParams(url), param, value);
    }

    /** Remove any bg= / bgurl= params from a URL, keeping everything else. */
    static String stripBgParams(String url) {
        if (url == null) return null;
        String fragment = "";
        int hash = url.indexOf('#');
        if (hash >= 0) {
            fragment = url.substring(hash);
            url = url.substring(0, hash);
        }
        int q = url.indexOf('?');
        if (q < 0) return url + fragment;
        String base = url.substring(0, q);
        StringBuilder kept = new StringBuilder();
        for (String part : url.substring(q + 1).split("&", -1)) {
            String key = part.split("=", 2)[0];
            if ("bg".equals(key) || "bgurl".equals(key)) continue;
            if (kept.length() > 0) kept.append('&');
            kept.append(part);
        }
        String out = kept.length() == 0 ? base : base + "?" + kept;
        return out + fragment;
    }

    private static String withQueryParam(String url, String param, String value) {
        String fragment = "";
        int hash = url.indexOf('#');
        if (hash >= 0) {
            fragment = url.substring(hash);
            url = url.substring(0, hash);
        }
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + param + "=" + value + fragment;
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return value; // UTF-8 is always present; unreachable in practice
        }
    }
}
