# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Gradle wrapper; JDK 17 for building (bytecode targets Java 8). Default task is `check`.

```bash
./gradlew clean assemble            # Build plugin jar (build/libs)
./gradlew test                      # Run unit tests (Kotest on JUnit Platform)
./gradlew test -PditaOtVersion=4.4.2   # Run unit tests against another DITA-OT release (default 3.6)
./gradlew test --tests "com.github.jyjeanne.DitaOtTaskSpec"        # One test class
./gradlew test --tests "com.github.jyjeanne.DitaOtTaskSpec.*some name*"  # Filter by spec/test name
./gradlew publishToMavenLocal       # Install plugin locally (used by CI integration jobs)
./gradlew dependencyUpdates         # Version check (ben-manes plugin)
```

- `test` depends on `installDitaOt`, which downloads DITA-OT 3.6 from GitHub into `build/dita-ot` (`dita.home` system property defaults to that path). Tests that need DITA-OT fail without it.
- Tests run with `examples.dir` pointing at `examples/`.
- On Windows use `.\gradlew.bat` instead of `./gradlew`.
- `scripts/version.sh <old> <new>` rewrites the version string in every `build.gradle` file. The version also lives in `build.gradle.kts` (`version = ...`), `test-project/build.gradle.kts` (plugin version under integration test, resolved from mavenLocal), `README.md`, and the docs, so bump those by hand as well.
- None of `test-project/` or `examples/` is part of the root build. `test-project/` is a standalone build that resolves the plugin from mavenLocal (`./gradlew publishToMavenLocal` first); CI runs its `dita` task per DITA-OT version with `-PditaOtVersion=`.
- `examples/` is itself a composite build: `examples/settings.gradle` does `includeBuild '../'` (plugin source) and includes `classpath`, `download`, `filetree`, `multi-task`, `multi-project`, `simple` as subprojects (`simple` and `multi-project` also have their own settings/wrapper and can run standalone). `gradle-plugin-self-documentation` also builds against the plugin source (`pluginManagement { includeBuild("../..") }`). The remaining examples (`ci-cd-publishing`, `configuration-cache`, `custom-plugin-dev`, `multi-language`, `plugin-test`, `version-docs`) are standalone builds that pull the released plugin from the Plugin Portal.

## Architecture

The project is a Gradle plugin (`io.github.jyjeanne.dita-ot-gradle`, entry point `DitaOtPlugin`) that wraps the DITA Open Toolkit (DITA-OT) CLI. All code is in `src/main/kotlin/com/github/jyjeanne/`. `docs/ARCHITECTURE.md` has a longer walkthrough; this section covers what requires reading several files together.

**Task model.** `DitaOtPlugin` only registers a `dita` task of type `DitaOtTask`. The other tasks (`DitaOtDownloadTask`, `DitaOtInstallPluginTask`, `DitaOtValidateTask`, `DitaLinkCheckTask`) are public task types users register themselves. Consumers reach them as `com.github.jyjeanne.<TaskName>` (no plugin-provided task names beyond `dita`).

**Execution path for `DitaOtTask`.** Inputs are built from the DSL (`DitaOtTask` setters and `PropertyBuilder`/`GroovyPropertyCapture` for Groovy closures), then `Options` holds the resolved settings. `antExecutionStrategy` (enum `Options.AntExecutionStrategy`: `ISOLATED_BUILDER`, `DITA_SCRIPT`, `CUSTOM_CLASSLOADER`, `GRADLE_EXEC`, `GROOVY_ANT_BINDING`) selects how DITA-OT runs, but `DitaOtTask` only has two code paths:
- `DITA_SCRIPT` (the task's default): `AntExecutor` launches `bin/dita` / `dita.bat` from the DITA-OT home via `ProcessBuilder`, with `DITA_HOME` set. This is the configuration-cache-compatible path.
- Every other value falls through to `renderViaIsolatedAntBuilder`, which uses Gradle's internal `IsolatedAntBuilder`. It is deprecated, not configuration-cache compatible, and logs a warning. The other enum values have no dedicated implementation (the enum's KDoc calling `ISOLATED_BUILDER` the default is stale).
Any change to how DITA-OT is invoked has to be checked against both paths.

**Supporting infrastructure.**
- `Classpath.kt` resolves DITA-OT's classpath and plugin locations; `Platform.kt` holds OS detection and the `bin/dita` / `bin/dita.bat` paths.
- `WindowsCommandLine.kt`: on Windows, `dita.bat` + `config\env.bat` put one absolute path per plugin JAR on the CLASSPATH, which can exceed cmd.exe's 8191-char limit (exit code 255). `resolveDitaScript` estimates the length and, only when it would overflow, swaps in the script's 8.3 short path. `AntExecutor` (transform) and `DitaOtValidateTask` call it; `DitaOtInstallPluginTask` doesn't need it (`dita install` skips `env.bat`).
- `ProgressReporter.kt` parses DITA-OT's `--verbose` log output to detect processing stages and drive the progress bar. It depends on stage-marker strings from DITA-OT output, so changes to those patterns need test updates.
- `Messages.kt` holds user-facing error text, including troubleshooting hints. Errors thrown from tasks should come from here.
- `DitaOtValidateTask` only classifies DITA-OT structured message codes (`[DOTJ012E]`-style) as errors/warnings. Generic `[ERROR]` lines are deliberately ignored to avoid false positives; keep that rule when editing it.
- `DitaLinkCheckTask` is a standalone parser/checker (own file walking and link extraction) that does not invoke DITA-OT. keyref/conkeyref links are skipped by design.

**Version coupling.** Supported DITA-OT versions are auto-detected from `plugin.xml` in the DITA-OT installation; `DitaOtDownloadTask` and the CI matrices pin specific versions (3.4–3.6, 4.x). The `.github/workflows/ci.yml` matrices are the source of truth for which Gradle and DITA-OT versions are exercised.

**Tests.** Kotest specs (`*Spec.kt`) and `*Test.kt` classes live in the same package under `src/test/kotlin`. `DitaOtIntegrationTest` is documentation-only and disabled. Real transformations are verified by `test-project` (`./gradlew verifyOutput -PditaOtVersion=<v>` after `publishToMavenLocal`; CI runs it per DITA-OT version) and the `examples/` projects. To test `test-project` against unpublished plugin changes without overwriting the released version in Maven Local, add `--include-build ..`. `test-project` runs both `validateDita` and `dita`; with `--info`, a log line shows when the Windows short-path fallback kicks in.
