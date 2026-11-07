# Project Bootstrap Agent Instructions (The "1st Commit Ritual")

When a user initializes a new repository from this template, their first prompt will typically be:
> *"Initialize this project for [Next.js / Vue / Svelte / C# .NET / Python / Go] with [specific tools/features]."*

As the **Bootstrap Agent**, your objective is to transform this polyglot template into a fully configured, stack-specific repository in a single atomic setup.

---

## 1. Step-by-Step Bootstrap Protocol

### Step 1: Detect or Confirm the Target Stack
Identify the target ecosystem from the user prompt:
* **Next.js**: TypeScript, Next.js 15 (App Router), Tailwind CSS, Biome or ESLint/Prettier, Vitest or Jest.
* **Vue.js**: TypeScript, Vue 3, Vite, Pinia, Biome/ESLint, Vitest.
* **Svelte.js**: TypeScript, SvelteKit 2, Vite, Vitest.
* **C# / .NET**: .NET 9/8, C# 13/12, xUnit/NUnit, Solution (`.sln`), `Directory.Build.props`.
* **Python**: Python 3.12+, `uv` package manager, `ruff` (linter/formatter), `pytest`, `mypy` or `pyright`.
* **Go**: Go 1.23+, `go mod`, `golangci-lint`, standard testing / `testify`.

---

### Step 2: Update `AGENTS.md`
Edit [AGENTS.md](../AGENTS.md):
1. Change **State** from `UNINITIALIZED_TEMPLATE` to `INITIALIZED`.
2. Set **Ecosystem**, **Primary Language**, and **Package Tool**.
3. Fill in the **Deterministic Command Matrix** table with exact commands:

#### Reference Command Table by Stack:

| Stack | Build | Full Verify | Test All | Test Single | Lint | Fix |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Next / Vue / Svelte (pnpm)** | `pnpm build` | `./scripts/verify.ps1` | `pnpm test` | `pnpm test -- <path>` | `pnpm lint` | `pnpm fix` |
| **C# / .NET (dotnet)** | `dotnet build` | `./scripts/verify.ps1` | `dotnet test` | `dotnet test --filter <name>` | `dotnet format --verify-no-changes` | `dotnet format` |
| **Python (uv)** | `uv sync` | `./scripts/verify.ps1` | `uv run pytest` | `uv run pytest <path>` | `uv run ruff check .` | `uv run ruff format .` |
| **Go (go)** | `go build ./...` | `./scripts/verify.ps1` | `go test ./...` | `go test -run <name> ./...` | `golangci-lint run` | `golangci-lint run --fix` |

---

### Step 3: Configure Deterministic Verification Scripts
Update both [scripts/verify.ps1](../scripts/verify.ps1) and [scripts/verify.sh](../scripts/verify.sh) to execute the exact compound check for the chosen stack:
* **Node/TS**: `pnpm lint && pnpm typecheck && pnpm test`
* **C# / .NET**: `dotnet format --verify-no-changes && dotnet build --no-incremental && dotnet test --no-build`
* **Python**: `uv run ruff check . && uv run ruff format --check . && uv run mypy . && uv run pytest`
* **Go**: `golangci-lint run && go test -race ./...`

---

### Step 4: Configure CI Workflow
Update [.github/workflows/ci.yml](../.github/workflows/ci.yml):
* Activate the matching job for the selected ecosystem (e.g., enable the `test-node`, `test-dotnet`, `test-python`, or `test-go` job).
* Remove or comment out the inactive placeholder jobs.

---

### Step 5: Record the Initial Architecture Decisions (ADR)
Create `docs/adr/0002-technology-stack.md` using [docs/adr/template.md](../docs/adr/template.md):
* **Title**: `ADR 0002: Adopt [Stack Name] Architecture and Tooling`
* **Status**: `Accepted`
* **Context**: Explain why this stack was chosen for the project.
* **Consequences**: List tools, packages, and conventions established.

---

### Step 6: Initialize Baseline Code & Tests
Generate a clean, minimal "Hello World" or skeleton structure that matches the stack:
* Ensure at least **one unit test** is written and passes.
* Run `./scripts/verify.ps1` (or `./scripts/verify.sh`) to prove 100% green verification.

---

### Step 7: Draft Commit 1
Stage all files and commit with a clean Conventional Commit message:
```bash
git add .
git commit -m "feat(init): bootstrap project with [Stack Name] architecture and agent harness"
```

Summarize the changes to the user and present the newly enabled command matrix.
