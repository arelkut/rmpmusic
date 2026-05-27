package com.musicapp.model;

import com.google.gson.annotations.SerializedName;

public class UserUpdateRequest {
    @SerializedName("display_name") public String displayName;
    public String bio;
    public String username;
}
