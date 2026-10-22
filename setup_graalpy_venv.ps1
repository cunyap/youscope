# /*******************************************************************************
 # * Copyright © 2026 ETH Zürich
 # *
 # * This program and the accompanying materials are made available under the
 # * terms of the Apache License, Version 2.0 OR the GNU Lesser General Public
 # * License, Version 2.1 (or at your option any later version).
 # *
 # * You may choose which license you wish to use when copying, modifying,
 # * or distributing this software.
 # *
 # * Licenses are available at:
 # * - Apache 2.0: http://apache.org
 # * - LGPL 2.1:   https://gnu.org
 # *
 # * SPDX-License-Identifier: Apache-2.0 OR LGPL-2.1-or-later
 # *
 # * Contributors:
 # *     Andreas P. Cuny - initial API and implementation
 # ******************************************************************************/

<#
.SYNOPSIS
    Creates the Python environments used by YouScope's Python-based plugins and
    (re)generates graalpy_config.txt.

.DESCRIPTION
    Uses uv (single ~20 MB binary, no Python/conda needed) to create, depending on the
    selected plugins:
      graalpy-venv     GraalPy venv for the GraalPy scripting engine
      cpython-env      Cellpose (torch), StarDist (tensorflow), Trackpy, BTrack,
                       napari / OME-Zarr (FAIR export), scikit-image, OpenCV, ...
      cellprofiler-env CellProfiler from PyPI (needs Python 3.8, a full JDK and the
                       Microsoft C++ Build Tools to compile python-javabridge)
      fiji-env         PyImageJ for the TrackMate plugin (Fiji is fetched on first start)

    Layout inside the YouScope folder:
      environments\   uv, graalpy-standalone, graalpy-venv, cpython-env, cellprofiler-env,
                      fiji-env (everything this script downloads; safe to delete and re-create)
      logs\           setup_graalpy_venv.log and one <env>_install.log per environment

    graalpy_config.txt is merged and never overwritten: environment paths are refreshed,
    missing plugin defaults (ports, timeouts, work dirs) are added, and every other
    line (manual edits, settings written by the plugin UIs) is kept. The previous file
    is saved as graalpy_config.txt.bak.

    Finally every package is import-tested and a plugin coverage table is printed.
    Exit code is 0 when everything that was requested works, 1 otherwise.

    Run from the YouScope installation directory (as Administrator when it lives under
    Program Files):
        powershell -ExecutionPolicy Bypass -File setup_graalpy_venv.ps1
        powershell -ExecutionPolicy Bypass -File setup_graalpy_venv.ps1 -Plugins cellpose,trackmate

.PARAMETER Plugins
    Plugins to set up (comma separated). One or more of: graalpy, cellpose, stardist,
    trackpy, btrack, napari, fairexport, faster, celldetection, cellprofiler, trackmate.
    faster / celldetection (FastER and the shared cell-detection backend) only need the
    base cpython-env. Omit it to set up everything. The installer passes the plugins that were selected.
