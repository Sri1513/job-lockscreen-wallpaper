package com.sriomkar.lockscreen;

import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebWallpaperService extends WallpaperService {
    private static final String TARGET_URL =
        "https://jobs.sriomkar.com/lockscreen?job_id=li-4474501423&deadline=2026-11-30T23:59:00-05:00";

    @Override
    public Engine onCreateEngine() {
        return new WebEngine();
    }

    private class WebEngine extends Engine {
        private WebView webView;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            webView = new WebView(WebWallpaperService.this);
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
            webView.setWebViewClient(new WebViewClient());
            webView.loadUrl(TARGET_URL);
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
                webView.onResume();
                webView.resumeTimers();
                drawFrame(getSurfaceHolder());
            } else {
                webView.onPause();
                webView.pauseTimers();
            }
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
