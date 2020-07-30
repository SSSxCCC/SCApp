package com.sc.scapp;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String, Long, Integer> {

    private DownloadListener listener;
    private String downloadUrl;

    private long downloadedLength;
    private long contentLength;

    private boolean isCanceled = false;
    private boolean deleteFile = false;
    private boolean isPaused = false;

    public DownloadTask(DownloadListener listener) {
        this.listener = listener;
    }

    @Override
    protected Integer doInBackground(String... strings) {
        downloadUrl = strings[0];

        InputStream is = null;
        RandomAccessFile raf = null;
        File file = null;
        try {
            downloadedLength = 0;
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            file = new File(directory + fileName);
            if (file.exists()) {
                downloadedLength = file.length();
            }

            contentLength = Long.parseLong(strings[1]);
            if (contentLength <= 0)
                contentLength = getContentLengthFromUrl();
            if (contentLength <= 0) {
                return DownloadState.DOWNLOAD_FAILED;
            } else if (contentLength == downloadedLength) {
                return DownloadState.DOWNLOAD_SUCCESS;
            }

            publishProgress(downloadedLength, contentLength);

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("RANGE", "bytes=" + downloadedLength + "-")
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if (response != null) {
                is = response.body().byteStream();
                raf = new RandomAccessFile(file, "rw");
                raf.seek(downloadedLength);
                byte[] bytes = new byte[1024];
                int len;
                int lastProgress = 0;
                long lastTime = System.currentTimeMillis();
                while ((len = is.read(bytes)) != -1) {
                    downloadedLength += len;
                    raf.write(bytes, 0, len);
                    int progress = (int) (downloadedLength * 100 / contentLength);
                    long time = System.currentTimeMillis();
                    if (progress > lastProgress || time > lastTime + 1000) {
                        lastProgress = progress;
                        lastTime = time;
                        publishProgress(downloadedLength, contentLength);
                    }

                    if (isCanceled) {
                        return DownloadState.DOWNLOAD_CANCELED;
                    } else if (isPaused) {
                        return DownloadState.DOWNLOAD_PAUSED;
                    }
                }
                response.body().close();
                return DownloadState.DOWNLOAD_SUCCESS;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (raf != null) {
                    raf.close();
                }
                if (isCanceled && deleteFile && file != null) {
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return DownloadState.DOWNLOAD_FAILED;
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        listener.onProgress(downloadUrl, values[0], values[1]);
    }

    @Override
    protected void onPostExecute(Integer integer) {
        switch (integer) {
            case DownloadState.DOWNLOAD_SUCCESS:
                listener.onSuccess(downloadUrl);
                break;
            case DownloadState.DOWNLOAD_FAILED:
                listener.onFailed(downloadUrl);
                break;
            case DownloadState.DOWNLOAD_PAUSED:
                listener.onPaused(downloadUrl);
                break;
            case DownloadState.DOWNLOAD_CANCELED:
                listener.onCanceled(downloadUrl);
                break;
        }
    }

    public void pauseDownload() {
        isPaused = true;
    }

    public void cancelDownload(boolean deleteFile) {
        isCanceled = true;
        this.deleteFile = deleteFile;
    }

    private long getContentLengthFromUrl() throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.close();
            return contentLength;
        }
        return -1;
    }

    public long getDownloadedLength() {
        return downloadedLength;
    }

    public long getContentLength() {
        return contentLength;
    }
}