.PARAMETER Gpu
    Install CUDA builds of PyTorch (index https://download.pytorch.org/whl/<CudaTag>).
    Falls back to the CPU build if that index/tag is not available.
.PARAMETER CudaTag
    PyTorch CUDA wheel tag used with -Gpu (default cu128). See pytorch.org/get-started.
.PARAMETER JdkHome
    Full JDK (javac, include\jni.h, lib\jvm.lib) used to build python-javabridge for
    CellProfiler. Auto-detected from CP_JAVA_HOME, JDK_HOME, JAVA_HOME, the YouScope
    runtime folder and the usual Program Files vendor folders when omitted.
.PARAMETER CellProfilerPython
    Python version of cellprofiler-env. 3.8 is the only version for which every pinned
    CellProfiler 4.2.x dependency has a Windows wheel.
.PARAMETER SkipFijiWarmup
    Do not start Fiji once after installing PyImageJ. The warm-up downloads a JVM and
    Fiji (a few hundred MB) so the first TrackMate run does not have to.
.PARAMETER ExtraPackages
    Additional packages to install into cpython-env, e.g. -ExtraPackages pandas,numba
.PARAMETER PauseAtEnd
    Wait for Enter before closing (used when started from the installer).
#>

[CmdletBinding()]
param(
    [string]$YouScopeDir        = $PSScriptRoot,
    [string[]]$Plugins          = @(),
    [string]$GraalPyVersion     = "24.1.2",
    [string]$CpythonVersion     = "3.11",
    [string]$CellProfilerPython = "3.8",
    [string]$FijiPython         = "",
    [string]$JdkHome            = "",
    [switch]$SkipGraalPy,
    [switch]$SkipCpython,
    [switch]$SkipTorch,        # Cellpose backend; ~200 MB CPU / ~2.5 GB CUDA
    [switch]$SkipTensorflow,   # StarDist backend; ~400 MB
    [switch]$SkipNapari,       # napari + Qt + OME-Zarr viewer stack
    [switch]$SkipFijiWarmup,   # do not download Fiji/JVM during setup
    [switch]$Gpu,              # CUDA builds of torch (native Windows TensorFlow is CPU-only)
    [string]$CudaTag            = "cu128",
    [string[]]$ExtraPackages    = @(),
    [switch]$PauseAtEnd
)

$ErrorActionPreference = "Continue"
Add-Type -AssemblyName System.IO.Compression.FileSystem

# Windows PowerShell 5.1 may default to TLS 1.0/1.1, which GitHub rejects.
try {
    [Net.ServicePointManager]::SecurityProtocol =
        [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
} catch { }

if (-not $YouScopeDir -and $MyInvocation.MyCommand.Path) { $YouScopeDir = Split-Path -Parent $MyInvocation.MyCommand.Path }
if (-not $YouScopeDir) { $YouScopeDir = (Get-Location).Path }
$YouScopeDir = $YouScopeDir.TrimEnd('\')
$EnvRoot = Join-Path $YouScopeDir "environments"   # all Python environments
$LogDir  = Join-Path $YouScopeDir "logs"           # setup and install logs
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
if (-not $FijiPython) { $FijiPython = $CpythonVersion }

$script:Problems   = New-Object 'System.Collections.Generic.List[string]'
$script:FailedPkgs = New-Object 'System.Collections.Generic.List[string]'
$Utf8NoBom         = New-Object System.Text.UTF8Encoding($false)

function Write-Step  { param([string]$T) Write-Host "`n$T" -ForegroundColor Cyan }
function Write-OK    { param([string]$T) Write-Host "  [OK] $T" -ForegroundColor Green }
function Write-Warn  { param([string]$T) Write-Host "  [WARN] $T" -ForegroundColor Yellow }
function Write-Fail  { param([string]$T) Write-Host "  [FAIL] $T" -ForegroundColor Red; $script:Problems.Add($T) }

# Keep a log so problems can be diagnosed after the window closes.
$LogFile = Join-Path $LogDir "setup_graalpy_venv.log"
$script:Transcribing = $false
try { Start-Transcript -Path $LogFile -Force | Out-Null; $script:Transcribing = $true } catch { }

# Plugin selection. No -Plugins equals to everything (double-click / manual run).
$KnownPlugins = @("graalpy","cellpose","stardist","trackpy","btrack","napari","fairexport","faster","celldetection","cellprofiler","trackmate")
$requested = @()
foreach ($item in $Plugins) {
    foreach ($p in ($item -split "[,; ]+")) { if ($p) { $requested += $p.Trim().ToLower() } }
}
$explicit = ($requested.Count -gt 0)
$sel = @{}
foreach ($k in $KnownPlugins) { $sel[$k] = ((-not $explicit) -or ($requested -contains $k)) }
foreach ($r in $requested) {
    if ($KnownPlugins -notcontains $r) { Write-Warn "Unknown plugin name ignored: $r" }
}

$wGraal        = $sel["graalpy"] -and (-not $SkipGraalPy)
$wCellpose     = $sel["cellpose"] -and (-not $SkipTorch)
$wStarDist     = $sel["stardist"] -and (-not $SkipTensorflow)
$wTrackpy      = $sel["trackpy"]
$wBTrack       = $sel["btrack"]
$wFair         = $sel["fairexport"]
$wFaster       = $sel["faster"]
$wCellDet      = $sel["celldetection"]
$wNapari       = ($sel["napari"] -or $sel["fairexport"]) -and (-not $SkipNapari)
if ($SkipCpython) { $wCellpose = $false; $wStarDist = $false; $wTrackpy = $false; $wBTrack = $false; $wNapari = $false; $wFair = $false; $wFaster = $false; $wCellDet = $false }
# Any plugin with a Python backend (not CellProfiler / TrackMate, which have their own envs)
# needs the base cpython-env; the order in which plugins were selected does not matter.
# GraalPy scripting also needs cpython-env: numpy/matplotlib do not work inside the embedded GraalPy,
# scripts reach them through exec_in_cpython_background() / run_in_cpython() (cpython_executable).
$wCp           = ($wGraal -or $wCellpose -or $wStarDist -or $wTrackpy -or $wBTrack -or $wNapari -or $wFair -or $wFaster -or $wCellDet)
$wCellProfiler = $sel["cellprofiler"]
$wTrackMate    = $sel["trackmate"]

$selNames = @()
if ($wGraal) { $selNames += "graalpy" }
if ($wCellpose) { $selNames += "cellpose" }
if ($wStarDist) { $selNames += "stardist" }
if ($wTrackpy) { $selNames += "trackpy" }
if ($wBTrack) { $selNames += "btrack" }
if ($wNapari) { $selNames += "napari" }
if ($wFair) { $selNames += "fairexport" }
if ($wFaster) { $selNames += "faster" }
if ($wCellDet) { $selNames += "celldetection" }
if ($wCellProfiler) { $selNames += "cellprofiler" }
if ($wTrackMate) { $selNames += "trackmate" }

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " YouScope Python Environment Setup"      -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "YouScope : $YouScopeDir"
Write-Host "Plugins  : $($selNames -join ', ')"
Write-Host "GraalPy  : $GraalPyVersion"
Write-Host "CPython  : $CpythonVersion (via uv)"
Write-Host "Envs     : $EnvRoot"
Write-Host "Log      : $LogFile"
Write-Host ""

$UvDir         = Join-Path $EnvRoot "uv"
$UvExe         = Join-Path $UvDir "uv.exe"
$StandaloneDir = Join-Path $EnvRoot "graalpy-standalone"
$GraalPyExe    = Join-Path $StandaloneDir "bin\graalpy.exe"
$VenvDir       = Join-Path $EnvRoot "graalpy-venv"
$VenvGraalPy   = Join-Path $VenvDir "Scripts\graalpy.exe"
$VenvPip       = Join-Path $VenvDir "Scripts\pip.exe"
$CpEnvDir      = Join-Path $EnvRoot "cpython-env"
$CpPython      = Join-Path $CpEnvDir "Scripts\python.exe"
$CpfEnvDir     = Join-Path $EnvRoot "cellprofiler-env"
$CpfPython     = Join-Path $CpfEnvDir "Scripts\python.exe"
$FijiEnvDir    = Join-Path $EnvRoot "fiji-env"
$FijiPyExe     = Join-Path $FijiEnvDir "Scripts\python.exe"
$GraalWheels   = "https://www.graalvm.org/python/wheels/"
$ConfigFile    = Join-Path $YouScopeDir "graalpy_config.txt"

# Keep everything uv downloads inside the YouScope folder: the managed CPython
# interpreters must not live in the profile of whichever admin ran this script,
# otherwise the environments break for other users. It lives in environments\uv.
$env:UV_PYTHON_INSTALL_DIR = Join-Path $UvDir "python"
$env:UV_CACHE_DIR          = Join-Path $UvDir "cache"
$env:UV_PYTHON_PREFERENCE  = "only-managed"
$env:UV_LINK_MODE          = "copy"

function Get-RemoteFile {
    param([string]$Url, [string]$Dest)
    $wc = New-Object System.Net.WebClient
    try {
        $wc.Proxy = [System.Net.WebRequest]::GetSystemWebProxy()
        $wc.Proxy.Credentials = [System.Net.CredentialCache]::DefaultCredentials
        $wc.DownloadFile($Url, $Dest)
    } finally { $wc.Dispose() }
}

# STEP 1: uv
Write-Step "STEP 1: uv package manager"

if (-not (Test-Path $UvExe)) {
    Write-Host "  Downloading uv (~20MB standalone binary)..."
    New-Item -ItemType Directory -Force -Path $UvDir | Out-Null
    $UvZip = Join-Path $env:TEMP "uv-windows.zip"
    $TmpUv = Join-Path $env:TEMP "ys-uv-$PID"
    try {
        Get-RemoteFile "https://github.com/astral-sh/uv/releases/latest/download/uv-x86_64-pc-windows-msvc.zip" $UvZip
        if (Test-Path $TmpUv) { Remove-Item $TmpUv -Recurse -Force }
        [System.IO.Compression.ZipFile]::ExtractToDirectory($UvZip, $TmpUv)
        # uv.exe may be in a subdirectory
        $uvFound = Get-ChildItem $TmpUv -Filter "uv.exe" -Recurse | Select-Object -First 1
        if (-not $uvFound) { throw "uv.exe not found in the downloaded archive" }
        Copy-Item $uvFound.FullName $UvExe -Force
        Write-OK "uv installed at $UvExe"
    } catch {
        Write-Fail "uv download failed: $_"
        Write-Host "  Download from https://github.com/astral-sh/uv/releases"
        Write-Host "  and extract uv.exe to $UvDir"
    } finally {
        Remove-Item $UvZip -Force -ErrorAction SilentlyContinue
        Remove-Item $TmpUv -Recurse -Force -ErrorAction SilentlyContinue
    }
} else {
    $uvVer = & $UvExe --version 2>&1
    Write-OK "uv present: $uvVer"
}

# STEP 2: GraalPy standalone and venv (YouScope Java interop)
$graalNumpyOk = $false
if ($wGraal) {
    Write-Step "STEP 2: GraalPy venv (YouScope Java interop)"

    if (-not (Test-Path $GraalPyExe)) {
        Write-Host "  Downloading GraalPy $GraalPyVersion standalone (~600MB)..."
        $ZipName = "graalpy-$GraalPyVersion-windows-amd64.zip"
        $ZipUrl  = "https://github.com/oracle/graalpython/releases/download/graal-$GraalPyVersion/$ZipName"
        $ZipPath = Join-Path $env:TEMP $ZipName
        $TmpDir  = Join-Path $env:TEMP "gpy-$PID"
        try {
            Get-RemoteFile $ZipUrl $ZipPath
            Write-OK "$([math]::Round((Get-Item $ZipPath).Length/1MB,1)) MB"
            if (Test-Path $TmpDir) { Remove-Item $TmpDir -Recurse -Force }
            [System.IO.Compression.ZipFile]::ExtractToDirectory($ZipPath, $TmpDir)
            $Inner = Get-ChildItem $TmpDir -Directory | Select-Object -First 1
            if (-not $Inner) { throw "archive is empty" }
            if (Test-Path $StandaloneDir) { Remove-Item $StandaloneDir -Recurse -Force }
            Move-Item $Inner.FullName $StandaloneDir
            Write-OK "GraalPy standalone at $StandaloneDir"
        } catch {
            Write-Fail "GraalPy download failed: $_"
            Write-Host "  URL: $ZipUrl"
        } finally {
            Remove-Item $ZipPath -Force -ErrorAction SilentlyContinue
            Remove-Item $TmpDir -Recurse -Force -ErrorAction SilentlyContinue
        }
    } else { Write-OK "GraalPy standalone present" }

    if ((Test-Path $GraalPyExe) -and (-not (Test-Path $VenvGraalPy))) {
        Write-Host "  Creating GraalPy venv..."
        if (Test-Path $VenvDir) { Remove-Item $VenvDir -Recurse -Force -ErrorAction SilentlyContinue }
        & $GraalPyExe -m venv $VenvDir 2>&1 | Where-Object { $_ -notmatch "^(WARNING|Jun )" }
        if (Test-Path $VenvGraalPy) { Write-OK "Venv at $VenvDir" }
        else { Write-Fail "GraalPy venv creation failed" }
    } elseif (Test-Path $VenvGraalPy) { Write-OK "GraalPy venv present" }

    if (Test-Path $VenvPip) {
        Write-Host "  Installing GraalPy packages (pre-built wheels)..."

        # numpy 1.26.4: only version with a GraalPy pre-built wheel
        $r = & $VenvPip install "numpy==1.26.4" --only-binary ":all:" `
             --extra-index-url $GraalWheels -q --no-warn-script-location 2>&1
        if ($LASTEXITCODE -eq 0) {
            # Patch numpy for missing nt._add_dll_directory on GraalPy (idempotent via marker)
            $nInit = Join-Path $VenvDir "Lib\site-packages\numpy\__init__.py"
            if (Test-Path $nInit) {
                $txt = [System.IO.File]::ReadAllText($nInit)
                if ($txt -notmatch "YOUSCOPE-GRAALPY-PATCH") {
                    $patch = "# YOUSCOPE-GRAALPY-PATCH`n" +
                             "import nt as _nt`n" +
                             "if not hasattr(_nt,'_add_dll_directory'): _nt._add_dll_directory=lambda p:type('C',(),{'__enter__':lambda s:s,'__exit__':lambda s,*a:None})()  `n" +
                             "if not hasattr(_nt,'_remove_dll_directory'): _nt._remove_dll_directory=lambda c:None`n"
                    [System.IO.File]::WriteAllText($nInit, ($patch + $txt), $Utf8NoBom)
                }
            }
            $graalNumpyOk = $true
            Write-OK "numpy 1.26.4"
        } else {
            Write-Fail "GraalPy numpy install failed: $($r | Select-Object -Last 1)"
        }

        # Pure-Python packages (sdist is fine, no C compilation needed)
        foreach ($pkg in @("tifffile","requests")) {
            Write-Host "    $pkg..." -NoNewline
            $r2 = & $VenvPip install $pkg -q --no-warn-script-location 2>&1
            if ($LASTEXITCODE -eq 0) { Write-Host " OK" -ForegroundColor Green }
            else { Write-Host " WARN" -ForegroundColor Yellow }
        }

        # Optional, best effort: these need C extensions, so they only install when a
        # GraalPy wheel exists. The full versions live in cpython-env (STEP 3).
        foreach ($pkg in @("imageio","matplotlib")) {
            Write-Host "    $pkg (wheel only)..." -NoNewline
            $r3 = & $VenvPip install $pkg --only-binary ":all:" -q --no-warn-script-location 2>&1
            if ($LASTEXITCODE -eq 0) { Write-Host " OK" -ForegroundColor Green }
            else { Write-Host " skipped (no wheel for GraalPy -- use CPython env)" -ForegroundColor Yellow }
        }
        Write-Host "    pybis (optional)..." -NoNewline
        $r5 = & $VenvPip install pybis -q --no-warn-script-location 2>&1
        if ($LASTEXITCODE -eq 0) { Write-Host " OK" -ForegroundColor Green }
        else { Write-Host " skipped (use CPython env)" -ForegroundColor Yellow }

        Write-Host ""
        Write-Host "  NOTE: matplotlib/imageio/pillow/pybis are always available in cpython-env." -ForegroundColor Cyan
        Write-Host "  Use run_in_cpython() or save/display via CPython bridge scripts." -ForegroundColor Cyan
    }
}

