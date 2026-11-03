# /*******************************************************************************
#  * Copyright © 2026 ETH Zürich
#  *
#  * This program and the accompanying materials are made available under the
#  * terms of the Apache License, Version 2.0 OR the GNU Lesser General Public
#  * License, Version 2.1 (or at your option any later version).
#  *
#  * You may choose which license you wish to use when copying, modifying,
#  * or distributing this software.
#  *
#  * Licenses are available at:
#  * - Apache 2.0: http://apache.org
#  * - LGPL 2.1:   https://gnu.org
#  *
#  * SPDX-License-Identifier: Apache-2.0 OR LGPL-2.1-or-later
#  *
#  * Contributors:
#  *     Andreas P. Cuny - initial API and implementation
#  ******************************************************************************/
<#
.SYNOPSIS
    Fetches the OME-NGFF validator and web viewers (vizarr) for offline use by the
    YouScope FAIR-export plugin (M6 S1/S2).

.DESCRIPTION
    Downloads prebuilt static web apps and unpacks them into a dedicated "validator"
    subfolder of the YouScope root, so the FAIR-export local data server can serve
    them with no internet connection:

        <YouScopeRoot>\validator\ome-ngff-validator\   (the validator SPA)
        <YouScopeRoot>\validator\vizarr\               (the vizarr viewer)

    It then points the plugin at the local copies by writing these keys into
    graalpy_config.txt:

        fairexport_self_hosted_validator=<...>\validator\ome-ngff-validator
        fairexport_open_launcher=1

    With those set, "Validate / view OME-Zarr" opens the local launcher page, whose
    "Open in napari" button launches napari on this machine, and whose validator link
    points at the locally-served build. The GitHub-hosted validator remains the
    fallback when no local copy is present.

    Run this once at installation time (needs internet). It is idempotent:
    re-running refreshes the copies. If a download fails (e.g. offline), the script
    reports it and leaves any existing copy in place.

.PARAMETER YouScopeRoot
    The YouScope working directory (the folder that contains graalpy_config.txt).
    Defaults to the current directory.

.PARAMETER ValidatorZipUrl
    Override the validator build archive URL (advanced / pinned mirrors).

.PARAMETER VizarrZipUrl
    Override the vizarr build archive URL.

.PARAMETER SkipViewers
    Only fetch the validator, not vizarr.

.EXAMPLE
    .\install_validator.ps1 -YouScopeRoot "C:\Program Files\YouScope"
#>

