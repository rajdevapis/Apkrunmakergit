package Rajmal.mm;

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
import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;
import com.startapp.sdk.adsbase.adlisteners.VideoListener;
import com.startapp.sdk.ads.banner.Banner;

public class MainActivity extends Activity {

    private static final String APP_ID = "207459046";
    private static final int RETRY_MS  = 5000;

    private WebView webView;
    private FrameLayout bannerLayout;
    private Button btnRewarded, btnInterstitial;
    private StartAppAd interstitialAd;
    private StartAppAd rewardedAd;
    private boolean rewardedReady = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_startapp);

        webView         = findViewById(R.id.webView);
        bannerLayout    = findViewById(R.id.bannerLayout);
        btnRewarded     = findViewById(R.id.btnRewarded);
        btnInterstitial = findViewById(R.id.btnInterstitial);

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
        });
        webView.loadUrl("https://github-9g6j.onrender.com");

        StartAppSDK.init(this, APP_ID, false);
        
        // StartApp Banner
        Banner startAppBanner = new Banner(this);
        bannerLayout.addView(startAppBanner);
        
        // StartApp Interstitial preload
        interstitialAd = new StartAppAd(this);
        interstitialAd.loadAd();
        
        // StartApp Rewarded Video preload
        loadRewarded();
        
        // Auto show on open
        handler.postDelayed(() -> {
            if (interstitialAd != null) interstitialAd.showAd();
        }, 2000);

        if (btnRewarded != null) {
            btnRewarded.setVisibility(View.VISIBLE);
            btnRewarded.setOnClickListener(v -> showRewarded());
        }
        if (btnInterstitial != null) {
            btnInterstitial.setVisibility(View.VISIBLE);
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

    private void loadRewarded() {
        rewardedAd = new StartAppAd(this);
        rewardedAd.setVideoListener(new VideoListener() {
            @Override public void onVideoCompleted() {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                    "🎁 Reward mila!", Toast.LENGTH_SHORT).show());
            }
        });
        rewardedAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, new AdEventListener() {
            @Override public void onReceiveAd(Ad ad)      { rewardedReady = true; }
            @Override public void onFailedToReceiveAd(Ad ad) {
                rewardedReady = false;
                handler.postDelayed(() -> loadRewarded(), RETRY_MS);
            }
        });
    }

    private void showRewarded() {
        if (rewardedAd != null && rewardedReady) {
            rewardedAd.showAd();
            rewardedReady = false;
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
        if (rewardedAd != null && !rewardedReady) loadRewarded();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
