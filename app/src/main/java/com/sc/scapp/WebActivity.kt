package com.sc.scapp

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
import com.sc.scapp.DownloadService.DownloadBinder

class WebActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar == null) {
            Toast.makeText(this, "ERROR: actionBar is null", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.mipmap.ic_x)
        actionBar.setDisplayShowTitleEnabled(false)
        val urlEditText = findViewById<EditText>(R.id.url_edit_text)
        webView = findViewById(R.id.web_view)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                urlEditText.setText(url)
            }

            // 这个也必须要，否则有时url不能正确显示
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                urlEditText.setText(url)
            }
        }
        val uri = intent.data
        if (uri == null) webView.loadUrl("https://www.baidu.com/") else webView.loadUrl(uri.toString())
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        // 点击WebView时隐藏输入法并取消对urlEditText的焦距
        webView.setOnTouchListener { v, event ->
            inputMethodManager.hideSoftInputFromWindow(urlEditText.applicationWindowToken, 0)
            urlEditText.clearFocus()
            false
        }
        // 点击输入法上的确认时隐藏输入法并跳转网页
        urlEditText.setOnEditorActionListener { textView, i, keyEvent ->
            inputMethodManager.hideSoftInputFromWindow(textView.applicationWindowToken, 0)
            webView.loadUrl(urlEditText.text.toString())
            false
        }
        webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            this@WebActivity.url = url
            this@WebActivity.contentLength = contentLength
            if (ContextCompat.checkSelfPermission(this@WebActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@WebActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            } else {
                download()
            }
            webView.goBack()
        })
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

            override fun onServiceDisconnected(name: ComponentName) {}
        }, BIND_AUTO_CREATE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
            R.id.refresh -> webView.reload()
            R.id.go_back -> webView.goBack()
            R.id.go_forward -> webView.goForward()
        }
        return true
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            finish()
        }
    }

    companion object {
        fun actionStart(context: Context) {
            val intent = Intent(context, WebActivity::class.java)
            context.startActivity(intent)
        }
    }
}