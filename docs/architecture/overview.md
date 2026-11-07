# System Architecture & Modularity Overview

> *Note to Developers & Agents: Keep this document updated whenever module boundaries or core data flows change.*

---

## 1. System Context & Purpose
* **High-Level Purpose**: [Describe the problem this project solves]
* **Target Audience / Consumers**: [End users, internal services, CLI users]

---

## 2. Directory Layout & Module Responsibilities

```text
src/ (or core project folder)
├── domain/       # Core business entities, models, and domain logic (zero external dependencies)
├── services/     # Application use-cases, orchestration, and business workflows
├── infrastructure/# Database, network clients, file system, external API adapters
└── presentation/ # UI components, CLI commands, HTTP controllers, or API routes
```

### Module Boundary Rules:
1. **Dependency Direction**: Presentation and Infrastructure depend inwards toward Domain. Domain **never** imports from Infrastructure or Presentation.
2. **Side Effects**: Isolate network and I/O operations into the infrastructure layer; keep core business rules purely deterministic.

---

## 3. Technology Stack Summary
*(To be populated during Commit 1 Bootstrap ritual)*
* **Runtime / Framework**: `[e.g. Next.js 15 / .NET 9 / Python 3.12 / Go 1.23]`
* **Database / Storage**: `[None / SQLite / PostgreSQL / Redis]`
* **Testing Stack**: `[Vitest / xUnit / pytest / go test]`
* **Linter & Formatter**: `[Biome / dotnet format / ruff / golangci-lint]`
