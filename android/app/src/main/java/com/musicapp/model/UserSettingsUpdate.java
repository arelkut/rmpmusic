package com.musicapp.model;

import com.google.gson.annotations.SerializedName;

public class UserSettingsUpdate {
    public String language;
    @SerializedName("audio_quality")    public String audioQuality;
    @SerializedName("download_quality") public String downloadQuality;
    @SerializedName("data_saver")       public Boolean dataSaver;
    @SerializedName("notifications_on") public Boolean notificationsOn;
}
