# 0001. Record Architecture Decisions

* **Status**: accepted
* **Deciders**: Repository Author & AI Agents
* **Date**: 2026-10-07
* **Technical Story**: Initial template setup

## Context and Problem Statement
We need a lightweight, in-repo mechanism to record architectural decisions so that both human developers and autonomous AI agents have shared, persistent knowledge of past technical choices. Without this, agents frequently suffer from "architectural drift" and re-invent or violate previous design choices across coding sessions.

## Decision Drivers
* Must be version-controlled in the repository alongside the code.
* Must be plain Markdown easily readable and writable by LLMs and humans.
* Must not require heavy external tools or databases.
* Must encourage documenting *why* a choice was made and what trade-offs were accepted.

## Considered Options
* Option 1: Architecture Decision Records (ADRs) using Markdown (MADR format).
* Option 2: Documenting decisions in pull request comments or commit messages only.
* Option 3: External wiki / Notion / Confluence.

## Decision Outcome
Chosen option: "Option 1: ADRs using Markdown", because it keeps decisions co-located with code, survives chat session resets, and allows agents to consult `docs/adr/` before proposing new dependencies or patterns.

### Positive Consequences
* AI agents can query `docs/adr/` to understand existing constraints and past decisions.
* Humans have a clear historical changelog of architecture evolution.
* Reduces verification debt and accidental dependency bloat.

### Negative Consequences / Trade-offs
* Requires a small amount of discipline to author an ADR when making significant architectural choices.
