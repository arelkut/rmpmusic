package com.musicapp.model;

import com.google.gson.annotations.SerializedName;

public class UserStatsOut {
    @SerializedName("total_listen_hours")  public double totalListenHours;
    @SerializedName("total_tracks_played") public int totalTracksPlayed;
    @SerializedName("favorite_genre")      public String favoriteGenre;
    @SerializedName("favorite_genre_pct")  public double favoriteGenrePct;
    @SerializedName("listener_top_pct")    public double listenerTopPct;
    @SerializedName("favorite_count")      public int favoriteCount;
    @SerializedName("playlist_count")      public int playlistCount;
}
