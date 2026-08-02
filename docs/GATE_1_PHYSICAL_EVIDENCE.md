# Gate 1 Galaxy A30 visual evidence

Date: 2026-08-02

Reference device: Samsung Galaxy A30-class phone, Android 11, 4 GB RAM.

## Confirmed from physical screenshots

- Stage renders edge-to-edge in portrait and landscape.
- The Liquid Graphite wallpaper, menu bar, status indicators and adaptive Dock render correctly.
- The Applications surface reads real installed app names and icons; the captured device reported 55 launchable apps.
- The portrait app grid, landscape compact app grid, Command Center and Settings window all render without clipping.
- Landscape keeps the left Dock below the menu bar and outside the active window.
- Window controls, active Dock indicators and orientation-specific desktop shortcut placement render correctly.

## Visual repairs carried into Gate 2

- Increase active-window opacity so desktop shortcuts do not ghost through portrait surfaces.
- Replace opaque Android-style desktop label pills with shadowed desktop labels.
- Hide Command Center keyboard hints when no physical keyboard is detected.
- Add the built-in Browse tool to the Dock and Command Center.

## Behavioral evidence still required

The screenshots show the **Make Stage Home** shortcut, so they do not prove that Stage held the Android Home role during capture. Before Gate 1 is formally closed, verify:

- choose Stage in Android's official Home-app picker;
- press Home twice and confirm there is no duplicate Stage task;
- launch an Android app and return Home to the same Stage surface;
- pin an app and confirm that it remains in the Dock after force-stop/relaunch;
- observe five minutes of portrait/landscape use for crashes or major jank.

The remaining checks are behavioral and cannot be inferred from still images.
