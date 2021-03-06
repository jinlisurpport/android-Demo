package com.example.alex.guoanh5game;

import android.Manifest;
import android.app.ActionBar;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;

import static com.example.alex.guoanh5game.WebViewProvider.buildUserAgentString;


public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";
    private WebView gameWebView;
    private static MainActivity instance;
    private DownloadManager mManager;
    private static MyDownloadListener downloadListener;

    public void findViews() {
        mManager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
        gameWebView = (WebView) findViewById(R.id.game_webView);
//		String urlString = "file:///android_asset/index.html";
        String urlString = "https://jlpay.jinlisoft.kr/";
//        urlString = "http://m.y.qq.com";
        Log.e(TAG, "yx访问的url地址：" + urlString);
        gameWebView.loadUrl(urlString);

        WebSettings webSettings = gameWebView.getSettings();
        configureDefaultSettings(this,webSettings);

//        gameWebView.setWebChromeClient();
        gameWebView.loadUrl(urlString);
        gameWebView.requestFocus();
        gameWebView.setWebViewClient(new FocusWebViewClient(this));
        downloadListener = createDownloadListener();
        gameWebView.setDownloadListener(downloadListener);

    }


    public  static MyDownloadListener  getDownLoad(){
        return downloadListener;
    }
    private static void configureDefaultSettings(Context context, WebSettings settings) {
        settings.setJavaScriptEnabled(true);
        settings.setBlockNetworkImage(false);
        // Needs to be enabled to display some HTML5 sites that use local storage
        settings.setDomStorageEnabled(true);

        // Enabling built in zooming shows the controls by default
        settings.setBuiltInZoomControls(true);

        // So we hide the controls after enabling zooming
        settings.setDisplayZoomControls(false);

        // To respect the html viewport:
        settings.setLoadWithOverviewMode(true);

        // Also increase text size to fill the viewport (this mirrors the behaviour of Firefox,
        // Chrome does this in the current Chrome Dev, but not Chrome release).
        if (Build.VERSION.SDK_INT >= 19) {
            settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        }

        // Disable access to arbitrary local files by webpages - assets can still be loaded
        // via file:///android_asset/res, so at least error page images won't be blocked.
        settings.setAllowFileAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);

        final String appName = context.getResources().getString(R.string.useragent_appname);
        settings.setUserAgentString(buildUserAgentString(context, settings, appName));

        // Right now I do not know why we should allow loading content from a content provider
        settings.setAllowContentAccess(false);

        // The default for those settings should be "false" - But we want to be explicit.
        settings.setAppCacheEnabled(false);
        settings.setDatabaseEnabled(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);

        // We do not implement the callbacks - So let's disable it.
        settings.setGeolocationEnabled(false);

        // We do not want to save any data...
        settings.setSaveFormData(false);
        //noinspection deprecation - This method is deprecated but let's call it in case WebView implementations still obey it.
        settings.setSavePassword(false);
    }
    @Override
    protected void onDestroy() {
        gameWebView.removeAllViews();
        gameWebView.destroy();
        if(receiver!=null){
            unregisterReceiver(receiver);
        }
        super.onDestroy();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
        findViews();
        initData();
    }

    private void initData() {
        IntentFilter filter = new IntentFilter( DownloadManager.ACTION_DOWNLOAD_COMPLETE ) ;
        registerReceiver( receiver , filter ) ;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && gameWebView.canGoBack()) {
            gameWebView.goBack();// 返回前一个页面
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    private String mUrl;

    class  MyDownloadListener implements DownloadListener{
            public void onDownloadStart(String url) {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                if(url.contains("wxp")){
                    request.setTitle("微信二维码");
                    request.setDescription("微信二维码正在下载");
                }else {
                    request.setTitle("支付宝二维码");
                    request.setDescription("支付宝二维码正在下载");
                }
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setAllowedOverRoaming(false);
                mManager.enqueue(request);

            }
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                final String scheme = Uri.parse(url).getScheme();
                Log.w(TAG, ":createDownloadListener" + url);

                if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
                    // We are ignoring everything that is not http or https. This is a limitation of
                    // Android's download manager. There's no reason to show a download dialog for
                    // something we can't download anyways.
                    Log.w(TAG, "Ignoring download from non http(s) URL: " + url);
                    return;
                }

                final Download download = new Download(url, userAgent, contentDisposition, mimetype, contentLength, Environment.DIRECTORY_DOWNLOADS);
                if (Build.VERSION.SDK_INT <= 23) {
                    queueDownload(download,url);
                    return;
                }

                if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Long press image displays its own dialog and we handle other download cases here

                    // Download dialog has already been shown from long press on image. Proceed with download.
                    queueDownload(download,url);
                } else {
                    // We do not have the permission to write to the external storage. Request the permission and start the
                    // download from onRequestPermissionsResult().

                    mUrl = url;
                    pendingDownload = download;
                    if (Build.VERSION.SDK_INT >23) {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
                    }

                }

            }
    }
    private MyDownloadListener createDownloadListener() {

        return new MyDownloadListener();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_CODE_STORAGE_PERMISSION|| pendingDownload == null || mUrl == null) {
            return;
        }

        if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            // We didn't get the storage permission: We are not able to start this download.
            pendingDownload = null;
        }
        queueDownload(pendingDownload,mUrl);
        pendingDownload =null;
        mUrl = null;

        // The actual download dialog will be shown from onResume(). If this activity/fragment is
        // getting restored then we need to 'resume' first before we can show a dialog (attaching
        // another fragment).
    }
    private static int REQUEST_CODE_STORAGE_PERMISSION = 101;

    Download pendingDownload;
    public boolean isDownloadFromLongPressImage(Download download){
        return download.getDestinationDirectory().equals(Environment.DIRECTORY_PICTURES);
    }

    public static final String FRAGMENT_TAG = "should-download-prompt-dialog";
    private void queueDownload(Download download, String url) {
        if (download == null) {
            return;
        }

        final String cookie = CookieManager.getInstance().getCookie(download.getUrl());
        final String fileName = DownloadUtils.guessFileName(download);

        final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(download.getUrl()))
                .addRequestHeader("User-Agent", download.getUserAgent())
                .addRequestHeader("Cookie", cookie)
                .addRequestHeader("Referer", url)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setMimeType(download.getMimeType());

        try {
            request.setDestinationInExternalPublicDir(
                    download.getDestinationDirectory(), fileName);
        } catch (IllegalStateException e) {
            Log.e(FRAGMENT_TAG, "Cannot create download directory");
            return;
        }

        request.allowScanningByMediaScanner();

        try {
//            setNotification(request);
            long downloadReference = mManager.enqueue(request);
//            downloadBroadcastReceiver.addQueuedDownload(downloadReference);
        } catch (RuntimeException e) {
            Log.e(FRAGMENT_TAG, "Download failed: " + e);
        }
    }
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction() ;
            if( action.equals( DownloadManager.ACTION_DOWNLOAD_COMPLETE  )){
                long  reference = intent.getLongExtra( DownloadManager.EXTRA_DOWNLOAD_ID , -1 );
                MyToast.show(MainActivity.this, "二維碼圖片下载完成了", true);

            }

            if( action.equals( DownloadManager.ACTION_NOTIFICATION_CLICKED )){
            }
        }
    };
}
