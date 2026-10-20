param(
    [string]$AppName = "VoidNavigator"
)

$ErrorActionPreference = "Stop"
if ($env:OS -ne "Windows_NT") {
    throw "Run this script on Windows with JDK 17 or newer."
}
if ($env:PROCESSOR_ARCHITECTURE -ne "AMD64") {
    throw "This script packages the x64 Windows build; run it on x64 Windows."
}

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$build = Join-Path $root "build\windows"
$classes = Join-Path $build "classes"
$dist = Join-Path $root "dist"
$archive = Join-Path $dist "$AppName-Windows-x64-0.zip"

foreach ($tool in @("javac", "jar", "jpackage")) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        throw "Missing $tool. Install a JDK 17 or newer and add it to PATH."
    }
}

if (Test-Path $build) { Remove-Item $build -Recurse -Force }
New-Item $classes -ItemType Directory -Force | Out-Null
$sources = @(Get-ChildItem (Join-Path $root "src\desktop") -Filter "*.java" |
    ForEach-Object { $_.FullName })
& javac --release 17 -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw "Java compilation failed." }

Copy-Item (Join-Path $root "src\desktop\assets") (Join-Path $classes "assets") -Recurse
& jar --create --file (Join-Path $build "AsteroidGame.jar") --main-class DesktopLauncher -C $classes .
if ($LASTEXITCODE -ne 0) { throw "JAR creation failed." }

New-Item $dist -ItemType Directory -Force | Out-Null
$stage = Join-Path $dist (".windows-package-" + [guid]::NewGuid().ToString("N"))
New-Item $stage -ItemType Directory | Out-Null
try {
    & jpackage --input $build --main-jar "AsteroidGame.jar" --name $AppName `
        --app-version "1" --type app-image --dest $stage
    if ($LASTEXITCODE -ne 0) { throw "Windows app packaging failed." }

    $app = Join-Path $stage $AppName
    if (-not (Test-Path (Join-Path $app "$AppName.exe"))) {
        throw "The packaged Windows launcher is missing."
    }
    Copy-Item (Join-Path $root "README.md") $app
    Copy-Item (Join-Path $root "GAME_MECHANICS.md") $app
    Copy-Item (Join-Path $root "STORY.md") $app
    $stagedArchive = Join-Path $stage "$AppName-Windows-x64-0.zip"
    Compress-Archive -Path $app -DestinationPath $stagedArchive -CompressionLevel Optimal
    Copy-Item $stagedArchive $archive -Force
    Write-Host "Packaged Windows download: $archive"
}
finally {
    if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
}
