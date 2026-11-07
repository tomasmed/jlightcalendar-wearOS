# ADR 0002: Kotlin 2.0, Jetpack Compose for Wear OS & Clean Architecture Stack

* **Status**: Accepted
* **Date**: 2026-11-07
* **Deciders**: Maintainers & Lead Architect Agent

## Context and Problem Statement

We need a modern, battery-efficient, and agent-maintainable technology stack for the **JlightCalendar** smartwatch companion app targeting Samsung Galaxy Watch (Wear OS 4 & 5, API 30–34). The repository began from a generic polyglot template and must be standardized around native Android/Wear OS tooling without brittle PowerShell or shell wrapper scripts.

## Decision Drivers

* **First-Class Circular Smartwatch UX**: Native support for curved lists (`ScalingLazyColumn`), physical rotating bezel/crown input, ProtoLayout Tiles, and Watch Face Complications.
* **Fast Headless Verification for AI Agents**: Domain and data tests must run on the local JVM (`./gradlew testDebugUnitTest`) in seconds without requiring a booted Android emulator for every TDD cycle.
* **Visual Alignment with Upstream `JlightCalendar` (ADR 0016)**: Support true-black (`#000000`) AMOLED backgrounds paired with high-contrast Fluorescent Light-Spill (`osl`) member color accents.
* **Single Dependency Catalog**: Prevent version drift across Gradle files.

## Considered Options

1. **Native Kotlin 2.0 + Jetpack Compose for Wear OS + ProtoLayout + Ktor + Room** (managed via `gradle/libs.versions.toml`).
2. **Flutter / React Native on Wear OS**: Poor or non-existent support for native Wear OS ProtoLayout Tiles, Watch Face Complications, and rotary bezel physics.
3. **Legacy Android XML Views (`WearableRecyclerView`)**: Deprecated by Google in favor of Jetpack Compose for Wear OS.

## Decision Outcome

Chosen option: **Option 1 — Native Kotlin 2.0 + Jetpack Compose for Wear OS + ProtoLayout + Ktor + Room**, structured in a 3-layer Clean Architecture (`ui`/`tile`/`complication` -> `domain` -> `data`) and built directly via `./gradlew`.

### Consequences

* **Positive**: First-class integration with Wear OS Tiles, Complications, rotary input, and fast JVM unit testing.
* **Positive**: Zero wrapper script maintenance—all local and CI tasks invoke `./gradlew` directly.
* **Negative**: Requires JDK 21 and Android SDK 34 installed on developer workstations and CI runners.
