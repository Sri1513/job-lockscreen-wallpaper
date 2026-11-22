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
            currentUrl = SettingsActivity.loadUrl(WebWallpaperService.this);
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
                // no rebuild, just a new link in the app.
                String url = SettingsActivity.loadUrl(WebWallpaperService.this);
                if (!url.equals(currentUrl)) {
                    currentUrl = url;
                    webView.loadUrl(url);
                }
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
