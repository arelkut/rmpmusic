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
import com.musicapp.model.TrackOut;
import com.musicapp.network.ApiClient;

import java.util.List;

public class TrackRecentAdapter extends RecyclerView.Adapter<TrackRecentAdapter.TrackViewHolder> {

    public interface OnTrackClickListener {
        // Теперь передаём позицию, чтобы фрагмент мог передать весь список в setQueue
        void onTrackClick(TrackOut track, int position);
    }

    private final List<TrackOut> tracks;
    private final OnTrackClickListener listener;

    public TrackRecentAdapter(List<TrackOut> tracks, OnTrackClickListener listener) {
        this.tracks = tracks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        TrackOut track = tracks.get(position);
        holder.tvTitle.setText(track.title);
        holder.tvArtist.setText(track.artistName);
        holder.tvDuration.setText(track.getFormattedDuration());

        String coverUrl = null;
        if (track.coverUrl != null && !track.coverUrl.isEmpty()) {
            coverUrl = ApiClient.BASE_URL.replaceAll("/$", "") + track.coverUrl;
        }

        Glide.with(holder.itemView.getContext())
                .load(coverUrl)
                .placeholder(R.drawable.placeholder_album)
                .error(R.drawable.placeholder_album)
                .into(holder.ivCover);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTrackClick(track, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return tracks == null ? 0 : tracks.size();
    }

    static class TrackViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle, tvArtist, tvDuration;

        TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover    = itemView.findViewById(R.id.ivTrackCover);
            tvTitle    = itemView.findViewById(R.id.tvTrackTitle);
            tvArtist   = itemView.findViewById(R.id.tvTrackArtist);
            tvDuration = itemView.findViewById(R.id.tvTrackDuration);
        }
    }
}