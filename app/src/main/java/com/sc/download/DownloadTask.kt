package com.sc.download

import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile

class DownloadTask(private val listener: DownloadListener) : AsyncTask<String?, Long?, Int>() {
    private var downloadUrl: String? = null
    var downloadedLength: Long = 0
        private set
    var contentLength: Long = 0
        private set
    private var isCanceled = false
    private var deleteFile = false
    private var isPaused = false
    override fun doInBackground(vararg strings: String?): Int {
        downloadUrl = strings[0]
        var `is`: InputStream? = null
        var raf: RandomAccessFile? = null
        var file: File? = null
        try {
            downloadedLength = 0
            val fileName = downloadUrl!!.substring(downloadUrl!!.lastIndexOf("/"))
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            file = File(directory, fileName)
            if (file.exists()) {
                downloadedLength = file.length()
            }
            contentLength = strings[1]!!.toLong()
            if (contentLength <= 0) contentLength = contentLengthFromUrl
            if (contentLength <= 0) {
                return DownloadState.DOWNLOAD_FAILED
            } else if (contentLength == downloadedLength) {
                return DownloadState.DOWNLOAD_SUCCESS
            }
            publishProgress(downloadedLength, contentLength)
            val client = OkHttpClient()
            val request = Request.Builder()
                    .addHeader("RANGE", "bytes=$downloadedLength-")
                    .url(downloadUrl)
                    .build()
            val response = client.newCall(request).execute()
            if (response != null) {
                `is` = response.body()!!.byteStream()
                raf = RandomAccessFile(file, "rw")
                raf.seek(downloadedLength)
                val bytes = ByteArray(1024)
                var len: Int
                var lastProgress = 0
                var lastTime = System.currentTimeMillis()
                while (`is`.read(bytes).also { len = it } != -1) {
                    downloadedLength += len.toLong()
                    raf.write(bytes, 0, len)
                    val progress = (downloadedLength * 100 / contentLength).toInt()
                    val time = System.currentTimeMillis()
                    if (progress > lastProgress || time > lastTime + 1000) {
                        lastProgress = progress
                        lastTime = time
                        publishProgress(downloadedLength, contentLength)
                    }
                    if (isCanceled) {
                        return DownloadState.DOWNLOAD_CANCELED
                    } else if (isPaused) {
                        return DownloadState.DOWNLOAD_PAUSED
                    }
                }
                response.body()!!.close()
                return DownloadState.DOWNLOAD_SUCCESS
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                `is`?.close()
                raf?.close()
                if (isCanceled && deleteFile && file != null) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return DownloadState.DOWNLOAD_FAILED
    }

    override fun onProgressUpdate(vararg values: Long?) {
        listener.onProgress(downloadUrl!!, values[0]!!, values[1]!!)
    }

    override fun onPostExecute(integer: Int) {
        when (integer) {
            DownloadState.DOWNLOAD_SUCCESS -> listener.onSuccess(downloadUrl!!)
            DownloadState.DOWNLOAD_FAILED -> listener.onFailed(downloadUrl!!)
            DownloadState.DOWNLOAD_PAUSED -> listener.onPaused(downloadUrl!!)
            DownloadState.DOWNLOAD_CANCELED -> listener.onCanceled(downloadUrl!!)
        }
    }

    fun pauseDownload() {
        isPaused = true
    }

    fun cancelDownload(deleteFile: Boolean) {
        isCanceled = true
        this.deleteFile = deleteFile
    }

    @get:Throws(IOException::class)
    private val contentLengthFromUrl: Long
        get() {
            val client = OkHttpClient()
            val request = Request.Builder()
                    .url(downloadUrl)
                    .build()
            val response = client.newCall(request).execute()
            if (response != null && response.isSuccessful) {
                val contentLength = response.body()!!.contentLength()
                response.close()
                return contentLength
            }
            return -1
        }
}