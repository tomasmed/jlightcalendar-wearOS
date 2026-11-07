<#
.SYNOPSIS
    Repository Stack Bootstrapper (PowerShell).
.DESCRIPTION
    Automates the "1st Commit Ritual" by reconfiguring AGENTS.md, docs/adr/0002,
    and scaffolding stack-specific templates.
.PARAMETER Stack
    Target stack: nextjs, vue, svelte, dotnet, python, go
#>

param (
    [ValidateSet("nextjs", "vue", "svelte", "dotnet", "python", "go")]
    [string]$Stack
)

$ErrorActionPreference = "Stop"

if (-not $Stack) {
    Write-Host "Select a stack to initialize:" -ForegroundColor Cyan
    Write-Host "1) nextjs  - Next.js 15 (TypeScript, Tailwind, Vitest)"
    Write-Host "2) vue     - Vue 3 (Vite, TypeScript, Vitest)"
    Write-Host "3) svelte  - SvelteKit 2 (TypeScript, Vitest)"
    Write-Host "4) dotnet  - C# / .NET 9 (xUnit, Solution)"
    Write-Host "5) python  - Python 3.12+ (uv, ruff, pytest)"
    Write-Host "6) go      - Go 1.23+ (golangci-lint, tests)"
    $choice = Read-Host "Enter selection (1-6)"
    switch ($choice) {
        "1" { $Stack = "nextjs" }
        "2" { $Stack = "vue" }
        "3" { $Stack = "svelte" }
        "4" { $Stack = "dotnet" }
        "5" { $Stack = "python" }
        "6" { $Stack = "go" }
        default { Write-Error "Invalid choice"; exit 1 }
    }
}

Write-Host "[INIT] Initializing project for stack: $Stack..." -ForegroundColor Green

$repoRoot = Join-Path $PSScriptRoot ".."
$agentsMd = Join-Path $repoRoot "AGENTS.md"

# Read existing AGENTS.md
$content = Get-Content $agentsMd -Raw

# Replace State
$content = $content -replace "State.*\[UNINITIALIZED_TEMPLATE \| INITIALIZED\]", "State: INITIALIZED"

