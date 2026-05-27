package com.musicapp.model;

import com.google.gson.annotations.SerializedName;

public class ResetPasswordRequest {
    public String token;
    @SerializedName("new_password") public String newPassword;
    public ResetPasswordRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }
}
