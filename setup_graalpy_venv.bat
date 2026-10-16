@echo off
REM ============================================================================
REM  setup_graalpy_venv.bat is a one-click YouScope Python setup
REM  Double-click from Windows Explorer or run from cmd/PowerShell.
REM  Automatically requests Administrator rights if needed.
REM  Any arguments are passed on to setup_graalpy_venv.ps1, e.g.:
REM      setup_graalpy_venv.bat                          (everything)
REM      setup_graalpy_venv.bat -Plugins cellpose,trackmate
REM      setup_graalpy_venv.bat -Gpu
REM  Requirements: Internet connection. No Python/conda pre-installed needed.
REM ============================================================================

setlocal
cd /d "%~dp0"
title YouScope Python Setup

echo =========================================
echo  YouScope Python Environment Setup
echo =========================================
echo.
echo This will (for the selected plugins; everything if none are given):
echo   1. Download uv (~20 MB, fast Python manager)
echo   2. Download GraalPy standalone (~600 MB) and create graalpy-venv
echo   3. Create cpython-env: scikit-image, OpenCV, zarr/ome-zarr, napari, trackpy,
echo      btrack, cellpose with PyTorch, stardist with TensorFlow
echo   4. Create cellprofiler-env: CellProfiler from PyPI (Python 3.8). Needs a full
echo      JDK and the Microsoft C++ Build Tools to compile python-javabridge.
echo   5. Create fiji-env: PyImageJ with scipy for TrackMate (starts Fiji once to download it)
echo   6. Merge graalpy_config.txt (your manual settings are kept)
echo   7. Check imports and report which YouScope plugins are ready
echo.
echo Total download: roughly 2-5 GB (more with -Gpu). Time estimate: 10-40 min.
echo Optional switches:
echo   -Plugins graalpy,cellpose,stardist,trackpy,btrack,napari,fairexport,faster,celldetection,cellprofiler,trackmate
echo   -Gpu  -CudaTag cu128  -JdkHome "C:\path\to\jdk"  -CellProfilerPython 3.8
echo   -SkipTorch  -SkipTensorflow  -SkipNapari  -SkipGraalPy  -SkipCpython  -SkipFijiWarmup
echo.

REM Administrator rights
net session >nul 2>&1
if %errorLevel% equ 0 goto :is_admin

echo Requesting Administrator privileges...
if "%~1"=="" goto :elevate_noargs
powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -ArgumentList '%*' -Verb RunAs"
exit /b
:elevate_noargs
powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
exit /b

:is_admin
REM PowerShell 5 or higher
powershell -NoProfile -Command "if ($PSVersionTable.PSVersion.Major -lt 5) { exit 1 }" >nul 2>&1
if errorlevel 1 goto :ps_too_old

echo Running setup script...
echo (This window stays open until setup completes.)
echo.

powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0setup_graalpy_venv.ps1" %*
set "RC=%errorLevel%"
if not "%RC%"=="0" goto :failed

echo.
echo =========================================
echo  Setup complete!
echo =========================================
echo.
echo Open YouScope and test in the scripting console (engine: GraalPy):
echo   import numpy; print(numpy.__version__)
echo   exec_in_cpython_background("import numpy; print(f'Numpy version: {numpy.__version__}')")
goto :done

:ps_too_old
echo ERROR: PowerShell 5 or higher is required (not found or too old).
echo Download from: https://aka.ms/PSWindows
set "RC=1"
goto :done

:failed
echo.
echo =========================================
echo  Setup encountered issues (see above)
echo =========================================
echo.
echo Details were written to: %~dp0setup_graalpy_venv.log
echo.
echo Common fixes:
echo   - Run as Administrator
echo   - Check internet connection / proxy
echo   - CellProfiler: install a full JDK (Temurin 17 recommended) and the Microsoft C++ Build Tools
echo   - Disable antivirus temporarily
echo   - Re-run this script: finished steps are skipped
echo   - Try: powershell -ExecutionPolicy Bypass -File setup_graalpy_venv.ps1

:done
echo.
pause
exit /b %RC%
