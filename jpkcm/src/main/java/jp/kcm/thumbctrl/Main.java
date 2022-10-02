package jp.kcm.thumbctrl;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Main extends AppCompatActivity {

    private static int mFontSize = 80;
    private static int mInitialInterval = 400;
    private static int mNormalInterval = 100;
    private boolean mBack = false;
    private WebView mWebView;
    private PrefValue mPrefValue;
    private final String PREFKEY_FONTSIZE = "fontsize";
    private final Handler mHandler = new Handler();
    private OnBackPressedCallback mOnBackPressedCallback;
    private OnBackInvokedCallback mOnBackInvokedCallback;

    @SuppressLint({"SetJavaScriptEnabled", "NewApi"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.webview_activity);
        mWebView = findViewById(R.id.WebView);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
//        mWebView.getSettings().setBlockNetworkImage(false);
//        mWebView.getSettings().setBlockNetworkLoads(false);
        mWebView.getSettings().setAllowFileAccess(true);
        mPrefValue = new PrefValue(this);
        mFontSize = mPrefValue.getInt(PREFKEY_FONTSIZE, mFontSize);
        // mWebView.getSettings().setCacheMode(LOAD_NO_CACHE);
        mWebView.getSettings().setTextZoom(mFontSize);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (mBack && url.startsWith("file:")) {
                    mBack = false;
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return overrideUrlLoading(webView, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, String url) {
                return overrideUrlLoading(webView, url);
            }
        });
        setButtonListener();
        setOnBackPressedCallback();
        setOnBackPressedCallbackEnabled(true);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            try {
                String size = bundle.getString("size");
                if (size != null) mFontSize = Integer.parseInt(size);
                mWebView.getSettings().setTextZoom(mFontSize);
            } catch (Exception e) {
                Log.d("WebViewAcitivity", e.toString());
            }
            String url = bundle.getString("url");
            if ((url == null) || (!load(mWebView, url, mWebView.getUrl()))) {
                Log.d("WebViewAcitivity", "Load error : " + url);
            }
        }

        String html = getString(R.string.description_html);
        mWebView.loadUrl("file:///android_asset/" + html);
    }

    private void setButtonListener() {
        findViewById(R.id.webview_back).setOnClickListener(mButtonListener);
        findViewById(R.id.webview_forward).setOnClickListener(mButtonListener);
        findViewById(R.id.webview_reload).setOnClickListener(mButtonListener);
        findViewById(R.id.webview_abort).setOnClickListener(mButtonListener);
        findViewById(R.id.webview_menu).setOnClickListener(mButtonListener);
        findViewById(R.id.webview_quit).setOnClickListener(mButtonListener);
        findViewById(R.id.webview_page_up).setOnClickListener(mButtonListener);
        findViewById(R.id.webview_page_down).setOnClickListener(mButtonListener);
        findViewById(R.id.webview_plus).setOnTouchListener(new RepeatListener(mInitialInterval, mNormalInterval, mButtonListener));
        findViewById(R.id.webview_minus).setOnTouchListener(new RepeatListener(mInitialInterval, mNormalInterval, mButtonListener));
    }

    View.OnClickListener mButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.webview_back) {
                if (mWebView.canGoBack()) {
                    mBack = true;
                    mWebView.goBack();
                } else {
                    mWebView.stopLoading();
                    finish();
                }
            } else if (id == R.id.webview_forward) {
                if (mWebView.canGoForward()) {
                    mWebView.goForward();
                }
            } else if (id == R.id.webview_plus) {
                mWebView.getSettings().setTextZoom(mWebView.getSettings().getTextZoom() + 10);
                mFontSize = mWebView.getSettings().getTextZoom();
                mPrefValue.setInt(PREFKEY_FONTSIZE, mFontSize);
            } else if (id == R.id.webview_minus) {
                mWebView.getSettings().setTextZoom(mWebView.getSettings().getTextZoom() - 10);
                mFontSize = mWebView.getSettings().getTextZoom();
                mPrefValue.setInt(PREFKEY_FONTSIZE, mFontSize);
            } else if (id == R.id.webview_reload) {
                mWebView.reload();
            } else if (id == R.id.webview_abort) {
                mWebView.stopLoading();
            } else if (id == R.id.webview_page_up) {
                mWebView.pageUp(false);
            } else if (id == R.id.webview_page_down) {
                mWebView.pageDown(false);
            } else if (id == R.id.webview_menu) {
                menu();
            } else if (id == R.id.webview_quit) {
                confirmQuit();
            }
        }
    };

    private void setOnBackPressedCallback() {
        if (mOnBackPressedCallback == null) {
            mOnBackPressedCallback = new OnBackPressedCallback(false) {
                @Override
                public void handleOnBackPressed() {
                    if (mWebView.canGoBack()) {
                        mBack = true;
                        mWebView.goBack();
                    } else {
                        finish();
                    }
                }
            };
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (mOnBackInvokedCallback == null) {
                mOnBackInvokedCallback = () -> {
                    if (mOnBackPressedCallback != null && mOnBackPressedCallback.isEnabled()) {
                        mOnBackPressedCallback.handleOnBackPressed();
                    }
                };
            }
        } else {
            getOnBackPressedDispatcher().addCallback(this, mOnBackPressedCallback);
        }
    }

    private void setOnBackPressedCallbackEnabled(boolean enabled) {
        if (mOnBackPressedCallback != null) mOnBackPressedCallback.setEnabled(enabled);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            OnBackInvokedDispatcher invokedDispatcher = getOnBackInvokedDispatcher();
            if (invokedDispatcher != null) {
                invokedDispatcher.unregisterOnBackInvokedCallback(mOnBackInvokedCallback);
                if (enabled) invokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, mOnBackInvokedCallback);
            }
        }
    }

    final Runnable mFinish = new Runnable() {
        public void run() {
            mWebView.stopLoading();
            finish();
        }
    };

    private void confirmQuit() {
        final AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setMessage(R.string.query_quit);
        b.setPositiveButton(android.R.string.yes, (dialog, id) -> {
            dialog.dismiss();
            mHandler.post(mFinish);
        });
        b.setNegativeButton(android.R.string.no, null);
        b.show();
    }

    private void menu() {
        String[] items = {getString(R.string.scroll_to_top), getString(R.string.scroll_to_bottom), getString(R.string.quit)};
        final int quit = items.length - 1;
        new AlertDialog.Builder(mWebView.getContext())
                .setSingleChoiceItems(items, -1, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == quit) {
                        mHandler.post(mFinish);
                    } else if (which == 0) {
                        mWebView.pageUp(true);
                    } else if (which == 1) {
                        mWebView.pageDown(true);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    static public void setRepeatInterval(int initial, int normal) {
        mInitialInterval = initial;
        mNormalInterval = normal;
    }

    private boolean load(WebView webView, String url, String prev) {
        if (url.matches("https?://.*")) {
            webView.loadUrl(url);
            return true;
        }
        try {
            File html = new File(url);
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(html), "UTF-8"));
            StringBuilder buffer = new StringBuilder();
            String str;
            while ((str = reader.readLine()) != null) {
                buffer.append(str);
                buffer.append(System.getProperty("line.separator"));
            }
            String data = buffer.toString();
            webView.loadDataWithBaseURL("file://" + url, data, "text/html", "UTF-8", prev);
            return true;
        } catch (Exception e) {
            Log.d("WebViewAcitivity", e.getMessage());
        }
        return false;
    }

    private boolean overrideUrlLoading(WebView view, String url) {
        if (url.startsWith("file:")) {
            return openSettings(url);
        }

        if (openPlayStorePage(url)) return true;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        return true;
    }

    private boolean openPlayStorePage(String url) {
        if (!url.matches("https?://play.google.com/store/apps/details\\?id=.*")) {
            return false;
        }
        String pname = url.replaceFirst("https?://.*/details\\?id=", "");
        Uri uri = Uri.parse("market://details?id=" + pname);
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(url)));
        }
        return true;
    }

    /*
     *  Open Android Settings
     *  If there is a link starting with "file:///android_asset/ACTION_SETTINGS/", open the corresponding setting screen.
     *  E.g. <a href="file:///android_asset/ACTION_SETTINGS/ACTION_HARD_KEYBOARD_SETTINGS">Physical Keyboard settings&</a>
     */
    static final private Map<String, String> mSettingsAction = new HashMap<String, String>() {
        {
            put("ACTION_SETTINGS", Settings.ACTION_SETTINGS);
            put("ACTION_INPUT_METHOD_SETTINGS", Settings.ACTION_INPUT_METHOD_SETTINGS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                put("ACTION_HARD_KEYBOARD_SETTINGS", Settings.ACTION_HARD_KEYBOARD_SETTINGS);
            }
        }
    };

    private boolean openSettings(String url) {
        final String ANDROID_SETTINGS = "file:///android_asset/ACTION_SETTINGS";
        if (!url.startsWith(ANDROID_SETTINGS)) {
            return false;
        }
        url = url.replaceFirst(ANDROID_SETTINGS + "/?", "");
        String action = getSettingsAction(url);

        if (url.equals("ACTION_ALT_INPUT_SETTINGS")) {
            return openLanguageAndInputSettigns();
        }

        Intent intent = new Intent(action);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (!startIntent(intent)) {
            intent = new Intent(Settings.ACTION_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return startIntent(intent);
        }

        return true;
    }

    private boolean openLanguageAndInputSettigns() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$InputMethodAndLanguageSettingsActivity"));
        if (!startIntent(intent)) {
            intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$LanguageAndInputSettingsActivity"));
            if (!startIntent(intent)) {
                intent = new Intent(Settings.ACTION_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startIntent(intent);
            }
        }
        return true;
    }

    private String getSettingsAction(String action) {
        String settingsAction = Settings.ACTION_SETTINGS;
        if ((action.equals("ACTION_HARD_KEYBOARD_SETTINGS") && (Build.VERSION.SDK_INT < Build.VERSION_CODES.N))) {
            action = "ACTION_INPUT_METHOD_SETTINGS";
        }
        if (mSettingsAction.containsKey(action)) return mSettingsAction.get(action);
        return settingsAction;
    }

    private boolean startIntent(Intent intent) {
        try {
            startActivity(intent);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}

