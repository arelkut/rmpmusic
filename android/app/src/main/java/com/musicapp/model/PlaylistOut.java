package com.musicapp.model;

import com.google.gson.annotations.SerializedName;

// ─────────────────────────────────────────────
// PLAYLIST MODELS
// ─────────────────────────────────────────────
public class PlaylistOut {
    @SerializedName("playlist_id") public int playlistId;
    public String name;
    public String description;
    @SerializedName("cover_url")   public String coverUrl;
    @SerializedName("track_count") public int trackCount;
    @SerializedName("is_public")   public boolean isPublic;
    @SerializedName("user_id")     public int userId;
}
