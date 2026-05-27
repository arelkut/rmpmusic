@echo off
echo ====================================
echo  Music App Backend — Setup
echo ====================================
echo.

cd /d "%~dp0"

:: Create virtual environment
python -m venv venv
call venv\Scripts\activate.bat

:: Install dependencies
echo Installing dependencies...
pip install -r requirements.txt

:: Create static directories
mkdir static\audio 2>nul
mkdir static\covers 2>nul
mkdir static\avatars 2>nul

echo.
echo ====================================
echo  Setup complete!
echo  Run start.bat to launch the API
echo  Then open: http://localhost:8000/docs
echo ====================================
pause
