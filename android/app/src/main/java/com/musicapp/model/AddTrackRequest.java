package com.musicapp.model;

import com.google.gson.annotations.SerializedName;

public class AddTrackRequest {
    @SerializedName("track_id") public int trackId;
    public AddTrackRequest(int trackId) { this.trackId = trackId; }
}
