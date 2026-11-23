package com.sriomkar.lockscreen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/**
 * Auto-follow for the live wallpaper: the server's current-dream-job
 * endpoint is the source of truth for which job the lock screen shows.
 *
 * Endpoint: GET https://jobs.sriomkar.com/api/lockscreen/current
 * returns JSON like
 * {"lockscreen_url": "https://jobs.sriomkar.com/lockscreen?job_id=...&deadline=..."}.
 *
 * Every failure mode returns null — the caller keeps showing the saved
 * Settings link. Nothing here ever throws or blanks the screen.
 */
public final class CurrentJobUrl {
    static final String ENDPOINT =
            "https://jobs.sriomkar.com/api/lockscreen/current";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;
    private static final int MAX_BODY_BYTES = 8192;

    private CurrentJobUrl() {}

    /** Same rule as the manual link in Settings: https + a lockscreen URL. */
    public static boolean isValidLockscreenUrl(String url) {
        return url != null && url.startsWith("https://") && url.contains("lockscreen");
    }

    /** Pull the lockscreen_url out of an endpoint response body; null when absent or invalid. */
    public static String extractLockscreenUrl(String json) {
        if (json == null) return null;
        try {
            String url = new JSONObject(json).optString("lockscreen_url", null);
            if (url == null || url.isEmpty()) return null;
            return isValidLockscreenUrl(url) ? url : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Blocking network call — run off the main thread. Null on any failure. */
    public static String fetchCurrentUrl() {
        return fetchCurrentUrl(ENDPOINT);
    }

    static String fetchCurrentUrl(String endpoint) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.connect();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            return extractLockscreenUrl(readBody(conn.getInputStream()));
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readBody(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) != -1) {
            if (out.size() + n > MAX_BODY_BYTES) return null;
            out.write(buf, 0, n);
        }
        return out.toString(StandardCharsets.UTF_8);
    }
}