[CmdletBinding()]
param(
    [string]$YouScopeRoot = (Get-Location).Path,
    [string]$ValidatorZipUrl = "https://github.com/ome/ome-ngff-validator/archive/refs/heads/main.zip",
    [string]$VizarrZipUrl = "https://github.com/hms-dbmi/vizarr/archive/refs/heads/main.zip",
    [switch]$SkipViewers
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Write-Info($msg) { Write-Host "[fair-export] $msg" }
function Write-Warn($msg) { Write-Host "[fair-export] WARNING: $msg" -ForegroundColor Yellow }

$validatorRoot = Join-Path $YouScopeRoot "validator"
$null = New-Item -ItemType Directory -Force -Path $validatorRoot

# TLS 1.2 for older PowerShell/.NET on Windows.
try { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 } catch { }

function Get-AndUnpack {
    param([string]$Url, [string]$TargetDir, [string]$Label)

    $tmp = Join-Path ([IO.Path]::GetTempPath()) ("fairexport_" + [Guid]::NewGuid().ToString("N") + ".zip")
    $extractTmp = Join-Path ([IO.Path]::GetTempPath()) ("fairexport_" + [Guid]::NewGuid().ToString("N"))
    try {
        Write-Info "Downloading $Label from $Url"
        Invoke-WebRequest -Uri $Url -OutFile $tmp -UseBasicParsing

        Write-Info "Unpacking $Label"
        $null = New-Item -ItemType Directory -Force -Path $extractTmp
        Expand-Archive -Path $tmp -DestinationPath $extractTmp -Force

        # GitHub "archive" zips contain a single top-level <repo>-<branch> folder.
        $top = Get-ChildItem -Path $extractTmp -Directory | Select-Object -First 1
        if ($null -eq $top) { throw "archive for $Label was empty" }

        # Prefer a prebuilt static output if the archive ships one, else copy the repo as-is.
        $buildDir = $null
        foreach ($candidate in @("dist", "build", "public", "out")) {
            $p = Join-Path $top.FullName $candidate
            if (Test-Path (Join-Path $p "index.html")) { $buildDir = $p; break }
        }
        if ($null -eq $buildDir) {
            if (Test-Path (Join-Path $top.FullName "index.html")) {
                $buildDir = $top.FullName
            } else {
                Write-Warn "$Label archive has no prebuilt static site (no dist/ or index.html). Copying the source tree; you may need to build it (npm ci && npm run build) and point fairexport_self_hosted_validator at the produced dist/ folder."
                $buildDir = $top.FullName
            }
        }

        if (Test-Path $TargetDir) { Remove-Item -Recurse -Force $TargetDir }
        $null = New-Item -ItemType Directory -Force -Path $TargetDir
        Copy-Item -Path (Join-Path $buildDir "*") -Destination $TargetDir -Recurse -Force
        Write-Info "$Label ready at $TargetDir"
        return $true
    }
    catch {
        Write-Warn "could not fetch $Label ($($_.Exception.Message)). Any existing copy is left untouched."
        return $false
    }
    finally {
        if (Test-Path $tmp) { Remove-Item -Force $tmp -ErrorAction SilentlyContinue }
        if (Test-Path $extractTmp) { Remove-Item -Recurse -Force $extractTmp -ErrorAction SilentlyContinue }
    }
}

$validatorDir = Join-Path $validatorRoot "ome-ngff-validator"
$validatorOk = Get-AndUnpack -Url $ValidatorZipUrl -TargetDir $validatorDir -Label "OME-NGFF validator"

if (-not $SkipViewers) {
    $vizarrDir = Join-Path $validatorRoot "vizarr"
    $vizarrOk = Get-AndUnpack -Url $VizarrZipUrl -TargetDir $vizarrDir -Label "vizarr viewer"
}

# Update graalpy_config.txt

function Set-ConfigKey {
    param([string[]]$Lines, [string]$Key, [string]$Value)
    $updated = $false
    for ($i = 0; $i -lt $Lines.Count; $i++) {
        $t = $Lines[$i].Trim()
        if ($t -notmatch '^\s*#' -and $t -match '^\s*([^=]+?)\s*=') {
            if ($Matches[1].Trim() -eq $Key) { $Lines[$i] = "$Key=$Value"; $updated = $true }
        }
    }
    if (-not $updated) { $Lines += "$Key=$Value" }
    return ,$Lines
}

$configPath = Join-Path $YouScopeRoot "graalpy_config.txt"
$lines = @()
if (Test-Path $configPath) { $lines = @(Get-Content -Path $configPath) }

if ($validatorOk -and (Test-Path (Join-Path $validatorDir "index.html"))) {
    $lines = Set-ConfigKey -Lines $lines -Key "fairexport_self_hosted_validator" -Value $validatorDir
    $lines = Set-ConfigKey -Lines $lines -Key "fairexport_open_launcher" -Value "1"
    Write-Info "Configured plugin to use the self-hosted validator (offline)."
} else {
    Write-Warn "Self-hosted validator not available; the plugin will keep using the GitHub-hosted validator."
}

if (-not $SkipViewers -and (Get-Variable -Name vizarrOk -ErrorAction SilentlyContinue) -and $vizarrOk -and (Test-Path (Join-Path $vizarrDir "index.html"))) {
    $lines = Set-ConfigKey -Lines $lines -Key "fairexport_self_hosted_vizarr" -Value $vizarrDir
    Write-Info "Configured plugin to use the self-hosted vizarr viewer (offline)."
}

Set-Content -Path $configPath -Value $lines -Encoding UTF8
Write-Info "Updated $configPath"
Write-Info "Done. Offline validation/viewing is available from the FAIR-export tool's 'Validate / view' action."
