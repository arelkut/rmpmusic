package com.musicapp.model;

import com.google.gson.annotations.SerializedName;

// ─────────────────────────────────────────────
// AUTH MODELS
// ─────────────────────────────────────────────
public class RegisterRequest {
    public String username;
    public String email;
    public String password;
    @SerializedName("display_name") public String displayName;

    public RegisterRequest(String username, String email, String password, String displayName) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
    }
}

