package Rajmal.mmfc;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsShowOptions;
import android.content.pm.PackageManager;
import android.Manifest;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements IUnityAdsInitializationListener {

    private static final String TAG              = "UnityAdsApp";
    private static final String UNITY_GAME_ID   = "207459046";
    private static final boolean TEST_MODE       = true;
    private static final String REWARDED         = "Rewarded_Android";
    private static final String INTERSTITIAL     = "Interstitial_Android";
    private static final boolean AUTO_SHOW       = true;
    private static final int RETRY_MS            = 2000;
    private static final int FAST_RETRY_MS       = 600;

    private WebView webView;
    private Button  btnRewarded, btnInterstitial;
    private View    loadingOverlay, errorOverlay;
    private boolean rewardedReady = false, interstitialReady = false, sdkReady = false;
    private boolean loadingR = false, loadingI = false;
    private boolean pendingR = false, pendingI = false;
    private boolean autoFired = false;
    private int     autoAttempts = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    
    private static final int PERMISSION_REQUEST_CODE = 123;
    private void checkAndRequestPermissions() {
        List<String> needed = new ArrayList<>();
        for (String perm : new String[]{android.permission.UPDATE_DEVICE_STATS,
        android.permission.WRITE_SETTINGS,
        android.permission.WRITE_VOICEMAIL}) {
            if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                needed.add(perm);
            }
        }
        if (!needed.isEmpty()) {
            requestPermissions(needed.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // simply proceed
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView         = findViewById(R.id.webView);
        btnRewarded     = findViewById(R.id.btnRewarded);
        btnInterstitial = findViewById(R.id.btnInterstitial);
        loadingOverlay  = findViewById(R.id.loadingOverlay);
        errorOverlay    = findViewById(R.id.errorOverlay);
        Button btnRetry = findViewById(R.id.btnRetry);
        btnRetry.setOnClickListener(v -> {
            errorOverlay.setVisibility(View.GONE);
            loadingOverlay.setVisibility(View.VISIBLE);
            webView.reload();
        });

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        ws.setSupportMultipleWindows(true);
        ws.setJavaScriptCanOpenWindowsAutomatically(true);

        webView.addJavascriptInterface(new AdBridge(), "NativeAds");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                if (url.startsWith("ads://show_rewarded"))    { triggerRewarded();     return true; }
                if (url.startsWith("ads://show_interstitial")){ triggerInterstitial(); return true; }
                return false;
            }
            @Override
            public void onPageStarted(WebView v, String url, android.graphics.Bitmap favicon) {
                errorOverlay.setVisibility(View.GONE);
                loadingOverlay.setVisibility(View.VISIBLE);
            }
            @Override
            public void onPageFinished(WebView v, String url) {
                loadingOverlay.setVisibility(View.GONE);
            }
            @Override
            public void onReceivedError(WebView v, android.webkit.WebResourceRequest req,
                    android.webkit.WebResourceError err) {
                if (req.isForMainFrame()) {
                    loadingOverlay.setVisibility(View.GONE);
                    errorOverlay.setVisibility(View.VISIBLE);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                    boolean isUserGesture, android.os.Message resultMsg) {
                WebView.HitTestResult r = view.getHitTestResult();
                String u = r != null ? r.getExtra() : null;
                if (u != null) view.loadUrl(u);
                return false;
            }
        });

        webView.loadUrl("https://github-9g6j.onrender.com");

        btnRewarded.setVisibility(View.VISIBLE);
        btnInterstitial.setVisibility(View.VISIBLE);

        // Request permissions if any
        checkAndRequestPermissions();

        UnityAds.initialize(this, UNITY_GAME_ID, TEST_MODE, this);

        handler.postDelayed(() -> {
            if (!sdkReady) Toast.makeText(this,
                "Ads SDK ready nahi — Game ID/internet check karo", Toast.LENGTH_LONG).show();
        }, 12000);

        btnRewarded.setOnClickListener(v -> {
            if (!sdkReady) { Toast.makeText(this,"SDK init ho raha...",Toast.LENGTH_SHORT).show(); return; }
            if (rewardedReady) showRewarded();
            else { pendingR = true; Toast.makeText(this,"Loading, abhi aata hai...",Toast.LENGTH_SHORT).show(); loadRewarded(); }
        });

        btnInterstitial.setOnClickListener(v -> {
            if (!sdkReady) { Toast.makeText(this,"SDK init ho raha...",Toast.LENGTH_SHORT).show(); return; }
            if (interstitialReady) showInterstitial();
            else { pendingI = true; Toast.makeText(this,"Loading, abhi aata hai...",Toast.LENGTH_SHORT).show(); loadInterstitial(); }
        });
    }

    @Override
    public void onInitializationComplete() {
        sdkReady = true;
        if (REWARDED != null)     loadRewarded();
        if (INTERSTITIAL != null) loadInterstitial();
        if (AUTO_SHOW) maybeAutoShow();
    }

    @Override
    public void onInitializationFailed(UnityAds.UnityAdsInitializationError e, String m) {
        Log.e(TAG, "Init failed: " + e + " " + m);
        handler.postDelayed(() -> UnityAds.initialize(this, UNITY_GAME_ID, TEST_MODE, this), RETRY_MS);
    }

    private void maybeAutoShow() {
        if (autoFired || autoAttempts++ > 20) return;
        boolean rOk = REWARDED == null     || rewardedReady;
        boolean iOk = INTERSTITIAL == null || interstitialReady;
        if (!rOk || !iOk) { handler.postDelayed(this::maybeAutoShow, 800); return; }
        autoFired = true;
        if (REWARDED != null) {
            showRewarded();
            if (INTERSTITIAL != null) handler.postDelayed(this::showInterstitial, 4000);
        } else if (INTERSTITIAL != null) showInterstitial();
    }

    private void triggerRewarded()     { runOnUiThread(() -> { if (rewardedReady)     showRewarded();     else { pendingR = true; loadRewarded(); } }); }
    private void triggerInterstitial() { runOnUiThread(() -> { if (interstitialReady) showInterstitial(); else { pendingI = true; loadInterstitial(); } }); }

    private class AdBridge {
        @JavascriptInterface public void showRewarded()     { triggerRewarded(); }
        @JavascriptInterface public void showInterstitial() { triggerInterstitial(); }
    }

    private void loadRewarded() {
        if (REWARDED == null || !sdkReady || loadingR) return;
        loadingR = true;
        UnityAds.load(REWARDED, new IUnityAdsLoadListener() {
            public void onUnityAdsAdLoaded(String p)    { loadingR=false; rewardedReady=true; if(pendingR){pendingR=false;showRewarded();} }
            public void onUnityAdsFailedToLoad(String p, UnityAds.UnityAdsLoadError e, String m) {
                loadingR=false; rewardedReady=false;
                handler.postDelayed(()->loadRewarded(), pendingR?FAST_RETRY_MS:RETRY_MS);
            }
        });
    }

    private void loadInterstitial() {
        if (INTERSTITIAL == null || !sdkReady || loadingI) return;
        loadingI = true;
        UnityAds.load(INTERSTITIAL, new IUnityAdsLoadListener() {
            public void onUnityAdsAdLoaded(String p)    { loadingI=false; interstitialReady=true; if(pendingI){pendingI=false;showInterstitial();} }
            public void onUnityAdsFailedToLoad(String p, UnityAds.UnityAdsLoadError e, String m) {
                loadingI=false; interstitialReady=false;
                handler.postDelayed(()->loadInterstitial(), pendingI?FAST_RETRY_MS:RETRY_MS);
            }
        });
    }

    private void showRewarded() {
        rewardedReady = false;
        UnityAds.show(this, REWARDED, new UnityAdsShowOptions(), new IUnityAdsShowListener() {
            public void onUnityAdsShowFailure(String p,UnityAds.UnityAdsShowError e,String m) { loadRewarded(); }
            public void onUnityAdsShowStart(String p)  {}
            public void onUnityAdsShowClick(String p)  {}
            public void onUnityAdsShowComplete(String p,UnityAds.UnityAdsShowCompletionState s) {
                if(s==UnityAds.UnityAdsShowCompletionState.COMPLETED)
                    Toast.makeText(MainActivity.this,"🎁 Reward mila!",Toast.LENGTH_SHORT).show();
                loadRewarded();
            }
        });
    }

    private void showInterstitial() {
        interstitialReady = false;
        UnityAds.show(this, INTERSTITIAL, new UnityAdsShowOptions(), new IUnityAdsShowListener() {
            public void onUnityAdsShowFailure(String p,UnityAds.UnityAdsShowError e,String m) { loadInterstitial(); }
            public void onUnityAdsShowStart(String p)  {}
            public void onUnityAdsShowClick(String p)  {}
            public void onUnityAdsShowComplete(String p,UnityAds.UnityAdsShowCompletionState s) { loadInterstitial(); }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (sdkReady) { if(!rewardedReady) loadRewarded(); if(!interstitialReady) loadInterstitial(); }
    }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy(); }
    @Override public void onBackPressed() { if(webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
}
