#/*******************************************************************************
# * Copyright (c) 2026 Andreas P. Cuny
# * All rights reserved. This program and the accompanying materials
# * are made available under the terms of the GNU Public License v2.0
# * which accompanies this distribution, and is available at
# * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
# *
# * Contributors:
# *     Andreas P. Cuny - initial API and implementation
# ******************************************************************************/

# YouScope Python environment setup using uv.
# uv: single ~20MB binary, installs Python + packages 10-100x faster than pip.
# Run from YouScope installation directory as Administrator:
#   powershell -ExecutionPolicy Bypass -File setup_graalpy_venv.ps1

param(
    [string]$YouScopeDir    = $PSScriptRoot,
    [string]$GraalPyVersion = "24.1.2",
    [string]$CpythonVersion = "3.11",
    [switch]$SkipGraalPy    = $false,
    [switch]$SkipCpython    = $false
)

$ErrorActionPreference = "Continue"
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Write-Step  { param([string]$T) Write-Host "`n$T" -ForegroundColor Cyan }
function Write-OK    { param([string]$T) Write-Host "  [OK] $T" -ForegroundColor Green }
function Write-Warn  { param([string]$T) Write-Host "  [WARN] $T" -ForegroundColor Yellow }
function Write-Fail  { param([string]$T) Write-Host "  [FAIL] $T" -ForegroundColor Red }

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " YouScope Python Environment Setup"     -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "YouScope : $YouScopeDir"
Write-Host "GraalPy  : $GraalPyVersion"
Write-Host "CPython  : $CpythonVersion (via uv)"
Write-Host ""

$UvExe         = Join-Path $YouScopeDir "uv\uv.exe"
$StandaloneDir = Join-Path $YouScopeDir "graalpy-standalone"
$GraalPyExe    = Join-Path $StandaloneDir "bin\graalpy.exe"
$VenvDir       = Join-Path $YouScopeDir "graalpy-venv"
$VenvGraalPy   = Join-Path $VenvDir "Scripts\graalpy.exe"
$VenvPip       = Join-Path $VenvDir "Scripts\pip.exe"
$CpEnvDir      = Join-Path $YouScopeDir "cpython-env"
$GraalWheels   = "https://www.graalvm.org/python/wheels/"
$ConfigFile    = Join-Path $YouScopeDir "graalpy_config.txt"

# STEP 1: Install uv (if not present)
Write-Step "STEP 1: uv package manager"

if (-not (Test-Path $UvExe)) {
    Write-Host "  Downloading uv (~20MB standalone binary)..."
    $UvDir = Join-Path $YouScopeDir "uv"
    New-Item -ItemType Directory -Force -Path $UvDir | Out-Null

    $UvZip = Join-Path $env:TEMP "uv-windows.zip"
    $wc = New-Object System.Net.WebClient
    try {
        $wc.DownloadFile(
            "https://github.com/astral-sh/uv/releases/latest/download/uv-x86_64-pc-windows-msvc.zip",
            $UvZip)
        [System.IO.Compression.ZipFile]::ExtractToDirectory($UvZip, $UvDir)
        # uv.exe may be in a subdirectory
        $uvFound = Get-ChildItem $UvDir -Filter "uv.exe" -Recurse | Select-Object -First 1
        if ($uvFound -and $uvFound.FullName -ne $UvExe) {
            Move-Item $uvFound.FullName $UvExe -Force
        }
        Remove-Item $UvZip -Force
        Write-OK "uv installed at $UvExe"
    } catch {
        Write-Fail "uv download failed: $_"
        Write-Host "  Download from https://github.com/astral-sh/uv/releases"
        Write-Host "  Extract uv.exe to $UvDir"
    }
} else {
    $uvVer = & $UvExe --version 2>&1
    Write-OK "uv present: $uvVer"
}

