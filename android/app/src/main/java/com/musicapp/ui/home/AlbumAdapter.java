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

        Glide.with(holder.itemView.getContext())
                .load(album.coverUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
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