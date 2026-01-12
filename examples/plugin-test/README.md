# DITA-OT Plugin Test Example

This example demonstrates how to:

1. **Download DITA-OT** from GitHub releases (configurable version)
2. **Install plugins** from the DITA-OT Plugin Registry
3. **Run transformations** using installed plugins
4. **Verify output** automatically

## Quick Start

```bash
# Run with defaults (DITA-OT 4.2.3, org.lwdita plugin, markdown output)
./gradlew

# Or with custom configuration
./gradlew -PditaOtVersion=4.1.0 -PpluginId=org.lwdita -Ptranstype=markdown
```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `ditaOtVersion` | `4.2.3` | DITA-OT version to download |
| `pluginId` | `org.lwdita` | Plugin ID from DITA-OT registry |
| `transtype` | `markdown` | Transformation type |

## Available Tasks

### Setup Tasks

| Task | Description |
|------|-------------|
| `downloadDitaOt` | Download DITA-OT from GitHub releases |
| `extractDitaOt` | Extract DITA-OT zip archive |
| `installPlugin` | Install plugin from DITA-OT registry |
| `listPlugins` | List installed plugins |
| `listTranstypes` | List available transtypes |

### Build Tasks

| Task | Description |
|------|-------------|
| `ditaTransform` | Run DITA-OT transformation |
| `verifyOutput` | Verify transformation output |
| `test` | Full test: download, install, transform, verify |
| `cleanAll` | Clean all outputs including downloaded DITA-OT |

## Examples

### Test Different DITA-OT Versions

```bash
./gradlew -PditaOtVersion=4.0.0 test
./gradlew -PditaOtVersion=4.1.0 test
./gradlew -PditaOtVersion=4.2.3 test
```

### Test Different Plugins

```bash
# Lightweight DITA (Markdown output)
./gradlew -PpluginId=org.lwdita -Ptranstype=markdown test

# Standard HTML5
./gradlew clean -Ptranstype=html5 test

# PDF output (requires FOP)
./gradlew clean -Ptranstype=pdf test
```

### List Available Transtypes

```bash
./gradlew listTranstypes
```

### List Installed Plugins

```bash
./gradlew listPlugins
```

## Project Structure

```
plugin-test/
├── build.gradle          # Gradle build configuration
├── settings.gradle       # Project settings
├── README.md             # This file
└── dita/
    ├── sample.ditamap    # Sample DITA map
    └── topics/
        ├── introduction.dita
        ├── getting-started.dita
        ├── installation.dita
        ├── configuration.dita
        ├── features.dita
        └── conclusion.dita
```

## Output

After running the test, output will be in `build/output/`:

```bash
# For markdown transtype
build/output/
├── introduction.md
├── getting-started.md
├── installation.md
├── configuration.md
├── features.md
└── conclusion.md

# For html5 transtype
build/output/
├── index.html
├── introduction.html
├── getting-started.html
└── ...
```

## CI/CD Integration

This project can be used in CI/CD pipelines to:

1. Test plugin compatibility with different DITA-OT versions
2. Validate documentation builds
3. Run automated documentation generation

Example GitHub Actions workflow:

```yaml
name: DITA-OT Plugin Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        dita-version: ['4.0.0', '4.1.0', '4.2.3']
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '11'
          distribution: 'temurin'
      - name: Run Plugin Test
        run: ./gradlew -PditaOtVersion=${{ matrix.dita-version }} test
```

## Troubleshooting

### Download Fails

If DITA-OT download fails, check:
- Internet connection
- DITA-OT version exists on GitHub releases
- Proxy settings (if applicable)

### Plugin Installation Fails

If plugin installation fails:
- Check plugin ID is correct (use `dita plugins` to list available)
- Check plugin compatibility with DITA-OT version
- Check registry at https://www.dita-ot.org/plugins

### Transformation Fails

If transformation fails:
- Check transtype is available (`./gradlew listTranstypes`)
- Check plugin is installed (`./gradlew listPlugins`)
- Run with `--info` for detailed logs: `./gradlew --info ditaTransform`
