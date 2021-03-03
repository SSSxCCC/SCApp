package com.sc.scapp

interface DownloadListener {
    fun onProgress(url: String, downloadedLength: Long, contentLength: Long)
    fun onSuccess(url: String)
    fun onFailed(url: String)
    fun onPaused(url: String)
    fun onCanceled(url: String)
}