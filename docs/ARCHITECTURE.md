# Gate 0 Architecture

## Stack decision

The launcher targets Android 17 (API 37) while keeping Android 11 (API 30) as the minimum and reference-device floor. The build uses Android Gradle Plugin 9.3.0, Gradle 9.5.1, JDK 17, Kotlin 2.4.10 and Compose BOM 2026.06.00.

This is intentionally a native Android/Compose project. Web technology will be contained inside the future browser surface; it will not power the launcher shell itself.

Authoritative compatibility references:

- Android 17/API 37: https://developer.android.com/about/versions/17
- AGP 9.3 compatibility: https://developer.android.com/build/releases/agp-9-3-0-release-notes
- Compose BOM: https://developer.android.com/develop/ui/compose/bom
- Preview screenshot testing: https://developer.android.com/studio/preview/compose-screenshot-testing
- Macrobenchmark: https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview
- Gradle checksums: https://gradle.org/release-checksums/

## Module boundaries

| Module | Responsibility | May depend on |
|---|---|---|
| `:app` | Android entry point, HOME activity and responsive foundation shell | Design system, device profile |
| `:core:designsystem` | Liquid Graphite tokens, theme and adaptive dimensions | Compose only |
| `:core:device` | Deterministic display, RAM, heap and input measurement | Android framework only |
| `:benchmark` | Cold-start and future frame/memory performance tests | Built APK only |

Features added in later gates should be split by capability rather than by UI screen. Browser, terminal, files and workspace state must not become a single app-module dependency knot.

## Runtime contracts

- The shell owns one immutable `DeviceProfile` snapshot for the current configuration.
- Configuration changes produce a new profile; components do not query global display metrics independently.
- `PerformanceTier.LEAN` is the Galaxy A30-class default: no continuous blur, two live WebViews and two simultaneous internal windows.
- All direct-manipulation surfaces retain at least a 48 dp target after font scaling.
- The app does not start a permanent background service in Gate 0.
- The benchmark build is release-like, non-debuggable and locally signed with the debug key only for measurement.

## Validation layers

1. JVM unit tests lock classification and adaptive-size rules.
2. Host-side Compose screenshot tests render portrait and landscape reference profiles.
3. Android lint runs with warnings treated as errors.
4. A signed debug APK is produced on every pull request.
5. The manual Android 11 workflow runs a dry macrobenchmark on a 4 GB emulator.
6. Physical Galaxy A30 measurements use the repeatable checklist in `docs/benchmarks/GALAXY_A30.md`.

The current Work runtime has JDK 17 but no Android SDK or Gradle distribution cache. Local checks therefore validate source structure, XML and repository integrity; GitHub Actions is the authoritative Android compile environment.
