package com.example.calapkmoderno;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final String SITE_URL = "https://pikachurg2.github.io/RG2-Calculadora-pro/";
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
        settings.setDatabaseEnabled(true);
        
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(true);
        
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript("(()=>{try{return !!(document.body&&document.body.innerText&&document.body.innerText.includes('Usuário autenticado'));}catch(e){return false}})()", value -> {
                    if ("true".equals(value)) {
                        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
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
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
