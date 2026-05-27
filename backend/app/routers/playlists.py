"""
Playlists router: CRUD, add/remove tracks, get tracks
"""
import os
import shutil
from fastapi import APIRouter, Depends, HTTPException, UploadFile, File
from typing import List, Optional
from app.database import db_context, dict_from_row
from app.dependencies import get_current_user
from app.schemas import (
    PlaylistOut, PlaylistCreateRequest, PlaylistUpdateRequest,
    AddTrackToPlaylistRequest, TrackOut, MessageResponse
)
from app.config import get_settings

settings = get_settings()
router = APIRouter(prefix="/playlists", tags=["Playlists"])


def _track_rows(rows, user_id: int, conn) -> List[TrackOut]:
    if not rows:
        return []
    result = []
    for row in rows:
        columns = [c[0] for c in row.cursor_description]
        d = dict(zip(columns, row))
        cursor2 = conn.cursor()
        cursor2.execute(
            "SELECT 1 FROM FavoriteTracks WHERE user_id = ? AND track_id = ?",
            user_id, d["track_id"]
        )
        d["is_liked"] = cursor2.fetchone() is not None
        result.append(TrackOut(**d))
    return result


# ─────────────────────────────────────────────
# GET /playlists/my  — мои плейлисты
# ─────────────────────────────────────────────
@router.get("/my", response_model=List[PlaylistOut])
def get_my_playlists(current_user=Depends(get_current_user)):
    user_id = current_user["user_id"]
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            SELECT playlist_id, user_id, name, description,
                   cover_url, track_count, is_public, created_at
            FROM Playlists
            WHERE user_id = ?
            ORDER BY created_at DESC
            """,
            user_id
        )
        rows = cursor.fetchall()

    result = []
    for row in rows:
        columns = [c[0] for c in row.cursor_description]
        result.append(PlaylistOut(**dict(zip(columns, row))))
    return result


# ─────────────────────────────────────────────
# GET /playlists/recommended
# ─────────────────────────────────────────────
@router.get("/recommended", response_model=List[PlaylistOut])
def get_recommended(
    limit: int = 6,
    current_user=Depends(get_current_user)
):
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            SELECT TOP (?) playlist_id, user_id, name, description,
                   cover_url, track_count, is_public, created_at
            FROM Playlists
            WHERE is_public = 1
            ORDER BY track_count DESC
            """,
            limit
        )
        rows = cursor.fetchall()

    result = []
    for row in rows:
        columns = [c[0] for c in row.cursor_description]
        result.append(PlaylistOut(**dict(zip(columns, row))))
    return result


# ─────────────────────────────────────────────
# POST /playlists
# ─────────────────────────────────────────────
@router.post("/", response_model=PlaylistOut, status_code=201)
def create_playlist(
    body: PlaylistCreateRequest,
    current_user=Depends(get_current_user)
):
    user_id = current_user["user_id"]
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            INSERT INTO Playlists (user_id, name, description, is_public)
            OUTPUT INSERTED.playlist_id, INSERTED.user_id, INSERTED.name,
                   INSERTED.description, INSERTED.cover_url, INSERTED.track_count,
                   INSERTED.is_public, INSERTED.created_at
            VALUES (?, ?, ?, ?)
            """,
            user_id, body.name, body.description, int(body.is_public)
        )
        row = cursor.fetchone()
        columns = [c[0] for c in row.cursor_description]
    return PlaylistOut(**dict(zip(columns, row)))


# ─────────────────────────────────────────────
# GET /playlists/{playlist_id}
# ─────────────────────────────────────────────
@router.get("/{playlist_id}", response_model=PlaylistOut)
def get_playlist(playlist_id: int, current_user=Depends(get_current_user)):
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            SELECT playlist_id, user_id, name, description,
                   cover_url, track_count, is_public, created_at
            FROM Playlists
            WHERE playlist_id = ? AND (user_id = ? OR is_public = 1)
            """,
            playlist_id, current_user["user_id"]
        )
        row = cursor.fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail="Playlist not found")
    columns = [c[0] for c in row.cursor_description]
    return PlaylistOut(**dict(zip(columns, row)))


