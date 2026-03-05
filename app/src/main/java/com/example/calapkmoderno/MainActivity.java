package com.example.calapkmoderno;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final String SITE_URL = "https://rg-2-cal-2026.vercel.app/";
    private static final String PREFS = "app_prefs";
    private static final String KEY_OFFLINE_READY = "firstLoginComplete";
    private static final String KEY_ETAG = "etag";
    private static final String KEY_LASTMOD = "lastModified";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        configureWebView();

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean offlineReady = prefs.getBoolean(KEY_OFFLINE_READY, false);

        if (isNetworkAvailable()) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            webView.getSettings().setBlockNetworkLoads(false);
            webView.loadUrl(SITE_URL);
            checkForUpdatesAsync();
        } else {
            if (offlineReady) {
                webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
                webView.getSettings().setBlockNetworkLoads(true);
            } else {
                webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            }
            webView.loadUrl(SITE_URL);
        }
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(settings, true);
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
            WebViewCompat.startSafeBrowsing(getApplicationContext(), new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean value) {
                }
            });
        }
        
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript("(()=>{try{const t=document.body&&document.body.innerText||'';return t.includes('Usuário autenticado')||t.includes('AUTO POSTO PARAÍSO')}catch(e){return false}})()", value -> {
                    SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                    if ("true".equals(value)) {
                        prefs.edit().putBoolean(KEY_OFFLINE_READY, true).apply();
                    }
                });
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (!isNetworkAvailable()) {
                    webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
                    webView.getSettings().setBlockNetworkLoads(true);
                    webView.reload();
                }
            }
        });
    }

    private void checkForUpdatesAsync() {
        new Thread(() -> {
            try {
                URL url = new URL(SITE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Cache-Control", "no-cache");
                conn.connect();
                String etag = conn.getHeaderField("ETag");
                String lastMod = conn.getHeaderField("Last-Modified");
                SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                String savedEtag = prefs.getString(KEY_ETAG, null);
                String savedLastMod = prefs.getString(KEY_LASTMOD, null);
                boolean changed = false;
                if (etag != null && !etag.equals(savedEtag)) changed = true;
                if (lastMod != null && !lastMod.equals(savedLastMod)) changed = true;
                if (changed) {
                    prefs.edit()
                            .putString(KEY_ETAG, etag)
                            .putString(KEY_LASTMOD, lastMod)
                            .apply();
                    runOnUiThread(() -> {
                        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                        webView.clearCache(true);
                        webView.reload();
                    });
                }
                conn.disconnect();
            } catch (Exception ignored) {
            }
        }).start();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities nc = cm.getNetworkCapabilities(network);
            return nc != null && (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    || nc.hasTransport(NetworkCapabilities.TRANSPORT_USB));
        } else {
            NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_clear_data) {
            clearAppData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearAppData() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().clear().apply();
        CookieManager cm = CookieManager.getInstance();
        cm.removeAllCookies(new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean value) {
            }
        });
        webView.clearCache(true);
        webView.clearHistory();
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.reload();
    }
}
