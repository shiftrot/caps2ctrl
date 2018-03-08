package jp.kcm.thumbctrl;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class Main extends AppCompatActivity {

    private static int mFontSize = 140;
    private boolean mBack = false;
    private WebView mWebView;
    private PrefValue mPrefValue;
    private final String PREFKEY_FONTSIZE = "fontsize";
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
        mWebView.getSettings().setTextZoom(mFontSize);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (mBack && url.startsWith("file:")) {
                    mWebView.reload();
                    mBack = false;
                }
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, String url) {
                if (url.startsWith("file:")) {
                    return false;
                }
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return true;
            }
        });
        setButtonListener();

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
                Log.d("WebViewAcitivity", "Load error : "+url);
            }
        }

        String html = "tcja.ja.html";
        if (!BuildConfig.APPLICATION_ID.equals("jp.kcm.thumbctrl")) html = "tcen.ja.html";
        mWebView.loadUrl("file:///android_asset/"+html);
    }

    private void setButtonListener() {
        findViewById(R.id.webview_back).setOnClickListener(mButtonListener);
        findViewById(R.id.webview_forward).setOnClickListener(mButtonListener);
        findViewById(R.id.webview_reload).setOnClickListener(mButtonListener);
        findViewById(R.id.webview_abort).setOnClickListener(mButtonListener);
        findViewById(R.id.webview_quit).setOnClickListener(mButtonListener);
        findViewById(R.id.webview_plus).setOnClickListener(mButtonListener);
        findViewById(R.id.webview_minus).setOnClickListener(mButtonListener);
    }

    View.OnClickListener mButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.webview_back:
                    if (mWebView.canGoBack()){
                        mBack = true;
                        mWebView.goBack();
                    } else {
                        mWebView.stopLoading();
                        finish();
                    }
                    break;
                case R.id.webview_forward:
                    if (mWebView.canGoForward()){
                        mWebView.goForward();
                    }
                    break;
                case R.id.webview_reload:
                    mWebView.reload();
                    break;
                case R.id.webview_abort:
                    mWebView.stopLoading();
                    break;
                case R.id.webview_quit:
                    mWebView.stopLoading();
                    finish();
                    break;
                case R.id.webview_plus:
                    mWebView.getSettings().setTextZoom(mWebView.getSettings().getTextZoom() + 10);
                    mFontSize = mWebView.getSettings().getTextZoom();
                    mPrefValue.setInt(PREFKEY_FONTSIZE, mFontSize);
                    break;
                case R.id.webview_minus:
                    mWebView.getSettings().setTextZoom(mWebView.getSettings().getTextZoom() - 10);
                    mFontSize = mWebView.getSettings().getTextZoom();
                    mPrefValue.setInt(PREFKEY_FONTSIZE, mFontSize);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            if (mWebView.canGoBack()){
                mBack = true;
                mWebView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean load(WebView webView, String url, String prev) {
        if (url.matches("https?://.*")) {
            webView.loadUrl(url);
            return true;
        }
        try {
            File html = new File (url);
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(html),"UTF-8"));
            StringBuilder buffer = new StringBuilder();
            String str;
            while ((str = reader.readLine()) != null) {
                buffer.append(str);
                buffer.append("\n");
            }
            String data = buffer.toString();
            webView.loadDataWithBaseURL("file://"+url, data, "text/html", "UTF-8", prev);
            return true;
        } catch (Exception e) {
            Log.d("WebViewAcitivity", e.getMessage());
        }
        return false;
    }
}
