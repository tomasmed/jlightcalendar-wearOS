# 🤖 Polyglot Agentic Git Template

A high-velocity, low-overhead repository template designed for the **Agentic SDLC** and solo/hobbyist developers. 

Built to support **Next.js, Vue 3, SvelteKit, C# (.NET), Python (uv), and Go** with zero initial clutter.

---

## 🌟 Core Concepts

1. **"Harness Over Model"**: The repository acts as the deterministic control harness for AI agents (Cursor, Claude Code, GitHub Copilot, Aider, Antigravity).
2. **Universal Contract (`AGENTS.md`)**: A single source of truth for build commands, test recipes, and inviolable guardrails stewarded under the Linux Foundation / Agentic AI Foundation standard.
3. **Spec-Driven Development (`specs/`)**: Eliminates "vibe-coding" drift by decoupling **Intent** (`spec.md`) from **Plan** (`plan.md`) and **Execution** (`tasks.md`).
4. **Deterministic Gate (`scripts/verify.*`)**: A single compound command (`lint + typecheck + test`) that agents must run before declaring work complete.
5. **Architectural Decision Records (`docs/adr/`)**: In-repo MADR records preventing AI from violating past architectural choices or hallucinating external dependencies.
6. **PR Review Agent Contract (`.agents/pr-reviewer.md`)**: Specific guidelines for automated code reviews focused on spec conformance, test integrity, and security (without linter nitpicking).

---

## 🚀 Quick Start: The "Commit 1" Bootstrap Ritual

When you create a new repository from this template, your first action is to specialize it for your target stack.

### Option A: Let an AI Agent Initialize It (Recommended)
Prompt your agent in the new repository:
> *"Initialize this project for **Next.js 15 (TypeScript, Tailwind)** using the bootstrap protocol in `.agents/bootstrap.md`."*
*(Replace with Vue, Svelte, .NET, Python, or Go as desired).*

The agent will:
1. Update `AGENTS.md` with your stack's exact build/test/lint commands.
2. Initialize `docs/adr/0002-technology-stack.md`.
3. Scaffold baseline files and verify them with `./scripts/verify.ps1`.
4. Stage and commit: `feat(init): bootstrap project with [Stack] architecture and agent harness`.

### Option B: Run the Bootstrap Helper Manually
```powershell
# In PowerShell:
./scripts/bootstrap.ps1 -Stack nextjs   # or vue, svelte, dotnet, python, go
```

---

## 📁 Repository Blueprint

```text
├── .agents/                           # Specialized agent persona rubrics
│   ├── bootstrap.md                   # Instructions for the Commit 1 ritual
│   ├── pr-reviewer.md                 # Rules & rubric for PR Review agents
│   ├── code-implementer.md            # TDD & implementation guidelines
│   └── architect.md                   # Architecture & ADR review guidelines
├── .cursor/rules/                     # IDE-specific pointers
│   └── 00-agent-core.mdc              # Points Cursor to AGENTS.md
├── .github/
│   ├── copilot-instructions.md        # Points Copilot to AGENTS.md
│   ├── PULL_REQUEST_TEMPLATE.md       # PR template with agent validation gates
│   ├── ISSUE_TEMPLATE/feature_spec.md # Feature spec issue starter
│   └── workflows/ci.yml               # Auto-detecting multi-stack CI gate
├── docs/
│   ├── adr/                           # Architectural Decision Records (MADR)
│   │   ├── 0001-record-architecture-decisions.md
│   │   ├── README.md                  # Index of ADRs
│   │   └── template.md                # Markdown ADR template
│   ├── architecture/                  # High-level architecture & module boundaries
│   │   └── overview.md
│   └── workflows/                     # Human-in-the-loop workflows
│       └── agentic-sdlc.md
├── specs/                             # Spec-Driven Development workspace
│   ├── _template/                     # Master templates
│   │   ├── spec.md                    # Business intent & acceptance criteria
│   │   ├── plan.md                    # Architecture plan & touched files
│   │   └── tasks.md                   # Atomic checklist with TDD checks
│   ├── active/                        # Currently active feature spec
│   └── archive/                       # Historical completed specs
├── scripts/
│   ├── verify.ps1                     # PowerShell deterministic test/lint/type gate
│   ├── verify.sh                      # Bash deterministic test/lint/type gate
│   └── bootstrap.ps1                  # Interactive stack switcher
├── AGENTS.md                          # Universal Root Agent Contract (SSOT)
├── CLAUDE.md                          # Points Claude Code to AGENTS.md
├── .gitignore                         # Comprehensive polyglot gitignore
└── README.md                          # This documentation
```

---

## 🔄 Daily Workflow with Agents (Spec-Driven SDLC)

```
1. Specify              2. Plan                 3. Implement (TDD)      4. Deterministic Gate     5. Review & Merge
(Human / spec.md) ──▶  (Agent / plan.md) ──▶   (Agent / tasks.md) ──▶  (./scripts/verify.*) ──▶  (.agents/pr-reviewer.md)
```

1. **Specify**: Copy `specs/_template/` to `specs/active/`. Define what you want in `spec.md`.
2. **Plan**: Ask your agent: *"Read `specs/active/spec.md` and draft `plan.md` and `tasks.md`."*
3. **Approve**: Review the plan to verify there is no over-engineering or unwanted dependencies.
4. **Implement**: Tell the agent: *"Work through `tasks.md` using TDD. Run `./scripts/verify.ps1` after each task."*
5. **Review**: Ask the PR review agent: *"Evaluate the git diff against `specs/active/spec.md` following `.agents/pr-reviewer.md`."*
6. **Merge & Archive**: Merge the code and move `specs/active/` to `specs/archive/YYYY-MM-feature-name/`.

---

## 🛡️ Supported Technology Stacks

| Ecosystem | Primary Tools | Full Verify Command |
| :--- | :--- | :--- |
| **Next.js** | Next.js 15, TypeScript, Tailwind, Vitest | `pnpm lint && pnpm typecheck && pnpm test` |
| **Vue.js** | Vue 3, Vite, TypeScript, Vitest | `pnpm lint && pnpm typecheck && pnpm test` |
| **Svelte.js** | SvelteKit 2, Vite, TypeScript, Vitest | `pnpm check && pnpm lint && pnpm test` |
| **C# (.NET)** | .NET 9/8, xUnit, C# 13, dotnet CLI | `dotnet format --verify-no-changes && dotnet test` |
| **Python** | Python 3.12+, uv, ruff, pytest, mypy | `uv run ruff check . && uv run mypy . && uv run pytest` |
| **Go** | Go 1.23+, golangci-lint, standard tests | `golangci-lint run && go test -race ./...` |
