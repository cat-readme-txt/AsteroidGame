$ErrorActionPreference = "Stop"

$uninstaller = Join-Path $PSScriptRoot "..\scripts\Uninstall-VoidNavigator.ps1"
$fixtureRoot = Join-Path ([System.IO.Path]::GetTempPath()) `
    ("voidnavigator-uninstall-" + [guid]::NewGuid().ToString("N"))
$previousAppData = $env:APPDATA

try {
    $env:APPDATA = Join-Path $fixtureRoot "AppData"
    $saveDir = Join-Path $env:APPDATA "VoidNavigator"
    New-Item $saveDir -ItemType Directory -Force | Out-Null
    Set-Content (Join-Path $saveDir "save.dat") "keep this save"

    $app = Join-Path $fixtureRoot "VoidNavigator"
    New-Item (Join-Path $app "app") -ItemType Directory -Force | Out-Null
    New-Item (Join-Path $app "VoidNavigator.exe") -ItemType File | Out-Null
    New-Item (Join-Path $app "app\AsteroidGame.jar") -ItemType File | Out-Null

    & $uninstaller -AppPath $app -DryRun -Yes
    if (-not (Test-Path -LiteralPath $app)) { throw "Dry run removed the app." }
    & $uninstaller -AppPath $app -Yes
    if ((Test-Path -LiteralPath $app) -or
        -not (Test-Path -LiteralPath (Join-Path $saveDir "save.dat"))) {
        throw "App removal did not preserve the save."
    }

    New-Item $app -ItemType Directory | Out-Null
    New-Item (Join-Path $app "AsteroidGame.jar") -ItemType File | Out-Null
    New-Item (Join-Path $app "Play-VoidNavigator.bat") -ItemType File | Out-Null
    & $uninstaller -AppPath $app -RemoveSaves -Yes
    if ((Test-Path -LiteralPath $app) -or (Test-Path -LiteralPath $saveDir)) {
        throw "Optional save removal did not remove both directories."
    }

    $unrelated = Join-Path $fixtureRoot "unrelated"
    New-Item $unrelated -ItemType Directory | Out-Null
    try {
        & $uninstaller -AppPath $unrelated -Yes
        throw "Unrecognized folder was accepted."
    }
    catch {
        if ($_.Exception.Message -eq "Unrecognized folder was accepted.") { throw }
    }
    if (-not (Test-Path -LiteralPath $unrelated)) {
        throw "Unrecognized folder was removed."
    }
    Write-Host "Windows uninstaller smoke checks passed."
}
finally {
    $env:APPDATA = $previousAppData
    if (Test-Path -LiteralPath $fixtureRoot) {
        Remove-Item -LiteralPath $fixtureRoot -Recurse -Force
    }
}
