#!/usr/bin/env bash
# Deterministic Verification Gate for the Repository (Bash / POSIX)
# Runs linter, typecheck, and test suite in a single command.
# Returns exit code 0 on success, non-zero on failure.

set -euo pipefail

echo "🔍 [Verify Gate] Running deterministic verification checks..."

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Check if uninitialized template
if grep -q "State.*UNINITIALIZED_TEMPLATE" "${REPO_ROOT}/AGENTS.md" 2>/dev/null; then
    echo "⚠️  Project is currently in UNINITIALIZED_TEMPLATE state."
    echo "👉 Run the Bootstrap Agent or execute the Commit 1 ritual (see .agents/bootstrap.md)."
    exit 0
fi

# Detect Stack
if [ -f "${REPO_ROOT}/package.json" ]; then
    echo "📦 Node / JS / TS ecosystem detected"
    if command -v pnpm &> /dev/null; then
        pnpm run check
    else
        npm run check
    fi
elif [ -n "$(find "${REPO_ROOT}" -maxdepth 2 -name "*.csproj" -print -quit 2>/dev/null)" ]; then
    echo "🔷 C# / .NET ecosystem detected"
    dotnet format --verify-no-changes
    dotnet test --no-restore
elif [ -f "${REPO_ROOT}/pyproject.toml" ]; then
    echo "🐍 Python ecosystem detected"
    uv run ruff check .
    uv run ruff format --check .
    uv run pytest
elif [ -f "${REPO_ROOT}/go.mod" ]; then
    echo "🐹 Go ecosystem detected"
    go vet ./...
    go test -race ./...
else
    echo "ℹ️  No specific language manifest detected yet. Verification passed as no-op."
fi

echo "✅ [Verify Gate] All deterministic checks passed successfully!"
