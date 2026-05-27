package com.musicapp.model;

import com.google.gson.annotations.SerializedName;

public class TokenResponse {
    @SerializedName("access_token")  public String accessToken;
    @SerializedName("refresh_token") public String refreshToken;
    @SerializedName("token_type")    public String tokenType;
    @SerializedName("user_id")       public int userId;
    @SerializedName("display_name")  public String displayName;
    public String username;
}
