package com.musicapp.model;

import com.google.gson.annotations.SerializedName;

// ─────────────────────────────────────────────
// TRACK MODELS
// ─────────────────────────────────────────────
public class TrackOut {
    @SerializedName("track_id")    public int trackId;
    public String title;
    @SerializedName("artist_name") public String artistName;
    @SerializedName("artist_id")   public int artistId;
    @SerializedName("album_title") public String albumTitle;
    @SerializedName("cover_url")   public String coverUrl;
    @SerializedName("duration_sec") public int durationSec;
    @SerializedName("file_url")    public String fileUrl;
    @SerializedName("listen_count") public long listenCount;
    @SerializedName("like_count")  public long likeCount;
    @SerializedName("is_liked")    public boolean isLiked;
    @SerializedName("genre_name")  public String genreName;

    public String getFormattedDuration() {
        int minutes = durationSec / 60;
        int seconds = durationSec % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public String getFullFileUrl(String baseUrl) {
        if (fileUrl != null && fileUrl.startsWith("/")) {
            return baseUrl.replaceAll("/$", "") + fileUrl;
        }
        return fileUrl;
    }
}
