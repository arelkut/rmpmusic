"""
Pydantic schemas (request / response models)
"""
from pydantic import BaseModel, EmailStr, field_validator
from typing import Optional, List
from datetime import datetime


# ─────────────────────────────────────────────
# AUTH
# ─────────────────────────────────────────────
class RegisterRequest(BaseModel):
    username:   str
    email:      EmailStr
    password:   str
    display_name: Optional[str] = None

    @field_validator("password")
    @classmethod
    def password_length(cls, v: str) -> str:
        if len(v) < 8:
            raise ValueError("Password must be at least 8 characters")
        return v

    @field_validator("username")
    @classmethod
    def username_length(cls, v: str) -> str:
        if len(v) < 3:
            raise ValueError("Username must be at least 3 characters")
        return v.lower()


class LoginRequest(BaseModel):
    email:    EmailStr
    password: str


class TokenResponse(BaseModel):
    access_token:  str
    refresh_token: str
    token_type:    str = "bearer"
    user_id:       int
    display_name:  str
    username:      str


class RefreshRequest(BaseModel):
    refresh_token: str


class ForgotPasswordRequest(BaseModel):
    email: EmailStr


class ResetPasswordRequest(BaseModel):
    token:       str
    new_password: str

    @field_validator("new_password")
    @classmethod
    def password_length(cls, v: str) -> str:
        if len(v) < 8:
            raise ValueError("Password must be at least 8 characters")
        return v


# ─────────────────────────────────────────────
# USER
# ─────────────────────────────────────────────
class UserOut(BaseModel):
    user_id:      int
    username:     str
    email:        str
    display_name: Optional[str]
    avatar_url:   Optional[str]
    bio:          Optional[str] = None
    created_at:   Optional[datetime] = None


class UserUpdateRequest(BaseModel):
    display_name: Optional[str] = None
    bio:          Optional[str] = None
    username:     Optional[str] = None


class UserStatsOut(BaseModel):
    total_listen_hours:  float
    total_tracks_played: int
    favorite_genre:      Optional[str]
    favorite_genre_pct:  float
    listener_top_pct:    float
    favorite_count:      int
    playlist_count:      int


# ─────────────────────────────────────────────
# GENRE
# ─────────────────────────────────────────────
class GenreOut(BaseModel):
    genre_id:  int
    name:      str
    color_hex: str


# ─────────────────────────────────────────────
# ARTIST
# ─────────────────────────────────────────────
class ArtistOut(BaseModel):
    artist_id:         int
    name:              str
    avatar_url:        Optional[str]
    monthly_listeners: int
    is_verified:       bool


# ─────────────────────────────────────────────
# TRACK
# ─────────────────────────────────────────────
class TrackOut(BaseModel):
    track_id:     int
    title:        str
    artist_name:  str
    artist_id:    int
    album_title:  Optional[str]
    cover_url:    Optional[str]
    duration_sec: int
    file_url:     str
    listen_count: int
    like_count:   int
    is_liked:     bool = False
    genre_name:   Optional[str] = None


# ─────────────────────────────────────────────
# ALBUM
# ─────────────────────────────────────────────
class AlbumOut(BaseModel):
    album_id:     int
    title:        str
    artist_name:  str
    cover_url:    Optional[str]
    listen_count: int
    release_date: Optional[str]


# ─────────────────────────────────────────────
# PLAYLIST
# ─────────────────────────────────────────────
class PlaylistOut(BaseModel):
    playlist_id: int
    name:        str
    description: Optional[str]
    cover_url:   Optional[str]
    track_count: int
    is_public:   bool
    user_id:     int


class PlaylistCreateRequest(BaseModel):
    name:        str
    description: Optional[str] = None
    is_public:   bool = True


class PlaylistUpdateRequest(BaseModel):
    name:        Optional[str] = None
    description: Optional[str] = None
    is_public:   Optional[bool] = None


class AddTrackToPlaylistRequest(BaseModel):
    track_id: int


# ─────────────────────────────────────────────
# SETTINGS
# ─────────────────────────────────────────────
class UserSettingsOut(BaseModel):
    language:         str
    audio_quality:    str
    download_quality: str
    data_saver:       bool
    notifications_on: bool


class UserSettingsUpdate(BaseModel):
    language:         Optional[str] = None
    audio_quality:    Optional[str] = None
    download_quality: Optional[str] = None
    data_saver:       Optional[bool] = None
    notifications_on: Optional[bool] = None


# ─────────────────────────────────────────────
# SEARCH
# ─────────────────────────────────────────────
class SearchResponse(BaseModel):
    tracks:    List[TrackOut]
    artists:   List[ArtistOut]
    albums:    List[AlbumOut]
    playlists: List[PlaylistOut]


# ─────────────────────────────────────────────
# RESPONSE WRAPPERS
# ─────────────────────────────────────────────
class MessageResponse(BaseModel):
    message: str
    success: bool = True
