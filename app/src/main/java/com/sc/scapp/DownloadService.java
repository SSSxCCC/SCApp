package com.sc.scapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;

public class DownloadService extends Service {

    public static final String DOWNLOAD_SHARED_PREFERENCE_NAME = "Download shared preference"; // 键是url，值是"已下载大小 总大小 状态"（由类DownloadState处理）
    private static final int DOWNLOAD_FOREGROUND_SERVICE_NOTIFICATION_ID = 1;

    private final HashMap<String, DownloadTask> urlDownloadTaskMap = new HashMap<>();
    private DownloadListener listener = new DownloadListener() {
        @Override
        public void onProgress(String url, long downloadedLength, long contentLength) {
            int progress = (int) (downloadedLength * 100 / contentLength);
            String fileName = url.substring(url.lastIndexOf("/") + 1);
            getNotificationManager().notify(url.hashCode(), getNotification(fileName + " " + getString(R.string.downloading), null, progress));

            DownloadState downloadState = new DownloadState(downloadedLength, contentLength, DownloadState.DOWNLOADING, -1);
            downloadBinder.onDownloadStateChange(downloadState, url);
        }

        @Override
        public void onSuccess(String url) {
            DownloadState downloadState = saveStateAndEndTask(url, DownloadState.DOWNLOAD_SUCCESS);
            downloadBinder.onDownloadStateChange(downloadState, url);

            String fileName = url.substring(url.lastIndexOf("/") + 1);
            getNotificationManager().notify(url.hashCode(), getNotification(getString(R.string.download_success) + " - " + fileName, null, -1));
            Toast.makeText(DownloadService.this, getString(R.string.download_success) + " - " + fileName, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(String url) {
            DownloadState downloadState = saveStateAndEndTask(url, DownloadState.DOWNLOAD_FAILED);
            downloadBinder.onDownloadStateChange(downloadState, url);

            String fileName = url.substring(url.lastIndexOf("/") + 1);
            getNotificationManager().notify(url.hashCode(), getNotification(getString(R.string.download_failed) + " - " + fileName, null, -1));
            Toast.makeText(DownloadService.this, getString(R.string.download_failed) + " - " + fileName, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPaused(String url) {
            DownloadState downloadState = saveStateAndEndTask(url, DownloadState.DOWNLOAD_PAUSED);
            downloadBinder.onDownloadStateChange(downloadState, url);

            String fileName = url.substring(url.lastIndexOf("/") + 1);
            getNotificationManager().notify(url.hashCode(), getNotification(getString(R.string.download_paused) + " - " + fileName, null, -1));
            Toast.makeText(DownloadService.this, getString(R.string.download_paused) + " - " + fileName, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCanceled(String url) {
            DownloadState downloadState = saveStateAndEndTask(url, DownloadState.DOWNLOAD_CANCELED);
            downloadBinder.onDownloadStateChange(downloadState, url);

            String fileName = url.substring(url.lastIndexOf("/") + 1);
            getNotificationManager().notify(url.hashCode(), getNotification(getString(R.string.download_canceled) + " - " + fileName, null, -1));
            Toast.makeText(DownloadService.this, getString(R.string.download_canceled) + " - " + fileName, Toast.LENGTH_SHORT).show();
        }

        private DownloadState saveStateAndEndTask(String url, int state) {
            DownloadState downloadState = DownloadState.fromString(pref.getString(url, null));
            DownloadTask downloadTask = urlDownloadTaskMap.get(url);
            if (downloadTask != null) {
                downloadState.setDownloadedLength(downloadTask.getDownloadedLength());
                downloadState.setContentLength(downloadTask.getContentLength());
            }
            downloadState.setState(state);
            if (state == DownloadState.DOWNLOAD_CANCELED) {
                prefEditor.remove(url);
            } else {
                prefEditor.putString(url, downloadState.toString());
            }
            prefEditor.apply();

            synchronized (urlDownloadTaskMap) {
                urlDownloadTaskMap.remove(url);
                if (urlDownloadTaskMap.isEmpty()) {
                    stopForeground(true);
                    stopSelf();
                }
            }

            return downloadState;
        }
    };
    private DownloadBinder downloadBinder = new DownloadBinder();
    private SharedPreferences pref;
    private SharedPreferences.Editor prefEditor;

    @Override
    public void onCreate() {
        super.onCreate();
        pref = getSharedPreferences(DOWNLOAD_SHARED_PREFERENCE_NAME, MODE_PRIVATE);
        prefEditor = pref.edit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        synchronized (urlDownloadTaskMap) {
            if (!urlDownloadTaskMap.isEmpty())
                Log.e(getClass().getName(), "严重错误：下载服务在还存在下载任务的情况下被销毁！！！");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return downloadBinder;
    }

    class DownloadBinder extends Binder {

        private DownloadListener downloadListener;

        public void setDownloadListener(DownloadListener downloadListener) {
            this.downloadListener = downloadListener;
        }

        public void onDownloadStateChange(DownloadState downloadState, String url) {
            if (downloadListener == null) {
                return;
            }
            switch (downloadState.getState()) {
                case DownloadState.DOWNLOAD_SUCCESS:
                    downloadListener.onSuccess(url);
                    break;
                case DownloadState.DOWNLOAD_FAILED:
                    downloadListener.onFailed(url);
                    break;
                case DownloadState.DOWNLOAD_PAUSED:
                    downloadListener.onPaused(url);
                    break;
                case DownloadState.DOWNLOAD_CANCELED:
                    downloadListener.onCanceled(url);
                    break;
                case DownloadState.DOWNLOADING:
                    downloadListener.onProgress(url, downloadState.getDownloadedLength(), downloadState.getContentLength());
                    break;
            }
        }

        public void startDownload(String url, long contentLength) {
            DownloadTask downloadTask;
            synchronized (urlDownloadTaskMap) {
                if (urlDownloadTaskMap.containsKey(url)) {
                    return;
                } else if (urlDownloadTaskMap.isEmpty()) {
                    startForeground(DOWNLOAD_FOREGROUND_SERVICE_NOTIFICATION_ID, getNotification(getString(R.string.download_task_is_running), null, -1));
                }
                downloadTask = new DownloadTask(listener);
                urlDownloadTaskMap.put(url, downloadTask);
            }

            String downloadStateString = pref.getString(url, null);
            if (downloadStateString == null) {
                prefEditor.putString(url, new DownloadState(0, contentLength, DownloadState.DOWNLOADING, System.currentTimeMillis()).toString());
            } else {
                DownloadState downloadState = DownloadState.fromString(downloadStateString);
                downloadState.setState(DownloadState.DOWNLOADING);
                prefEditor.putString(url, downloadState.toString());
            }
            prefEditor.apply();

            downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url, Long.toString(contentLength));

            String fileName = url.substring(url.lastIndexOf("/") + 1);
            getNotificationManager().notify(url.hashCode(), getNotification(fileName + " " + getString(R.string.downloading), null, 0));
            Toast.makeText(DownloadService.this, fileName + " " + getString(R.string.downloading), Toast.LENGTH_SHORT).show();
        }

        public void startDownload(String url) {
            startDownload(url, -1);
        }

        public void pauseDownload(String url) {
            synchronized (urlDownloadTaskMap) {
                if (urlDownloadTaskMap.containsKey(url)) {
                    urlDownloadTaskMap.get(url).pauseDownload();
                }
            }
        }

        public void cancelDownload(String url, boolean deleteFile) {
            synchronized (urlDownloadTaskMap) {
                if (urlDownloadTaskMap.containsKey(url)) {
                    urlDownloadTaskMap.get(url).cancelDownload(deleteFile);
                    return;
                }
            }

            // 如果代码可以执行到这里，说明不能通过downloadTask去取消下载任务，所以这里需要补上取消下载任务的代码。
            // 取消下载任务包括：看情况删除文件；删除pref保存的任务；downloaBinder回调删除任务事件。
            if (deleteFile) {
                String fileName = url.substring(url.lastIndexOf("/"));
                String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                File file = new File(directory + fileName);
                file.delete();
            }

            DownloadState downloadState = DownloadState.fromString(pref.getString(url, null));
            downloadState.setState(DownloadState.DOWNLOAD_CANCELED);
            prefEditor.remove(url);
            prefEditor.apply();
            downloadBinder.onDownloadStateChange(downloadState, url);
        }

        public void redownload(String url) {
            String fileName = url.substring(url.lastIndexOf("/"));
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            File file = new File(directory + fileName);
            file.delete();

            startDownload(url);
        }

        public boolean isDownloading(String url) {
            synchronized (urlDownloadTaskMap) {
                return urlDownloadTaskMap.containsKey(url);
            }
        }
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private Notification getNotification(String title, String text, int progress) {
        Intent intent = new Intent(this, DownloadActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_download);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_download));
        builder.setContentIntent(pi);
        builder.setContentTitle(title);
        builder.setAutoCancel(true);
        if (text != null) {
            builder.setContentText(text);
        } else if (progress >= 0) {
            builder.setContentText(progress + "%");
            builder.setProgress(100, progress, false);
        }
        return builder.build();
    }
}
