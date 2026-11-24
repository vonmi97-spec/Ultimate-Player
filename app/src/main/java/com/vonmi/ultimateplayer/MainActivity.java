package com.vonmi.ultimateplayer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
    private static final int REQ_CAM = 1, REQ_FILE = 2;
    private WebView web;
    private PermissionRequest pending;
    private ValueCallback<Uri[]> fileCb;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#0d0f14"));
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0d0f14"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(final PermissionRequest req) {
                runOnUiThread(() -> {
                    if (Build.VERSION.SDK_INT >= 23 &&
                        checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        pending = req;
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAM);
                    } else req.grant(req.getResources());
                });
            }
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                try { startActivityForResult(Intent.createChooser(i, "Photo du joueur"), REQ_FILE); }
                catch (Exception e) { fileCb = null; return false; }
                return true;
            }
        });
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
        setContentView(web);
        pleinEcran();
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        if (code == REQ_CAM && pending != null) {
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) pending.grant(pending.getResources());
            else pending.deny();
            pending = null;
        }
    }

    @Override protected void onActivityResult(int code, int result, Intent data) {
        if (code == REQ_FILE && fileCb != null) {
            Uri u = (result == RESULT_OK && data != null) ? data.getData() : null;
            fileCb.onReceiveValue(u != null ? new Uri[]{u} : null);
            fileCb = null;
        } else super.onActivityResult(code, result, data);
    }

    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }

    // Plein écran immersif : barres d'état et de navigation masquées (glisser depuis le bord pour les afficher).
    private void pleinEcran() {
        android.view.Window w = getWindow();
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            android.view.WindowInsetsController c = w.getInsetsController();
            if (c != null) {
                c.hide(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            w.getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) pleinEcran();
    }
}