# Helpers for the uv-managed environments
function New-UvVenv {
    # Creates a uv venv with a uv-managed Python; returns $true when python.exe exists.
    param([string]$Dir, [string]$PythonVersion)
    $py = Join-Path $Dir "Scripts\python.exe"
    if (Test-Path $py) { Write-OK "venv present: $Dir"; return $true }
    Write-Host "  Creating Python $PythonVersion venv at $Dir ..."
    if (Test-Path $Dir) { Remove-Item $Dir -Recurse -Force -ErrorAction SilentlyContinue }
    $prevEap = $ErrorActionPreference; $ErrorActionPreference = "SilentlyContinue"   # uv logs to stderr
    $venvOut = @(& $UvExe venv $Dir --python $PythonVersion 2>&1 | ForEach-Object { "$_" })
    $ErrorActionPreference = $prevEap
    $venvOut | Select-Object -Last 3 | Out-Host
    Write-EnvLog $py "venv $Dir --python $PythonVersion" $venvOut
    if (Test-Path $py) { Write-OK "venv at $Dir (Python $PythonVersion)"; return $true }
    Write-Fail "uv venv creation failed: $Dir"
    return $false
}

function Write-EnvLog {
    # Appends the full output of an uv call to logs\<env>_install.log (the console only shows the tail).
    param([string]$Python, [string]$Command, [object[]]$Output)
    try {
        $envName = Split-Path -Leaf (Split-Path -Parent (Split-Path -Parent $Python))
        $text = "=== " + (Get-Date -Format "yyyy-MM-dd HH:mm:ss") + " uv " + $Command + "`r`n" +
                (($Output | ForEach-Object { "$_" }) -join "`r`n") + "`r`n`r`n"
        [System.IO.File]::AppendAllText((Join-Path $LogDir "${envName}_install.log"), $text, $Utf8NoBom)
    } catch { }
}