# ─────────────────────────────────────────────
# PATCH /playlists/{playlist_id}
# ─────────────────────────────────────────────
@router.patch("/{playlist_id}", response_model=PlaylistOut)
def update_playlist(
    playlist_id: int,
    body: PlaylistUpdateRequest,
    current_user=Depends(get_current_user)
):
    user_id = current_user["user_id"]
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            "SELECT user_id FROM Playlists WHERE playlist_id = ?", playlist_id
        )
        row = cursor.fetchone()
        if row is None:
            raise HTTPException(status_code=404, detail="Playlist not found")
        if row[0] != user_id:
            raise HTTPException(status_code=403, detail="Not your playlist")

        updates, params = [], []
        if body.name is not None:
            updates.append("name = ?"); params.append(body.name)
        if body.description is not None:
            updates.append("description = ?"); params.append(body.description)
        if body.is_public is not None:
            updates.append("is_public = ?"); params.append(int(body.is_public))

        if updates:
            updates.append("updated_at = GETDATE()")
            sql = f"UPDATE Playlists SET {', '.join(updates)} WHERE playlist_id = ?"
            params.append(playlist_id)
            cursor.execute(sql, *params)

        cursor.execute(
            """
            SELECT playlist_id, user_id, name, description,
                   cover_url, track_count, is_public, created_at
            FROM Playlists WHERE playlist_id = ?
            """,
            playlist_id
        )
        row = cursor.fetchone()
        columns = [c[0] for c in row.cursor_description]
    return PlaylistOut(**dict(zip(columns, row)))


# ─────────────────────────────────────────────
# DELETE /playlists/{playlist_id}
# ─────────────────────────────────────────────
@router.delete("/{playlist_id}", response_model=MessageResponse)
def delete_playlist(playlist_id: int, current_user=Depends(get_current_user)):
    user_id = current_user["user_id"]
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            "SELECT user_id FROM Playlists WHERE playlist_id = ?", playlist_id
        )
        row = cursor.fetchone()
        if row is None:
            raise HTTPException(status_code=404, detail="Playlist not found")
        if row[0] != user_id:
            raise HTTPException(status_code=403, detail="Not your playlist")
        cursor.execute("DELETE FROM Playlists WHERE playlist_id = ?", playlist_id)
    return MessageResponse(message="Playlist deleted")


# ─────────────────────────────────────────────
# GET /playlists/{playlist_id}/tracks
# ─────────────────────────────────────────────
@router.get("/{playlist_id}/tracks", response_model=List[TrackOut])
def get_playlist_tracks(playlist_id: int, current_user=Depends(get_current_user)):
    user_id = current_user["user_id"]
    with db_context() as conn:
        cursor = conn.cursor()
        # Check access
        cursor.execute(
            "SELECT user_id, is_public FROM Playlists WHERE playlist_id = ?", playlist_id
        )
        pl = cursor.fetchone()
        if pl is None:
            raise HTTPException(status_code=404, detail="Playlist not found")
        if pl[0] != user_id and not pl[1]:
            raise HTTPException(status_code=403, detail="Playlist is private")

        cursor.execute(
            """
            SELECT t.track_id, t.title,
                   a.name  AS artist_name, a.artist_id,
                   al.title AS album_title,
                   COALESCE(t.cover_url, al.cover_url) AS cover_url,
                   t.duration_sec, t.file_url,
                   t.listen_count, t.like_count,
                   g.name AS genre_name
            FROM PlaylistTracks pt
            JOIN Tracks  t  ON t.track_id  = pt.track_id AND t.is_active = 1
            JOIN Artists a  ON a.artist_id  = t.artist_id
            LEFT JOIN Albums al ON al.album_id = t.album_id
            LEFT JOIN Genres g  ON g.genre_id  = t.genre_id
            WHERE pt.playlist_id = ?
            ORDER BY pt.track_order
            """,
            playlist_id
        )
        rows = cursor.fetchall()
        return _track_rows(rows, user_id, conn)


