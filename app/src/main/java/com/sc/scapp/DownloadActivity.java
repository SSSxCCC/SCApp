package com.sc.scapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class DownloadActivity extends AppCompatActivity {

    private ArrayList<DownloadState> downloadStateList = new ArrayList<>();
    private DownloadAdapter downloadAdapter;
    private DownloadService.DownloadBinder downloadBinder;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadBinder = (DownloadService.DownloadBinder) service;
            downloadBinder.setDownloadListener(downloadListener);

            SharedPreferences pref = getSharedPreferences(DownloadService.DOWNLOAD_SHARED_PREFERENCE_NAME, MODE_PRIVATE);
            Map<String, ?> prefMap = pref.getAll();
            for (String url : prefMap.keySet()) {
                String value = (String) prefMap.get(url);
                DownloadState downloadState = DownloadState.fromString(value);
                if (downloadState.getState() == DownloadState.DOWNLOADING && !downloadBinder.isDownloading(url)) {
                    downloadState.setState(DownloadState.DOWNLOAD_PAUSED);
                }
                downloadState.setUrl(url);
                downloadStateList.add(downloadState);
            }
            Collections.sort(downloadStateList);
            downloadAdapter.notifyDataSetChanged();
            downloadAdapter.setDownloadBinder(downloadBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    private DownloadListener downloadListener = new DownloadListener() {
        @Override
        public void onProgress(String url, long downloadedLength, long contentLength) {
            for (int i = 0; i < downloadStateList.size(); i++) {
                DownloadState downloadState = downloadStateList.get(i);
                if (downloadState.getUrl().equals(url)) {
                    downloadState.setDownloadedLength(downloadedLength);
                    downloadState.setContentLength(contentLength);
                    downloadState.setState(DownloadState.DOWNLOADING);
                    downloadAdapter.notifyItemChanged(i);
                    break;
                }
            }
        }

        @Override
        public void onSuccess(String url) {
            for (int i = 0; i < downloadStateList.size(); i++) {
                DownloadState downloadState = downloadStateList.get(i);
                if (downloadState.getUrl().equals(url)) {
                    downloadState.setState(DownloadState.DOWNLOAD_SUCCESS);
                    downloadAdapter.notifyItemChanged(i);
                    break;
                }
            }
        }

        @Override
        public void onFailed(String url) {
            for (int i = 0; i < downloadStateList.size(); i++) {
                DownloadState downloadState = downloadStateList.get(i);
                if (downloadState.getUrl().equals(url)) {
                    downloadState.setState(DownloadState.DOWNLOAD_SUCCESS);
                    downloadAdapter.notifyItemChanged(i);
                    break;
                }
            }
        }

        @Override
        public void onPaused(String url) {
            for (int i = 0; i < downloadStateList.size(); i++) {
                DownloadState downloadState = downloadStateList.get(i);
                if (downloadState.getUrl().equals(url)) {
                    downloadState.setState(DownloadState.DOWNLOAD_PAUSED);
                    downloadAdapter.notifyItemChanged(i);
                    break;
                }
            }
        }

        @Override
        public void onCanceled(String url) {
            for (int i = 0; i < downloadStateList.size(); i++) {
                DownloadState downloadState = downloadStateList.get(i);
                if (downloadState.getUrl().equals(url)) {
                    //downloadState.setState(DownloadState.DOWNLOAD_CANCELED);
                    //downloadAdapter.notifyItemChanged(i);
                    downloadStateList.remove(i);
                    downloadAdapter.notifyItemRemoved(i);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(DownloadActivity.this));
        downloadAdapter = new DownloadAdapter(downloadStateList);
        recyclerView.setAdapter(downloadAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        Intent bindIntent = new Intent(this, DownloadService.class);
        bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        downloadBinder.setDownloadListener(null);
        unbindService(serviceConnection);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    public static void actionStart(Context context) {
        Intent intent = new Intent(context, DownloadActivity.class);
        context.startActivity(intent);
    }
}
