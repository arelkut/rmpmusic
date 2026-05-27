package com.musicapp.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages user session data (tokens, user info) in SharedPreferences.
 */
public class SessionManager {

    private static final String PREF_NAME = "MusicAppPrefs";

    // Keys
    private static final String KEY_ACCESS_TOKEN  = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID       = "user_id";
    private static final String KEY_USERNAME      = "username";
    private static final String KEY_DISPLAY_NAME  = "display_name";
    private static final String KEY_AVATAR_URL    = "avatar_url";
    private static final String KEY_IS_LOGGED_IN  = "is_logged_in";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveSession(String accessToken, String refreshToken,
                            int userId, String username, String displayName) {
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_DISPLAY_NAME, displayName);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getAccessToken()  { return prefs.getString(KEY_ACCESS_TOKEN, null); }
    public String getRefreshToken() { return prefs.getString(KEY_REFRESH_TOKEN, null); }
    public int getUserId()          { return prefs.getInt(KEY_USER_ID, -1); }
    public String getUsername()     { return prefs.getString(KEY_USERNAME, ""); }
    public String getDisplayName()  { return prefs.getString(KEY_DISPLAY_NAME, ""); }
    public String getAvatarUrl()    { return prefs.getString(KEY_AVATAR_URL, null); }

    public void updateAvatarUrl(String url) {
        editor.putString(KEY_AVATAR_URL, url);
        editor.apply();
    }

    public void updateDisplayName(String name) {
        editor.putString(KEY_DISPLAY_NAME, name);
        editor.apply();
    }

    public void updateTokens(String accessToken, String refreshToken) {
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.apply();
    }
}
