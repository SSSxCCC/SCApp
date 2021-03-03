package com.sc.scapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sc.download.DownloadActivity
import com.sc.screenrecorder.ScreenRecorderActivity
import com.sc.web.WebActivity

class ProgramAdapter(private val programList: List<Program>) : RecyclerView.Adapter<ProgramAdapter.ViewHolder>() {
    private lateinit var context: Context

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var cardView: CardView = itemView as CardView
        var imageView: ImageView = cardView.findViewById(R.id.program_image)
        var textView: TextView = cardView.findViewById(R.id.program_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.program_item, parent, false)
        val viewHolder = ViewHolder(view)
        viewHolder.cardView.setOnClickListener {
            val position = viewHolder.adapterPosition
            val program = programList[position]
            when (program.nameId) {
                R.string.file_manager -> { }
                R.string.notebook -> { }
                R.string.music -> { }
                R.string.video -> { }
                R.string.web -> WebActivity.actionStart(context)
                R.string.download -> DownloadActivity.actionStart(context)
                R.string.timer -> { }
                R.string.screen_recorder -> ScreenRecorderActivity.actionStart(context)
            }
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val program = programList[position]
        holder.textView.setText(program.nameId)
        Glide.with(context).load(program.imageId).into(holder.imageView)
    }

    override fun getItemCount(): Int {
        return programList.size
    }
}