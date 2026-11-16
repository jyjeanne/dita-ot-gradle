# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Gradle wrapper; JDK 17 for building (bytecode targets Java 8). Default task is `check`.

```bash
./gradlew clean assemble            # Build plugin jar (build/libs)
./gradlew test                      # Run unit tests (Kotest on JUnit Platform)
./gradlew test --tests "com.github.jyjeanne.DitaOtTaskSpec"        # One test class
./gradlew test --tests "com.github.jyjeanne.DitaOtTaskSpec.*some name*"  # Filter by spec/test name
./gradlew publishToMavenLocal       # Install plugin locally (used by CI integration jobs)
./gradlew dependencyUpdates         # Version check (ben-manes plugin)
```

- `test` depends on `installDitaOt`, which downloads DITA-OT 3.6 from GitHub into `build/dita-ot` (`dita.home` system property defaults to that path). Tests that need DITA-OT fail without it.
- Tests run with `examples.dir` pointing at `examples/`.
- On Windows use `.\gradlew.bat` instead of `./gradlew`.
- CI runs `test` with `continue-on-error: true`, so a red test run does not fail the pipeline; check the test report yourself.
- `scripts/version.sh <old> <new>` rewrites the version string in every `build.gradle` file. The version also lives in `build.gradle.kts` (`version = ...`), `README.md`, and the docs, so bump those by hand as well.
- `test-project/` and `examples/*` are standalone Gradle builds that consume the plugin; they are not part of the root build.

## Architecture

The project is a Gradle plugin (`io.github.jyjeanne.dita-ot-gradle`, entry point `DitaOtPlugin`) that wraps the DITA Open Toolkit (DITA-OT) CLI. All code is in `src/main/kotlin/com/github/jyjeanne/`. `docs/ARCHITECTURE.md` has a longer walkthrough; this section covers what requires reading several files together.

**Task model.** `DitaOtPlugin` only registers a `dita` task of type `DitaOtTask`. The other tasks (`DitaOtDownloadTask`, `DitaOtInstallPluginTask`, `DitaOtValidateTask`, `DitaLinkCheckTask`) are public task types users register themselves. Consumers reach them as `com.github.jyjeanne.<TaskName>` (no plugin-provided task names beyond `dita`).

**Execution path for `DitaOtTask`.** Inputs are built from the DSL (`DitaOtTask` setters and `PropertyBuilder`/`GroovyPropertyCapture` for Groovy closures), then `Options` holds the resolved settings. The actual DITA-OT run goes through one of two strategies selected by `antExecutionStrategy` (enum in `Options.kt`):
- `DITA_SCRIPT` (default): `AntExecutor` launches `bin/dita` / `dita.bat` from the DITA-OT home via `ProcessBuilder`, with `DITA_HOME` set. This is the configuration-cache-compatible path.
- `ANT` / legacy: `renderViaIsolatedAntBuilder` uses Gradle's internal `IsolatedAntBuilder`. It is deprecated and not configuration-cache compatible; it logs a warning.
Any change to how DITA-OT is invoked has to be checked against both paths.

**Supporting infrastructure.**
- `Classpath.kt` resolves DITA-OT's classpath and plugin locations; `Platform.kt` holds Windows/Unix differences (script names, path quoting, command-line length).
- `ProgressReporter.kt` parses DITA-OT's `--verbose` log output to detect processing stages and drive the progress bar. It depends on stage-marker strings from DITA-OT output, so changes to those patterns need test updates.
- `Messages.kt` holds user-facing error text, including troubleshooting hints. Errors thrown from tasks should come from here.
- `DitaOtValidateTask` only classifies DITA-OT structured message codes (`[DOTJ012E]`-style) as errors/warnings. Generic `[ERROR]` lines are deliberately ignored to avoid false positives; keep that rule when editing it.
- `DitaLinkCheckTask` is a standalone parser/checker (own file walking and link extraction) that does not invoke DITA-OT. keyref/conkeyref links are skipped by design.

**Version coupling.** Supported DITA-OT versions are auto-detected from `plugin.xml` in the DITA-OT installation; `DitaOtDownloadTask` and the CI matrices pin specific versions (3.4–3.6, 4.x). The `.github/workflows/ci.yml` matrices are the source of truth for which Gradle and DITA-OT versions are exercised.

**Tests.** Kotest specs (`*Spec.kt`) and `*Test.kt` classes live in the same package under `src/test/kotlin`. `DitaOtIntegrationTest` is documentation-only and disabled; real transformations are verified via the `examples/` projects.
