<#
    fetch-nsis-plugins.ps1  (run once, locally; commit the result)

    Populates installer\nsis-plugins\x86-ansi\ and \x86-unicode\ with
    AccessControl.dll so CI never has to download it (SourceForge hotlinks 403
    from CI). Place this file in installer\nsis-plugins\ and run:

        pwsh -File installer\nsis-plugins\fetch-nsis-plugins.ps1

    Then: git add installer\nsis-plugins ; git commit.

    AccessControl is GPL -> compatible with YouScope's GPLv2. Keep its readme/
    license next to the DLLs.

    If the automatic download is blocked, download AccessControl.zip by hand in a
    browser from https://nsis.sourceforge.io/AccessControl_plug-in and drop the
    two DLLs into the folders below.
#>
param(
    [string]$OutDir = $PSScriptRoot,
    # A browser User-Agent gets past the 403 that CI's default agent hits.
    [string]$Url    = "https://nsis.sourceforge.io/mediawiki/images/4/4a/AccessControl.zip"
)

$ansi    = Join-Path $OutDir "x86-ansi"
$unicode = Join-Path $OutDir "x86-unicode"
New-Item -ItemType Directory -Force -Path $ansi,$unicode | Out-Null

$zip = Join-Path $env:TEMP "AccessControl.zip"
$dst = Join-Path $env:TEMP "AccessControl"
$ua  = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125 Safari/537.36"

try {
    Write-Host "Downloading AccessControl.zip ..."
    Invoke-WebRequest -Uri $Url -OutFile $zip -UseBasicParsing -UserAgent $ua -Headers @{ 'Referer' = 'https://nsis.sourceforge.io/AccessControl_plug-in' }
} catch {
    Write-Error "Automatic download failed ($($_.Exception.Message))."
    Write-Host  "Download AccessControl.zip manually from https://nsis.sourceforge.io/AccessControl_plug-in"
    Write-Host  "and copy the ANSI DLL to $ansi and the Unicode DLL to $unicode."
    exit 1
}

if (Test-Path $dst) { Remove-Item $dst -Recurse -Force }
Expand-Archive $zip -DestinationPath $dst -Force

# The archive uses either  Plugins\AccessControl.dll + Unicode\Plugins\AccessControl.dll
# or the newer            Plugins\x86-ansi\ + Plugins\x86-unicode\  layout. Handle both.
$ansiDll = Get-ChildItem $dst -Recurse -Filter AccessControl.dll |
    Where-Object { $_.FullName -match 'x86-ansi' -or $_.FullName -notmatch 'Unicode|x86-unicode' } |
    Select-Object -First 1
$uniDll  = Get-ChildItem $dst -Recurse -Filter AccessControl.dll |
    Where-Object { $_.FullName -match 'Unicode|x86-unicode' } |
    Select-Object -First 1

if (-not $ansiDll) { throw "ANSI AccessControl.dll not found in archive" }
Copy-Item $ansiDll.FullName (Join-Path $ansi "AccessControl.dll") -Force
if ($uniDll) { Copy-Item $uniDll.FullName (Join-Path $unicode "AccessControl.dll") -Force }

# Keep the license text alongside the binaries.
Get-ChildItem $dst -Recurse -Include *.txt,*.md,LICENSE* |
    ForEach-Object { Copy-Item $_.FullName $OutDir -Force -ErrorAction SilentlyContinue }

Write-Host ""
Write-Host "Vendored:" -ForegroundColor Green
Get-ChildItem $ansi,$unicode -Filter *.dll | ForEach-Object { Write-Host "    $($_.FullName)" }
Write-Host ""
Write-Host "Now: git add installer\nsis-plugins ; git commit -m 'Vendor NSIS AccessControl plugin'"
