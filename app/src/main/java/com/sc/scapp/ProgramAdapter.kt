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
import com.sc.media.MediaActivity
import com.sc.notebook.NotebookActivity
import com.sc.opengl.OpenGLActivity
import com.sc.recorder.RecorderActivity
import com.sc.web.WebActivity

class ProgramAdapter(private val mProgramList: List<Program>) : RecyclerView.Adapter<ProgramAdapter.ViewHolder>() {
    private lateinit var mContext: Context

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var cardView: CardView = itemView as CardView
        var imageView: ImageView = cardView.findViewById(R.id.program_image)
        var textView: TextView = cardView.findViewById(R.id.program_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        mContext = parent.context
        val view = LayoutInflater.from(mContext).inflate(R.layout.program_item, parent, false)
        val viewHolder = ViewHolder(view)
        viewHolder.cardView.setOnClickListener {
            val position = viewHolder.adapterPosition
            val program = mProgramList[position]
            when (program.nameId) {
                R.string.files -> { }
                R.string.notebook -> { NotebookActivity.actionStart(mContext) }
                R.string.media -> { MediaActivity.actionStart(mContext) }
                R.string.web -> WebActivity.actionStart(mContext)
                R.string.download -> DownloadActivity.actionStart(mContext)
                R.string.timer -> { }
                R.string.recorder -> RecorderActivity.actionStart(mContext)
                R.string.opengl -> OpenGLActivity.actionStart(mContext)
            }
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val program = mProgramList[position]
        holder.textView.setText(program.nameId)
        Glide.with(mContext).load(program.imageId).into(holder.imageView)
    }

    override fun getItemCount(): Int {
        return mProgramList.size
    }
}