function Install-CpPkg {
    # Installs one or more packages into an env (default cpython-env); returns $true/$false.
    param([string[]]$Spec, [string]$Label = "", [string[]]$ExtraArgs = @(), [string]$Python = "", [int]$Tail = 2)
    if (-not $Python) { $Python = $CpPython }
    if (-not $Label) { $Label = ($Spec -join ' ') }
    Write-Host "    $Label..." -NoNewline
    $uvArgs = @("pip", "install") + $Spec + @("--python", $Python, "-q") + $ExtraArgs
    $out = & $UvExe @uvArgs 2>&1
    $uvExit = $LASTEXITCODE
    Write-EnvLog $Python ($uvArgs -join " ") $out
    if ($uvExit -eq 0) { Write-Host " OK" -ForegroundColor Green; return $true }
    Write-Host " WARN" -ForegroundColor Yellow
    # NB: PowerShell variables are case-insensitive, so this must not be named $tail ($Tail is the [int] parameter)
    $errText = ($out | ForEach-Object { "$_" } | Where-Object { $_.Trim() } | Select-Object -Last $Tail) -join " | "
    Write-Host "      $errText" -ForegroundColor DarkYellow
    return $false
}

function Install-CpGroup {
    # One resolver run for the whole group (consistent versions, fast). If it fails,
    # retry package by package so one bad package does not block the rest.
    param([string]$Title, [string[]]$Specs)
    Write-Host "  $Title"
    if (Install-CpPkg -Spec $Specs -Label ($Specs -join ", ")) { return }
    Write-Warn "Combined install failed; retrying package by package"
    foreach ($s in $Specs) {
        if (-not (Install-CpPkg -Spec @($s))) { $script:FailedPkgs.Add($s) }
    }
}

function Install-Torch {
    $cpuIdx = "https://download.pytorch.org/whl/cpu"
    if ($Gpu) {
        Write-Host "  Installing PyTorch ($CudaTag, ~2.5 GB)..."
        $gpuIdx = "https://download.pytorch.org/whl/$CudaTag"
        if (Install-CpPkg -Spec @("torch","torchvision") -Label "torch ($CudaTag)" -ExtraArgs @("--extra-index-url", $gpuIdx)) { return }
        Write-Warn "CUDA build '$CudaTag' not available; falling back to the CPU build"
    } else {
        Write-Host "  Installing PyTorch (CPU, ~200 MB)..."
    }
    if (Install-CpPkg -Spec @("torch","torchvision") -Label "torch (cpu)" -ExtraArgs @("--extra-index-url", $cpuIdx)) { return }
    Write-Warn "PyTorch CPU index failed; trying PyPI (Windows wheels there are CPU builds)"
    if (-not (Install-CpPkg -Spec @("torch","torchvision") -Label "torch (PyPI)")) {
        $script:FailedPkgs.Add("torch"); $script:FailedPkgs.Add("torchvision")
    }
}

# STEP 3: CPython env via uv (Cellpose, StarDist, trackers, napari, ...)
if ($wCp) {
    Write-Step "STEP 3: CPython $CpythonVersion env (Cellpose, StarDist, trackers, napari)"

    if (-not (Test-Path $UvExe)) {
        Write-Fail "uv is missing; cannot create the CPython environment"
    } else {
        [void](New-UvVenv $CpEnvDir $CpythonVersion)
    }

    if (Test-Path $CpPython) {
        Write-Host "  Installing packages via uv pip (10-100x faster than pip)..."

        # Base scientific stack (FAIR export / OME-Zarr tooling, image I/O, OpenCV)
        Install-CpGroup "Base scientific stack" @(
            "numpy","scipy","pandas","scikit-image","tifffile","imageio","matplotlib","pillow",
            "zarr","ome-zarr","pybis","opencv-python-headless")

        # Trackers: Trackpy tracker plugin, BTrack tracker plugin
        $trk = @()
        if ($wTrackpy) { $trk += "trackpy" }
        if ($wBTrack)  { $trk += "btrack" }
        if ($trk.Count -gt 0) { Install-CpGroup "Tracking ($($trk -join ', '))" $trk }

        # napari: the extra [all] pulls a Qt backend (PyQt6); plain "napari" cannot open a window.
        if ($wNapari) {
            Install-CpGroup "napari viewer stack" @("napari[all]","napari-ome-zarr","useq-schema")
        }

        # PyTorch (Cellpose backend). Installed before cellpose so the chosen build is kept.
        if ($wCellpose) { Install-Torch }

        # TensorFlow (StarDist backend). Native Windows TensorFlow is CPU-only (GPU needs WSL2).
        if ($wStarDist) {
            Write-Host "  Installing TensorFlow (~400 MB)..."
            if (-not (Install-CpPkg -Spec @("tensorflow"))) { $script:FailedPkgs.Add("tensorflow") }
        }

        # Segmentation frameworks: cellpose reuses the torch above; stardist pulls csbdeep and tensorflow.
        if ($wCellpose -or $wStarDist) { Write-Host "  Installing segmentation frameworks..." }
        if ($wCellpose) {
            if (-not (Install-CpPkg -Spec @("cellpose"))) { $script:FailedPkgs.Add("cellpose") }
        }
        if ($wStarDist) {
            if (-not (Install-CpPkg -Spec @("stardist","csbdeep") -Label "stardist, csbdeep")) {
                $script:FailedPkgs.Add("stardist")
            }
        }

        if ($ExtraPackages.Count -gt 0) {
            Write-Host "  Installing extra packages..."
            foreach ($p in $ExtraPackages) {
                if (-not (Install-CpPkg -Spec @($p))) { Write-Fail "extra package failed: $p" }
            }
        }

        # Later installs can silently change earlier pins; make sure the final env is consistent.
        $chk = & $UvExe pip check --python $CpPython 2>&1
        if ($LASTEXITCODE -eq 0) { Write-OK "uv pip check: no conflicting requirements" }
        else {
            Write-Warn "uv pip check reports conflicts:"
            $chk | Select-Object -First 8 | ForEach-Object { Write-Host "      $_" -ForegroundColor DarkYellow }
        }
    }
}

