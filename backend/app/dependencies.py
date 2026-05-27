"""
FastAPI dependency injection
"""
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from app.security import decode_token
from app.database import db_context, dict_from_row

security = HTTPBearer()


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
):
    token = credentials.credentials
    payload = decode_token(token)

    if payload is None or payload.get("type") != "access":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
            headers={"WWW-Authenticate": "Bearer"},
        )

    user_id = payload.get("sub")
    if user_id is None:
        raise HTTPException(status_code=401, detail="Token missing subject")

    with db_context() as conn:
        cursor = conn.cursor()
        cursor.execute(
            "SELECT user_id, username, email, display_name, avatar_url, is_active "
            "FROM Users WHERE user_id = ? AND is_active = 1",
            int(user_id),
        )
        row = cursor.fetchone()

    if row is None:
        raise HTTPException(status_code=401, detail="User not found")

    return dict_from_row(row)
