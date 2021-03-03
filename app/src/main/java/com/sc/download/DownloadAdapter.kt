package com.sc.download

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.sc.download.DownloadService.DownloadBinder
import com.sc.scapp.R
import java.text.SimpleDateFormat
import java.util.*

class DownloadAdapter(private val downloadStateList: List<DownloadState>) : RecyclerView.Adapter<DownloadAdapter.ViewHolder>() {
    private var downloadBinder: DownloadBinder? = null

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var timeTextView: TextView = itemView.findViewById(R.id.time_text)
        var fileNameTextView: TextView = itemView.findViewById(R.id.file_name_text)
        var startPauseRefreshImageButton: ImageButton = itemView.findViewById(R.id.start_pause_refresh_image_button)
        var progressTextView: TextView = itemView.findViewById(R.id.progress_text)
        var downloadStateTextView: TextView = itemView.findViewById(R.id.download_state_text)
        var progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
        var cancelImageButton: ImageButton = itemView.findViewById(R.id.cancel_image_button)
    }

    fun setDownloadBinder(downloadBinder: DownloadBinder?) {
        this.downloadBinder = downloadBinder
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.download_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val downloadState = downloadStateList[position]
        holder.timeTextView.text = SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(Date(downloadState.time))
        holder.fileNameTextView.text = downloadState.url.substring(downloadState.url.lastIndexOf("/") + 1)
        holder.progressTextView.text = downloadState.downloadedLength.toString() + "b/" + downloadState.contentLength + "b" // 还要优化文件大小显示
        when (downloadState.state) {
            DownloadState.DOWNLOAD_SUCCESS -> holder.downloadStateTextView.setText(R.string.download_success)
            DownloadState.DOWNLOAD_FAILED -> holder.downloadStateTextView.setText(R.string.download_failed)
            DownloadState.DOWNLOAD_PAUSED -> holder.downloadStateTextView.setText(R.string.download_paused)
            DownloadState.DOWNLOAD_CANCELED -> holder.downloadStateTextView.setText(R.string.download_canceled)
            DownloadState.DOWNLOADING -> holder.downloadStateTextView.setText(R.string.downloading)
            else -> holder.downloadStateTextView.text = "Unknown State: " + downloadState.state
        }
        holder.progressBar.max = downloadState.contentLength.toInt() // 还要考虑long转int精度丢失问题
        holder.progressBar.progress = downloadState.downloadedLength.toInt()
        when (downloadState.state) {
            DownloadState.DOWNLOADING -> {
                holder.startPauseRefreshImageButton.setImageResource(R.drawable.ic_pause)
                holder.startPauseRefreshImageButton.setOnClickListener { downloadBinder?.pauseDownload(downloadState.url) }
            }
            DownloadState.DOWNLOAD_PAUSED -> {
                holder.startPauseRefreshImageButton.setImageResource(R.drawable.ic_play)
                holder.startPauseRefreshImageButton.setOnClickListener { downloadBinder?.startDownload(downloadState.url) }
            }
            else -> {
                holder.startPauseRefreshImageButton.setImageResource(R.drawable.ic_refresh_black)
                holder.startPauseRefreshImageButton.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View) {
                        if (downloadBinder == null) return
                        val alertDialogBuilder = AlertDialog.Builder(v.context)
                        alertDialogBuilder.setTitle(R.string.confirm_refresh_download_task)
                        alertDialogBuilder.setPositiveButton(R.string.re_download) { dialog, which -> downloadBinder!!.redownload(downloadState.url) }
                        alertDialogBuilder.setNegativeButton(R.string.continue_download) { dialog, which -> downloadBinder!!.startDownload(downloadState.url) }
                        alertDialogBuilder.setNeutralButton(R.string.cancel) { dialog, which -> }
                        alertDialogBuilder.show()
                    }
                })
            }
        }
        holder.cancelImageButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                if (downloadBinder == null) return
                val alertDialogBuilder = AlertDialog.Builder(v.context)
                alertDialogBuilder.setTitle(R.string.confirm_delete_download_task)
                alertDialogBuilder.setPositiveButton(R.string.delete_task) { dialog, which -> downloadBinder!!.cancelDownload(downloadState.url, false) }
                alertDialogBuilder.setNeutralButton(R.string.delete_task_and_file) { dialog, which -> downloadBinder!!.cancelDownload(downloadState.url, true) }
                alertDialogBuilder.setNegativeButton(R.string.cancel) { dialog, which -> }
                alertDialogBuilder.show()
            }
        })
    }

    override fun getItemCount(): Int {
        return downloadStateList.size
    }
}