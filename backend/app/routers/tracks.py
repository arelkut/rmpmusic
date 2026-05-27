"""
Tracks router: list, trending, recently played, like/unlike, stream
"""
import os
import shutil
from fastapi import APIRouter, Depends, HTTPException, Query, UploadFile, File
from fastapi.responses import FileResponse
from typing import Optional, List
from app.database import db_context, dict_from_row, list_from_rows
from app.dependencies import get_current_user
from app.schemas import TrackOut, MessageResponse
from app.config import get_settings

settings = get_settings()
router = APIRouter(prefix="/tracks", tags=["Tracks"])

TRACK_QUERY = """
    SELECT
        t.track_id, t.title,
        a.name  AS artist_name, a.artist_id,
        al.title AS album_title,
        COALESCE(t.cover_url, al.cover_url) AS cover_url,
        t.duration_sec, t.file_url,
        t.listen_count, t.like_count,
        g.name AS genre_name
    FROM Tracks t
    JOIN Artists a  ON a.artist_id = t.artist_id
    LEFT JOIN Albums al ON al.album_id = t.album_id
    LEFT JOIN Genres g  ON g.genre_id  = t.genre_id
    WHERE t.is_active = 1
"""


def _rows_to_tracks(rows, user_id: int, conn) -> List[TrackOut]:
    if not rows:
        return []
    result = []
    for row in rows:
        d = dict_from_row(row)
        # Check if liked
        cursor2 = conn.cursor()
        cursor2.execute(
            "SELECT 1 FROM FavoriteTracks WHERE user_id = ? AND track_id = ?",
            user_id, d["track_id"]
        )
        d["is_liked"] = cursor2.fetchone() is not None
        result.append(TrackOut(**d))
    return result


# ─────────────────────────────────────────────
# GET /tracks/trending
# ─────────────────────────────────────────────
@router.get("/trending", response_model=List[TrackOut])
def get_trending(
    limit: int = Query(10, ge=1, le=50),
    current_user=Depends(get_current_user)
):
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            f"{TRACK_QUERY} ORDER BY t.listen_count DESC OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY",
            limit
        )
        rows = cursor.fetchall()
        return _rows_to_tracks(rows, current_user["user_id"], conn)


# ─────────────────────────────────────────────
# GET /tracks/recent
# ─────────────────────────────────────────────
@router.get("/recent", response_model=List[TrackOut])
def get_recent(
    limit: int = Query(20, ge=1, le=50),
    current_user=Depends(get_current_user)
):
    user_id = current_user["user_id"]
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            f"""
            SELECT DISTINCT TOP (?)
                t.track_id, t.title,
                a.name  AS artist_name, a.artist_id,
                al.title AS album_title,
                COALESCE(t.cover_url, al.cover_url) AS cover_url,
                t.duration_sec, t.file_url,
                t.listen_count, t.like_count,
                g.name AS genre_name,
                MAX(lh.listened_at) AS last_listened
            FROM ListenHistory lh
            JOIN Tracks  t  ON t.track_id  = lh.track_id  AND t.is_active = 1
            JOIN Artists a  ON a.artist_id  = t.artist_id
            LEFT JOIN Albums al ON al.album_id = t.album_id
            LEFT JOIN Genres g  ON g.genre_id  = t.genre_id
            WHERE lh.user_id = ?
            GROUP BY t.track_id, t.title, a.name, a.artist_id,
                     al.title, t.cover_url, al.cover_url,
                     t.duration_sec, t.file_url, t.listen_count, t.like_count, g.name
            ORDER BY last_listened DESC
            """,
            limit, user_id
        )
        rows = cursor.fetchall()
        return _rows_to_tracks(rows, user_id, conn)


# ─────────────────────────────────────────────
# GET /tracks/{track_id}
# ─────────────────────────────────────────────
@router.get("/{track_id}", response_model=TrackOut)
def get_track(track_id: int, current_user=Depends(get_current_user)):
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            f"{TRACK_QUERY} AND t.track_id = ?", track_id
        )
        row = cursor.fetchone()
        if row is None:
            raise HTTPException(status_code=404, detail="Track not found")
        result = _rows_to_tracks([row], current_user["user_id"], conn)
    return result[0]


# ─────────────────────────────────────────────
# POST /tracks/{track_id}/play  — запись прослушивания
# ─────────────────────────────────────────────
@router.post("/{track_id}/play", response_model=MessageResponse)
def record_play(
    track_id: int,
    duration_listened: int = 0,
    current_user=Depends(get_current_user)
):
    user_id = current_user["user_id"]
    with db_context() as conn:
        cursor = conn.cursor()
        # Check track exists
        cursor.execute("SELECT track_id, duration_sec FROM Tracks WHERE track_id = ? AND is_active = 1", track_id)
        track = cursor.fetchone()
        if track is None:
            raise HTTPException(status_code=404, detail="Track not found")

        # Insert history
        cursor.execute(
            "INSERT INTO ListenHistory (user_id, track_id, duration_listened) VALUES (?, ?, ?)",
            user_id, track_id, duration_listened
        )

        # Increment listen count
        cursor.execute(
            "UPDATE Tracks SET listen_count = listen_count + 1 WHERE track_id = ?", track_id
        )

        # Update user stats
        duration_hours = duration_listened / 3600.0
        cursor.execute(
            """
            UPDATE UserStats
            SET total_listen_hours  = total_listen_hours  + ?,
                total_tracks_played = total_tracks_played + 1,
                updated_at = GETDATE()
            WHERE user_id = ?
            """,
            duration_hours, user_id
        )

    return MessageResponse(message="Play recorded")


