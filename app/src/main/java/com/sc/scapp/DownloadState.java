package com.sc.scapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DownloadState implements Comparable<DownloadState> {

    public static final int DOWNLOAD_SUCCESS = 0;
    public static final int DOWNLOAD_FAILED = 1;
    public static final int DOWNLOAD_PAUSED = 2;
    public static final int DOWNLOAD_CANCELED = 3;

    public static final int DOWNLOADING = 4;

    private long downloadedLength; // 已下载大小
    private long contentLength; // 总大小
    private int state; // 状态
    private long time; // 时间

    private String url; // 下载的地址，不参与存储！

    // 不包括url
    public DownloadState(long downloadedLength, long contentLength, int state, long time) {
        this.downloadedLength = downloadedLength;
        this.contentLength = contentLength;
        this.state = state;
        this.time = time;
    }

    public long getDownloadedLength() {
        return downloadedLength;
    }

    public void setDownloadedLength(long downloadedLength) {
        this.downloadedLength = downloadedLength;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // 不包括url
    @Override
    public String toString() {
        return downloadedLength + " " + contentLength + " " + state + " " + time;
    }

    // 不包括url
    @NonNull
    public static DownloadState fromString(@Nullable String string) {
        try {
            String[] strings = string.split(" ");
            return new DownloadState(Long.parseLong(strings[0]), Long.parseLong(strings[1]), Integer.parseInt(strings[2]), Long.parseLong(strings[3]));
        } catch (Exception e) {
            return new DownloadState(-1, -1, -1, System.currentTimeMillis());
        }
    }

    @Override
    public int compareTo(@NonNull DownloadState that) {
        return Long.compare(that.time, this.time);
    }
}
