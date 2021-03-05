package com.sc.download

import android.app.*
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.sc.scapp.R
import java.io.File
import java.util.*

class DownloadService : Service() {

    private val urlDownloadTaskMap = HashMap<String, DownloadTask>()

    private val listener: DownloadListener = object : DownloadListener {
        override fun onProgress(url: String, downloadedLength: Long, contentLength: Long) {
            val progress = (downloadedLength * 100 / contentLength).toInt()
            val fileName = url.substring(url.lastIndexOf("/") + 1)
            notificationManager.notify(url.hashCode(), getNotification(fileName + " " + getString(R.string.downloading), null, progress))
            val downloadState = DownloadState(downloadedLength, contentLength, DownloadState.DOWNLOADING, -1)
            downloadBinder.onDownloadStateChange(downloadState, url)
        }

        override fun onSuccess(url: String) {
            val downloadState = saveStateAndEndTask(url, DownloadState.DOWNLOAD_SUCCESS)
            downloadBinder.onDownloadStateChange(downloadState, url)
            val fileName = url.substring(url.lastIndexOf("/") + 1)
            notificationManager.notify(url.hashCode(), getNotification(getString(R.string.download_success) + " - " + fileName, null, -1))
            Toast.makeText(this@DownloadService, getString(R.string.download_success) + " - " + fileName, Toast.LENGTH_SHORT).show()
        }

        override fun onFailed(url: String) {
            val downloadState = saveStateAndEndTask(url, DownloadState.DOWNLOAD_FAILED)
            downloadBinder.onDownloadStateChange(downloadState, url)
            val fileName = url.substring(url.lastIndexOf("/") + 1)
            notificationManager.notify(url.hashCode(), getNotification(getString(R.string.download_failed) + " - " + fileName, null, -1))
            Toast.makeText(this@DownloadService, getString(R.string.download_failed) + " - " + fileName, Toast.LENGTH_SHORT).show()
        }

        override fun onPaused(url: String) {
            val downloadState = saveStateAndEndTask(url, DownloadState.DOWNLOAD_PAUSED)
            downloadBinder.onDownloadStateChange(downloadState, url)
            val fileName = url.substring(url.lastIndexOf("/") + 1)
            notificationManager.notify(url.hashCode(), getNotification(getString(R.string.download_paused) + " - " + fileName, null, -1))
            Toast.makeText(this@DownloadService, getString(R.string.download_paused) + " - " + fileName, Toast.LENGTH_SHORT).show()
        }

        override fun onCanceled(url: String) {
            val downloadState = saveStateAndEndTask(url, DownloadState.DOWNLOAD_CANCELED)
            downloadBinder.onDownloadStateChange(downloadState, url)
            val fileName = url.substring(url.lastIndexOf("/") + 1)
            notificationManager.notify(url.hashCode(), getNotification(getString(R.string.download_canceled) + " - " + fileName, null, -1))
            Toast.makeText(this@DownloadService, getString(R.string.download_canceled) + " - " + fileName, Toast.LENGTH_SHORT).show()
        }

        private fun saveStateAndEndTask(url: String, state: Int): DownloadState {
            val downloadState = DownloadState.fromString(pref.getString(url, null))
            val downloadTask = urlDownloadTaskMap[url]
            if (downloadTask != null) {
                downloadState.downloadedLength = downloadTask.downloadedLength
                downloadState.contentLength = downloadTask.contentLength
            }
            downloadState.state = state
            if (state == DownloadState.DOWNLOAD_CANCELED) {
                prefEditor.remove(url)
            } else {
                prefEditor.putString(url, downloadState.toString())
            }
            prefEditor.apply()
            synchronized(urlDownloadTaskMap) {
                urlDownloadTaskMap.remove(url)
                if (urlDownloadTaskMap.isEmpty()) {
                    stopForeground(true)
                    stopSelf()
                }
            }
            return downloadState
        }
    }

    private val downloadBinder = DownloadBinder()

    private lateinit var pref: SharedPreferences
    private lateinit var prefEditor: SharedPreferences.Editor

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        pref = getSharedPreferences(DOWNLOAD_SHARED_PREFERENCE_NAME, MODE_PRIVATE)
        prefEditor = pref.edit()

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel("Download", "Download", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        synchronized(urlDownloadTaskMap) { if (!urlDownloadTaskMap.isEmpty()) Log.e(javaClass.name, "严重错误：下载服务在还存在下载任务的情况下被销毁！！！") }
    }

    override fun onBind(intent: Intent): IBinder? {
        return downloadBinder
    }

