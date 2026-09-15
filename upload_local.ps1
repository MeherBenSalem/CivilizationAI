# Build is assumed done into dist/. Upload to Modrinth + CurseForge.
# Tokens: C:\Users\mahou\NightBeam-Knowledge-Base\secrets\local.env
#
# Usage:
#   .\upload_local.ps1
#   .\upload_local.ps1 -DryRun
#   .\upload_local.ps1 -ModrinthOnly
#   .\upload_local.ps1 -CurseForgeOnly

param(
    [switch]$CurseForgeOnly,
    [switch]$ModrinthOnly,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
Set-Location $root

if (-not (Test-Path (Join-Path $root "dist"))) {
    throw "dist/ missing - build jars first"
}

# Ensure PATCH_NOTES exists on current branch checkout
$notes = Join-Path $root "PATCH_NOTES.md"
if (-not (Test-Path $notes)) {
    @"
# Smart Villagers AI 1.0.0

Initial public MultiLoader release: proximity chat, DeepSeek AI, editable personas.
Supports Minecraft 1.20.1 (Fabric/Forge), 1.21.1 (Fabric/Forge/NeoForge), 26.2 (Fabric/NeoForge).
"@ | Set-Content -Path $notes -Encoding UTF8
}

$nodeArgs = @("scripts/upload_platforms.mjs")
if ($CurseForgeOnly) { $nodeArgs += "--curseforge-only" }
if ($ModrinthOnly) { $nodeArgs += "--modrinth-only" }
if ($DryRun) { $nodeArgs += "--dry-run" }

Write-Host "=== Local upload Smart Villagers AI ===" -ForegroundColor Green
node @nodeArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ""
Write-Host "Done. Verify:" -ForegroundColor Cyan
Write-Host "  https://modrinth.com/mod/smart-villagers-ai/versions"
Write-Host "  https://www.curseforge.com/minecraft/mc-mods/smart-villagers-ai/files"
