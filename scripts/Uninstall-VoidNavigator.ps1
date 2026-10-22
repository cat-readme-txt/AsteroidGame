[CmdletBinding()]
param(
    [string]$AppPath,
    [switch]$RemoveSaves,
    [switch]$Yes,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($AppPath)) {
    $AppPath = Join-Path $PSScriptRoot "VoidNavigator"
}

$app = Get-Item -LiteralPath $AppPath -ErrorAction Stop
if (-not $app.PSIsContainer -or
    ($app.Attributes -band [System.IO.FileAttributes]::ReparsePoint)) {
    throw "The app path must be a normal directory: $AppPath"
}

$nativeExe = Join-Path $app.FullName ($app.Name + ".exe")
$nativeJar = Join-Path $app.FullName "app\AsteroidGame.jar"
$portableJar = Join-Path $app.FullName "AsteroidGame.jar"
$portableLauncher = Join-Path $app.FullName "Play-VoidNavigator.bat"
if ((Test-Path -LiteralPath $nativeExe -PathType Leaf) -and
    (Test-Path -LiteralPath $nativeJar -PathType Leaf)) {
    $appKind = "Windows app"
}
elseif ((Test-Path -LiteralPath $portableJar -PathType Leaf) -and
        (Test-Path -LiteralPath $portableLauncher -PathType Leaf)) {
    $appKind = "portable download"
}
else {
    throw "Refusing to remove an unrecognized folder: $($app.FullName)"
}

$appDataRoot = $env:APPDATA
if ([string]::IsNullOrWhiteSpace($appDataRoot)) {
    $appDataRoot = Join-Path ([Environment]::GetFolderPath("UserProfile")) "AppData\Roaming"
}
$saveDir = Join-Path $appDataRoot "VoidNavigator"

Write-Host "App to remove ($appKind): $($app.FullName)"
if ($RemoveSaves) {
    Write-Host "Save data to remove: $saveDir"
}
else {
    Write-Host "Save data will be kept: $saveDir"
}

if ($DryRun) {
    Write-Host "Dry run: nothing removed."
    return
}

if (-not $Yes) {
    $reply = Read-Host "Quit the game, then type REMOVE to continue"
    if ($reply -cne "REMOVE") {
        Write-Host "Cancelled."
        return
    }
}

if (Get-Process -Name "VoidNavigator" -ErrorAction SilentlyContinue) {
    throw "Quit VoidNavigator before uninstalling it."
}

Remove-Item -LiteralPath $app.FullName -Recurse -Force
if ($RemoveSaves -and (Test-Path -LiteralPath $saveDir)) {
    $saveItem = Get-Item -LiteralPath $saveDir
    if (-not $saveItem.PSIsContainer -or
        ($saveItem.Attributes -band [System.IO.FileAttributes]::ReparsePoint)) {
        throw "App removed; save path is not a normal directory, so it was kept: $saveDir"
    }
    Remove-Item -LiteralPath $saveDir -Recurse -Force
}
Write-Host "VoidNavigator removed."