# ─────────────────────────────────────────────
# POST /playlists/{playlist_id}/tracks
# ─────────────────────────────────────────────
@router.post("/{playlist_id}/tracks", response_model=MessageResponse)
def add_track_to_playlist(
    playlist_id: int,
    body: AddTrackToPlaylistRequest,
    current_user=Depends(get_current_user)
):
    user_id = current_user["user_id"]
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            "SELECT user_id FROM Playlists WHERE playlist_id = ?", playlist_id
        )
        pl = cursor.fetchone()
        if pl is None:
            raise HTTPException(status_code=404, detail="Playlist not found")
        if pl[0] != user_id:
            raise HTTPException(status_code=403, detail="Not your playlist")

        # Check duplicate
        cursor.execute(
            "SELECT 1 FROM PlaylistTracks WHERE playlist_id = ? AND track_id = ?",
            playlist_id, body.track_id
        )
        if cursor.fetchone():
            raise HTTPException(status_code=400, detail="Track already in playlist")

        # Max order
        cursor.execute(
            "SELECT ISNULL(MAX(track_order), 0) + 1 FROM PlaylistTracks WHERE playlist_id = ?",
            playlist_id
        )
        next_order = cursor.fetchone()[0]

        cursor.execute(
            "INSERT INTO PlaylistTracks (playlist_id, track_id, track_order) VALUES (?, ?, ?)",
            playlist_id, body.track_id, next_order
        )

    return MessageResponse(message="Track added to playlist")


# ─────────────────────────────────────────────
# DELETE /playlists/{playlist_id}/tracks/{track_id}
# ─────────────────────────────────────────────
@router.delete("/{playlist_id}/tracks/{track_id}", response_model=MessageResponse)
def remove_track_from_playlist(
    playlist_id: int,
    track_id: int,
    current_user=Depends(get_current_user)
):
    user_id = current_user["user_id"]
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            "SELECT user_id FROM Playlists WHERE playlist_id = ?", playlist_id
        )
        pl = cursor.fetchone()
        if pl is None:
            raise HTTPException(status_code=404, detail="Playlist not found")
        if pl[0] != user_id:
            raise HTTPException(status_code=403, detail="Not your playlist")

        cursor.execute(
            "DELETE FROM PlaylistTracks WHERE playlist_id = ? AND track_id = ?",
            playlist_id, track_id
        )
    return MessageResponse(message="Track removed from playlist")


# ─────────────────────────────────────────────
# POST /playlists/{playlist_id}/cover
# ─────────────────────────────────────────────
@router.post("/{playlist_id}/cover", response_model=MessageResponse)
def upload_playlist_cover(
    playlist_id: int,
    file: UploadFile = File(...),
    current_user=Depends(get_current_user)
):
    user_id = current_user["user_id"]
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            "SELECT user_id FROM Playlists WHERE playlist_id = ?", playlist_id
        )
        pl = cursor.fetchone()
        if pl is None:
            raise HTTPException(status_code=404, detail="Playlist not found")
        if pl[0] != user_id:
            raise HTTPException(status_code=403, detail="Not your playlist")

    ext = os.path.splitext(file.filename)[1].lower()
    if ext not in [".jpg", ".jpeg", ".png", ".webp"]:
        raise HTTPException(status_code=400, detail="Invalid image format")

    os.makedirs(settings.covers_dir, exist_ok=True)
    filename = f"playlist_{playlist_id}{ext}"
    filepath = os.path.join(settings.covers_dir, filename)
    with open(filepath, "wb") as f:
        shutil.copyfileobj(file.file, f)

    cover_url = f"/static/covers/{filename}"
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            "UPDATE Playlists SET cover_url = ?, updated_at = GETDATE() WHERE playlist_id = ?",
            cover_url, playlist_id
        )
    return MessageResponse(message=f"Cover updated: {cover_url}")
