package com.musicapp.model;

import com.google.gson.annotations.SerializedName;

// ─────────────────────────────────────────────
// GENRE MODELS
// ─────────────────────────────────────────────
public class GenreOut {
    @SerializedName("genre_id") public int genreId;
    public String name;
    @SerializedName("color_hex") public String colorHex;
}
