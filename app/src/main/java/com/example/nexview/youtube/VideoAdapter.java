package com.example.nexview.youtube;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nexview.R;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private final Context context;
    private final List<VideoModel> videoList;
    private final OnVideoClickListener onVideoClickListener;

    public interface OnVideoClickListener {
        void onVideoClick(VideoModel video);
    }

    public VideoAdapter(Context context, List<VideoModel> videoList, OnVideoClickListener onVideoClickListener) {
        this.context = context;
        this.videoList = videoList;
        this.onVideoClickListener = onVideoClickListener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_item, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoModel video = videoList.get(position);

        holder.title.setText(video.getTitle());
        Glide.with(context).load(video.getThumbnailUrl()).into(holder.thumbnail);

        holder.itemView.setOnClickListener(v -> onVideoClickListener.onVideoClick(video));
        if (video.getDuration() != null) {
            holder.duration.setText(VideoModel.formatDuration(video.getDuration()));
        }

    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView duration;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            title = itemView.findViewById(R.id.videoTitle);
        }
    }
}
