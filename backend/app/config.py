from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    # Database
    db_server:   str = r"localhost\SQLEXPRESS"
    db_name:     str = "MusicAppDB"
    db_driver:   str = "ODBC Driver 17 for SQL Server"
    db_user:     str = "sa"
    db_password: str = "YourPassword123!"
    db_trusted:  str = "no"

    # JWT
    secret_key:                   str = "supersecretkey_change_in_production_12345678"
    algorithm:                    str = "HS256"
    access_token_expire_minutes:  int = 60
    refresh_token_expire_days:    int = 30

    # App
    app_host:  str  = "0.0.0.0"
    app_port:  int  = 8000
    app_debug: bool = True

    # Media
    static_dir:  str = "static"
    audio_dir:   str = "static/audio"
    covers_dir:  str = "static/covers"
    avatars_dir: str = "static/avatars"

    class Config:
        env_file = ".env"


@lru_cache()
def get_settings() -> Settings:
    return Settings()
