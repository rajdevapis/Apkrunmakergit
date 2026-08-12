package Rajmal.mmfc;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.util.Log;
import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;
import com.startapp.sdk.adsbase.adlisteners.VideoListener;
import com.startapp.sdk.ads.banner.Banner;

public class MainActivity extends Activity {

    private static final String APP_ID = "207459046";
    private static final String TAG    = "StartAppAds";
    private static final int RETRY_MS  = 2000;

    private WebView webView;
    private FrameLayout bannerLayout;
    private Button btnRewarded, btnInterstitial;
    private View loadingOverlay, errorOverlay;
    private StartAppAd interstitialAd;
    private StartAppAd rewardedAd;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_startapp);

        webView         = findViewById(R.id.webView);
        bannerLayout    = findViewById(R.id.bannerLayout);
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

        // JS Bridge: window.NativeAds.showRewarded() / showInterstitial()
        webView.addJavascriptInterface(new AdBridge(), "NativeAds");

        // URL Bridge: <a href="ads://show_rewarded"> or <a href="ads://show_interstitial">
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                if (url.startsWith("ads://show_rewarded"))     { showRewarded();     return true; }
                if (url.startsWith("ads://show_interstitial")) { showInterstitial(); return true; }
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
        webView.loadData("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>Start.io Ad Integration</title>\n    <style>\n        body {\n            font-family: Arial, sans-serif;\n            text-align: center;\n            padding: 40px 20px;\n            background-color: #121212;\n            color: #ffffff;\n        }\n        .btn {\n            display: inline-block;\n            margin: 10px;\n            padding: 12px 24px;\n            font-size: 16px;\n            color: #fff;\n            background-color: #6200ee;\n            border: none;\n            border-radius: 8px;\n            cursor: pointer;\n            text-decoration: none;\n        }\n        .btn:hover {\n            background-color: #3700b3;\n        }\n    </style>\n</head>\n<body>\n\n    <h2>Start.io Custom Ads Setup</h2>\n\n    <!-- JavaScript API Buttons -->\n    <div>\n        <h3>Method 1: JavaScript Trigger</h3>\n        <button class=\"btn\" onclick=\"showAd('rewarded')\">🎁 Watch Ad for Reward</button>\n        <button class=\"btn\" onclick=\"showAd('interstitial')\">📺 Show Interstitial Ad</button>\n    </div>\n\n    <!-- URL Bridge Alternative -->\n    <div style=\"margin-top: 30px;\">\n        <h3>Method 2: Direct URL Bridge</h3>\n        <a class=\"btn\" href=\"ads://show_rewarded\">🎁 Watch Ad (URL Bridge)</a>\n        <a class=\"btn\" href=\"ads://show_interstitial\">📺 Show Ad (URL Bridge)</a>\n    </div>\n\n    <script>\n        function showAd(type) {\n            const adType = type || 'rewarded';\n            const apiUrl = 'https://github-9g6j.onrender.com/api/show/3fbce53431074d8fa03d83f?type=' + adType;\n\n            fetch(apiUrl, {\n                method: 'POST'\n            })\n            .then(response => response.json())\n            .then(data => {\n                if (data.success && window.NativeAds) {\n                    if (adType === 'rewarded') {\n                        window.NativeAds.showRewarded();\n                    } else {\n                        window.NativeAds.showInterstitial();\n                    }\n                } else {\n                    console.log('Ad signal sent or NativeAds interface missing.');\n                }\n            })\n            .catch(error => console.error('Ad Request Error:', error));\n        }\n    </script>\n\n</body>\n</html>", "text/html", "UTF-8");

        StartAppSDK.init(this, APP_ID, false);
        
        // StartApp Banner
        Banner startAppBanner = new Banner(this);
        bannerLayout.addView(startAppBanner);
        
        // StartApp Interstitial preload
        interstitialAd = new StartAppAd(this);
        interstitialAd.loadAd();
        
        // StartApp Rewarded Video preload
        loadRewarded();
        

        if (btnRewarded != null) {
            btnRewarded.setVisibility(View.GONE);
            btnRewarded.setOnClickListener(v -> showRewarded());
        }
        if (btnInterstitial != null) {
            btnInterstitial.setVisibility(View.GONE);
            btnInterstitial.setOnClickListener(v -> showInterstitial());
        }
    }

    private void showInterstitial() {
        if (interstitialAd != null && interstitialAd.isReady()) {
            interstitialAd.showAd();
            interstitialAd.loadAd();
        } else {
            Toast.makeText(this, "Ad load ho raha hai, ek pal ruko...", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean loadingRewarded = false;

    private void loadRewarded() {
        if (loadingRewarded) return;
        loadingRewarded = true;
        if (rewardedAd == null) {
            rewardedAd = new StartAppAd(this);
            rewardedAd.setVideoListener(new VideoListener() {
                @Override public void onVideoCompleted() {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "🎁 Reward mila!", Toast.LENGTH_SHORT).show());
                }
            });
        }
        rewardedAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, new AdEventListener() {
            @Override public void onReceiveAd(Ad ad) {
                loadingRewarded = false;
                Log.d(TAG, "Rewarded ad ready");
            }
            @Override public void onFailedToReceiveAd(Ad ad) {
                loadingRewarded = false;
                Log.e(TAG, "Rewarded ad load FAILED — no fill ya App ID galat ho sakta hai");
                handler.postDelayed(() -> loadRewarded(), RETRY_MS);
            }
        });
    }

    // .isReady() SDK se seedha poochte hain — interstitial ke jaisa reliable pattern,
    // ek manually-tracked boolean pe depend nahi karte jo callback miss hone par stuck reh sakta tha.
    private void showRewarded() {
        if (rewardedAd != null && rewardedAd.isReady()) {
            rewardedAd.showAd();
            handler.postDelayed(() -> loadRewarded(), 1000);
        } else {
            Toast.makeText(this, "Ad load ho raha hai, ek pal ruko...", Toast.LENGTH_SHORT).show();
            loadRewarded();
        }
    }

    private class AdBridge {
        @JavascriptInterface public void showRewarded()     { runOnUiThread(MainActivity.this::showRewarded); }
        @JavascriptInterface public void showInterstitial() { runOnUiThread(MainActivity.this::showInterstitial); }
    }

    @Override protected void onResume() {
        super.onResume();
        if (interstitialAd != null) interstitialAd.loadAd();
        if (rewardedAd != null && !rewardedAd.isReady()) loadRewarded();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
