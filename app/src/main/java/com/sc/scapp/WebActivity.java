package com.sc.scapp;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Debug;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class WebActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            Toast.makeText(this, "ERROR: actionBar is null", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.mipmap.ic_x);
        actionBar.setDisplayShowTitleEnabled(false);

        final EditText urlEditText = findViewById(R.id.url_edit_text);
        webView = findViewById(R.id.web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                urlEditText.setText(url);
            }

            @Override // 这个也必须要，否则有时url不能正确显示
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                urlEditText.setText(url);
            }
        });

        Uri uri = getIntent().getData();
        if (uri == null)
            webView.loadUrl("https://www.baidu.com/");
        else
            webView.loadUrl(uri.toString());

        final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (inputMethodManager == null) {
            Toast.makeText(this, "ERROR: inputMethodManager is null", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // 点击WebView时隐藏输入法并取消对urlEditText的焦距
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                inputMethodManager.hideSoftInputFromWindow(urlEditText.getApplicationWindowToken(), 0);
                urlEditText.clearFocus();
                return false;
            }
        });
        // 点击输入法上的确认时隐藏输入法并跳转网页
        urlEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                inputMethodManager.hideSoftInputFromWindow(textView.getApplicationWindowToken(), 0);
                webView.loadUrl(urlEditText.getText().toString());
                return false;
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                WebActivity.this.url = url;
                WebActivity.this.contentLength = contentLength;

                if (ContextCompat.checkSelfPermission(WebActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(WebActivity.this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1);
                } else {
                    download();
                }

                webView.goBack();
            }
        });
    }

    private String url;
    private long contentLength;
    private void download() {
        Intent intent = new Intent(this, DownloadService.class);
        startService(intent);
        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                DownloadService.DownloadBinder downloadBinder = (DownloadService.DownloadBinder) service;
                downloadBinder.startDownload(url, contentLength);
                unbindService(this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        }, BIND_AUTO_CREATE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    download();
                } else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_web, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // 左上角的X退出当前活动
                finish();
                break;
            case R.id.refresh: // 刷新
                webView.reload();
                break;
            case R.id.go_back: // 后退
                webView.goBack();
                break;
            case R.id.go_forward: // 前进
                webView.goForward();
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }

    public static void actionStart(Context context) {
        Intent intent = new Intent(context, WebActivity.class);
        context.startActivity(intent);
    }
}
