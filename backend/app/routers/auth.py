"""
Authentication router: register, login, logout, refresh, forgot/reset password
"""
import secrets
from datetime import datetime, timedelta, timezone
from fastapi import APIRouter, HTTPException, status
from app.database import db_context, dict_from_row
from app.security import hash_password, verify_password, create_access_token, create_refresh_token, decode_token
from app.schemas import (
    RegisterRequest, LoginRequest, TokenResponse,
    RefreshRequest, ForgotPasswordRequest, ResetPasswordRequest, MessageResponse
)
from app.config import get_settings

settings = get_settings()
router = APIRouter(prefix="/auth", tags=["Auth"])


# ─────────────────────────────────────────────
# POST /auth/register
# ─────────────────────────────────────────────
@router.post("/register", response_model=TokenResponse, status_code=201)
def register(body: RegisterRequest):
    with db_context() as conn:
        cursor = conn.cursor()

        # Check existing email
        cursor.execute("SELECT user_id FROM Users WHERE email = ?", body.email)
        if cursor.fetchone():
            raise HTTPException(status_code=400, detail="Email already registered")

        # Check existing username
        cursor.execute("SELECT user_id FROM Users WHERE username = ?", body.username)
        if cursor.fetchone():
            raise HTTPException(status_code=400, detail="Username already taken")

        # Insert user
        pw_hash = hash_password(body.password)
        display = body.display_name or body.username
        cursor.execute(
            """
            INSERT INTO Users (username, email, password_hash, display_name, avatar_url, is_active, is_verified)
            OUTPUT INSERTED.user_id
            VALUES (?, ?, ?, ?, '/static/avatars/default.jpg', 1, 0)
            """,
            body.username, body.email, pw_hash, display
        )
        row = cursor.fetchone()
        user_id = row[0]

        # Create default stats & settings
        cursor.execute(
            "INSERT INTO UserStats (user_id) VALUES (?)", user_id
        )
        cursor.execute(
            "INSERT INTO UserSettings (user_id) VALUES (?)", user_id
        )

        # Create default "Любимые треки" playlist
        cursor.execute(
            """
            INSERT INTO Playlists (user_id, name, description, is_public)
            VALUES (?, N'Любимые треки', N'Мои любимые треки', 1)
            """,
            user_id
        )

    # Generate tokens
    access  = create_access_token({"sub": str(user_id)})
    refresh = create_refresh_token({"sub": str(user_id)})

    # Save refresh token
    with db_context() as conn:
        cursor = conn.cursor()
        expires_at = datetime.now(timezone.utc) + timedelta(days=settings.refresh_token_expire_days)
        cursor.execute(
            "INSERT INTO RefreshTokens (user_id, token, expires_at) VALUES (?, ?, ?)",
            user_id, refresh, expires_at
        )

    return TokenResponse(
        access_token=access,
        refresh_token=refresh,
        user_id=user_id,
        display_name=display,
        username=body.username,
    )


# ─────────────────────────────────────────────
# POST /auth/login
# ─────────────────────────────────────────────
@router.post("/login", response_model=TokenResponse)
def login(body: LoginRequest):
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            "SELECT user_id, username, password_hash, display_name, is_active FROM Users WHERE email = ?",
            body.email
        )
        row = cursor.fetchone()

    if row is None:
        raise HTTPException(status_code=401, detail="Invalid email or password")

    user_id, username, pw_hash, display_name, is_active = row

    if not is_active:
        raise HTTPException(status_code=403, detail="Account is disabled")

    if not verify_password(body.password, pw_hash):
        raise HTTPException(status_code=401, detail="Invalid email or password")

    access  = create_access_token({"sub": str(user_id)})
    refresh = create_refresh_token({"sub": str(user_id)})

    with db_context() as conn:
        cursor = conn.cursor()
        expires_at = datetime.now(timezone.utc) + timedelta(days=settings.refresh_token_expire_days)
        cursor.execute(
            "INSERT INTO RefreshTokens (user_id, token, expires_at) VALUES (?, ?, ?)",
            user_id, refresh, expires_at
        )

    return TokenResponse(
        access_token=access,
        refresh_token=refresh,
        user_id=user_id,
        display_name=display_name or username,
        username=username,
    )


