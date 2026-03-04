package com.example.calapkmoderno;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final String GITHUB_BASE_URL = "https://raw.githubusercontent.com/PikachuRG2/Calculadora-Pro/main/";
    private static final String[] SITE_FILES = {"index.html", "icon_192.png", "icon_512.png", "manifest.json", "service-worker.js"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        webView.setWebViewClient(new WebViewClient());

        loadLocalContent();
        checkForUpdates();
    }

    private void loadLocalContent() {
        File cachedIndex = new File(getCacheDir(), "index.html");
        if (cachedIndex.exists()) {
            Log.d("WebViewDebug", "Loading from cache: " + cachedIndex.toURI().toString());
            webView.loadUrl(cachedIndex.toURI().toString());
        } else {
            Log.d("WebViewDebug", "Loading from assets");
            webView.loadUrl("file:///android_asset/index.html");
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void checkForUpdates() {
        if (!isNetworkAvailable()) {
            Log.d("UpdateDebug", "No network connection, skipping update check.");
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Lógica de verificação de atualização (simplificada por enquanto)
                // Apenas baixa os arquivos para o cache
                for (String fileName : SITE_FILES) {
                    URL url = new URL(GITHUB_BASE_URL + fileName);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    try {
                        InputStream inputStream = urlConnection.getInputStream();
                        File file = new File(getCacheDir(), fileName);
                        FileOutputStream fileOutputStream = new FileOutputStream(file);

                        byte[] buffer = new byte[1024];
                        int bufferLength;
                        while ((bufferLength = inputStream.read(buffer)) > 0) {
                            fileOutputStream.write(buffer, 0, bufferLength);
                        }
                        fileOutputStream.close();
                        Log.d("UpdateDebug", "File downloaded successfully: " + fileName);
                    } finally {
                        urlConnection.disconnect();
                    }
                }
            } catch (Exception e) {
                Log.e("UpdateDebug", "Error checking for updates", e);
            }
        });
    }
}