    inner class DownloadBinder : Binder() {
        private var downloadListener: DownloadListener? = null
        fun setDownloadListener(downloadListener: DownloadListener?) {
            this.downloadListener = downloadListener
        }

        fun onDownloadStateChange(downloadState: DownloadState, url: String?) {
            if (downloadListener == null) {
                return
            }
            when (downloadState.state) {
                DownloadState.DOWNLOAD_SUCCESS -> downloadListener!!.onSuccess(url!!)
                DownloadState.DOWNLOAD_FAILED -> downloadListener!!.onFailed(url!!)
                DownloadState.DOWNLOAD_PAUSED -> downloadListener!!.onPaused(url!!)
                DownloadState.DOWNLOAD_CANCELED -> downloadListener!!.onCanceled(url!!)
                DownloadState.DOWNLOADING -> downloadListener!!.onProgress(url!!, downloadState.downloadedLength, downloadState.contentLength)
            }
        }

        @JvmOverloads
        fun startDownload(url: String, contentLength: Long = -1) {
            var downloadTask: DownloadTask
            synchronized(urlDownloadTaskMap) {
                if (urlDownloadTaskMap.containsKey(url)) {
                    return
                } else if (urlDownloadTaskMap.isEmpty()) {
                    startForeground(DOWNLOAD_FOREGROUND_SERVICE_NOTIFICATION_ID, getNotification(getString(R.string.download_task_is_running), null, -1))
                }
                downloadTask = DownloadTask(listener)
                urlDownloadTaskMap.put(url, downloadTask)
            }
            val downloadStateString = pref.getString(url, null)
            if (downloadStateString == null) {
                prefEditor.putString(url, DownloadState(0, contentLength, DownloadState.DOWNLOADING, System.currentTimeMillis()).toString())
            } else {
                val downloadState = DownloadState.fromString(downloadStateString)
                downloadState.state = DownloadState.DOWNLOADING
                prefEditor.putString(url, downloadState.toString())
            }
            prefEditor.apply()
            downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url, java.lang.Long.toString(contentLength))
            val fileName = url.substring(url.lastIndexOf("/") + 1)
            notificationManager.notify(url.hashCode(), getNotification(fileName + " " + getString(R.string.downloading), null, 0))
            Toast.makeText(this@DownloadService, fileName + " " + getString(R.string.downloading), Toast.LENGTH_SHORT).show()
        }

        fun pauseDownload(url: String) {
            synchronized(urlDownloadTaskMap) {
                if (urlDownloadTaskMap.containsKey(url)) {
                    urlDownloadTaskMap[url]!!.pauseDownload()
                }
            }
        }

        fun cancelDownload(url: String, deleteFile: Boolean) {
            synchronized(urlDownloadTaskMap) {
                if (urlDownloadTaskMap.containsKey(url)) {
                    urlDownloadTaskMap[url]!!.cancelDownload(deleteFile)
                    return
                }
            }

            // 如果代码可以执行到这里，说明不能通过downloadTask去取消下载任务，所以这里需要补上取消下载任务的代码。
            // 取消下载任务包括：看情况删除文件；删除pref保存的任务；downloaBinder回调删除任务事件。
            if (deleteFile) {
                val fileName = url.substring(url.lastIndexOf("/"))
                val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
                val file = File(directory + fileName)
                file.delete()
            }
            val downloadState = DownloadState.fromString(pref.getString(url, null))
            downloadState.state = DownloadState.DOWNLOAD_CANCELED
            prefEditor.remove(url)
            prefEditor.apply()
            downloadBinder.onDownloadStateChange(downloadState, url)
        }

        fun redownload(url: String) {
            val fileName = url.substring(url.lastIndexOf("/"))
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
            val file = File(directory + fileName)
            file.delete()
            startDownload(url)
        }

        fun isDownloading(url: String): Boolean {
            synchronized(urlDownloadTaskMap) { return urlDownloadTaskMap.containsKey(url) }
        }
    }

    private fun getNotification(title: String, text: String?, progress: Int): Notification {
        val intent = Intent(this, DownloadActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, 0)
        return Notification.Builder(this, "Download").run {
            setSmallIcon(R.drawable.ic_download)
            setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_download))
            setContentIntent(pi)
            setContentTitle(title)
            setAutoCancel(true)
            if (text != null) {
                setContentText(text)
            } else if (progress >= 0) {
                setContentText("$progress%")
                setProgress(100, progress, false)
            }
            build()
        }
    }

    companion object {
        const val DOWNLOAD_SHARED_PREFERENCE_NAME = "Download shared preference" // 键是url，值是"已下载大小 总大小 状态"（由类DownloadState处理）
        private const val DOWNLOAD_FOREGROUND_SERVICE_NOTIFICATION_ID = 1
    }
}