# ─────────────────────────────────────────────
# POST /tracks/{track_id}/like
# ─────────────────────────────────────────────
@router.post("/{track_id}/like", response_model=MessageResponse)
def like_track(track_id: int, current_user=Depends(get_current_user)):
    user_id = current_user["user_id"]
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            "SELECT 1 FROM FavoriteTracks WHERE user_id = ? AND track_id = ?",
            user_id, track_id
        )
        if cursor.fetchone():
            raise HTTPException(status_code=400, detail="Already liked")
        cursor.execute(
            "INSERT INTO FavoriteTracks (user_id, track_id) VALUES (?, ?)",
            user_id, track_id
        )
    return MessageResponse(message="Track liked")


# ─────────────────────────────────────────────
# DELETE /tracks/{track_id}/like
# ─────────────────────────────────────────────
@router.delete("/{track_id}/like", response_model=MessageResponse)
def unlike_track(track_id: int, current_user=Depends(get_current_user)):
    user_id = current_user["user_id"]
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            "DELETE FROM FavoriteTracks WHERE user_id = ? AND track_id = ?",
            user_id, track_id
        )
    return MessageResponse(message="Track unliked")


# ─────────────────────────────────────────────
# GET /tracks/favorites/list
# ─────────────────────────────────────────────
@router.get("/favorites/list", response_model=List[TrackOut])
def get_favorites(current_user=Depends(get_current_user)):
    user_id = current_user["user_id"]
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            f"""
            SELECT t.track_id, t.title,
                   a.name  AS artist_name, a.artist_id,
                   al.title AS album_title,
                   COALESCE(t.cover_url, al.cover_url) AS cover_url,
                   t.duration_sec, t.file_url,
                   t.listen_count, t.like_count,
                   g.name AS genre_name
            FROM FavoriteTracks ft
            JOIN Tracks  t  ON t.track_id  = ft.track_id AND t.is_active = 1
            JOIN Artists a  ON a.artist_id  = t.artist_id
            LEFT JOIN Albums al ON al.album_id = t.album_id
            LEFT JOIN Genres g  ON g.genre_id  = t.genre_id
            WHERE ft.user_id = ?
            ORDER BY ft.added_at DESC
            """,
            user_id
        )
        rows = cursor.fetchall()
        return _rows_to_tracks(rows, user_id, conn)


# ─────────────────────────────────────────────
# POST /tracks/upload  — загрузка нового трека (admin/artist)
# ─────────────────────────────────────────────
@router.post("/upload", response_model=MessageResponse, status_code=201)
def upload_track(
    title: str,
    artist_id: int,
    album_id: Optional[int] = None,
    genre_id: Optional[int] = None,
    duration_sec: int = 0,
    audio_file: UploadFile = File(...),
    cover_file: Optional[UploadFile] = File(None),
    current_user=Depends(get_current_user)
):
    os.makedirs(settings.audio_dir, exist_ok=True)
    os.makedirs(settings.covers_dir, exist_ok=True)

    # Save audio
    audio_ext = os.path.splitext(audio_file.filename)[1].lower()
    if audio_ext not in [".mp3", ".flac", ".wav", ".ogg", ".m4a"]:
        raise HTTPException(status_code=400, detail="Invalid audio format")

    safe_title = "".join(c for c in title if c.isalnum() or c in " _-").strip().replace(" ", "_")
    audio_filename = f"{safe_title}_{artist_id}{audio_ext}"
    audio_path = os.path.join(settings.audio_dir, audio_filename)

    with open(audio_path, "wb") as f:
        shutil.copyfileobj(audio_file.file, f)

    audio_url = f"/static/audio/{audio_filename}"

    # Save cover (optional)
    cover_url = None
    if cover_file and cover_file.filename:
        cover_ext = os.path.splitext(cover_file.filename)[1].lower()
        cover_filename = f"{safe_title}_{artist_id}{cover_ext}"
        cover_path = os.path.join(settings.covers_dir, cover_filename)
        with open(cover_path, "wb") as f:
            shutil.copyfileobj(cover_file.file, f)
        cover_url = f"/static/covers/{cover_filename}"

    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            INSERT INTO Tracks (title, artist_id, album_id, genre_id, duration_sec, file_url, cover_url)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            title, artist_id, album_id, genre_id, duration_sec, audio_url, cover_url
        )

    return MessageResponse(message=f"Track '{title}' uploaded successfully")


# ─────────────────────────────────────────────
# GET /tracks/stream/{track_id}  — стриминг аудио
# ─────────────────────────────────────────────
@router.get("/stream/{track_id}")
def stream_track(track_id: int, current_user=Depends(get_current_user)):
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            "SELECT file_url, title FROM Tracks WHERE track_id = ? AND is_active = 1",
            track_id
        )
        row = cursor.fetchone()

    if row is None:
        raise HTTPException(status_code=404, detail="Track not found")

    file_url, title = row

    # Resolve local path
    # file_url like "/static/audio/privet.mp3"
    local_path = file_url.lstrip("/")

    if not os.path.exists(local_path):
        raise HTTPException(status_code=404, detail=f"Audio file not found: {local_path}")

    return FileResponse(
        path=local_path,
        media_type="audio/mpeg",
        filename=os.path.basename(local_path),
    )
