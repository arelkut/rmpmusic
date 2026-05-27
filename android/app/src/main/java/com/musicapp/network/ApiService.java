package com.musicapp.network;

import com.musicapp.model.*;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ─── AUTH ──────────────────────────────────────────────────
    @POST("api/v1/auth/register")
    Call<TokenResponse> register(@Body RegisterRequest body);

    @POST("api/v1/auth/login")
    Call<TokenResponse> login(@Body LoginRequest body);

    @POST("api/v1/auth/refresh")
    Call<TokenResponse> refreshToken(@Body RefreshRequest body);

    @POST("api/v1/auth/logout")
    Call<MessageResponse> logout(@Body RefreshRequest body);

    @POST("api/v1/auth/forgot-password")
    Call<MessageResponse> forgotPassword(@Body ForgotPasswordRequest body);

    @POST("api/v1/auth/reset-password")
    Call<MessageResponse> resetPassword(@Body ResetPasswordRequest body);

    // ─── USERS ─────────────────────────────────────────────────
    @GET("api/v1/users/me")
    Call<UserOut> getMe();

    @PATCH("api/v1/users/me")
    Call<UserOut> updateMe(@Body UserUpdateRequest body);

    @GET("api/v1/users/me/stats")
    Call<UserStatsOut> getMyStats();

    @GET("api/v1/users/me/settings")
    Call<UserSettingsOut> getMySettings();

    @PATCH("api/v1/users/me/settings")
    Call<UserSettingsOut> updateMySettings(@Body UserSettingsUpdate body);

    // ─── TRACKS ────────────────────────────────────────────────
    @GET("api/v1/tracks/trending")
    Call<List<TrackOut>> getTrendingTracks(@Query("limit") int limit);

    @GET("api/v1/tracks/recent")
    Call<List<TrackOut>> getRecentTracks(@Query("limit") int limit);

    @GET("api/v1/tracks/favorites/list")
    Call<List<TrackOut>> getFavoriteTracks();

    @GET("api/v1/tracks/{id}")
    Call<TrackOut> getTrack(@Path("id") int trackId);

    @POST("api/v1/tracks/{id}/play")
    Call<MessageResponse> recordPlay(
            @Path("id") int trackId,
            @Query("duration_listened") int duration
    );

    @POST("api/v1/tracks/{id}/like")
    Call<MessageResponse> likeTrack(@Path("id") int trackId);

    @DELETE("api/v1/tracks/{id}/like")
    Call<MessageResponse> unlikeTrack(@Path("id") int trackId);

    // ─── PLAYLISTS ─────────────────────────────────────────────
    @GET("api/v1/playlists/my")
    Call<List<PlaylistOut>> getMyPlaylists();

    @GET("api/v1/playlists/recommended")
    Call<List<PlaylistOut>> getRecommendedPlaylists(@Query("limit") int limit);

    @POST("api/v1/playlists/")
    Call<PlaylistOut> createPlaylist(@Body PlaylistCreateRequest body);

    @GET("api/v1/playlists/{id}")
    Call<PlaylistOut> getPlaylist(@Path("id") int playlistId);

    @PATCH("api/v1/playlists/{id}")
    Call<PlaylistOut> updatePlaylist(
            @Path("id") int playlistId,
            @Body PlaylistUpdateRequest body
    );

    @DELETE("api/v1/playlists/{id}")
    Call<MessageResponse> deletePlaylist(@Path("id") int playlistId);

    @GET("api/v1/playlists/{id}/tracks")
    Call<List<TrackOut>> getPlaylistTracks(@Path("id") int playlistId);

    @POST("api/v1/playlists/{id}/tracks")
    Call<MessageResponse> addTrackToPlaylist(
            @Path("id") int playlistId,
            @Body AddTrackRequest body
    );

    @DELETE("api/v1/playlists/{id}/tracks/{trackId}")
    Call<MessageResponse> removeTrackFromPlaylist(
            @Path("id") int playlistId,
            @Path("trackId") int trackId
    );

    // ─── SEARCH ────────────────────────────────────────────────
    @GET("api/v1/search/")
    Call<SearchResponse> search(
            @Query("q") String query,
            @Query("limit") int limit
    );

    @GET("api/v1/search/genres")
    Call<List<GenreOut>> getGenres();

    @GET("api/v1/search/by-genre/{genreId}")
    Call<List<TrackOut>> getTracksByGenre(
            @Path("genreId") int genreId,
            @Query("limit") int limit
    );

    @GET("api/v1/search/albums")
    Call<List<AlbumOut>> getTrendingAlbums(@Query("limit") int limit);
}