# ─────────────────────────────────────────────
# POST /auth/refresh
# ─────────────────────────────────────────────
@router.post("/refresh", response_model=TokenResponse)
def refresh_token(body: RefreshRequest):
    payload = decode_token(body.refresh_token)
    if payload is None or payload.get("type") != "refresh":
        raise HTTPException(status_code=401, detail="Invalid refresh token")

    user_id = payload.get("sub")

    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            SELECT token_id FROM RefreshTokens
            WHERE token = ? AND is_revoked = 0 AND expires_at > GETDATE()
            """,
            body.refresh_token
        )
        row = cursor.fetchone()
        if row is None:
            raise HTTPException(status_code=401, detail="Refresh token expired or revoked")

        # Revoke old token
        cursor.execute(
            "UPDATE RefreshTokens SET is_revoked = 1 WHERE token = ?",
            body.refresh_token
        )

        # Get user info
        cursor.execute(
            "SELECT username, display_name FROM Users WHERE user_id = ?",
            int(user_id)
        )
        u = cursor.fetchone()
        if u is None:
            raise HTTPException(status_code=401, detail="User not found")
        username, display_name = u

    new_access  = create_access_token({"sub": user_id})
    new_refresh = create_refresh_token({"sub": user_id})

    with db_context() as conn:
        cursor = conn.cursor()
        expires_at = datetime.now(timezone.utc) + timedelta(days=settings.refresh_token_expire_days)
        cursor.execute(
            "INSERT INTO RefreshTokens (user_id, token, expires_at) VALUES (?, ?, ?)",
            int(user_id), new_refresh, expires_at
        )

    return TokenResponse(
        access_token=new_access,
        refresh_token=new_refresh,
        user_id=int(user_id),
        display_name=display_name or username,
        username=username,
    )


# ─────────────────────────────────────────────
# POST /auth/logout
# ─────────────────────────────────────────────
@router.post("/logout", response_model=MessageResponse)
def logout(body: RefreshRequest):
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            "UPDATE RefreshTokens SET is_revoked = 1 WHERE token = ?",
            body.refresh_token
        )
    return MessageResponse(message="Logged out successfully")


# ─────────────────────────────────────────────
# POST /auth/forgot-password
# ─────────────────────────────────────────────
@router.post("/forgot-password", response_model=MessageResponse)
def forgot_password(body: ForgotPasswordRequest):
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            "SELECT user_id FROM Users WHERE email = ?", body.email
        )
        row = cursor.fetchone()

        if row is None:
            # Don't reveal if email exists
            return MessageResponse(message="If that email exists, instructions were sent.")

        user_id = row[0]
        token = secrets.token_urlsafe(32)
        expires_at = datetime.now(timezone.utc) + timedelta(hours=1)

        cursor.execute(
            "INSERT INTO PasswordResetTokens (user_id, token, expires_at) VALUES (?, ?, ?)",
            user_id, token, expires_at
        )

    # In production: send email with token. For dev: return in response
    return MessageResponse(
        message=f"Reset token (dev mode): {token}"
    )


# ─────────────────────────────────────────────
# POST /auth/reset-password
# ─────────────────────────────────────────────
@router.post("/reset-password", response_model=MessageResponse)
def reset_password(body: ResetPasswordRequest):
    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            SELECT token_id, user_id FROM PasswordResetTokens
            WHERE token = ? AND is_used = 0 AND expires_at > GETDATE()
            """,
            body.token
        )
        row = cursor.fetchone()

        if row is None:
            raise HTTPException(status_code=400, detail="Invalid or expired reset token")

        token_id, user_id = row
        new_hash = hash_password(body.new_password)

        cursor.execute(
            "UPDATE Users SET password_hash = ?, updated_at = GETDATE() WHERE user_id = ?",
            new_hash, user_id
        )
        cursor.execute(
            "UPDATE PasswordResetTokens SET is_used = 1 WHERE token_id = ?",
            token_id
        )

    return MessageResponse(message="Password reset successfully")
