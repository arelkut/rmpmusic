"""
Search router: full-text search across tracks, artists, albums, playlists
"""
from fastapi import APIRouter, Depends, Query
from typing import List
from app.database import db_context
from app.dependencies import get_current_user
from app.schemas import SearchResponse, TrackOut, ArtistOut, AlbumOut, PlaylistOut, GenreOut

router = APIRouter(prefix="/search", tags=["Search"])


def _track_rows(rows, user_id: int, conn) -> List[TrackOut]:
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
# GET /search?q=...
# ─────────────────────────────────────────────
@router.get("/", response_model=SearchResponse)
def search(
    q: str = Query(..., min_length=1),
    limit: int = Query(10, ge=1, le=50),
    current_user=Depends(get_current_user)
):
    user_id = current_user["user_id"]
    pattern = f"%{q}%"

    with db_context() as conn:
        cursor = conn.cursor()

        # Tracks
        cursor.execute(
            f"""
            SELECT TOP (?)
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
              AND (t.title LIKE ? OR a.name LIKE ? OR g.name LIKE ?)
            ORDER BY t.listen_count DESC
            """,
            limit, pattern, pattern, pattern
        )
        track_rows = cursor.fetchall()
        tracks = _track_rows(track_rows, user_id, conn)

        # Artists
        cursor.execute(
            f"""
            SELECT TOP (?) artist_id, name, avatar_url, monthly_listeners, is_verified
            FROM Artists
            WHERE name LIKE ?
            ORDER BY monthly_listeners DESC
            """,
            limit, pattern
        )
        artist_rows = cursor.fetchall()
        artists = []
        for row in artist_rows:
            columns = [c[0] for c in row.cursor_description]
            artists.append(ArtistOut(**dict(zip(columns, row))))

        # Albums
        cursor.execute(
            f"""
            SELECT TOP (?) al.album_id, al.title,
                   a.name AS artist_name,
                   al.cover_url, al.listen_count,
                   CONVERT(VARCHAR(10), al.release_date, 120) AS release_date
            FROM Albums al
            JOIN Artists a ON a.artist_id = al.artist_id
            WHERE al.title LIKE ? OR a.name LIKE ?
            ORDER BY al.listen_count DESC
            """,
            limit, pattern, pattern
        )
        album_rows = cursor.fetchall()
        albums = []
        for row in album_rows:
            columns = [c[0] for c in row.cursor_description]
            albums.append(AlbumOut(**dict(zip(columns, row))))

        # Playlists
        cursor.execute(
            f"""
            SELECT TOP (?) playlist_id, user_id, name, description,
                   cover_url, track_count, is_public, created_at
            FROM Playlists
            WHERE is_public = 1 AND name LIKE ?
            ORDER BY track_count DESC
            """,
            limit, pattern
        )
        playlist_rows = cursor.fetchall()
        playlists = []
        for row in playlist_rows:
            columns = [c[0] for c in row.cursor_description]
            playlists.append(PlaylistOut(**dict(zip(columns, row))))

    return SearchResponse(tracks=tracks, artists=artists, albums=albums, playlists=playlists)


# ─────────────────────────────────────────────
# GET /search/genres
# ─────────────────────────────────────────────
@router.get("/genres", response_model=List[GenreOut])
def get_genres(current_user=Depends(get_current_user)):
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute("SELECT genre_id, name, color_hex FROM Genres ORDER BY genre_id")
        rows = cursor.fetchall()
    result = []
    for row in rows:
        columns = [c[0] for c in row.cursor_description]
        result.append(GenreOut(**dict(zip(columns, row))))
    return result


# ─────────────────────────────────────────────
# GET /search/by-genre/{genre_id}
# ─────────────────────────────────────────────
@router.get("/by-genre/{genre_id}", response_model=List[TrackOut])
def search_by_genre(
    genre_id: int,
    limit: int = Query(20, ge=1, le=50),
    current_user=Depends(get_current_user)
):
    user_id = current_user["user_id"]
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            f"""
            SELECT TOP (?)
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
            WHERE t.is_active = 1 AND t.genre_id = ?
            ORDER BY t.listen_count DESC
            """,
            limit, genre_id
        )
        rows = cursor.fetchall()
        return _track_rows(rows, user_id, conn)


# ─────────────────────────────────────────────
# GET /search/albums  — топ альбомов
# ─────────────────────────────────────────────
@router.get("/albums", response_model=List[AlbumOut])
def get_trending_albums(
    limit: int = Query(10, ge=1, le=50),
    current_user=Depends(get_current_user)
):
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            f"""
            SELECT TOP (?) al.album_id, al.title,
                   a.name AS artist_name,
                   al.cover_url, al.listen_count,
                   CONVERT(VARCHAR(10), al.release_date, 120) AS release_date
            FROM Albums al
            JOIN Artists a ON a.artist_id = al.artist_id
            ORDER BY al.listen_count DESC
            """,
            limit
        )
        rows = cursor.fetchall()
    result = []
    for row in rows:
        columns = [c[0] for c in row.cursor_description]
        result.append(AlbumOut(**dict(zip(columns, row))))
    return result
