@echo off
echo Starting Music App API...
echo.
cd /d "%~dp0"

:: Activate virtual environment if exists
if exist "venv\Scripts\activate.bat" (
    call venv\Scripts\activate.bat
) else (
    echo [WARNING] No virtual environment found. Using system Python.
)

:: Start uvicorn
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

pause
