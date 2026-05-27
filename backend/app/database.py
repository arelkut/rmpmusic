"""
Database connection module using pyodbc (SQL Server / SSMS)
"""
import pyodbc
from contextlib import contextmanager
from typing import Generator
from app.config import get_settings

settings = get_settings()


def get_connection_string() -> str:
    """Build the ODBC connection string based on settings."""
    if settings.db_trusted.lower() == "yes":
        return (
            f"DRIVER={{{settings.db_driver}}};"
            f"SERVER={settings.db_server};"
            f"DATABASE={settings.db_name};"
            "Trusted_Connection=yes;"
            "TrustServerCertificate=yes;"
        )
    else:
        return (
            f"DRIVER={{{settings.db_driver}}};"
            f"SERVER={settings.db_server};"
            f"DATABASE={settings.db_name};"
            f"UID={settings.db_user};"
            f"PWD={settings.db_password};"
            "TrustServerCertificate=yes;"
        )


def get_connection() -> pyodbc.Connection:
    """Return a new pyodbc connection."""
    conn_str = get_connection_string()
    return pyodbc.connect(conn_str, autocommit=False)


@contextmanager
def db_context() -> Generator[pyodbc.Connection, None, None]:
    """Context manager for database connections with auto-commit/rollback."""
    conn = get_connection()
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def dict_from_row(row) -> dict:
    """Convert pyodbc row to dict."""
    if row is None:
        return None
    columns = [column[0] for column in row.cursor_description]
    return dict(zip(columns, row))


def list_from_rows(rows) -> list[dict]:
    """Convert list of pyodbc rows to list of dicts."""
    if not rows:
        return []
    columns = [column[0] for column in rows[0].cursor_description]
    return [dict(zip(columns, row)) for row in rows]
