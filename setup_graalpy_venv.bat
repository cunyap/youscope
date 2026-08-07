@echo off
REM One-click YouScope Python setup
REM Double-click from Windows Explorer or run from cmd/PowerShell.
REM Automatically requests Administrator rights if needed.
REM Requirements: Internet connection. No Python/conda pre-installed needed.

setlocal
cd /d "%~dp0"
title YouScope Python Setup

echo =========================================
echo  YouScope Python Environment Setup
echo =========================================
echo.
echo This will:
echo   1. Download uv (~20MB, fast Python manager)
echo   2. Download GraalPy standalone (~600MB, for YouScope Java scripting)
echo   3. Create GraalPy venv  (numpy, tifffile, pybis)
echo   4. Create CPython venv  (cellpose, stardist, scikit-image)
echo.
echo Total download: ~1.2 GB. Time: 5-15 min depending on connection.
echo.

REM Check for admin rights
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo Requesting Administrator privileges...
    powershell -Command "Start-Process cmd -ArgumentList '/c cd /d \"%~dp0\" && \"%~f0\"' -Verb RunAs"
    exit /b
)

REM Check PowerShell version
for /f "tokens=2" %%v in ('powershell -Command "$PSVersionTable.PSVersion.Major" 2^>nul') do set PSMajor=%%v
if "%PSMajor%"=="" set PSMajor=0
if %PSMajor% LSS 5 (
    echo ERROR: PowerShell 5 or higher required.
    echo Current version: %PSMajor%
    echo Download from: https://aka.ms/PSWindows
    pause
    exit /b 1
)

echo Running setup script...
echo (This window will stay open until setup completes)
echo.

powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0setup_graalpy_venv.ps1"

if %errorLevel% neq 0 (
    echo.
    echo =========================================
    echo  Setup encountered issues (see above)
    echo =========================================
    echo.
    echo Common fixes:
    echo   - Run as Administrator
    echo   - Check internet connection
    echo   - Disable antivirus temporarily
    echo   - Try: powershell -ExecutionPolicy Bypass -File setup_graalpy_venv.ps1
) else (
    echo.
    echo =========================================
    echo  Setup complete!
    echo =========================================
    echo.
    echo Open YouScope and test in the scripting console (engine: GraalPy):
    echo   from pathlib import Path
    echo   exec_in_cpython_background("import numpy; print(f'Numpy version: {numpy.__version__}')")
)

echo.
pause
