# Bug Fix: DitaOtInstallPluginTask incompatible with DITA-OT 4.4 CLI

## Problem

`DitaOtInstallPluginTask` fails when used with DITA-OT 4.4 because it places the `--force` flag **before** the positional `<file>` argument. DITA-OT 4.4's picocli-based parser requires `--force` **after** the positional argument.

### Current behavior (broken)

The task builds the command in this order (from `installPlugin()` method, lines ~230-240):

```kotlin
command.add(ditaExecutable.absolutePath)
command.add("install")

if (force.get()) {
    command.add("--force")   // ← --force BEFORE plugin argument
}

command.add(plugin)          // ← positional argument last
```

This produces:

```
dita install --force C:\path\to\plugin.zip
```

### Expected behavior

DITA-OT 4.4 CLI requires:

```
dita install C:\path\to\plugin.zip --force
```

Source: [DITA-OT 4.4 documentation](https://www.dita-ot.org/4.4/parameters/dita-command-arguments.html):
> "The --force option can be passed as an additional option to the installation subcommand to force-install an existing plug-in: `dita install plug-in-zip --force`."

### Error observed

```
Unsupported option C:\Tmp\dita2epub\build\plugin\io.github.jyjeanne.dita2epub.zip
Usage:
  dita install [<id> | <url> | <file>]
```

When `--force` precedes the positional argument, picocli treats the file path as an unsupported option rather than the required positional parameter.

### Secondary issue: troubleshooting message uses deprecated syntax

In the error handler (~line 185), the troubleshooting hint uses the old `--install` flag syntax:

```kotlin
"5. Try running: ${ditaExecutable.absolutePath} --install $plugin"
```

This should use the subcommand syntax: `dita install <plugin>`.

## File to modify

`src/main/kotlin/com/github/jyjeanne/DitaOtInstallPluginTask.kt`

## Fix

### 1. Move `--force` after the positional argument

In the `installPlugin()` method, change the command build order:

```kotlin
// Before (broken):
command.add(ditaExecutable.absolutePath)
command.add("install")
if (force.get()) {
    command.add("--force")
}
command.add(plugin)

// After (fixed):
command.add(ditaExecutable.absolutePath)
command.add("install")
command.add(plugin)
if (force.get()) {
    command.add("--force")
}
```

### 2. Fix troubleshooting message

In the `InstallResult.Failed` error handler, update the hint:

```kotlin
// Before:
"5. Try running: ${ditaExecutable.absolutePath} --install $plugin"

// After:
"5. Try running: ${ditaExecutable.absolutePath} install $plugin"
```

## Tests to add

Add these tests to `src/test/kotlin/com/github/jyjeanne/DitaOtInstallPluginTaskTest.kt`:

### 1. Verify command argument order with --force

Test that the positional argument comes before `--force` in the generated command. Since `installPlugin()` is private, either:

- Make the command-building logic extractable/testable, or
- Add an integration test that invokes install with `force = true` on a real DITA-OT 4.4 and verifies success.

Recommended approach — extract a `buildInstallCommand()` method:

```kotlin
// In DitaOtInstallPluginTask.kt, extract from installPlugin():
internal fun buildInstallCommand(plugin: String): List<String> {
    val command = mutableListOf<String>()

    if (Platform.isWindows) {
        command.add("cmd")
        command.add("/c")
    }

    command.add(ditaExecutable.absolutePath)
    command.add("install")
    command.add(plugin)

    if (force.get()) {
        command.add("--force")
    }

    return command
}
```

Then test:

```kotlin
"Command places --force after plugin argument" {
    val task = project.tasks.register("installPlugins", DitaOtInstallPluginTask::class.java) {
        it.ditaOtDir.set(File("/path/to/dita-ot"))
        it.force.set(true)
    }

    val command = task.get().buildInstallCommand("org.dita.pdf2")

    val installIndex = command.indexOf("install")
    val pluginIndex = command.indexOf("org.dita.pdf2")
    val forceIndex = command.indexOf("--force")

    installIndex shouldBeLessThan pluginIndex
    pluginIndex shouldBeLessThan forceIndex
}

"Command omits --force when not set" {
    val task = project.tasks.register("installPlugins", DitaOtInstallPluginTask::class.java) {
        it.ditaOtDir.set(File("/path/to/dita-ot"))
        it.force.set(false)
    }

    val command = task.get().buildInstallCommand("org.dita.pdf2")

    command shouldNot contain("--force")
}
```

### 2. Verify with local file path (Windows-style)

```kotlin
"Command handles Windows file path with --force" {
    val task = project.tasks.register("installPlugins", DitaOtInstallPluginTask::class.java) {
        it.ditaOtDir.set(File("/path/to/dita-ot"))
        it.force.set(true)
    }

    val command = task.get().buildInstallCommand("C:\\build\\plugin\\my-plugin.zip")

    val pluginIndex = command.indexOf("C:\\build\\plugin\\my-plugin.zip")
    val forceIndex = command.indexOf("--force")

    pluginIndex shouldBeLessThan forceIndex
}
```

## Verification

After applying the fix, run from the `dita2epub` consumer project:

```bash
./gradlew clean buildSampleEpub --no-daemon
```

This exercises the full chain: `downloadDitaOt` (4.4) → `packagePlugin` → `installLocalPlugin` (with `force = true`) → `dita` → `validateEpub`.

Expected result: `installLocalPlugin` succeeds with output:

```
✓ Successfully installed: io.github.jyjeanne.dita2epub
```

## Compatibility

### Why the bug was hidden on DITA-OT 4.3.5 and earlier

DITA-OT versions 3.5 through 4.3.x used a lenient picocli configuration that accepted options in any position relative to the positional argument. Both orderings worked in practice:

```
dita install --force plugin.zip   ← worked (lenient parsing)
dita install plugin.zip --force   ← worked (documented syntax)
```

DITA-OT 4.4 tightened the picocli argument parsing. The `--force` flag now acts as a **terminal option** — picocli stops interpreting options after consuming `--force` and treats the next token as a positional argument. When `--force` comes first, picocli consumes it and then fails to recognize the file path as the expected positional `<file>` parameter, reporting "Unsupported option".

### The fix is safe across all versions

The documented CLI syntax has been `dita install <file> --force` (positional argument first) since DITA-OT 3.5, across all versions:

| Version | Documented syntax | `<file> --force` | `--force <file>` |
|---------|-------------------|:-----------------:|:----------------:|
| 3.5     | `dita install plug-in-zip --force` | works | works (lenient) |
| 4.2     | `dita install plug-in-zip --force` | works | works (lenient) |
| 4.3.5   | `dita install plug-in-zip --force` | works | works (lenient) |
| **4.4** | `dita install plug-in-zip --force` | **works** | **fails** |

The fix aligns with the documented syntax and is therefore backward-compatible with all DITA-OT versions from 3.5 onward. No conditional logic per DITA-OT version is needed.

### Verified

Tested on DITA-OT 4.4 (Windows 11):

```
> dita install build/plugin/io.github.jyjeanne.dita2epub.zip --force
Added io.github.jyjeanne.dita2epub                                    ← SUCCESS

> dita install --force build/plugin/io.github.jyjeanne.dita2epub.zip
Unsupported option build/plugin/io.github.jyjeanne.dita2epub.zip      ← FAILS
```
