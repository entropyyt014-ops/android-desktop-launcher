# Android Desktop Launcher

Working title for a non-root, phone-first desktop workspace for Android.

The product is not a Windows skin and not a literal macOS clone. It uses familiar desktop interaction ideas—menu strip, dock, command search, windows, keyboard shortcuts and workspaces—but reshapes them for a phone screen. The target is one complete `v1.0` that remains smooth on a Samsung Galaxy A30-class device with Android 11 and 4 GB RAM.

## Product promise

Install one launcher, complete a short adaptive setup, and receive a coherent desktop workspace containing:

- an Android home/launcher shell;
- a desktop-oriented browser with compact, hideable chrome;
- a built-in terminal surface with a safe Android shell and an optional full Termux bridge;
- a file hub, download shelf and code/text editor;
- internal tiling/floating windows and saved workspaces;
- first-class touch, mouse and physical-keyboard interaction;
- a launcher settings and personalization center;
- safe bridges to Android system settings and installed apps.

## Non-negotiable constraints

- No root, custom ROM, OEM privileges, ADB dependency, accessibility-service takeover or risky phone debugging.
- No claim that a normal launcher can replace Android System UI or globally redesign other apps.
- No arbitrary third-party Android apps embedded inside our own windows.
- No visual effect may compromise responsiveness on the 4 GB reference device.
- No fake desktop controls: unavailable system actions open the correct Android panel and say what Android controls.

## Product documents

- [Product blueprint](docs/PRODUCT_BLUEPRINT.md)
- [Design system](docs/DESIGN_SYSTEM.md)
- [Platform truth](docs/PLATFORM_TRUTH.md)
- [Single-release build plan](docs/V1_BUILD_PLAN.md)

Implementation begins after the planning contract is approved.
