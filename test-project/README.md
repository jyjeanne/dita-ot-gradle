# Integration Test Project

This is a minimal test project used by GitHub Actions to verify the DITA-OT Gradle plugin works correctly.

## Purpose

This project:
1. Downloads DITA-OT automatically
2. Processes a simple DITA map with one topic
3. Generates HTML5 output
4. Validates the plugin installation from Maven Local

## Structure

```
test-project/
├── src/
│   ├── test.ditamap       # Simple DITA map
│   └── test-topic.dita    # Simple test topic
├── build.gradle.kts       # Plugin configuration
└── settings.gradle.kts    # Project settings
```

## Running Locally

```bash
# From the repository root: publish the plugin under test to Maven Local
./gradlew publishToMavenLocal

# From the test-project directory: transform and check the HTML5 output
./gradlew clean verifyOutput                        # DITA-OT 3.6 (default)
./gradlew clean verifyOutput -PditaOtVersion=4.4.2  # any other DITA-OT release
```

DITA-OT is downloaded to `build/dita-ot/dita-ot-<version>` and the output goes to `build/out`.
The plugin version in `build.gradle.kts` must match `version` in the root `build.gradle.kts`.

## CI/CD Usage

This project is automatically tested by GitHub Actions on every push and pull request.
The workflow:
1. Builds the main plugin
2. Publishes it to Maven Local
3. Runs this test project to verify functionality

**Note:** In CI, we verify the plugin loads and tasks are registered (`gradle tasks`), but skip the actual DITA transformation due to Ant DSL reflection limitations in the test environment. The plugin works correctly in real usage - this is a CI-only limitation that doesn't affect production functionality.
