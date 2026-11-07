<#
.SYNOPSIS
    Deterministic Verification Gate for the Repository (PowerShell).
.DESCRIPTION
    Runs linter, typecheck, and test suite in a single command.
    Returns exit code 0 on success, non-zero on failure.
#>

$ErrorActionPreference = "Stop"

Write-Host "[Verify Gate] Running deterministic verification checks..." -ForegroundColor Cyan

# Check if project is still an uninitialized template
$agentsFile = Join-Path $PSScriptRoot "..\AGENTS.md"
if (Test-Path $agentsFile) {
    $content = Get-Content $agentsFile -Raw
    if ($content -match "State.*UNINITIALIZED_TEMPLATE") {
        Write-Host "[INFO] Project is currently in UNINITIALIZED_TEMPLATE state." -ForegroundColor Yellow
        Write-Host "-> Run the Bootstrap Agent or execute the Commit 1 ritual (see .agents/bootstrap.md)." -ForegroundColor Yellow
        exit 0
    }
}

# ------------------------------------------------------------------------------
# STACK-SPECIFIC COMMAND RUNNER
# (During Commit 1 Bootstrap, the bootstrap agent or script configures this section)
# ------------------------------------------------------------------------------

# Detect stack if not manually customized:
$hasPackageJson = Test-Path (Join-Path $PSScriptRoot "..\package.json")
$hasCsproj      = (Get-ChildItem -Path (Join-Path $PSScriptRoot "..") -Filter "*.csproj" -Recurse -Depth 2 -ErrorAction SilentlyContinue).Count -gt 0
$hasPyproject   = Test-Path (Join-Path $PSScriptRoot "..\pyproject.toml")
$hasGoMod       = Test-Path (Join-Path $PSScriptRoot "..\go.mod")

try {
    if ($hasPackageJson) {
        Write-Host "[DETECTED] Node / JS / TS ecosystem" -ForegroundColor Green
        if (Get-Command "pnpm" -ErrorAction SilentlyContinue) {
            pnpm run check
        } else {
            npm run check
        }
    }
    elseif ($hasCsproj) {
        Write-Host "[DETECTED] C# / .NET ecosystem" -ForegroundColor Green
        dotnet format --verify-no-changes
        dotnet test --no-restore
    }
    elseif ($hasPyproject) {
        Write-Host "[DETECTED] Python ecosystem" -ForegroundColor Green
        uv run ruff check .
        uv run ruff format --check .
        uv run pytest
    }
    elseif ($hasGoMod) {
        Write-Host "[DETECTED] Go ecosystem" -ForegroundColor Green
        go vet ./...
        go test -race ./...
    }
    else {
        Write-Host "[INFO] No specific language manifest detected yet. Verification passed as no-op." -ForegroundColor Yellow
    }

    Write-Host "[SUCCESS] All deterministic checks passed successfully!" -ForegroundColor Green
    exit 0
}
catch {
    Write-Host "[ERROR] Verification failed!" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
