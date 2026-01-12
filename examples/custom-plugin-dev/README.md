# Custom DITA-OT Plugin Development Example

This example demonstrates how to develop and test a custom DITA-OT plugin using the DITA-OT Gradle Plugin.

## Quick Start

```bash
# Run development build (download DITA-OT, install plugin, transform)
./gradlew dev

# Auto-rebuild when files change
./gradlew dev --continuous
```

## Project Structure

```
custom-plugin-dev/
├── build.gradle                    # Gradle build configuration
├── README.md                       # This file
├── src/
│   └── my-custom-plugin/           # Your plugin source
│       ├── plugin.xml              # Plugin descriptor
│       ├── css/
│       │   └── custom-style.css    # Custom styles
│       └── xsl/
│           └── custom-html5.xsl    # XSLT overrides
└── test-content/                   # Test DITA content
    ├── test.ditamap
    └── topics/
        ├── overview.dita
        ├── features.dita
        └── usage.dita
```

## Development Workflow

### 1. Initial Setup

```bash
# Download and extract DITA-OT (only needed once)
./gradlew extractDitaOt
```

### 2. Development Cycle

```bash
# Install plugin and run test transformation
./gradlew dev

# Or with continuous build (auto-rebuilds on file changes)
./gradlew dev --continuous
```

### 3. Debugging

```bash
# View detailed installation logs
./gradlew installMyPlugin --info

# List installed plugins
./gradlew listPlugins
```

## Available Tasks

| Task | Description |
|------|-------------|
| `downloadDitaOt` | Download DITA-OT from GitHub releases |
| `extractDitaOt` | Extract DITA-OT zip archive |
| `installMyPlugin` | Install your plugin into DITA-OT |
| `testMyPlugin` | Run test transformation |
| `dev` | Full development cycle |
| `listPlugins` | List installed plugins |
| `cleanAll` | Clean all build outputs |

## Customizing for Your Plugin

### 1. Update Plugin ID

Edit `src/my-custom-plugin/plugin.xml`:

```xml
<plugin id="com.yourcompany.your-plugin">
  ...
</plugin>
```

### 2. Update Transtype Name

```xml
<transtype name="your-transtype" extends="html5" desc="Your description">
  ...
</transtype>
```

### 3. Update build.gradle

```groovy
task testMyPlugin(type: com.github.jyjeanne.DitaOtTask, dependsOn: installMyPlugin) {
    transtype 'your-transtype'  // Match your plugin's transtype
    ...
}
```

## Plugin Development Tips

1. **Use `--continuous` mode** for rapid iteration
2. **Check logs with `--info`** when debugging installation issues
3. **Test with different DITA-OT versions**: `./gradlew dev -PditaOtVersion=4.1.0`
4. **Clean rebuild** if plugin changes don't take effect: `./gradlew cleanAll dev`

## Resources

- [DITA-OT Plugin Development Guide](https://www.dita-ot.org/dev/topics/plugin-development.html)
- [DITA-OT Gradle Plugin Documentation](../../README.md)
- [DITA-OT Plugin Registry](https://www.dita-ot.org/plugins)
