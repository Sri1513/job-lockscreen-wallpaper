package com.sriomkar.lockscreen;

import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new WebEngine();
    }

    private class WebEngine extends Engine {
        private WebView webView;
        private String currentUrl;
        // The saved-link value (with the stored background applied) that
        // the engine last actually loaded. The saved link is only
        // reloaded when this changes — otherwise every lock-screen show
        // would yank the wallpaper back to the saved link after the
        // auto-follow fetch switched it to the server's job.
        private String lastSavedUrl;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            webView = new WebView(WebWallpaperService.this);
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
            // A page that finishes loading after the lock screen is
            // already showing (the auto-follow fetch) is never painted
            // unless we draw it: drawFrame only runs from visibility /
            // surface events, which already happened before the async
            // fetch completed. Repaint on every page finish so the
            // server's job actually appears.
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    drawFrame(WebEngine.this.getSurfaceHolder());
                }
            });
            // The stored background choice is applied to every URL the
            // wallpaper loads (saved link and server link alike), so it
            // survives the endpoint switching jobs.
            currentUrl = SettingsActivity.applyStoredBackground(
                    WebWallpaperService.this,
                    SettingsActivity.loadUrl(WebWallpaperService.this));
            lastSavedUrl = currentUrl;
            webView.loadUrl(currentUrl);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            if (webView != null) {
                webView.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY)
                );
                webView.layout(0, 0, width, height);
                drawFrame(holder);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (webView == null) return;
            if (visible) {
                // Pick up a link saved in settings — changing jobs needs
                // no rebuild, just a new link in the app. Only reload
                // when the saved value changed since it was last
                // applied: after auto-follow switches to the server's
                // job, the saved link is unchanged, so it must not
                // override the server's job on every show.
                String url = SettingsActivity.applyStoredBackground(
                        WebWallpaperService.this,
                        SettingsActivity.loadUrl(WebWallpaperService.this));
                if (!url.equals(lastSavedUrl)) {
                    lastSavedUrl = url;
                    currentUrl = url;
                    webView.loadUrl(url);
                }
                // Auto-follow: ask the server which dream job is current.
                // When it answers, its link wins; when it doesn't
                // (offline, 404, bad body), the saved link above keeps
                // showing — the screen never goes blank.
                fetchCurrentJobUrl();
                webView.onResume();
                webView.resumeTimers();
                drawFrame(getSurfaceHolder());
            } else {
                webView.onPause();
                webView.pauseTimers();
            }
        }

        // Guards against a slow fetch applying after a newer one: only the
        // latest request may change the URL.
        private int fetchSeq;

        private void fetchCurrentJobUrl() {
            final int seq = ++fetchSeq;
            new Thread(() -> {
                String url = CurrentJobUrl.fetchCurrentUrl();
                if (url == null) return; // endpoint silent — keep the saved link
                // Deliver on the main looper, NOT view.post(): this
                // WebView is created with the Service context and is
                // never attached to a window (the engine draws it
                // manually with webView.draw(canvas)), so View.post()
                // queues the Runnable in the view's RunQueue, which
                // only flushes on attach — the callback would never
                // run and the switch would never happen. The main
                // looper always delivers, and WebView calls need the
                // UI thread anyway.
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (seq != fetchSeq) return; // a newer fetch already answered
                    if (webView == null) return; // engine destroyed meanwhile
                    // The stored background applies to the server link too
                    // — the endpoint URL carries no bg param of its own.
                    String effective = SettingsActivity.applyStoredBackground(
                            WebWallpaperService.this, url);
                    if (!effective.equals(currentUrl)) {
                        currentUrl = effective;
                        webView.loadUrl(effective);
                    }
                });
            }).start();
        }

        private void drawFrame(SurfaceHolder holder) {
            if (webView == null || holder == null) return;
            android.graphics.Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    webView.draw(canvas);
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }
        }

        @Override
        public void onDestroy() {
            if (webView != null) {
                webView.destroy();
                webView = null;
            }
            super.onDestroy();
        }
    }
}
