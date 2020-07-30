package com.sc.scapp;

public interface DownloadListener {
    void onProgress(String url, long downloadedLength, long contentLength);
    void onSuccess(String url);
    void onFailed(String url);
    void onPaused(String url);
    void onCanceled(String url);
}