switch ($Stack) {
    "nextjs" {
        $content = $content -replace "Ecosystem.*", "Ecosystem: Next.js 15 (TypeScript, Tailwind CSS)"
        $content = $content -replace "Primary Language.*", "Primary Language: TypeScript"
        $content = $content -replace "Package / Build Tool.*", "Package / Build Tool: pnpm"
        $content = $content -replace "\[BUILD_CMD\]", "pnpm build"
        $content = $content -replace "\[TEST_CMD\]", "pnpm test"
        $content = $content -replace "\[TEST_SINGLE_CMD\]", "pnpm test -- <path>"
        $content = $content -replace "\[LINT_CMD\]", "pnpm lint"
        $content = $content -replace "\[FIX_CMD\]", "pnpm fix"
    }
    "vue" {
        $content = $content -replace "Ecosystem.*", "Ecosystem: Vue 3 (Vite, TypeScript, Pinia)"
        $content = $content -replace "Primary Language.*", "Primary Language: TypeScript"
        $content = $content -replace "Package / Build Tool.*", "Package / Build Tool: pnpm"
        $content = $content -replace "\[BUILD_CMD\]", "pnpm build"
        $content = $content -replace "\[TEST_CMD\]", "pnpm test"
        $content = $content -replace "\[TEST_SINGLE_CMD\]", "pnpm test -- <path>"
        $content = $content -replace "\[LINT_CMD\]", "pnpm lint"
        $content = $content -replace "\[FIX_CMD\]", "pnpm fix"
    }
    "svelte" {
        $content = $content -replace "Ecosystem.*", "Ecosystem: SvelteKit 2 (Vite, TypeScript)"
        $content = $content -replace "Primary Language.*", "Primary Language: TypeScript"
        $content = $content -replace "Package / Build Tool.*", "Package / Build Tool: pnpm"
        $content = $content -replace "\[BUILD_CMD\]", "pnpm build"
        $content = $content -replace "\[TEST_CMD\]", "pnpm test"
        $content = $content -replace "\[TEST_SINGLE_CMD\]", "pnpm test -- <path>"
        $content = $content -replace "\[LINT_CMD\]", "pnpm check && pnpm lint"
        $content = $content -replace "\[FIX_CMD\]", "pnpm format"
    }
    "dotnet" {
        $content = $content -replace "Ecosystem.*", "Ecosystem: C# / .NET 9"
        $content = $content -replace "Primary Language.*", "Primary Language: C#"
        $content = $content -replace "Package / Build Tool.*", "Package / Build Tool: dotnet CLI"
        $content = $content -replace "\[BUILD_CMD\]", "dotnet build"
        $content = $content -replace "\[TEST_CMD\]", "dotnet test"
        $content = $content -replace "\[TEST_SINGLE_CMD\]", "dotnet test --filter <name>"
        $content = $content -replace "\[LINT_CMD\]", "dotnet format --verify-no-changes"
        $content = $content -replace "\[FIX_CMD\]", "dotnet format"
    }
    "python" {
        $content = $content -replace "Ecosystem.*", "Ecosystem: Python 3.12+ (uv, pytest, ruff)"
        $content = $content -replace "Primary Language.*", "Primary Language: Python"
        $content = $content -replace "Package / Build Tool.*", "Package / Build Tool: uv"
        $content = $content -replace "\[BUILD_CMD\]", "uv sync"
        $content = $content -replace "\[TEST_CMD\]", "uv run pytest"
        $content = $content -replace "\[TEST_SINGLE_CMD\]", "uv run pytest <path>"
        $content = $content -replace "\[LINT_CMD\]", "uv run ruff check ."
        $content = $content -replace "\[FIX_CMD\]", "uv run ruff format ."
    }
    "go" {
        $content = $content -replace "Ecosystem.*", "Ecosystem: Go 1.23+ (standard toolchain, golangci-lint)"
        $content = $content -replace "Primary Language.*", "Primary Language: Go"
        $content = $content -replace "Package / Build Tool.*", "Package / Build Tool: go CLI"
        $content = $content -replace "\[BUILD_CMD\]", "go build ./..."
        $content = $content -replace "\[TEST_CMD\]", "go test ./..."
        $content = $content -replace "\[TEST_SINGLE_CMD\]", "go test -run <name> ./..."
        $content = $content -replace "\[LINT_CMD\]", "golangci-lint run"
        $content = $content -replace "\[FIX_CMD\]", "golangci-lint run --fix"
    }
}

Set-Content -Path $agentsMd -Value $content -NoNewline
Write-Host "[SUCCESS] AGENTS.md updated with $Stack commands." -ForegroundColor Green

# Create docs/adr/0002-technology-stack.md
$adrDate = (Get-Date).ToString("yyyy-MM-dd")
$adr0002 = @"
# 0002. Adopt $Stack Technology Stack

* **Status**: accepted
* **Deciders**: Repository Author & AI Bootstrap Agent
* **Date**: $adrDate
* **Technical Story**: Project Initialization (Commit 1)

## Context and Problem Statement
We need an established foundation and toolchain for this project.

## Decision Outcome
Adopted **$Stack** ecosystem. All agent commands in `AGENTS.md` and `./scripts/verify.ps1` are configured for this stack.
"@

Set-Content -Path (Join-Path $repoRoot "docs\adr\0002-technology-stack.md") -Value $adr0002
Write-Host "[SUCCESS] Created docs/adr/0002-technology-stack.md" -ForegroundColor Green

Write-Host "`n[DONE] Project successfully configured for $Stack!" -ForegroundColor Cyan
Write-Host "-> Next step: Stage files and commit: git commit -m 'feat(init): bootstrap project with $Stack architecture and agent harness'" -ForegroundColor Yellow
