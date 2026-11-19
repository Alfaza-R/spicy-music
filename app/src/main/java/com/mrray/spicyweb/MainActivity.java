package com.mrray.spicyweb;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class MainActivity extends Activity {

    private static final int MAX_BYTES = 2_000_000;

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setBackgroundColor(0xFF000000);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        // Jembatan fetch native untuk plugin (tanpa batasan CORS, hanya https)
        webView.addJavascriptInterface(new Bridge(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                Uri u = req.getUrl();
                if ("file".equals(u.getScheme())) return false;
                // Link luar dibuka di browser, jangan pernah di dalam WebView ini
                if ("http".equals(u.getScheme()) || "https".equals(u.getScheme())) {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                }
                return true;
            }
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl("file:///android_asset/index.html");
        }
    }

    private class Bridge {
        @JavascriptInterface
        public void fetch(final int id, final String url, final String method,
                          final String headersJson, final String body) {
            new Thread(() -> {
                int status = -1;
                String text = "";
                HttpURLConnection c = null;
                try {
                    URL u = new URL(url);
                    if (!"https".equals(u.getProtocol())) {
                        throw new IOException("Hanya https yang diizinkan");
                    }
                    c = (HttpURLConnection) u.openConnection();
                    c.setConnectTimeout(10000);
                    c.setReadTimeout(20000);
                    c.setInstanceFollowRedirects(true);
                    c.setRequestMethod(method == null || method.isEmpty() ? "GET" : method);
                    c.setRequestProperty("User-Agent", "SpicyMusic/1.0");
                    JSONObject h = new JSONObject(headersJson == null || headersJson.isEmpty() ? "{}" : headersJson);
                    Iterator<String> it = h.keys();
                    while (it.hasNext()) {
                        String k = it.next();
                        c.setRequestProperty(k, h.getString(k));
                    }
                    if (body != null && !body.isEmpty()) {
                        c.setDoOutput(true);
                        try (OutputStream os = c.getOutputStream()) {
                            os.write(body.getBytes(StandardCharsets.UTF_8));
                        }
                    }
                    status = c.getResponseCode();
                    InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
                    text = in == null ? "" : readLimited(in);
                } catch (Exception e) {
                    status = -1;
                    text = e.getMessage() == null ? e.toString() : e.getMessage();
                } finally {
                    if (c != null) c.disconnect();
                }
                final int st = status;
                final String tx = text;
                runOnUiThread(() -> {
                    if (webView != null) {
                        webView.evaluateJavascript("window.__spicyFetchDone(" + id + "," + st + ","
                                + JSONObject.quote(tx) + ")", null);
                    }
                });
            }).start();
        }
    }

    private static String readLimited(InputStream in) throws IOException {
        try (InputStream is = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > MAX_BYTES) {
                    throw new IOException("Respons terlalu besar (maks 2 MB)");
                }
            }
            return out.toString("UTF-8");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        webView.evaluateJavascript(
                "(window.spicyBack ? window.spicyBack() : false)",
                value -> {
                    if (!"true".equals(value)) {
                        moveTaskToBack(true);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
