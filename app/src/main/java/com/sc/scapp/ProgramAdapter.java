package com.sc.scapp;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ProgramAdapter extends RecyclerView.Adapter<ProgramAdapter.ViewHolder> {

    private Context context;
    private List<Program> programList;

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView imageView;
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            imageView = cardView.findViewById(R.id.program_image);
            textView = cardView.findViewById(R.id.program_name);
        }
    }

    public ProgramAdapter(List<Program> programList) {
        this.programList = programList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (context == null)
            context = parent.getContext();

        View view = LayoutInflater.from(context).inflate(R.layout.program_item, parent, false);
        final ViewHolder viewHolder = new ViewHolder(view);

        viewHolder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = viewHolder.getAdapterPosition();
                Program program = programList.get(position);
                switch (program.getNameId()) {
                    case R.string.file_manager:
                        break;
                    case R.string.notebook:
                        break;
                    case R.string.music:
                        break;
                    case R.string.video:
                        break;
                    case R.string.web:
                        WebActivity.actionStart(view.getContext());
                        break;
                    case R.string.download:
                        DownloadActivity.actionStart(view.getContext());
                        break;
                    case R.string.timer:
                        break;
                }
            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Program program = programList.get(position);
        holder.textView.setText(program.getNameId());
        Glide.with(context).load(program.getImageId()).into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return programList.size();
    }
}
