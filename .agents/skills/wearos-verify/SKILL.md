---
name: wearos-verify
description: Runs headless Gradle verification gates, unit tests, and Android Lint diagnostics for JlightCalendar-wearOS.
---

# Wear OS Verification & Diagnostics Skill

Use this skill whenever verifying code changes, running unit tests, or diagnosing Kotlin compiler and Android Lint errors in `JlightCalendar-wearOS`.

## 1. Environment Prerequisites

Ensure `JAVA_HOME` points to JDK 21 (`C:\Program Files\Android\openjdk\jdk-21.0.8` on Windows) and `ANDROID_HOME` points to the Android SDK (`C:\Users\tomas\AppData\Local\Android\Sdk` on Windows, or configured via `local.properties`).

## 2. Canonical Verification Commands

Run these directly from the repository root (`E:\Projects\JlightCalendar-wearOS`) without any wrapper scripts:

```powershell
# Full deterministic verification gate (Unit Tests + Android Lint)
.\gradlew.bat check lintDebug

# Build the debug Wear OS APK
.\gradlew.bat assembleDebug

# Run only JVM unit tests (fast ~3s feedback loop)
.\gradlew.bat testDebugUnitTest

# Run a single unit test class
.\gradlew.bat testDebugUnitTest --tests "dev.tomasmed.jlight.wear.domain.WallClockRecurrenceExpanderTest"
```

## 3. Inspecting Failure Reports

When a verification gate fails:
1. **Unit Test Failures**: Inspect console stack traces or read `app/build/reports/tests/testDebugUnitTest/index.html` and XML results in `app/build/test-results/testDebugUnitTest/`.
2. **Android Lint Failures**: Read `app/build/reports/lint-results-debug.txt` or `app/build/reports/lint-results-debug.xml` for exact file, line number, and rule ID.
3. **Kotlin Compiler Errors**: Re-run with `--stacktrace --info` if type inference or Compose compiler diagnostics are truncated.
