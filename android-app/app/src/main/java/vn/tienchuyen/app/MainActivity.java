package vn.tienchuyen.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Android beta shell for the LIVE Tiện Chuyến web backend. No payment or map SDK is embedded. */
public final class MainActivity extends Activity {
    private static final String HOST = "tien-chuyen-online.onrender.com";
    private static final String HOME = "https://" + HOST + "/";
    private static final int LOCATION_REQUEST = 4101;
    private static final int GREEN = Color.rgb(13, 94, 88);

    private WebView webView;
    private ProgressBar progress;
    private LinearLayout offlinePanel;
    private GeolocationPermissions.Callback pendingGeo;
    private String pendingGeoOrigin;
    private boolean mainPageFailed = false;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        getWindow().setStatusBarColor(GREEN);
        getWindow().setNavigationBarColor(Color.rgb(246,248,247));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(246, 248, 247));
        setContentView(root);
        applySafeInsets(root);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(18), 0, dp(12), 0);
        toolbar.setBackgroundColor(GREEN);
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(56)));

        TextView title = new TextView(this);
        title.setText("TIỆN CHUYẾN");
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextSize(19);
        title.setContentDescription("Ứng dụng Tiện Chuyến");
        toolbar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView reload = new TextView(this);
        reload.setText("⟳");
        reload.setTextSize(29);
        reload.setTextColor(Color.WHITE);
        reload.setGravity(Gravity.CENTER);
        reload.setContentDescription("Tải lại trang");
        toolbar.addView(reload, new LinearLayout.LayoutParams(dp(48), dp(48)));
        reload.setOnClickListener(v -> retry());

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setIndeterminate(false);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));

        FrameLayout content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        webView = new WebView(this);
        content.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        WebView.setWebContentsDebuggingEnabled(false);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                mainPageFailed = false;
                offlinePanel.setVisibility(View.GONE);
                super.onPageStarted(view, url, favicon);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri uri = request.getUrl();
                if (isTrustedUrl(uri)) return false;
                openOutside(uri);
                return true;
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                                       WebResourceResponse response) {
                if (request.isForMainFrame() && response.getStatusCode() >= 500) {
                    mainPageFailed = true;
                    offlinePanel.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    mainPageFailed = true;
                    offlinePanel.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onPageFinished(WebView view, String url) {
                offlinePanel.setVisibility(mainPageFailed ? View.VISIBLE : View.GONE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (!isTrustedUrl(Uri.parse(origin))) {
                    callback.invoke(origin, false, false);
                    return;
                }
                if (hasLocationPermission()) {
                    callback.invoke(origin, true, false);
                } else {
                    if (pendingGeo != null) pendingGeo.invoke(pendingGeoOrigin, false, false);
                    pendingGeo = callback;
                    pendingGeoOrigin = origin;
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
                }
            }
        });

        offlinePanel = new LinearLayout(this);
        offlinePanel.setOrientation(LinearLayout.VERTICAL);
        offlinePanel.setGravity(Gravity.CENTER);
        offlinePanel.setPadding(dp(28), dp(16), dp(28), dp(16));
        offlinePanel.setBackgroundColor(Color.rgb(246,248,247));
        content.addView(offlinePanel, new FrameLayout.LayoutParams(-1, -1));

        TextView message = new TextView(this);
        message.setText("Chưa kết nối được Tiện Chuyến\n\nKiểm tra Internet. Render Free có thể cần thêm thời gian để khởi động lại.");
        message.setTextColor(Color.rgb(31,46,43));
        message.setTextSize(17);
        message.setGravity(Gravity.CENTER);
        offlinePanel.addView(message, new LinearLayout.LayoutParams(-1, -2));

        Button retry = new Button(this);
        retry.setText("THỬ LẠI");
        retry.setTextColor(Color.WHITE);
        retry.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GREEN));
        LinearLayout.LayoutParams retryLp = new LinearLayout.LayoutParams(dp(180), dp(54));
        retryLp.topMargin = dp(24);
        offlinePanel.addView(retry, retryLp);
        retry.setOnClickListener(v -> retry());
        offlinePanel.setVisibility(View.GONE);
        if (savedState != null) {
            webView.restoreState(savedState);
        } else {
            webView.loadUrl(HOME);
        }
    }

    private void applySafeInsets(View root) {
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets s = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(0, s.top, 0, s.bottom);
                return insets;
            });
        } else {
            root.setFitsSystemWindows(true);
        }
    }

    private int dp(float value) { return Math.round(getResources().getDisplayMetrics().density * value); }

    private boolean isTrustedUrl(Uri uri) {
        return "https".equalsIgnoreCase(uri.getScheme()) && HOST.equalsIgnoreCase(uri.getHost());
    }

    private void openOutside(Uri uri) {
        try {
            if ("intent".equalsIgnoreCase(uri.getScheme())) {
                Intent intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException notInstalled) {
                    String fallback = intent.getStringExtra("browser_fallback_url");
                    if (fallback != null && (fallback.startsWith("https://") || fallback.startsWith("http://"))) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallback)));
                    }
                }
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Không mở được liên kết ngoài", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST && pendingGeo != null) {
            boolean granted = hasLocationPermission();
            pendingGeo.invoke(pendingGeoOrigin, granted, false);
            pendingGeo = null;
            pendingGeoOrigin = null;
        }
    }

    private void retry() {
        if (webView != null) {
            offlinePanel.setVisibility(View.GONE);
            mainPageFailed = false;
            webView.loadUrl(HOME);
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override protected void onDestroy() {
        if (pendingGeo != null) {
            pendingGeo.invoke(pendingGeoOrigin, false, false);
            pendingGeo = null;
        }
        if (webView != null) {
            ((ViewGroup)webView.getParent()).removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
