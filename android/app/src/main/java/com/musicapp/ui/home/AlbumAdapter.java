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
import com.musicapp.model.AlbumOut;
import com.musicapp.network.ApiClient;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {

    public interface OnAlbumClickListener {
        void onAlbumClick(AlbumOut album);
    }

    private final List<AlbumOut> albums;
    private final OnAlbumClickListener listener;

    public AlbumAdapter(List<AlbumOut> albums, OnAlbumClickListener listener) {
        this.albums = albums;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        AlbumOut album = albums.get(position);
        holder.tvTitle.setText(album.title);
        holder.tvArtist.setText(album.artistName);

        String coverUrl = null;
        if (album.coverUrl != null && !album.coverUrl.isEmpty()) {
            coverUrl = ApiClient.BASE_URL.replaceAll("/$", "") + album.coverUrl;
        }

        Glide.with(holder.itemView.getContext())
                .load(coverUrl)
                .placeholder(R.drawable.placeholder_album)
                .error(R.drawable.placeholder_album)
                .into(holder.ivCover);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAlbumClick(album);
        });
    }

    @Override
    public int getItemCount() {
        return albums == null ? 0 : albums.size();
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle, tvArtist;

        AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover  = itemView.findViewById(R.id.ivAlbumCover);
            tvTitle  = itemView.findViewById(R.id.tvAlbumTitle);
            tvArtist = itemView.findViewById(R.id.tvAlbumArtist);
        }
    }
}