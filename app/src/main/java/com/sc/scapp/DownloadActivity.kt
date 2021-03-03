package com.sc.scapp

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sc.scapp.DownloadService.DownloadBinder
import java.util.*

class DownloadActivity : AppCompatActivity() {
    private val downloadStateList = ArrayList<DownloadState>()
    private lateinit var downloadAdapter: DownloadAdapter
    private lateinit var downloadBinder: DownloadBinder
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            downloadBinder = service as DownloadBinder
            downloadBinder.setDownloadListener(downloadListener)
            val pref = getSharedPreferences(DownloadService.DOWNLOAD_SHARED_PREFERENCE_NAME, MODE_PRIVATE)
            val prefMap = pref.all
            for (url in prefMap.keys) {
                val value = prefMap[url] as String?
                val downloadState = DownloadState.fromString(value)
                if (downloadState.state == DownloadState.DOWNLOADING && !downloadBinder.isDownloading(url)) {
                    downloadState.state = DownloadState.DOWNLOAD_PAUSED
                }
                downloadState.url = url
                downloadStateList.add(downloadState)
            }
            downloadStateList.sort()
            downloadAdapter.notifyDataSetChanged()
            downloadAdapter.setDownloadBinder(downloadBinder)
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }
    private val downloadListener: DownloadListener = object : DownloadListener {
        override fun onProgress(url: String, downloadedLength: Long, contentLength: Long) {
            for (i in downloadStateList.indices) {
                val downloadState = downloadStateList[i]
                if (downloadState.url == url) {
                    downloadState.downloadedLength = downloadedLength
                    downloadState.contentLength = contentLength
                    downloadState.state = DownloadState.DOWNLOADING
                    downloadAdapter.notifyItemChanged(i)
                    break
                }
            }
        }

        override fun onSuccess(url: String) {
            for (i in downloadStateList.indices) {
                val downloadState = downloadStateList[i]
                if (downloadState.url == url) {
                    downloadState.state = DownloadState.DOWNLOAD_SUCCESS
                    downloadAdapter.notifyItemChanged(i)
                    break
                }
            }
        }

        override fun onFailed(url: String) {
            for (i in downloadStateList.indices) {
                val downloadState = downloadStateList[i]
                if (downloadState.url == url) {
                    downloadState.state = DownloadState.DOWNLOAD_SUCCESS
                    downloadAdapter.notifyItemChanged(i)
                    break
                }
            }
        }

        override fun onPaused(url: String) {
            for (i in downloadStateList.indices) {
                val downloadState = downloadStateList[i]
                if (downloadState.url == url) {
                    downloadState.state = DownloadState.DOWNLOAD_PAUSED
                    downloadAdapter.notifyItemChanged(i)
                    break
                }
            }
        }

        override fun onCanceled(url: String) {
            for (i in downloadStateList.indices) {
                val downloadState = downloadStateList[i]
                if (downloadState.url == url) {
                    //downloadState.setState(DownloadState.DOWNLOAD_CANCELED);
                    //downloadAdapter.notifyItemChanged(i);
                    downloadStateList.removeAt(i)
                    downloadAdapter.notifyItemRemoved(i)
                    break
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this@DownloadActivity)
        downloadAdapter = DownloadAdapter(downloadStateList)
        recyclerView.adapter = downloadAdapter
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        val bindIntent = Intent(this, DownloadService::class.java)
        bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadBinder.setDownloadListener(null)
        unbindService(serviceConnection)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

    companion object {
        @JvmStatic
        fun actionStart(context: Context) {
            val intent = Intent(context, DownloadActivity::class.java)
            context.startActivity(intent)
        }
    }
}