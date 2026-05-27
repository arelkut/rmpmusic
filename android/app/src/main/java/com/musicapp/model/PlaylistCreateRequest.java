package com.musicapp.model;

import com.google.gson.annotations.SerializedName;

public class PlaylistCreateRequest {
    public String name;
    public String description;
    @SerializedName("is_public") public boolean isPublic;

    public PlaylistCreateRequest(String name, String description, boolean isPublic) {
        this.name = name;
        this.description = description;
        this.isPublic = isPublic;
    }
}
