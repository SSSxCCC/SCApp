package com.sc.download

import android.os.AsyncTask
import android.os.Environment
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile

class DownloadTask(private val listener: DownloadListener) : AsyncTask<String?, Long?, Int>() {
    private var mDownloadUrl: String? = null
    var mDownloadedLength: Long = 0
        private set
    var mContentLength: Long = 0
        private set
    private var mIsCanceled = false
    private var mDeleteFile = false
    private var mIsPaused = false

    override fun doInBackground(vararg strings: String?): Int {
        mDownloadUrl = strings[0]
        var `is`: InputStream? = null
        var raf: RandomAccessFile? = null
        var file: File? = null
        try {
            mDownloadedLength = 0
            val fileName = mDownloadUrl!!.substring(mDownloadUrl!!.lastIndexOf("/"))
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            file = File(directory, fileName)
            if (file.exists()) {
                mDownloadedLength = file.length()
            }
            mContentLength = strings[1]!!.toLong()
            if (mContentLength <= 0) mContentLength = contentLengthFromUrl
            if (mContentLength <= 0) {
                return DownloadState.DOWNLOAD_FAILED
            } else if (mContentLength == mDownloadedLength) {
                return DownloadState.DOWNLOAD_SUCCESS
            }
            publishProgress(mDownloadedLength, mContentLength)
            val client = OkHttpClient()
            val request = Request.Builder()
                    .addHeader("RANGE", "bytes=$mDownloadedLength-")
                    .url(mDownloadUrl)
                    .build()
            val response = client.newCall(request).execute()
            if (response != null) {
                `is` = response.body()!!.byteStream()
                raf = RandomAccessFile(file, "rw")
                raf.seek(mDownloadedLength)
                val bytes = ByteArray(1024)
                var len: Int
                var lastProgress = 0
                var lastTime = System.currentTimeMillis()
                while (`is`.read(bytes).also { len = it } != -1) {
                    mDownloadedLength += len.toLong()
                    raf.write(bytes, 0, len)
                    val progress = (mDownloadedLength * 100 / mContentLength).toInt()
                    val time = System.currentTimeMillis()
                    if (progress > lastProgress || time > lastTime + 1000) {
                        lastProgress = progress
                        lastTime = time
                        publishProgress(mDownloadedLength, mContentLength)
                    }
                    if (mIsCanceled) {
                        return DownloadState.DOWNLOAD_CANCELED
                    } else if (mIsPaused) {
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
                if (mIsCanceled && mDeleteFile && file != null) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return DownloadState.DOWNLOAD_FAILED
    }

    override fun onProgressUpdate(vararg values: Long?) {
        listener.onProgress(mDownloadUrl!!, values[0]!!, values[1]!!)
    }

    override fun onPostExecute(integer: Int) {
        when (integer) {
            DownloadState.DOWNLOAD_SUCCESS -> listener.onSuccess(mDownloadUrl!!)
            DownloadState.DOWNLOAD_FAILED -> listener.onFailed(mDownloadUrl!!)
            DownloadState.DOWNLOAD_PAUSED -> listener.onPaused(mDownloadUrl!!)
            DownloadState.DOWNLOAD_CANCELED -> listener.onCanceled(mDownloadUrl!!)
        }
    }

    fun pauseDownload() {
        mIsPaused = true
    }

    fun cancelDownload(deleteFile: Boolean) {
        mIsCanceled = true
        this.mDeleteFile = deleteFile
    }

    @get:Throws(IOException::class)
    private val contentLengthFromUrl: Long
        get() {
            val client = OkHttpClient()
            val request = Request.Builder()
                    .url(mDownloadUrl)
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