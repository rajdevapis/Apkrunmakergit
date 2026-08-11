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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.ads.banner.Banner;

public class MainActivity extends Activity {

    private static final String APP_ID = "207459046";
    private WebView webView;
    private StartAppAd startAppAd;
    private FrameLayout bannerLayout;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_startapp);

        webView      = findViewById(R.id.webView);
        bannerLayout = findViewById(R.id.bannerLayout);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);

        // JS Bridge
        webView.addJavascriptInterface(new AdBridge(), "NativeAds");

        // URL Bridge: <a href="ads://show_interstitial">
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                if (url.startsWith("ads://show_interstitial")) {
                    if (startAppAd != null) startAppAd.showAd();
                    return true;
                }
                return false;
            }
        });
        webView.loadUrl("https://github-9g6j.onrender.com");

        StartAppSDK.init(this, APP_ID, false);
        
        // StartApp Banner
        Banner startAppBanner = new Banner(this);
        bannerLayout.addView(startAppBanner);
        
        // StartApp Interstitial preload
        startAppAd = new StartAppAd(this);
        startAppAd.loadAd();
        
        // Auto show on open
        handler.postDelayed(() -> {
            if (startAppAd != null) startAppAd.showAd();
        }, 2000);

        // Interstitial button
        View btnI = findViewById(R.id.btnInterstitial);
        if (btnI != null) btnI.setOnClickListener(v -> {
            if (startAppAd != null) startAppAd.showAd();
        });
    }

    private class AdBridge {
        @JavascriptInterface
        public void showInterstitial() {
            runOnUiThread(() -> { if (startAppAd != null) startAppAd.showAd(); });
        }
        @JavascriptInterface
        public void showRewarded() { showInterstitial(); }
    }

    @Override protected void onResume() {
        super.onResume();
        if (startAppAd != null) startAppAd.loadAd();
    }
    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
