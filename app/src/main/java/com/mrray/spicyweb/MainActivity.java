package com.mrray.spicyweb;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

public class MainActivity extends Activity {

    private static final String YT_URL = "https://music.youtube.com/";
    // User agent Chrome mobile biasa (tanpa tanda "wv") supaya YouTube Music mau jalan normal
    private static final String MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/124.0.0.0 Mobile Safari/537.36";

    // bg, bg2, teks, aksen
    private static final String[] NAMES = {
            "Ungu Malam", "Hijau Klasik", "Sunset", "Samudra", "Mawar", "Terang"
    };
    private static final String[][] COLORS = {
            {"#000000", "#14141c", "#f2f2f7", "#b388ff"},
            {"#000000", "#121212", "#ffffff", "#1ed760"},
            {"#120a0a", "#201010", "#fff1ea", "#ff7a45"},
            {"#040b14", "#0a1726", "#e8f4ff", "#4cc9f0"},
            {"#0d0509", "#1a0b13", "#ffeef5", "#ff5c9d"},
            {"#ffffff", "#f2f2f5", "#111118", "#6d28d9"}
    };

    private WebView yt;
    private WebView aud;
    private TextView btnYt;
    private TextView btnAud;
    private TextView btnTheme;
    private LinearLayout bar;
    private SharedPreferences prefs;
    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("spicy", MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        FrameLayout content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        yt = makeWebView(true);
        aud = makeWebView(false);
        content.addView(yt, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        content.addView(aud, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        btnYt = makeTab("YouTube Music");
        btnAud = makeTab("Audius");
        btnTheme = makeTab("Tema");
        bar.addView(btnYt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(btnAud, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(btnTheme, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        btnYt.setOnClickListener(v -> selectTab(0));
        btnAud.setOnClickListener(v -> selectTab(1));
        btnTheme.setOnClickListener(v -> showThemeDialog());

        setContentView(root);

        yt.loadUrl(YT_URL);
        aud.loadUrl("file:///android_asset/index.html");

        selectTab(0);
        applyTheme();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView makeTab(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(13);
        t.setPadding(dp(4), dp(14), dp(4), dp(14));
        return t;
    }

    private WebView makeWebView(boolean youtube) {
        WebView w = new WebView(this);
        w.setBackgroundColor(Color.BLACK);

        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        if (youtube) {
            s.setUserAgentString(MOBILE_UA);
            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            cm.setAcceptThirdPartyCookies(w, true);
        } else {
            s.setAllowFileAccess(true);
        }

        w.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                // Izinkan DRM hanya untuk domain YouTube
                String host = request.getOrigin().getHost();
                final boolean ok = host != null
                        && (host.endsWith("youtube.com") || host.endsWith("google.com"));
                runOnUiThread(() -> {
                    if (ok) {
                        for (String r : request.getResources()) {
                            if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(r)) {
                                request.grant(new String[]{r});
                                return;
                            }
                        }
                    }
                    request.deny();
                });
            }
        });

        w.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                Uri u = req.getUrl();
                String scheme = u.getScheme();
                if (scheme == null) return true;
                if (scheme.equals("file")) return false;
                if (!scheme.equals("http") && !scheme.equals("https")) {
                    return true; // blokir intent:// dan skema aneh lain
                }
                if (!youtube) {
                    // Audius: link luar dibuka di browser
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView v, String url, android.graphics.Bitmap f) {
                pushTheme(v == yt);
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                pushTheme(v == yt);
            }
        });
        return w;
    }

    private void selectTab(int tab) {
        currentTab = tab;
        yt.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        aud.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        paintBar();
    }

