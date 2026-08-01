# Platform Truth

This file prevents the product from drifting into claims that a non-root Android launcher cannot deliver.

| Goal | Safe implementation | Boundary |
|---|---|---|
| Become the phone's launcher | Request Android's Home role | Does not make the app System UI |
| Desktop windows | Tile/float our built-in surfaces | Arbitrary third-party apps cannot be embedded or resized by an ordinary launcher |
| macOS-like menu/status area | Draw our own strip inside the launcher and use edge-to-edge/immersive behavior carefully | Android status/navigation UI returns in other apps |
| Settings center | Own product settings plus Android Settings panels and deep links | Most protected global settings cannot be silently changed |
| Desktop browser | Native browser UI around Android System WebView, desktop UA, wide viewport, zoom and compact chrome | A site can still detect Android or depend on unsupported desktop APIs |
| Full terminal | Built-in Android shell plus optional Termux Connect | A current-target app cannot execute arbitrary downloaded binaries from writable app storage |
| File manager | Own workspace, MediaStore and user-granted directories through SAF | Other apps' private data and `Android/data`/`Android/obb` remain restricted on Android 11+ |
| Mouse and keyboard | Full shortcuts, hover, context menus and pointer behavior inside our shell | Global remapping inside other apps is outside launcher authority |
| Notifications | Optional Notification Access | Permission is sensitive and cannot be assumed |
| Recent/running apps | Package launcher data and optional Usage Access | Android does not expose unrestricted task control to a normal launcher |

## Terminal decision

The main APK will target current Android security requirements. Android blocks apps targeting API 29+ from directly executing files placed in their writable home directory because that violates write-xor-execute policy. Therefore we will not recreate Termux by lowering the target SDK, bypassing security policy or silently downloading executable packages.

The compliant design is:

1. A built-in PTY terminal backed by Android's available shell and packaged, reviewed helpers.
2. A Termux Connect path using explicit user permission for advanced packages and the user's existing environment.
3. Clear visual separation between the two execution contexts.

## Permission policy

The launcher must not require:

- root;
- ADB or wireless debugging;
- Accessibility Service control;
- Device Administrator;
- `MANAGE_EXTERNAL_STORAGE` for the base experience;
- overlay permission for its normal desktop shell.

Optional permissions are requested at the moment their feature is enabled and can be revoked without breaking Home, browser, files or the built-in shell.

## Primary references

- [Android Home role API](https://developer.android.com/reference/android/app/role/RoleManager)
- [Android multi-window and same-task activity embedding](https://developer.android.com/develop/ui/views/layout/support-multi-window-mode)
- [Android immersive mode](https://developer.android.com/develop/ui/views/layout/immersive)
- [Android Settings panels](https://developer.android.com/reference/android/provider/Settings.Panel)
- [Android Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files)
- [Android 11 storage restrictions](https://developer.android.com/about/versions/11/privacy/storage)
- [Android 10 executable-file restriction](https://developer.android.com/about/versions/10/behavior-changes-10)
- [Android keyboard and pointer compatibility](https://developer.android.com/develop/ui/compose/touch-input/input-compatibility-on-large-screens)
- [Termux RUN_COMMAND integration](https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent)
