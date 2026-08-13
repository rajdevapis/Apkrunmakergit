package Rajmal.mmfcr;

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

    private static final String APP_ID    = "207459046";
    private static final boolean TEST_MODE = false;
    private static final String TAG    = "StartAppAds";
    private static final int RETRY_MS  = 2000;
    private static final int FAST_RETRY_MS = 600;

    private WebView webView;
    private FrameLayout bannerLayout;
    private Button btnRewarded, btnInterstitial;
    private View loadingOverlay, errorOverlay;
    private StartAppAd interstitialAd;
    private StartAppAd rewardedAd;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean loadingRewarded = false;
    private boolean pendingRewarded = false;

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

        webView.addJavascriptInterface(new AdBridge(), "NativeAds");

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
        webView.loadDataWithBaseURL(null, "<!DOCTYPE html>\n<html lang=\"hi\">\n<head>\n  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n  <title>Advanced Web File Manager & Text Editor</title>\n  <style>\n    :root {\n      --bg: #121212;\n      --panel: #1e1e1e;\n      --accent: #007acc;\n      --text: #e0e0e0;\n      --border: #333;\n    }\n    * { box-sizing: border-box; margin: 0; padding: 0; font-family: sans-serif; }\n    body { background-color: var(--bg); color: var(--text); display: flex; flex-direction: column; height: 100vh; }\n    \n    header { background: var(--panel); padding: 15px; border-bottom: 1px solid var(--border); display: flex; gap: 10px; align-items: center; }\n    button { background: var(--accent); color: white; border: none; padding: 8px 15px; border-radius: 4px; cursor: pointer; font-weight: bold; }\n    button:hover { opacity: 0.9; }\n    button:disabled { background: #555; cursor: not-allowed; }\n\n    .main-container { display: flex; flex: 1; overflow: hidden; }\n    \n    /* File List Section */\n    .sidebar { width: 350px; background: var(--panel); border-right: 1px solid var(--border); display: flex; flex-direction: column; }\n    .sidebar-header { padding: 10px; font-size: 0.9em; background: #252526; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; }\n    .file-list { flex: 1; overflow-y: auto; list-style: none; padding: 10px; }\n    .file-item { padding: 8px; border-radius: 4px; display: flex; justify-content: space-between; align-items: center; cursor: pointer; margin-bottom: 5px; background: #2a2a2a; }\n    .file-item:hover { background: #333; }\n    .file-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 180px; }\n    .actions { display: flex; gap: 5px; }\n    .actions button { padding: 3px 8px; font-size: 0.8em; }\n\n    /* Editor Section */\n    .editor-container { flex: 1; display: flex; flex-direction: column; background: #1e1e1e; }\n    .editor-toolbar { padding: 10px; background: #252526; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; }\n    textarea { flex: 1; background: #1e1e1e; color: #d4d4d4; border: none; padding: 15px; font-family: monospace; font-size: 14px; resize: none; outline: none; }\n\n    .empty-state { flex: 1; display: flex; justify-content: center; align-items: center; color: #777; }\n  </style>\n</head>\n<body>\n\n  <header>\n    <button id=\"btnGrant\">📁 Storage / Folder Access Dein</button>\n    <span id=\"status\">Status: Folder select karein.</span>\n  </header>\n\n  <div class=\"main-container\">\n    <!-- Left Sidebar: File Browser -->\n    <div class=\"sidebar\">\n      <div class=\"sidebar-header\">\n        <span>Files List</span>\n        <button id=\"btnPaste\" disabled>📋 Paste Here</button>\n      </div>\n      <ul class=\"file-list\" id=\"fileList\">\n        <li style=\"color: #888; text-align: center; margin-top: 20px;\">Koyi folder open nahi hai.</li>\n      </ul>\n    </div>\n\n    <!-- Right Area: Text Editor -->\n    <div class=\"editor-container\">\n      <div class=\"editor-toolbar\">\n        <span id=\"currentFileName\">Koi file open nahi hai</span>\n        <button id=\"btnSave\" disabled>💾 Save Changes</button>\n      </div>\n      <textarea id=\"textEditor\" placeholder=\"File open karke yahan edit karein...\" disabled></textarea>\n    </div>\n  </div>\n\n  <script>\n    let dirHandle = null;\n    let currentFileHandle = null;\n    let copiedFileHandle = null;\n\n    const btnGrant = document.getElementById('btnGrant');\n    const btnSave = document.getElementById('btnSave');\n    const btnPaste = document.getElementById('btnPaste');\n    const fileListEl = document.getElementById('fileList');\n    const textEditor = document.getElementById('textEditor');\n    const currentFileName = document.getElementById('currentFileName');\n    const status = document.getElementById('status');\n\n    // 1. Permission aur Folder Access Maangna\n    btnGrant.addEventListener('click', async () => {\n      try {\n        // Direct folder access permission prompt\n        dirHandle = await window.showDirectoryPicker({ mode: 'readwrite' });\n        status.innerText = `Active Folder: ${dirHandle.name}`;\n        await loadFiles();\n      } catch (err) {\n        console.error(err);\n        status.innerText = \"Permission fail ya cancel ho gayi.\";\n      }\n    });\n\n    // 2. Directory ki saari files read & display karna\n    async function loadFiles() {\n      fileListEl.innerHTML = '';\n      for await (const entry of dirHandle.values()) {\n        if (entry.kind === 'file') {\n          const li = document.createElement('li');\n          li.className = 'file-item';\n          \n          li.innerHTML = `\n            <span class=\"file-name\" title=\"${entry.name}\">${entry.name}</span>\n            <div class=\"actions\">\n              <button onclick=\"openFile('${entry.name}')\">Edit</button>\n              <button style=\"background:#444\" onclick=\"copyFile('${entry.name}')\">Copy</button>\n            </div>\n          `;\n          fileListEl.appendChild(li);\n        }\n      }\n    }\n\n    // 3. File Read karna aur Editor me show karna\n    async function openFile(filename) {\n      try {\n        currentFileHandle = await dirHandle.getFileHandle(filename);\n        const file = await currentFileHandle.getFile();\n        const content = await file.text();\n\n        textEditor.value = content;\n        textEditor.disabled = false;\n        btnSave.disabled = false;\n        currentFileName.innerText = `Editing: ${filename}`;\n      } catch (err) {\n        alert(\"File open karne me problem hui: \" + err.message);\n      }\n    }\n\n    // 4. Edited File Ko Save Karna\n    btnSave.addEventListener('click', async () => {\n      if (!currentFileHandle) return;\n      try {\n        const writable = await currentFileHandle.createWritable();\n        await writable.write(textEditor.value);\n        await writable.close();\n        alert(\"File successfully save ho gayi!\");\n      } catch (err) {\n        alert(\"Save nahi ho paya: \" + err.message);\n      }\n    });\n\n    // 5. File Copy karna\n    async function copyFile(filename) {\n      copiedFileHandle = await dirHandle.getFileHandle(filename);\n      btnPaste.disabled = false;\n      alert(`\"${filename}\" copy ho gayi hai! Kisi doosre folder me jaakar 'Paste' dabayein ya isi folder me paste karein.`);\n    }\n\n    // 6. File Paste karna (Dusri jagah ya usi folder me naye naam se)\n    btnPaste.addEventListener('click', async () => {\n      if (!copiedFileHandle) return;\n      \n      let newName = prompt(\"Paste karne ke liye file ka naam likhein:\", \"copy_\" + copiedFileHandle.name);\n      if (!newName) return;\n\n      try {\n        const sourceFile = await copiedFileHandle.getFile();\n        const content = await sourceFile.arrayBuffer();\n\n        // Nayi file create karke content write karna\n        const newFileHandle = await dirHandle.getFileHandle(newName, { create: true });\n        const writable = await newFileHandle.createWritable();\n        await writable.write(content);\n        await writable.close();\n\n        alert(\"File Paste ho gayi!\");\n        await loadFiles(); // List refresh karein\n      } catch (err) {\n        alert(\"Paste fail ho gaya: \" + err.message);\n      }\n    });\n  </script>\n</body>\n</html>", "text/html", "UTF-8", null);

        StartAppSDK.setTestAdsEnabled(TEST_MODE);
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
        // Use placement if provided, else default REWARDED_VIDEO
        AdEventListener listener = new AdEventListener() {
            @Override public void onReceiveAd(Ad ad) {
                loadingRewarded = false;
                Log.d(TAG, "Rewarded ad ready");
                if (pendingRewarded) {
                    pendingRewarded = false;
                    showRewarded();
                }
            }
            @Override public void onFailedToReceiveAd(Ad ad) {
                loadingRewarded = false;
                Log.e(TAG, "Rewarded ad load FAILED — no fill ya App ID galat ho sakta hai");
                handler.postDelayed(() -> loadRewarded(), pendingRewarded ? FAST_RETRY_MS : RETRY_MS);
            }
        };
        if (null != null) {
            rewardedAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, null, listener);
        } else {
            rewardedAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, listener);
        }
    }

    private void showRewarded() {
        if (rewardedAd != null && rewardedAd.isReady()) {
            rewardedAd.showAd();
            handler.postDelayed(() -> loadRewarded(), 1000);
        } else {
            Toast.makeText(this, "Ad load ho raha hai, ek pal ruko...", Toast.LENGTH_SHORT).show();
            pendingRewarded = true;
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
