package com.musicapp.model;

import com.google.gson.annotations.SerializedName;

public class PlaylistUpdateRequest {
    public String name;
    public String description;
    @SerializedName("is_public") public Boolean isPublic;
}
