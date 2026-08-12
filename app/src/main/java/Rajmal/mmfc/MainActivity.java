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
        webView.loadData("<!DOCTYPE html>\n<html lang=\"hi\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>Start.io Ads Setup</title>\n    <style>\n        body {\n            background-color: #0f172a;\n            color: #ffffff;\n            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n            display: flex;\n            flex-direction: column;\n            align-items: center;\n            justify-content: center;\n            min-height: 100vh;\n            margin: 0;\n            padding: 20px;\n        }\n\n        .container {\n            background-color: #1e293b;\n            padding: 30px;\n            border-radius: 16px;\n            box-shadow: 0 10px 25px rgba(0,0,0,0.5);\n            text-align: center;\n            max-width: 400px;\n            width: 100%;\n            border: 1px solid #334155;\n        }\n\n        h2 {\n            margin-top: 0;\n            color: #38bdf8;\n            font-size: 22px;\n        }\n\n        p {\n            color: #94a3b8;\n            font-size: 14px;\n            margin-bottom: 25px;\n        }\n\n        .btn {\n            display: flex;\n            align-items: center;\n            justify-content: center;\n            gap: 10px;\n            width: 100%;\n            padding: 14px 20px;\n            margin: 12px 0;\n            font-size: 16px;\n            font-weight: bold;\n            color: #ffffff;\n            border: none;\n            border-radius: 10px;\n            cursor: pointer;\n            transition: all 0.3s ease;\n            box-shadow: 0 4px 12px rgba(0,0,0,0.2);\n        }\n\n        .btn-rewarded {\n            background: linear-gradient(135deg, #6366f1, #4f46e5);\n        }\n\n        .btn-rewarded:hover {\n            background: linear-gradient(135deg, #4f46e5, #4338ca);\n            transform: translateY(-2px);\n        }\n\n        .btn-interstitial {\n            background: linear-gradient(135deg, #06b6d4, #0891b2);\n        }\n\n        .btn-interstitial:hover {\n            background: linear-gradient(135deg, #0891b2, #0e7490);\n            transform: translateY(-2px);\n        }\n\n        /* URL Bridge Buttons (Fallback / Android Webview Bridge) */\n        .bridge-section {\n            margin-top: 25px;\n            padding-top: 20px;\n            border-top: 1px solid #334155;\n        }\n\n        .bridge-section h4 {\n            color: #cbd5e1;\n            margin-bottom: 12px;\n            font-size: 14px;\n        }\n\n        .link-btn {\n            display: block;\n            padding: 10px;\n            margin: 8px 0;\n            background: #334155;\n            color: #38bdf8;\n            text-decoration: none;\n            border-radius: 8px;\n            font-size: 14px;\n            font-weight: 600;\n        }\n\n        .link-btn:hover {\n            background: #475569;\n        }\n    </style>\n</head>\n<body>\n\n    <div class=\"container\">\n        <h2>⚡ Start.io Ads Demo</h2>\n        <p>Aap niche diye gaye dono buttons me se kisi par bhi click karke alag-alag Ads dekh sakte hain:</p>\n\n        <!-- Button 1: Rewarded Ad -->\n        <button class=\"btn btn-rewarded\" onclick=\"showAd('rewarded')\">\n            🎁 Watch Rewarded Ad\n        </button>\n\n        <!-- Button 2: Interstitial Ad -->\n        <button class=\"btn btn-interstitial\" onclick=\"showAd('interstitial')\">\n            📺 Show Interstitial Ad\n        </button>\n\n        <!-- Alternative: Bina JS (URL Bridge Option) -->\n        <div class=\"bridge-section\">\n            <h4>Direct URL Bridge Mode:</h4>\n            <a href=\"ads://show_rewarded\" class=\"link-btn\">🎁 Watch Ad for Reward</a>\n            <a href=\"ads://show_interstitial\" class=\"link-btn\">📺 Show Interstitial Ad</a>\n        </div>\n    </div>\n\n    <!-- Script (Aapke API Backend se Connected) -->\n    <script>\n        function showAd(type) {\n            // Default type 'rewarded' rakha hai agar missing ho\n            var adType = type || 'rewarded';\n\n            // API call aapki API key aur StartApp App ID (207459046) ke sath\n            fetch('https://github-9g6j.onrender.com/api/show/3fbce53431074d8fa03d83f?type=' + adType, {\n                method: 'POST'\n            })\n            .then(function(r) { \n                return r.json(); \n            })\n            .then(function(d) {\n                if (d.success && window.NativeAds) {\n                    if (adType === 'rewarded') {\n                        window.NativeAds.showRewarded();\n                    } else {\n                        window.NativeAds.showInterstitial();\n                    }\n                } else if (window.NativeAds) {\n                    // Fallback Direct Show\n                    if (adType === 'rewarded') {\n                        window.NativeAds.showRewarded();\n                    } else {\n                        window.NativeAds.showInterstitial();\n                    }\n                } else {\n                    console.log('App WebView Native Interface Not Detected.');\n                }\n            })\n            .catch(function(err) {\n                console.error('Ad Request Error:', err);\n                // Fail-safe trigger\n                if (window.NativeAds) {\n                    if (adType === 'rewarded') {\n                        window.NativeAds.showRewarded();\n                    } else {\n                        window.NativeAds.showInterstitial();\n                    }\n                }\n            });\n        }\n    </script>\n</body>\n</html>", "text/html", "UTF-8");

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
