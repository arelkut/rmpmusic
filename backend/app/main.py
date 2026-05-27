"""
Music App — FastAPI Main Application
Run with: uvicorn app.main:app --reload --port 8000
"""
import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from app.config import get_settings
from app.routers import auth, users, tracks, playlists, search

settings = get_settings()

# ─────────────────────────────────────────────
# App instance
# ─────────────────────────────────────────────
app = FastAPI(
    title="🎵 Music App API",
    description="Backend API for Music Streaming App",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

# ─────────────────────────────────────────────
# CORS (Android needs this for local dev)
# ─────────────────────────────────────────────
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],        # В production укажи точные origins
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ─────────────────────────────────────────────
# Static files (audio, covers, avatars)
# ─────────────────────────────────────────────
os.makedirs("static/audio",   exist_ok=True)
os.makedirs("static/covers",  exist_ok=True)
os.makedirs("static/avatars", exist_ok=True)

app.mount("/static", StaticFiles(directory="static"), name="static")

# ─────────────────────────────────────────────
# Routers
# ─────────────────────────────────────────────
app.include_router(auth.router,      prefix="/api/v1")
app.include_router(users.router,     prefix="/api/v1")
app.include_router(tracks.router,    prefix="/api/v1")
app.include_router(playlists.router, prefix="/api/v1")
app.include_router(search.router,    prefix="/api/v1")


# ─────────────────────────────────────────────
# Health check
# ─────────────────────────────────────────────
@app.get("/", tags=["Health"])
def root():
    return {
        "status": "ok",
        "message": "🎵 Music App API is running",
        "version": "1.0.0",
        "docs": "/docs"
    }


@app.get("/health", tags=["Health"])
def health():
    from app.database import get_connection
    try:
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT 1")
        conn.close()
        db_status = "connected"
    except Exception as e:
        db_status = f"error: {str(e)}"

    return {
        "status": "ok",
        "database": db_status,
        "version": "1.0.0"
    }