# STEP 2: GraalPy standalone + venv
if (-not $SkipGraalPy) {
    Write-Step "STEP 2: GraalPy venv (YouScope Java interop)"

    if (-not (Test-Path $GraalPyExe)) {
        Write-Host "  Downloading GraalPy $GraalPyVersion standalone (~600MB)..."
        $ZipName = "graalpy-$GraalPyVersion-windows-amd64.zip"
        $ZipUrl  = "https://github.com/oracle/graalpython/releases/download/graal-$GraalPyVersion/$ZipName"
        $ZipPath = Join-Path $env:TEMP $ZipName
        $wc2 = New-Object System.Net.WebClient
        try {
            $wc2.DownloadFile($ZipUrl, $ZipPath)
            Write-OK "$([math]::Round((Get-Item $ZipPath).Length/1MB,1)) MB"
            $TmpDir = Join-Path $env:TEMP "gpy-$PID"
            [System.IO.Compression.ZipFile]::ExtractToDirectory($ZipPath, $TmpDir)
            $Inner = Get-ChildItem $TmpDir -Directory | Select-Object -First 1
            if (Test-Path $StandaloneDir) { Remove-Item $StandaloneDir -Recurse -Force }
            Move-Item $Inner.FullName $StandaloneDir
            Remove-Item $TmpDir -Recurse -Force
            Remove-Item $ZipPath -Force
            Write-OK "GraalPy standalone at $StandaloneDir"
        } catch {
            Write-Fail "Download failed: $_"; Write-Host "  URL: $ZipUrl"
        }
    } else { Write-OK "GraalPy standalone present" }

    if ((Test-Path $GraalPyExe) -and (-not (Test-Path $VenvDir))) {
        Write-Host "  Creating GraalPy venv..."
        & $GraalPyExe -m venv $VenvDir 2>&1 | Where-Object { $_ -notmatch "^(WARNING|Jun )" }
        if (Test-Path $VenvGraalPy) { Write-OK "Venv at $VenvDir" }
        else { Write-Fail "Venv creation failed" }
    } elseif (Test-Path $VenvDir) { Write-OK "GraalPy venv present" }

    if (Test-Path $VenvPip) {
        Write-Host "  Installing GraalPy packages (pre-built wheels)..."

        # numpy 1.26.4: only version with GraalPy pre-built wheel
        $r = & $VenvPip install "numpy==1.26.4" --only-binary ":all:" `
             --extra-index-url $GraalWheels -q --no-warn-script-location 2>&1
        if ($LASTEXITCODE -eq 0) {
            # Patch numpy for missing nt._add_dll_directory on GraalPy
            $nInit = Join-Path $VenvDir "Lib\site-packages\numpy\__init__.py"
            if (Test-Path $nInit) {
                $txt = Get-Content $nInit -Raw
                if ($txt -notmatch "_add_dll_directory = lambda") {
                    $patch = "import nt as _nt`nif not hasattr(_nt,'_add_dll_directory'): _nt._add_dll_directory=lambda p:type('C',(),{'__enter__':lambda s:s,'__exit__':lambda s,*a:None})()  `nif not hasattr(_nt,'_remove_dll_directory'): _nt._remove_dll_directory=lambda c:None`n"
                    Set-Content $nInit ($patch + $txt)
                }
            }
            Write-OK "numpy 1.26.4"
        } else { Write-Warn "numpy: $($r | Select-Object -Last 1)" }

        # Pure-Python packages: install without --only-binary so they can use sdist
        # These have no wheels but are pure Python and install fine from source
        # GraalPy venv: Only pure-Python packages with confirmed pre-built wheels.
        # matplotlib/imageio/pillow/pybis need C compilation or have no GraalPy wheels
        # and are therefore installed ONLY in cpython-env (see STEP 3).
        foreach ($pkg in @("tifffile","requests")) {
            Write-Host "    $pkg..." -NoNewline
            $r2 = & $VenvPip install $pkg -q --no-warn-script-location 2>&1
            if ($LASTEXITCODE -eq 0) { Write-Host " OK" -ForegroundColor Green }
            else { Write-Host " WARN" -ForegroundColor Yellow }
        }
        Write-Host ""
        Write-Host "  GraalPy venv packages: numpy 1.26.4, tifffile, requests" -ForegroundColor Green
        Write-Host "  matplotlib/imageio/pillow/pybis -> cpython-env only (see STEP 3)" -ForegroundColor Cyan
    }
}

