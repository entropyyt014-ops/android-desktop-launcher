# Gate 1 Physical Device Checklist

Reference device: Samsung Galaxy A30, Android 11, 4 GB RAM.

## Install and Home role

- Install the Gate 1 debug APK over the Gate 0 debug build.
- Confirm the first visible screen is the Stage Setup Assistant, not device diagnostics.
- Tap **Choose Stage as Home** and confirm Android shows the official launcher picker.
- Select Stage, press Home twice and confirm one desktop task is restored without duplicate activities.
- Change the default launcher back through Settings and confirm Stage remains launchable as a normal app.

## Shell and installed applications

- Open **Applications** from both the desktop folder and Dock.
- Confirm names and icons correspond to real installed launchable apps.
- Search by app label and package-name fragment.
- Open an app, press Home and confirm Stage returns to the previous surface.
- Long-press an app, pin it to the Dock, relaunch Stage and confirm the pin persists.
- Open Android app information and the system uninstall confirmation from the context menu.

## Interaction and adaptation

- Open Command Center from the Dock and, with a keyboard, with `Ctrl/Meta + Space`.
- Use arrow keys and Enter to open a result.
- Rotate to landscape; drag the active Stage window and use close, minimize and maximize controls.
- Connect a mouse and confirm Dock hover magnification and pointer scrolling.
- Open Control Center and verify Wi-Fi, Bluetooth, Display and Sound route to Android Settings.

## Persistence and performance

- Change Reduce Motion, Desktop Texture and Dock Magnification; force-stop and reopen Stage.
- Confirm the last Stage surface and settings restore.
- Open Settings → Performance and confirm device information is real and no longer shown on Home.
- Observe interaction for five minutes and record any visible jank, icon-loading stalls, crashes or system launcher-picker loops.

Report failures with the exact action, portrait/landscape state, whether Stage was default Home, and a screenshot or screen recording.
