package com.sc.download

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sc.download.DownloadService.DownloadBinder
import com.sc.scapp.R
import java.util.*

class DownloadActivity : AppCompatActivity() {
    private val mDownloadStateList = ArrayList<DownloadState>()
    private lateinit var mDownloadAdapter: DownloadAdapter
    private lateinit var mDownloadBinder: DownloadBinder

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mDownloadBinder = service as DownloadBinder
            mDownloadBinder.setDownloadListener(mDownloadListener)
            val pref = getSharedPreferences(DownloadService.DOWNLOAD_SHARED_PREFERENCE_NAME, MODE_PRIVATE)
            val prefMap = pref.all
            for (url in prefMap.keys) {
                val value = prefMap[url] as String?
                val downloadState = DownloadState.fromString(value)
                if (downloadState.mState == DownloadState.DOWNLOADING && !mDownloadBinder.isDownloading(url)) {
                    downloadState.mState = DownloadState.DOWNLOAD_PAUSED
                }
                downloadState.mUrl = url
                mDownloadStateList.add(downloadState)
            }
            mDownloadStateList.sort()
            mDownloadAdapter.notifyDataSetChanged()
            mDownloadAdapter.setDownloadBinder(mDownloadBinder)
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }

    private val mDownloadListener = object : DownloadListener {
        override fun onProgress(url: String, downloadedLength: Long, contentLength: Long) {
            for (i in mDownloadStateList.indices) {
                val downloadState = mDownloadStateList[i]
                if (downloadState.mUrl == url) {
                    downloadState.mDownloadedLength = downloadedLength
                    downloadState.mContentLength = contentLength
                    downloadState.mState = DownloadState.DOWNLOADING
                    mDownloadAdapter.notifyItemChanged(i)
                    break
                }
            }
        }

        override fun onSuccess(url: String) {
            for (i in mDownloadStateList.indices) {
                val downloadState = mDownloadStateList[i]
                if (downloadState.mUrl == url) {
                    downloadState.mState = DownloadState.DOWNLOAD_SUCCESS
                    mDownloadAdapter.notifyItemChanged(i)
                    break
                }
            }
        }

        override fun onFailed(url: String) {
            for (i in mDownloadStateList.indices) {
                val downloadState = mDownloadStateList[i]
                if (downloadState.mUrl == url) {
                    downloadState.mState = DownloadState.DOWNLOAD_SUCCESS
                    mDownloadAdapter.notifyItemChanged(i)
                    break
                }
            }
        }

        override fun onPaused(url: String) {
            for (i in mDownloadStateList.indices) {
                val downloadState = mDownloadStateList[i]
                if (downloadState.mUrl == url) {
                    downloadState.mState = DownloadState.DOWNLOAD_PAUSED
                    mDownloadAdapter.notifyItemChanged(i)
                    break
                }
            }
        }

        override fun onCanceled(url: String) {
            for (i in mDownloadStateList.indices) {
                val downloadState = mDownloadStateList[i]
                if (downloadState.mUrl == url) {
                    //downloadState.setState(DownloadState.DOWNLOAD_CANCELED);
                    //downloadAdapter.notifyItemChanged(i);
                    mDownloadStateList.removeAt(i)
                    mDownloadAdapter.notifyItemRemoved(i)
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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this@DownloadActivity)
        mDownloadAdapter = DownloadAdapter(mDownloadStateList)
        recyclerView.adapter = mDownloadAdapter
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        val bindIntent = Intent(this, DownloadService::class.java)
        bindService(bindIntent, mServiceConnection, BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        mDownloadBinder.setDownloadListener(null)
        unbindService(mServiceConnection)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

    companion object {
        fun actionStart(context: Context) {
            val intent = Intent(context, DownloadActivity::class.java)
            context.startActivity(intent)
        }
    }
}