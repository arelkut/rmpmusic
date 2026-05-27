"""
Users router: profile, stats, settings, avatar upload
"""
import os
import shutil
from fastapi import APIRouter, Depends, HTTPException, UploadFile, File
from app.database import db_context, dict_from_row
from app.dependencies import get_current_user
from app.schemas import (
    UserOut, UserUpdateRequest, UserStatsOut,
    UserSettingsOut, UserSettingsUpdate, MessageResponse
)
from app.config import get_settings

settings = get_settings()
router = APIRouter(prefix="/users", tags=["Users"])


# ─────────────────────────────────────────────
# GET /users/me
# ─────────────────────────────────────────────
@router.get("/me", response_model=UserOut)
def get_me(current_user=Depends(get_current_user)):
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            SELECT user_id, username, email, display_name, avatar_url, bio, created_at
            FROM Users WHERE user_id = ?
            """,
            current_user["user_id"]
        )
        row = cursor.fetchone()
    return dict_from_row(row)


# ─────────────────────────────────────────────
# PATCH /users/me
# ─────────────────────────────────────────────
@router.patch("/me", response_model=UserOut)
def update_me(body: UserUpdateRequest, current_user=Depends(get_current_user)):
    user_id = current_user["user_id"]

    with db_context() as conn:
        cursor = conn.cursor()

        if body.username:
            cursor.execute(
                "SELECT user_id FROM Users WHERE username = ? AND user_id != ?",
                body.username, user_id
            )
            if cursor.fetchone():
                raise HTTPException(status_code=400, detail="Username already taken")

        updates = []
        params  = []
        if body.display_name is not None:
            updates.append("display_name = ?")
            params.append(body.display_name)
        if body.bio is not None:
            updates.append("bio = ?")
            params.append(body.bio)
        if body.username is not None:
            updates.append("username = ?")
            params.append(body.username.lower())

        if updates:
            updates.append("updated_at = GETDATE()")
            sql = f"UPDATE Users SET {', '.join(updates)} WHERE user_id = ?"
            params.append(user_id)
            cursor.execute(sql, *params)

        cursor.execute(
            """
            SELECT user_id, username, email, display_name, avatar_url, bio, created_at
            FROM Users WHERE user_id = ?
            """,
            user_id
        )
        row = cursor.fetchone()

    return dict_from_row(row)


# ─────────────────────────────────────────────
# POST /users/me/avatar
# ─────────────────────────────────────────────
@router.post("/me/avatar", response_model=MessageResponse)
def upload_avatar(
    file: UploadFile = File(...),
    current_user=Depends(get_current_user)
):
    user_id = current_user["user_id"]
    ext = os.path.splitext(file.filename)[1].lower()
    if ext not in [".jpg", ".jpeg", ".png", ".webp"]:
        raise HTTPException(status_code=400, detail="Invalid image format")

    os.makedirs(settings.avatars_dir, exist_ok=True)
    filename = f"avatar_{user_id}{ext}"
    filepath = os.path.join(settings.avatars_dir, filename)

    with open(filepath, "wb") as f:
        shutil.copyfileobj(file.file, f)

    avatar_url = f"/static/avatars/{filename}"

    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            "UPDATE Users SET avatar_url = ?, updated_at = GETDATE() WHERE user_id = ?",
            avatar_url, user_id
        )

    return MessageResponse(message=f"Avatar updated: {avatar_url}")


# ─────────────────────────────────────────────
# GET /users/me/stats
# ─────────────────────────────────────────────
@router.get("/me/stats", response_model=UserStatsOut)
def get_stats(current_user=Depends(get_current_user)):
    user_id = current_user["user_id"]

    with db_context() as conn:
        cursor = conn.cursor()

        cursor.execute(
            """
            SELECT s.total_listen_hours, s.total_tracks_played,
                   g.name AS favorite_genre, s.favorite_genre_pct, s.listener_top_pct
            FROM UserStats s
            LEFT JOIN Genres g ON g.genre_id = s.favorite_genre_id
            WHERE s.user_id = ?
            """,
            user_id
        )
        stats_row = cursor.fetchone()

        cursor.execute(
            "SELECT COUNT(*) FROM FavoriteTracks WHERE user_id = ?", user_id
        )
        fav_count = cursor.fetchone()[0]

        cursor.execute(
            "SELECT COUNT(*) FROM Playlists WHERE user_id = ?", user_id
        )
        playlist_count = cursor.fetchone()[0]

    if stats_row is None:
        return UserStatsOut(
            total_listen_hours=0,
            total_tracks_played=0,
            favorite_genre=None,
            favorite_genre_pct=0,
            listener_top_pct=100,
            favorite_count=fav_count,
            playlist_count=playlist_count,
        )

    return UserStatsOut(
        total_listen_hours=float(stats_row[0] or 0),
        total_tracks_played=int(stats_row[1] or 0),
        favorite_genre=stats_row[2],
        favorite_genre_pct=float(stats_row[3] or 0),
        listener_top_pct=float(stats_row[4] or 100),
        favorite_count=fav_count,
        playlist_count=playlist_count,
    )


# ─────────────────────────────────────────────
# GET /users/me/settings
# ─────────────────────────────────────────────
@router.get("/me/settings", response_model=UserSettingsOut)
def get_settings_user(current_user=Depends(get_current_user)):
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            SELECT language, audio_quality, download_quality, data_saver, notifications_on
            FROM UserSettings WHERE user_id = ?
            """,
            current_user["user_id"]
        )
        row = cursor.fetchone()

    if row is None:
        return UserSettingsOut(
            language="ru", audio_quality="high",
            download_quality="high", data_saver=False,
            notifications_on=True
        )

    return UserSettingsOut(
        language=row[0], audio_quality=row[1],
        download_quality=row[2], data_saver=bool(row[3]),
        notifications_on=bool(row[4])
    )


# ─────────────────────────────────────────────
# PATCH /users/me/settings
# ─────────────────────────────────────────────
@router.patch("/me/settings", response_model=UserSettingsOut)
def update_settings_user(
    body: UserSettingsUpdate,
    current_user=Depends(get_current_user)
):
    user_id = current_user["user_id"]
    with db_context() as conn:
        cursor = conn.cursor()

        updates = []
        params  = []
        if body.language is not None:
            updates.append("language = ?"); params.append(body.language)
        if body.audio_quality is not None:
            updates.append("audio_quality = ?"); params.append(body.audio_quality)
        if body.download_quality is not None:
            updates.append("download_quality = ?"); params.append(body.download_quality)
        if body.data_saver is not None:
            updates.append("data_saver = ?"); params.append(int(body.data_saver))
        if body.notifications_on is not None:
            updates.append("notifications_on = ?"); params.append(int(body.notifications_on))

        if updates:
            updates.append("updated_at = GETDATE()")
            sql = f"UPDATE UserSettings SET {', '.join(updates)} WHERE user_id = ?"
            params.append(user_id)
            cursor.execute(sql, *params)

        cursor.execute(
            """
            SELECT language, audio_quality, download_quality, data_saver, notifications_on
            FROM UserSettings WHERE user_id = ?
            """,
            user_id
        )
        row = cursor.fetchone()

    return UserSettingsOut(
        language=row[0], audio_quality=row[1],
        download_quality=row[2], data_saver=bool(row[3]),
        notifications_on=bool(row[4])
    )
