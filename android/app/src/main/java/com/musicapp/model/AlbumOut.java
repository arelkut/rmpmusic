package com.musicapp.model;

import com.google.gson.annotations.SerializedName;

// ─────────────────────────────────────────────
// ALBUM MODELS
// ─────────────────────────────────────────────
public class AlbumOut {
    @SerializedName("album_id")    public int albumId;
    public String title;
    @SerializedName("artist_name") public String artistName;
    @SerializedName("cover_url")   public String coverUrl;
    @SerializedName("listen_count") public long listenCount;
    @SerializedName("release_date") public String releaseDate;

    public String getFormattedListenCount() {
        if (listenCount >= 1_000_000) {
            return String.format("%.1fM", listenCount / 1_000_000.0);
        } else if (listenCount >= 1_000) {
            return String.format("%.1fK", listenCount / 1_000.0);
        }
        return String.valueOf(listenCount);
    }
}