# STEP 4: CellProfiler env (cellprofiler-env)
# The PyPI package cellprofiler 4.2.x pins scipy==1.9.0, scikit-image==0.18.3,
# pyzmq~=22.3, mysqlclient==1.4.6, wxPython and python-javabridge. That cannot coexist
# with numpy 2 / torch / tensorflow / napari, so it gets its own env. Python 3.8 is the
# only version where all pins have Windows wheels, except python-javabridge, which has
# no wheels at all and is compiled here (full JDK + MSVC C++ build tools). @ToDo add request.
$script:CpfStatus = "SKIPPED"
$script:CpfDetail = ""

function Find-Jdk {
    # Returns the home of a FULL JDK (javac, include\jni.h, lib\jvm.lib) or $null.
    $cands = New-Object 'System.Collections.Generic.List[string]'
    foreach ($v in @($JdkHome, $env:CP_JAVA_HOME, $env:JDK_HOME, $env:JAVA_HOME, (Join-Path $YouScopeDir "runtime"))) {
        if ($v) { $cands.Add($v.TrimEnd('\')) }
    }
    foreach ($root in @($env:ProgramW6432, $env:ProgramFiles)) {
        if (-not $root) { continue }
        foreach ($sub in @("Eclipse Adoptium","Java","Microsoft","Zulu","BellSoft","Amazon Corretto")) {
            $p = Join-Path $root $sub
            if (Test-Path $p) {
                Get-ChildItem $p -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending |
                    ForEach-Object { $cands.Add($_.FullName) }
            }
        }
    }
    # Collect every full JDK, then prefer an explicit -JdkHome, then JDK 17 (the version the
    # CellProfiler recipe was tested with), then whatever else was found first.
    $valid = New-Object 'System.Collections.Generic.List[string]'
    foreach ($c in $cands) {
        if ((Test-Path (Join-Path $c "bin\javac.exe")) -and (Test-Path (Join-Path $c "include\jni.h")) -and
            (Test-Path (Join-Path $c "lib\jvm.lib")) -and (-not $valid.Contains($c))) { $valid.Add($c) }
    }
    if ($JdkHome -and $valid.Contains($JdkHome.TrimEnd('\'))) { return $JdkHome.TrimEnd('\') }
    foreach ($c in $valid) { if ($c -match '(?i)jdk-?17(\.|-|\\|$)|[-_]17[._-]|\\17\.') { return $c } }
    if ($valid.Count -gt 0) { return $valid[0] }
    return $null
}

function Test-MsvcBuildTools {
    $pf86 = ${env:ProgramFiles(x86)}
    if (-not $pf86) { return $false }
    $vw = Join-Path $pf86 "Microsoft Visual Studio\Installer\vswhere.exe"
    if (-not (Test-Path $vw)) { return $false }
    $r = & $vw -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath 2>$null
    return [bool]$r
}

if ($wCellProfiler) {
    Write-Step "STEP 4: CellProfiler env (cellprofiler-env, Python $CellProfilerPython)"
    $hard = $explicit   # selected explicitly (e.g. by the installer): failures count as problems

    $jdk = Find-Jdk
    if (-not (Test-Path $UvExe)) {
        $script:CpfStatus = "FAIL"; $script:CpfDetail = "uv is missing"
        Write-Fail "uv is missing; cannot create cellprofiler-env"
    } elseif (-not $jdk) {
        $script:CpfStatus = "NEEDS TOOLCHAIN"; $script:CpfDetail = "full JDK not found"
        $msg = "CellProfiler needs a FULL JDK (bin\javac.exe, include\jni.h, lib\jvm.lib) to compile python-javabridge. " +
               "Install e.g. Eclipse Temurin JDK 17/21 (https://adoptium.net) or pass -JdkHome."
        if ($hard) { Write-Fail $msg } else { Write-Warn $msg }
    } else {
        Write-OK "JDK for python-javabridge: $jdk"
        if (-not (Test-MsvcBuildTools)) {
            Write-Warn "Microsoft C++ Build Tools not detected; python-javabridge must be compiled."
            Write-Host "      winget install Microsoft.VisualStudio.2022.BuildTools --override `"--quiet --wait --add Microsoft.VisualStudio.Workload.VCTools --includeRecommended`""
        }
        if (New-UvVenv $CpfEnvDir $CellProfilerPython) {
            $env:JAVA_HOME = $jdk; $env:JDK_HOME = $jdk; $env:CP_JAVA_HOME = $jdk
            # Recipe verified by hand (uv, Python 3.8, JDK 17):
            #   uv pip install --no-build-isolation python-javabridge==4.0.5 python-bioformats
            #   uv pip install cellprofiler==4.2.8.1
            #   uv pip install setuptools
            # A fresh uv venv contains no build tools, and --no-build-isolation builds inside the venv.
            [void](Install-CpPkg -Spec @("setuptools","wheel","cython","numpy") -Label "build prerequisites" -Python $CpfPython -Tail 4)
            Write-Host "  Installing python-javabridge 4.0.5 + python-bioformats (compiles javabridge, can take several minutes)..."
            $ok = Install-CpPkg -Spec @("python-javabridge==4.0.5","python-bioformats") -Python $CpfPython -ExtraArgs @("--no-build-isolation") -Tail 8
            if ($ok) {
                Write-Host "  Installing cellprofiler 4.2.8.1..."
                $ok = Install-CpPkg -Spec @("cellprofiler==4.2.8.1") -Python $CpfPython -Tail 8
            }
            if ($ok) {
                # cellprofiler_core imports pkg_resources, which needs setuptools in the env.
                $ok = Install-CpPkg -Spec @("setuptools") -Python $CpfPython -Tail 4
            }
            if ($ok) {
                # javabridge finds the JVM through CP_JAVA_HOME / JAVA_HOME at run time. Pin it for this
                # env only (setdefault: an explicit variable still wins) so the backend works even when
                # the machine has no JAVA_HOME.
                $sp = Join-Path $CpfEnvDir "Lib\site-packages"
                if (Test-Path $sp) {
                    $pthLine = "import os; os.environ.setdefault('CP_JAVA_HOME', r'$jdk')`r`n"
                    [System.IO.File]::WriteAllText((Join-Path $sp "youscope_java.pth"), $pthLine, $Utf8NoBom)
                }
                $script:CpfStatus = "OK"
            } else {
                $script:CpfStatus = "FAIL"; $script:CpfDetail = "cellprofiler install failed (see log)"
                if ($hard) { Write-Fail "cellprofiler install failed (python-javabridge build? see $LogFile)" }
                else { Write-Warn "cellprofiler install failed (python-javabridge build? see $LogFile)" }
            }
        } else { $script:CpfStatus = "FAIL"; $script:CpfDetail = "venv creation failed" }
    }
}

# STEP 5: Fiji env (fiji-env) for the TrackMate plugin
$script:FijiStatus = "SKIPPED"
if ($wTrackMate) {
    Write-Step "STEP 5: Fiji / TrackMate env (fiji-env, Python $FijiPython)"
    if (-not (Test-Path $UvExe)) {
        $script:FijiStatus = "FAIL"
        Write-Fail "uv is missing; cannot create fiji-env"
    } elseif (New-UvVenv $FijiEnvDir $FijiPython) {
        Write-Host "  Installing PyImageJ + scipy..."
        if (Install-CpPkg -Spec @("pyimagej","scipy") -Python $FijiPyExe -Tail 4) {
            $script:FijiStatus = "OK"
            if ($SkipFijiWarmup) {
                Write-Host "  Fiji warm-up skipped; PyImageJ downloads Fiji on the first TrackMate start." -ForegroundColor Cyan
            } else {
                # Same test as the manual recipe: triggers the download of a JVM and Fiji.
                Write-Host "  Starting Fiji headless once (downloads JVM and Fiji and takes a few minutes)..."
                $FijiTest = Join-Path $env:TEMP "ys_fiji_$PID.py"
                $fijiSrc = "import imagej`nij = imagej.init('sc.fiji:fiji', mode='headless')`nprint('YSFIJI ok', ij.getVersion())`n"
                [System.IO.File]::WriteAllText($FijiTest, $fijiSrc, $Utf8NoBom)
                $fo = & $FijiPyExe $FijiTest 2>&1 | ForEach-Object { "$_" }
                Remove-Item $FijiTest -Force -ErrorAction SilentlyContinue
                $okLine = $fo | Where-Object { $_ -like "YSFIJI ok*" } | Select-Object -Last 1
                if ($okLine) { Write-OK "Fiji headless start works ($okLine)" }
                else {
                    $script:FijiStatus = "WARMUP FAILED"
                    Write-Warn "Fiji could not be started now (needs internet; it will retry on the first TrackMate start)"
                    $fo | Where-Object { $_.Trim() } | Select-Object -Last 4 | ForEach-Object { Write-Host "      $_" -ForegroundColor DarkYellow }
                }
            }
        } else {
            $script:FijiStatus = "FAIL"
            Write-Fail "PyImageJ install failed"
        }
    } else { $script:FijiStatus = "FAIL" }
}

# STEP 6: graalpy_config.txt (merge, never overwrite)
Write-Step "STEP 6: Updating graalpy_config.txt"

$script:cfg = New-Object 'System.Collections.Generic.List[string]'
$cfgIsNew = -not (Test-Path $ConfigFile)
if (-not $cfgIsNew) {
    foreach ($l in [System.IO.File]::ReadAllLines($ConfigFile)) { $script:cfg.Add([string]$l) }
    Copy-Item $ConfigFile "$ConfigFile.bak" -Force
} else {
    $script:cfg.Add("# YouScope Python config auto-generated by setup_graalpy_venv.ps1")
}
$cfgBefore = ($script:cfg -join "`n")

function Merge-ConfigSection {
    # Pairs: array of @{K=key; V=value; M=$true; Old=oldDefault}. M (managed) = refresh the value of
    # existing keys (all occurrences, since the last duplicate wins in a properties file); without M a
    # key is only added when it is missing, so manual edits and plugin-UI settings are never touched.
    # Old = a previous default value: an existing key that still holds exactly that value is migrated.
    param([string]$Header, [object[]]$Pairs)
    $new = New-Object 'System.Collections.Generic.List[string]'
    foreach ($p in $Pairs) {
        $pat = '^\s*' + [regex]::Escape($p.K) + '\s*='
        $hit = $false
        for ($i = 0; $i -lt $script:cfg.Count; $i++) {
            if ($script:cfg[$i] -match $pat) {
                $hit = $true
                $curVal = ($script:cfg[$i] -replace '^\s*[^=]*=\s*', '').Trim()
                if ($p.M -or ($p.Old -and ($curVal -eq $p.Old))) { $script:cfg[$i] = "$($p.K)=$($p.V)" }
            }
        }
        if (-not $hit) { $new.Add("$($p.K)=$($p.V)") }
    }
    if ($new.Count -gt 0) {
        $script:cfg.Add("")
        if ($Header) { $script:cfg.Add($Header) }
        foreach ($n in $new) { $script:cfg.Add($n) }
    }
}

function New-WorkDir {
    param([string]$Dir)
    try { New-Item -ItemType Directory -Force -Path $Dir | Out-Null } catch { Write-Warn "Could not create $Dir" }
}

# Environment paths: refreshed on every run so a moved/reinstalled YouScope stays consistent.
$envPairs = @()
if (Test-Path $VenvGraalPy) { $envPairs += @{ K = "graalpy_venv"; V = $VenvDir; M = $true } }
if (Test-Path $CpPython)    { $envPairs += @{ K = "cpython_executable"; V = $CpPython; M = $true }; $envPairs += @{ K = "cpython_env"; V = $CpEnvDir; M = $true } }
if (Test-Path $UvExe)       { $envPairs += @{ K = "uv_executable"; V = $UvExe; M = $true } }
if ($envPairs.Count -gt 0) { Merge-ConfigSection "# environments" $envPairs }

if ($wCellpose) {
    $wd = Join-Path $LogDir "cellpose"; New-WorkDir $wd; $oldWd = Join-Path $YouScopeDir "cellpose"
    Merge-ConfigSection "# cellpose" @(
        @{ K = "cellpose_timeout_seconds";      V = "1200" },
        @{ K = "cellpose_server_start_seconds"; V = "300" },
        @{ K = "cellpose_work_dir"; V = $wd; Old = $oldWd })
}
if (Test-Path $CpfPython) {
    $wd = Join-Path $LogDir "cellprofiler"; New-WorkDir $wd; $oldWd = Join-Path $YouScopeDir "cellprofiler"
    Merge-ConfigSection "# cellprofiler" @(
        @{ K = "cellprofiler_cpython_executable";   V = $CpfPython; M = $true },
        @{ K = "cellprofiler_backend_port";         V = "59716" },
        @{ K = "cellprofiler_backend_host";         V = "127.0.0.1" },
        @{ K = "cellprofiler_work_dir"; V = $wd; Old = $oldWd },
        @{ K = "cellprofiler_idle_timeout_seconds"; V = "1800" })
}
if (Test-Path $FijiPyExe) {
    $wd = Join-Path $LogDir "trackmate"; New-WorkDir $wd; $oldWd = Join-Path $YouScopeDir "trackmate"
    Merge-ConfigSection "# trackmate" @(
        @{ K = "trackmate_cpython_executable";   V = $FijiPyExe; M = $true },
        @{ K = "trackmate_backend_port";         V = "59717" },
        @{ K = "trackmate_backend_host";         V = "127.0.0.1" },
        @{ K = "trackmate_work_dir"; V = $wd; Old = $oldWd },
        @{ K = "trackmate_idle_timeout_seconds"; V = "1800" },
        @{ K = "trackmate_server_start_seconds"; V = "300" })
}

if ((($script:cfg -join "`n") -ne $cfgBefore) -or $cfgIsNew) {
    [System.IO.File]::WriteAllLines($ConfigFile, $script:cfg.ToArray(), $Utf8NoBom)
    Write-OK "Config updated: $ConfigFile (previous version: graalpy_config.txt.bak)"
} else {
    Write-OK "Config already up to date: $ConfigFile"
    Remove-Item "$ConfigFile.bak" -Force -ErrorAction SilentlyContinue
}

# STEP 7: Verify (real imports, one process per module as the plugins use them)
Write-Step "STEP 7: Verifying plugin dependencies"

$VerifyPy = Join-Path $env:TEMP "ys_verify_$PID.py"
$verifySrc = @'
import importlib, os, sys
os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "3")
os.environ.setdefault("QT_QPA_PLATFORM", "offscreen")
name, _, dist = sys.argv[1].partition("=")
dist = dist or name
try:
    importlib.import_module(name)
    try:
        from importlib.metadata import version
        v = version(dist)
    except Exception:
        v = "?"
    print("YSCHECK|OK|%s|%s" % (name, v))
except BaseException as e:
    msg = (str(e).strip().splitlines() or [type(e).__name__])[0]
    print("YSCHECK|FAIL|%s|%s" % (name, msg))
'@
[System.IO.File]::WriteAllText($VerifyPy, $verifySrc, $Utf8NoBom)

function Test-PyModule {
    param([string]$Python, [string]$Spec)   # Spec: "import_name" or "import_name=dist-name"
    $o = & $Python $VerifyPy $Spec 2>&1 | ForEach-Object { "$_" } | Where-Object { $_ -like "YSCHECK|*" } | Select-Object -Last 1
    if (-not $o) { return @{ Ok = $false; Info = "no output (crashed?)" } }
    $p = $o.Split("|")
    return @{ Ok = ($p[1] -eq "OK"); Info = $p[3] }
}

$coverage = New-Object System.Collections.ArrayList

function Test-Row {
    param([string]$Name, [string]$Python, [string[]]$Mods)
    Write-Host "  $Name..." -NoNewline
    $bad = @(); $vers = @()
    foreach ($m in $Mods) {
        $res = Test-PyModule $Python $m
        $short = $m.Split("=")[0]
        if ($res.Ok) { $vers += "$short $($res.Info)" } else { $bad += "$short ($($res.Info))" }
    }
    if ($bad.Count -eq 0) {
        Write-Host " OK" -ForegroundColor Green
        [void]$coverage.Add(@{ Name = $Name; Status = "OK"; Detail = ($vers -join ", ") })
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $bad | ForEach-Object { Write-Host "      $_" -ForegroundColor DarkYellow }
        [void]$coverage.Add(@{ Name = $Name; Status = "FAIL"; Detail = ($bad -join "; ") })
        $script:Problems.Add("${Name}: " + ($bad -join "; "))
    }
}

if ($wCp -and (Test-Path $CpPython)) {
    Test-Row "Base stack (scikit-image, OpenCV, zarr, pybis, ...)" $CpPython @(
        "numpy","scipy","pandas","skimage=scikit-image","tifffile","imageio","matplotlib","PIL=pillow","zarr","ome_zarr=ome-zarr","pybis","cv2=opencv-python-headless")
    if ($wCellpose) { Test-Row "Cellpose plugin" $CpPython @("torch","torchvision","cellpose") }
    if ($wStarDist) { Test-Row "StarDist plugin" $CpPython @("tensorflow","csbdeep","stardist") }
    if ($wTrackpy)  { Test-Row "Trackpy tracker plugin" $CpPython @("trackpy") }
    if ($wBTrack)   { Test-Row "BTrack tracker plugin" $CpPython @("btrack") }
    if ($wNapari)   { Test-Row "napari / OME-Zarr viewer" $CpPython @("napari","PyQt6","napari_ome_zarr=napari-ome-zarr","useq=useq-schema") }
} elseif ($wCp) {
    [void]$coverage.Add(@{ Name = "CPython-based plugins"; Status = "FAIL"; Detail = "cpython-env missing" })
}

if ($wCellProfiler) {
    if (($script:CpfStatus -eq "OK") -and (Test-Path $CpfPython)) {
        Test-Row "CellProfiler plugin" $CpfPython @("cellprofiler","cellprofiler_core","pkg_resources=setuptools","javabridge=python-javabridge","bioformats=python-bioformats")
    } else {
        [void]$coverage.Add(@{ Name = "CellProfiler plugin"; Status = $script:CpfStatus; Detail = $script:CpfDetail })
    }
}

if ($wTrackMate) {
    if ((@("OK","WARMUP FAILED") -contains $script:FijiStatus) -and (Test-Path $FijiPyExe)) {
        Test-Row "TrackMate plugin (PyImageJ)" $FijiPyExe @("imagej=pyimagej","scyjava","jpype=jpype1","numpy","scipy")
    } else {
        [void]$coverage.Add(@{ Name = "TrackMate plugin (PyImageJ)"; Status = "FAIL"; Detail = "fiji-env not created" })
    }
}

if ($wGraal) {
    # numpy cannot be imported inside GraalPy on Windows (by design: scripts reach numpy/matplotlib through
    # the CPython bridge, exec_in_cpython_background). So the GraalPy check covers what really works:
    # the interpreter of graalpy-venv starts and imports its pure-Python packages, and the bridge target
    # (cpython-env, cpython_executable) can import numpy. A numpy import in GraalPy is only reported as info.
    $gv = $null
    $gOut = @()
    if (Test-Path $VenvGraalPy) {
        # Script file instead of -c solving quoting problems with spaces in the install path.
        $GraalTest = Join-Path $env:TEMP "ys_graal_$PID.py"
        $graalSrc = "import sys, requests`nprint('YSGRAAL ' + sys.version.split()[0])`n" +
                    "try:`n    import numpy`n    print('YSNUMPY ' + numpy.__version__)`nexcept BaseException as e:`n    print('YSNUMPY-NO ' + type(e).__name__)`n"
        [System.IO.File]::WriteAllText($GraalTest, $graalSrc, $Utf8NoBom)
        $gOut = @(& $VenvGraalPy $GraalTest 2>&1 | ForEach-Object { "$_" })
        $gExit = $LASTEXITCODE
        Remove-Item $GraalTest -Force -ErrorAction SilentlyContinue
        $gv = $gOut | Where-Object { $_ -like "YSGRAAL *" } | Select-Object -Last 1
        if (-not $gv) {
            Write-Warn "graalpy interpreter check failed (exit code $gExit). Output:"
            $gOut | Where-Object { $_.Trim() } | Select-Object -Last 12 | ForEach-Object { Write-Host "      $_" -ForegroundColor DarkYellow }
        }
    }
    $bridgeOk = $false
    $bridgeVer = ""
    if (Test-Path $CpPython) {
        $br = Test-PyModule $CpPython "numpy"
        $bridgeOk = $br.Ok; $bridgeVer = $br.Info
    }
    if ($gv -and $bridgeOk) {
        $gnum = $gOut | Where-Object { $_ -like "YSNUMPY *" } | Select-Object -Last 1
        $note = "GraalPy " + $gv.Substring(8) + "; numpy " + $bridgeVer + " via CPython bridge"
        if ($gnum) { $note += " (numpy also importable in GraalPy)" }
        [void]$coverage.Add(@{ Name = "GraalPy scripting + CPython bridge"; Status = "OK"; Detail = $note })
    } else {
        $why = @()
        if (-not $gv) { $why += "graalpy-venv interpreter not working" }
        if (-not $bridgeOk) { $why += "cpython-env numpy not importable (exec_in_cpython_background needs it)" }
        [void]$coverage.Add(@{ Name = "GraalPy scripting + CPython bridge"; Status = "FAIL"; Detail = ($why -join "; ") })
        $script:Problems.Add("GraalPy scripting: " + ($why -join "; "))
    }
}

Remove-Item $VerifyPy -Force -ErrorAction SilentlyContinue

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Setup Summary"                           -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
if ($wGraal) {
    Write-Host "GraalPy venv     : $VenvDir" -ForegroundColor Green
    Write-Host "  For            : YouScope Java interop, measurement scripting"
    Write-Host "  Packages       : numpy 1.26.4, tifffile, requests (+ imageio, matplotlib, pybis if wheels exist)"
    Write-Host ""
}
if (Test-Path $CpPython) {
    Write-Host "CPython env      : $CpEnvDir (via uv)" -ForegroundColor Green
    Write-Host "  For            : cellpose (torch), stardist (tensorflow), trackpy, btrack, napari, OME-Zarr, scikit-image, OpenCV"
    Write-Host ""
}
if (Test-Path $CpfPython) { Write-Host "CellProfiler env : $CpfEnvDir (Python $CellProfilerPython)" -ForegroundColor Green; Write-Host "" }
if (Test-Path $FijiPyExe) { Write-Host "Fiji env         : $FijiEnvDir (PyImageJ for TrackMate)" -ForegroundColor Green; Write-Host "" }
Write-Host "Config           : $ConfigFile" -ForegroundColor Green
Write-Host ""
Write-Host "Plugin coverage:" -ForegroundColor Cyan
foreach ($c in $coverage) {
    $color = "Red"
    if ($c.Status -eq "OK") { $color = "Green" }
    elseif ($c.Status -eq "SKIPPED") { $color = "DarkGray" }
    elseif ($c.Status -eq "NEEDS TOOLCHAIN") { $color = "Yellow" }
    Write-Host ("  {0,-16} {1}" -f $c.Status, $c.Name) -ForegroundColor $color
    if ($c.Status -ne "OK" -and $c.Detail) { Write-Host ("  {0,-16} {1}" -f "", $c.Detail) -ForegroundColor DarkYellow }
}
if ($script:FailedPkgs.Count -gt 0) {
    Write-Host ""
    Write-Warn ("Packages that did not install: " + ($script:FailedPkgs -join ", "))
}
Write-Host ""
Write-Host "Add more CPython packages (fast):" -ForegroundColor Cyan
Write-Host "  `"$UvExe`" pip install <pkg> --python `"$CpPython`""
Write-Host ""
Write-Host "Test in YouScope scripting console (GraalPy). numpy runs through the CPython bridge:" -ForegroundColor Cyan
Write-Host "  exec_in_cpython_background(`"import numpy; print(numpy.__version__)`")"
Write-Host "  result = run_cellpose(img)  # uses CPython bridge"
Write-Host ""

$exitCode = 0
if ($script:Problems.Count -gt 0) {
    $exitCode = 1
    Write-Host "Setup finished WITH PROBLEMS:" -ForegroundColor Red
    $script:Problems | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    Write-Host "Details are in $LogFile" -ForegroundColor Red
} else {
    Write-Host "Setup completed successfully." -ForegroundColor Green
    # The uv download cache is several GB; it is only useful for retries after a failure.
    Remove-Item $env:UV_CACHE_DIR -Recurse -Force -ErrorAction SilentlyContinue
}

if ($script:Transcribing) { try { Stop-Transcript | Out-Null } catch { } }
if ($PauseAtEnd) { Read-Host "Press Enter to close" | Out-Null }
exit $exitCode
