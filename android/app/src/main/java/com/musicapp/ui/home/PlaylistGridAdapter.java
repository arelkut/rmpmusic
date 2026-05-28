package com.musicapp.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.musicapp.R;
import com.musicapp.model.PlaylistOut;

import java.util.List;

public class PlaylistGridAdapter extends RecyclerView.Adapter<PlaylistGridAdapter.PlaylistViewHolder> {

    public interface OnPlaylistClickListener {
        void onPlaylistClick(PlaylistOut playlist);
    }

    private final List<PlaylistOut> playlists;
    private final OnPlaylistClickListener listener;

    public PlaylistGridAdapter(List<PlaylistOut> playlists, OnPlaylistClickListener listener) {
        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        PlaylistOut playlist = playlists.get(position);
        holder.tvName.setText(playlist.name);
        holder.tvTrackCount.setText(playlist.trackCount + " tracks");

        Glide.with(holder.itemView.getContext())
                .load(playlist.coverUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(holder.ivCover);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPlaylistClick(playlist);
        });
    }

    @Override
    public int getItemCount() {
        return playlists == null ? 0 : playlists.size();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvName, tvTrackCount;

        PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover      = itemView.findViewById(R.id.ivPlaylistCover);
            tvName       = itemView.findViewById(R.id.tvPlaylistName);
            tvTrackCount = itemView.findViewById(R.id.tvTrackCount);
        }
    }
}