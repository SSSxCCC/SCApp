package com.sc.scapp;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.ViewHolder> {

    private List<DownloadState> downloadStateList;
    private DownloadService.DownloadBinder downloadBinder;

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView timeTextView;
        TextView fileNameTextView;
        ImageButton startPauseRefreshImageButton;
        TextView progressTextView;
        TextView downloadStateTextView;
        ProgressBar progressBar;
        ImageButton cancelImageButton;

        public ViewHolder(View itemView) {
            super(itemView);
            timeTextView = itemView.findViewById(R.id.time_text);
            fileNameTextView = itemView.findViewById(R.id.file_name_text);
            startPauseRefreshImageButton = itemView.findViewById(R.id.start_pause_refresh_image_button);
            progressTextView = itemView.findViewById(R.id.progress_text);
            downloadStateTextView = itemView.findViewById(R.id.download_state_text);
            progressBar = itemView.findViewById(R.id.progress_bar);
            cancelImageButton = itemView.findViewById(R.id.cancel_image_button);
        }
    }

    public DownloadAdapter(List<DownloadState> downloadStateList) {
        this.downloadStateList = downloadStateList;
    }

    public void setDownloadBinder(DownloadService.DownloadBinder downloadBinder) {
        this.downloadBinder = downloadBinder;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.download_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final DownloadState downloadState = downloadStateList.get(position);
        holder.timeTextView.setText(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date(downloadState.getTime())));
        holder.fileNameTextView.setText(downloadState.getUrl().substring(downloadState.getUrl().lastIndexOf("/") + 1));
        holder.progressTextView.setText(downloadState.getDownloadedLength() + "b/" + downloadState.getContentLength() + "b"); // 还要优化文件大小显示
        switch (downloadState.getState()) {
            case DownloadState.DOWNLOAD_SUCCESS:
                holder.downloadStateTextView.setText(R.string.download_success);
                break;
            case DownloadState.DOWNLOAD_FAILED:
                holder.downloadStateTextView.setText(R.string.download_failed);
                break;
            case DownloadState.DOWNLOAD_PAUSED:
                holder.downloadStateTextView.setText(R.string.download_paused);
                break;
            case DownloadState.DOWNLOAD_CANCELED:
                holder.downloadStateTextView.setText(R.string.download_canceled);
                break;
            case DownloadState.DOWNLOADING:
                holder.downloadStateTextView.setText(R.string.downloading);
                break;
            default:
                holder.downloadStateTextView.setText("Unknown State: " + downloadState.getState());
                break;
        }
        holder.progressBar.setMax((int)downloadState.getContentLength()); // 还要考虑long转int精度丢失问题
        holder.progressBar.setProgress((int)downloadState.getDownloadedLength());
        switch (downloadState.getState()) {
            case DownloadState.DOWNLOADING:
                holder.startPauseRefreshImageButton.setImageResource(R.drawable.ic_pause);
                holder.startPauseRefreshImageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (downloadBinder != null) downloadBinder.pauseDownload(downloadState.getUrl());
                    }
                });
                break;
            case DownloadState.DOWNLOAD_PAUSED:
                holder.startPauseRefreshImageButton.setImageResource(R.drawable.ic_play);
                holder.startPauseRefreshImageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (downloadBinder != null) downloadBinder.startDownload(downloadState.getUrl());
                    }
                });
                break;
            default:
                holder.startPauseRefreshImageButton.setImageResource(R.drawable.ic_refresh_black);
                holder.startPauseRefreshImageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (downloadBinder == null)
                            return;

                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(v.getContext());
                        alertDialogBuilder.setTitle(R.string.confirm_refresh_download_task);
                        alertDialogBuilder.setPositiveButton(R.string.re_download, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                downloadBinder.redownload(downloadState.getUrl());
                            }
                        });
                        alertDialogBuilder.setNegativeButton(R.string.continue_download, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                downloadBinder.startDownload(downloadState.getUrl());
                            }
                        });
                        alertDialogBuilder.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                        alertDialogBuilder.show();
                    }
                });
                break;
        }
        holder.cancelImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (downloadBinder == null)
                    return;

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(v.getContext());
                alertDialogBuilder.setTitle(R.string.confirm_delete_download_task);
                alertDialogBuilder.setPositiveButton(R.string.delete_task, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadBinder.cancelDownload(downloadState.getUrl(), false);
                    }
                });
                alertDialogBuilder.setNeutralButton(R.string.delete_task_and_file, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadBinder.cancelDownload(downloadState.getUrl(), true);
                    }
                });
                alertDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                alertDialogBuilder.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return downloadStateList.size();
    }

}