    private int themeIndex() {
        String n = prefs.getString("theme", NAMES[0]);
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(n)) return i;
        }
        return 0;
    }

    private void paintBar() {
        String[] c = COLORS[themeIndex()];
        int bg = Color.parseColor(c[0]);
        int sub = Color.parseColor(c[2]);
        int accent = Color.parseColor(c[3]);
        bar.setBackgroundColor(bg);
        btnYt.setTextColor(currentTab == 0 ? accent : sub);
        btnAud.setTextColor(currentTab == 1 ? accent : sub);
        btnTheme.setTextColor(sub);
    }

    private void applyTheme() {
        String[] c = COLORS[themeIndex()];
        int bg = Color.parseColor(c[0]);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        paintBar();
        pushTheme(true);
        pushTheme(false);
    }

    private String ytCss(String[] c, String custom) {
        String bg = c[0], bg2 = c[1], tx = c[2], ac = c[3];
        return ":root, html, body {"
                + "--yt-spec-base-background:" + bg + " !important;"
                + "--yt-spec-raised-background:" + bg2 + " !important;"
                + "--yt-spec-menu-background:" + bg2 + " !important;"
                + "--yt-spec-general-background-a:" + bg + " !important;"
                + "--yt-spec-general-background-b:" + bg + " !important;"
                + "--yt-spec-general-background-c:" + bg2 + " !important;"
                + "--yt-spec-text-primary:" + tx + " !important;"
                + "--yt-spec-call-to-action:" + ac + " !important;"
                + "--ytmusic-general-background-a:" + bg + " !important;"
                + "--ytmusic-general-background-b:" + bg + " !important;"
                + "--ytmusic-general-background-c:" + bg2 + " !important;"
                + "--ytmusic-color-white-primary:" + tx + " !important;"
                + "--ytmusic-nav-bar-background:" + bg + " !important;"
                + "--ytmusic-player-bar-background:" + bg2 + " !important;"
                + "background:" + bg + " !important;}"
                + custom;
    }

    private void pushTheme(boolean youtube) {
        int i = themeIndex();
        String custom = prefs.getString("css", "");
        if (youtube) {
            String js = "(function(){var e=document.getElementById('spicy-yt');"
                    + "if(!e){e=document.createElement('style');e.id='spicy-yt';"
                    + "(document.head||document.documentElement).appendChild(e);}"
                    + "e.textContent=" + JSONObject.quote(ytCss(COLORS[i], custom)) + ";})();";
            yt.evaluateJavascript(js, null);
        } else {
            String js = "if(window.spicySetTheme){spicySetTheme("
                    + JSONObject.quote(NAMES[i]) + "," + JSONObject.quote(custom) + ");}";
            aud.evaluateJavascript(js, null);
        }
    }

    private void showThemeDialog() {
        final String[] items = new String[NAMES.length + 1];
        System.arraycopy(NAMES, 0, items, 0, NAMES.length);
        items[NAMES.length] = "CSS kustom...";
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Tema")
                .setItems(items, (d, which) -> {
                    if (which < NAMES.length) {
                        prefs.edit().putString("theme", NAMES[which]).apply();
                        applyTheme();
                    } else {
                        showCssDialog();
                    }
                })
                .show();
    }

    private void showCssDialog() {
        final EditText et = new EditText(this);
        et.setText(prefs.getString("css", ""));
        et.setHint("contoh: body { font-family: serif; }");
        et.setMinLines(5);
        et.setGravity(Gravity.TOP);
        et.setTextSize(12);
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("CSS kustom")
                .setView(et)
                .setPositiveButton("Terapkan", (d, w) -> {
                    prefs.edit().putString("css", et.getText().toString()).apply();
                    applyTheme();
                })
                .setNeutralButton("Reset", (d, w) -> {
                    prefs.edit().putString("css", "").apply();
                    applyTheme();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (currentTab == 0) {
            if (yt.canGoBack()) {
                yt.goBack();
            } else {
                moveTaskToBack(true);
            }
        } else {
            aud.evaluateJavascript(
                    "(window.spicyBack ? window.spicyBack() : false)",
                    value -> {
                        if (!"true".equals(value)) {
                            moveTaskToBack(true);
                        }
                    });
        }
    }

    @Override
    protected void onDestroy() {
        if (yt != null) yt.destroy();
        if (aud != null) aud.destroy();
        super.onDestroy();
    }
}
