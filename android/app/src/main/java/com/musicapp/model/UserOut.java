package com.musicapp.model;

import com.google.gson.annotations.SerializedName;

// ─────────────────────────────────────────────
// USER MODELS
// ─────────────────────────────────────────────
public class UserOut {
    @SerializedName("user_id")      public int userId;
    public String username;
    public String email;
    @SerializedName("display_name") public String displayName;
    @SerializedName("avatar_url")   public String avatarUrl;
    public String bio;
    @SerializedName("created_at")   public String createdAt;
}
