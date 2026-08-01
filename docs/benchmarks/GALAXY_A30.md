# Galaxy A30 Reference Benchmark

This checklist defines repeatable evidence for the 4 GB / Android 11 performance floor. ADB is a development-only measurement tool; the released launcher must never require ADB, root or developer options.

## Fixed test conditions

- Physical Samsung Galaxy A30 with Android 11 and at least 2 GB free storage.
- Launcher debug or benchmark APK built from the commit under test.
- Battery above 40%, battery saver off and device temperature returned to normal.
- Display at 60 Hz, native resolution and the same brightness for every comparison.
- Airplane mode on unless the case explicitly measures networking.
- Reboot before the cold-start set; wait two minutes after boot.
- Record commit SHA, APK size, Android build number and test timestamp.

## Gate 0 measurements

| Signal | How to collect | Provisional guardrail |
|---|---|---|
| Cold startup | Macrobenchmark, 10 iterations | Median at or below 900 ms |
| Time to initial display | `StartupTimingMetric` | No regression above 10% from accepted baseline |
| Shell memory | Android Studio profiler or `dumpsys meminfo` after 60 seconds idle | PSS at or below 140 MB |
| Direct manipulation | Perfetto/frame timeline during orientation and dock interaction | No recurring visible jank |
| Idle behavior | Five minutes on the shell with screen on | No sustained background CPU work |
| Process recovery | Force-stop, relaunch and rotate twice | No crash or corrupted profile |

The numeric guardrails remain provisional until the first physical-device run. Do not claim them as achieved until the raw benchmark output is attached to the pull request.

## Run sequence

1. Install the benchmark APK and open it once.
2. Confirm the device-profile card reports compact phone posture and the lean tier.
3. Capture ten cold starts and save the Macrobenchmark JSON plus trace files.
4. Leave the shell untouched for 60 seconds, then capture total PSS and heap.
5. Rotate portrait to landscape and back five times while recording a system trace.
6. Connect mouse and keyboard, relaunch, and confirm both inputs are detected.
7. Disconnect peripherals, force-stop, relaunch and repeat the profile check.
8. Attach outputs to the pull request under a heading named Physical API 30 evidence.

## Result record

| Field | Value |
|---|---|
| Commit | Pending |
| APK SHA-256 | Pending |
| Cold-start median | Pending |
| Initial-display median | Pending |
| Idle PSS | Pending |
| Frame finding | Pending |
| Pass/fail | Pending |
