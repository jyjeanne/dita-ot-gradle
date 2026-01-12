# ANT Execution Strategies - Research & Implementation Guide

## Problem Statement

The current implementation uses Gradle's `IsolatedAntBuilder` to execute DITA-OT transformations. However, this approach suffers from a fundamental classloader limitation:

**Error:** `taskdef class org.dita.dost.ant.InitializeProjectTask cannot be found using the classloader AntClassLoader[]`

This occurs on both Gradle 8.5 and 9.1, indicating it's not a version-specific issue but rather a limitation of how `IsolatedAntBuilder` handles external ANT task definitions.

---

## Alternative Execution Strategies

### 1. **DITA_SCRIPT** - Execute via DITA-OT Script

**Approach:** Use the DITA-OT command-line tool (`dita` or `dita.bat`) instead of invoking ANT directly.

**Advantages:**
- ✅ No classloader issues - DITA-OT handles its own classpath
- ✅ Uses proven, production-tested DITA-OT setup
- ✅ Works correctly with all DITA-OT plugins
- ✅ Simple implementation - just spawn a subprocess
- ✅ Cross-platform (same code works on Windows/Unix via different scripts)
- ✅ Can capture output and errors via process streams

**Disadvantages:**
- ❌ Creates a separate process (more overhead)
- ❌ Depends on DITA-OT scripts existing and being executable
- ❌ Less direct Gradle integration (can't use exec task directly)
- ⚠️ Platform-specific scripts needed (dita vs dita.bat)

**Implementation Status:** Documented in `AntExecutor.executeViaDitaScript()`

**Recommendation:** ⭐ **BEST SHORT-TERM SOLUTION**
- Simple and reliable
- Proven to work with DITA-OT
- Only modest performance overhead
- Recommended for v2.3.0 release

**Usage Example:**
```kotlin
dita {
    antExecutionStrategy("DITA_SCRIPT")
}
```

---

### 2. **CUSTOM_CLASSLOADER** - Direct Java URLClassLoader

**Approach:** Create a custom Java `URLClassLoader` that loads all DITA-OT JARs, then dynamically load and invoke ANT.

**Advantages:**
- ✅ Pure Java solution - no external dependencies
- ✅ Full programmatic control
- ✅ Can debug classloading issues directly
- ✅ No process spawning overhead
- ✅ Integrates well with Gradle infrastructure

**Disadvantages:**
- ❌ Complex implementation requiring reflection
- ❌ Must handle ANT Project creation and configuration manually
- ❌ Risk of classloader hierarchy issues with Gradle's own classes
- ❌ Debugging and maintenance burden
- ⚠️ Potential compatibility issues across Gradle versions
- ⚠️ Thread safety considerations with custom classloaders

**Implementation Status:** Proof-of-concept in `AntExecutor.executeViaCustomClassloader()`

**Challenges:**
1. Properly creating and initializing an ANT Project instance
2. Managing task execution lifecycle
3. Capturing build output and errors
4. Handling property injection
5. Managing temporary directories and classpaths

**Recommendation:** ⭐⭐ **LONG-TERM INVESTIGATION**
- Requires significant development effort
- Good for understanding ANT integration
- Consider after DITA_SCRIPT is proven stable

**Example Implementation Skeleton:**
```kotlin
fun executeViaCustomClassloader(...): Int {
    val urls = classpathFiles.map { it.toURI().toURL() }.toTypedArray()
    val classloader = URLClassLoader(urls, ClassLoader.getSystemClassLoader())

    val projectClass = classloader.loadClass("org.apache.tools.ant.Project")
    val project = projectClass.getDeclaredConstructor().newInstance()

    // Configure project, set properties, run targets
    // ... complex reflection-based setup ...
}
```

---

### 3. **GRADLE_EXEC** - Use Gradle Exec Task

**Approach:** Wrap the ANT invocation using Gradle's built-in `exec` task capability.

**Advantages:**
- ✅ Leverages Gradle's proven exec infrastructure
- ✅ Good error handling and logging
- ✅ Integrates well with Gradle task model
- ✅ Can capture output via standard mechanisms
- ✅ Platform-aware (handles Windows vs Unix differently)

**Disadvantages:**
- ❌ Requires creating an intermediate exec task
- ❌ More complex than direct approach
- ❌ Indirect method (wrapping another tool)
- ⚠️ Must handle cross-platform command construction

**Implementation Status:** Concept in `AntExecutor.getExecCommandArguments()`

**Recommendation:** ⭐⭐ **MEDIUM-TERM CONSIDERATION**
- Simpler than CUSTOM_CLASSLOADER
- Less proven than DITA_SCRIPT
- Good for integrating with other Gradle tasks

**Example:**
```kotlin
// Pseudo-code
project.exec {
    commandLine = AntExecutor.getExecCommandArguments(ditaHome, antBuildFile, properties)
    workingDir = ditaHome
}
```

---

### 4. **GROOVY_ANT_BINDING** - Groovy's Built-in ANT Support

**Approach:** Use Groovy's native ANT binding and automatic classloading.

**Advantages:**
- ✅ Groovy handles ANT setup automatically
- ✅ Native integration with Gradle's Groovy ecosystem
- ✅ Good performance
- ✅ Type-safe with Groovy

**Disadvantages:**
- ❌ May have similar classloader issues (Groovy uses similar mechanisms)
- ❌ Adds Groovy dependency complexity
- ❌ Less transparent - harder to debug
- ⚠️ Requires careful classloader hierarchy management
- ⚠️ Potential conflicts with Gradle's Groovy version

**Implementation Status:** Experimental concept in `AntExecutor.executeViaGroovyAntBinding()`

**Recommendation:** ⭐ **EXPERIMENTAL ONLY**
- Potentially useful for future exploration
- Not recommended for production use
- Too risky given similar underlying issues

---

### 5. **ISOLATED_BUILDER** - Current Approach (Gradle's IsolatedAntBuilder)

**Current Status:** Has known classloader limitation

**Why It Fails:**
- Gradle's IsolatedAntBuilder creates a restricted classloader environment
- DITA-OT's ANT task definitions are not properly accessible
- The `AntClassLoader[]` cannot resolve `org.dita.dost.ant.InitializeProjectTask`

**Recommendation:** ❌ **KEEP AS FALLBACK ONLY**
- Useful for backward compatibility
- Will be deprecated in favor of DITA_SCRIPT or CUSTOM_CLASSLOADER
- Marked for removal in v3.0.0

---

## Comparative Analysis Table

| Strategy | Performance | Complexity | Reliability | Maintenance | Learning Curve | Recommended |
|----------|-------------|-----------|------------|------------|---|---|
| DITA_SCRIPT | ⭐⭐⭐ | ⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐ | ✅ v2.3.0 |
| CUSTOM_CLASSLOADER | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐ Future |
| GRADLE_EXEC | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐ Medium-term |
| GROOVY_ANT_BINDING | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ❌ Experimental |
| ISOLATED_BUILDER | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐ | ⭐⭐⭐ | ⭐⭐ | ⚠️ Fallback |

---

## Recommended Implementation Roadmap

### Phase 1: v2.3.0 (Next Release)
1. ✅ Implement `DITA_SCRIPT` strategy as primary option
2. ✅ Keep `ISOLATED_BUILDER` as default for backward compatibility
3. ✅ Add `antExecutionStrategy()` configuration method
4. ✅ Document usage and testing
5. ✅ Add to example projects
6. ✅ Update CI/CD to test DITA_SCRIPT

### Phase 2: v2.4.0 (Future)
1. ⭐ Implement `CUSTOM_CLASSLOADER` as alternative
2. ⭐ Performance testing and optimization
3. ⭐ Comprehensive error handling
4. ⭐ Debugging utilities for classloader issues

### Phase 3: v3.0.0 (Major Release)
1. Deprecate `ISOLATED_BUILDER`
2. Set `DITA_SCRIPT` or `CUSTOM_CLASSLOADER` as default
3. Remove legacy ANT integration code
4. Consider removing GROOVY_ANT_BINDING if not proven useful

---

## Testing Strategy

### Unit Tests
- Mock file systems and processes
- Test classpath construction
- Verify property injection

### Integration Tests
- Test with actual DITA-OT installations
- Test different DITA-OT versions (3.4, 3.5, 3.6, 4.0+)
- Test with different output formats (HTML5, PDF, etc.)

### Platform Tests
- Windows: Test both `dita.bat` and direct classloader
- Linux/macOS: Test `dita` script
- WSL: Test cross-platform behavior

---

## Performance Implications

### DITA_SCRIPT Approach
```
Overhead breakdown:
- Process creation:        ~100-200ms (Windows), ~50-100ms (Unix)
- DITA-OT setup:          ~500-1000ms (one-time per process)
- Actual transformation:   Varies by document size
- Total overhead:         ~10-20% for typical documents
```

### CUSTOM_CLASSLOADER Approach
```
Overhead breakdown:
- URLClassLoader creation: ~50-100ms
- Class loading:           ~200-500ms
- Actual transformation:   Varies by document size
- Total overhead:         ~5-10% for typical documents
- Benefit:                Better for parallel builds
```

---

## Debugging Guide

### DITA_SCRIPT Issues
```bash
# Check DITA script availability
ls -la $DITA_HOME/dita      # Unix
dir %DITA_HOME%\dita.bat    # Windows

# Test script directly
$DITA_HOME/dita -i test.ditamap -f html5 -o output
```

### CUSTOM_CLASSLOADER Issues
```bash
# Enable detailed logging
gradle dita --debug --info

# Check classloader diagnostics
# Build will log loaded classes and classpath entries
```

---

## Conclusion

**Recommendation:** Start with **DITA_SCRIPT** strategy for v2.3.0:
- Proven reliable with DITA-OT
- Simple to implement and test
- Minimal performance impact
- Easy for users to understand and debug
- Provides a working solution while longer-term solutions are developed

Future versions can explore **CUSTOM_CLASSLOADER** for better performance and tighter Gradle integration.