# STEP 3: CPython env via uv (cellpose, scikit-image, torch)
if (-not $SkipCpython -and (Test-Path $UvExe)) {
    Write-Step "STEP 3: CPython $CpythonVersion env via uv (cellpose / scikit-image)"

    # uv creates a venv and installs a specific Python version automatically
    if (-not (Test-Path $CpEnvDir)) {
        Write-Host "  Creating CPython $CpythonVersion venv via uv..."
        & $UvExe venv $CpEnvDir --python $CpythonVersion 2>&1 | Select-Object -Last 3
        if (Test-Path (Join-Path $CpEnvDir "Scripts\python.exe")) { Write-OK "CPython venv at $CpEnvDir" }
        else { Write-Fail "uv venv creation failed" }
    } else { Write-OK "CPython venv present" }

    $CpPython = Join-Path $CpEnvDir "Scripts\python.exe"
    if (Test-Path $CpPython) {
        Write-Host "  Installing packages via uv pip..."
        # uv pip is 10-100x faster than pip
        $pkgs = @("cellpose","scikit-image","tifffile","imageio","matplotlib",
                  "pillow","zarr","ome-zarr","pybis","numpy","opencv-python-headless")
        foreach ($pkg in $pkgs) {
            Write-Host "    $pkg..." -NoNewline
            $r = & $UvExe pip install $pkg --python $CpPython -q 2>&1
            if ($LASTEXITCODE -eq 0) { Write-Host " OK" -ForegroundColor Green }
            else { Write-Host " WARN" -ForegroundColor Yellow }
        }

        # Verify cellpose
        $cpCheck = & $CpPython -c "import importlib.metadata; print(importlib.metadata.version('cellpose'))" 2>&1
        if ($LASTEXITCODE -eq 0) { Write-OK "cellpose $cpCheck" }
        else { Write-Warn "cellpose not importable: $cpCheck" }
    }
}

# STEP 4: Write config
Write-Step "STEP 4: Writing config"
$cpExe = Join-Path $CpEnvDir "Scripts\python.exe"
$lines = @(
    "# YouScope Python config - auto-generated by setup_graalpy_venv.ps1",
    "graalpy_venv=$VenvDir",
    "cpython_executable=$cpExe",
    "uv_executable=$UvExe",
    "cpython_env=$CpEnvDir"
)
$lines | Set-Content $ConfigFile
Write-OK "Config: $ConfigFile"

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Setup Complete"                          -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "GraalPy venv : $VenvDir" -ForegroundColor Green
Write-Host "  For        : YouScope Java interop, measurement scripting"
Write-Host "  Packages   : numpy 1.26.4, tifffile, requests, pybis"
Write-Host ""
if (Test-Path $CpEnvDir) {
    Write-Host "CPython env  : $CpEnvDir (via uv)" -ForegroundColor Green
    Write-Host "  For        : cellpose, scikit-image, OpenCV, torch"
    Write-Host "  Packages   : cellpose, scikit-image, numpy, tifffile, ome-zarr, pybis"
}
Write-Host ""
Write-Host "Add more CPython packages (fast):" -ForegroundColor Cyan
Write-Host "  $UvExe pip install <pkg> --python $cpExe"
Write-Host ""
Write-Host "Add more GraalPy packages:" -ForegroundColor Cyan
Write-Host "  $VenvPip install <pkg>"
Write-Host ""
Write-Host "Test in YouScope scripting console (GraalPy):" -ForegroundColor Cyan
Write-Host "  import numpy; print(numpy.__version__)"
Write-Host '  result = run_cellpose(img)  # uses CPython bridge'