package com.sc.web

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.webkit.DownloadListener
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sc.download.DownloadService
import com.sc.download.DownloadService.DownloadBinder
import com.sc.scapp.R

class WebActivity : AppCompatActivity() {
    private lateinit var mWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.mipmap.ic_x)
            setDisplayShowTitleEnabled(false)
        }

        val urlEditText = findViewById<EditText>(R.id.url_edit_text)
        mWebView = findViewById(R.id.web_view)
        mWebView.settings.javaScriptEnabled = true
        mWebView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                urlEditText.setText(url)
            }
            override fun onPageFinished(view: WebView, url: String) {  // 这个也必须要，否则有时url不能正确显示
                super.onPageFinished(view, url)
                urlEditText.setText(url)
            }
        }

        val uri = intent.data
        if (uri == null) {  // 未指定url则默认进入百度页面
            mWebView.loadUrl("https://www.baidu.com/")
        } else {  // 进入指定的url页面
            mWebView.loadUrl(uri.toString())
        }

        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        // 点击WebView时隐藏输入法并取消对urlEditText的焦距
        mWebView.setOnTouchListener { v, event ->
            inputMethodManager.hideSoftInputFromWindow(urlEditText.applicationWindowToken, 0)
            urlEditText.clearFocus()
            false
        }
        // 点击输入法上的确认时隐藏输入法并跳转网页
        urlEditText.setOnEditorActionListener { textView, i, keyEvent ->
            inputMethodManager.hideSoftInputFromWindow(textView.applicationWindowToken, 0)
            mWebView.loadUrl(urlEditText.text.toString())
            false
        }
        // 处理点击下载文件的情况
        mWebView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            this@WebActivity.url = url
            this@WebActivity.contentLength = contentLength
            if (ContextCompat.checkSelfPermission(this@WebActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@WebActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_DOWNLAOD)
            } else {
                download()
            }
        }
    }

    private var url: String? = null
    private var contentLength: Long = 0

    private fun download() {
        val intent = Intent(this, DownloadService::class.java)
        startService(intent)
        bindService(intent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                val downloadBinder = service as DownloadBinder
                downloadBinder.startDownload(url!!, contentLength)
                unbindService(this)
            }
            override fun onServiceDisconnected(name: ComponentName) { }
        }, BIND_AUTO_CREATE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_DOWNLAOD -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                download()
            } else {
                Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_web, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.refresh -> mWebView.reload()
            R.id.go_back -> mWebView.goBack()
            R.id.go_forward -> mWebView.goForward()
        }
        return true
    }

    override fun onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack()
        } else {
            finish()
        }
    }

    companion object {

        private const val REQUEST_CODE_DOWNLAOD = 1

        fun actionStart(context: Context) {
            val intent = Intent(context, WebActivity::class.java)
            context.startActivity(intent)
        }
    }
}