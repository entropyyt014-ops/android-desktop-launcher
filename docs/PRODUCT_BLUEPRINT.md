# Product Blueprint

## 1. Product thesis

Android Desktop Launcher is a compact work operating environment that sits on top of ordinary, non-rooted Android. Its advantage is not merely looking like a desktop. Its browser, terminal, files, editor, launcher, input system and workspaces share one visual language and one command layer.

The primary reference device is a Samsung Galaxy A30-class phone running Android 11 with 4 GB RAM. If an interaction is not comfortable and reliable there, it is not accepted as a core interaction.

## 2. Experience model

The shell adapts to posture instead of forcing the same desktop layout everywhere.

| Posture | Default interaction |
|---|---|
| Portrait + touch | One active Stage, compact top strip, bottom dock, edge shelf, fast workspace overview |
| Landscape + touch | One large surface or two-tile split, persistent dock, optional compact sidebar |
| Mouse/keyboard connected | Hover states, right-click menus, resizable built-in windows, desktop shortcuts |
| Large/external display | Denser menu strip, larger workspace, up to three live built-in surfaces when memory permits |

Only our built-in surfaces—browser, terminal, files, editor, settings and utilities—are managed as real internal windows. Installed Android apps launch normally through Android.

## 3. First-run adaptive setup

The first launch performs a transparent, reversible setup:

1. Explain the safe platform boundary in one screen.
2. Request the Android Home role.
3. Measure screen size, density, refresh rate, memory class, low-RAM signal, CPU count and available storage.
4. Choose conservative visual and memory defaults.
5. Calibrate comfortable text, icon, dock and cursor sizes with a live preview.
6. Detect physical input devices and enable the relevant shortcut hints.
7. Offer user-selected storage folders through Android's document picker.
8. Detect Termux and offer a guided bridge; the launcher remains usable without it.
9. Import selected browser bookmarks or create web-app shortcuts when supported.
10. Save a recovery-safe configuration that can be reset without reinstalling.

This is one app and one release. Creator, Developer and Focus are workspace presets, not separate editions.

## 4. Core systems

### Desktop shell

- Android default-home role
- app library, folders, pinned items and Android widgets
- menu strip with time, battery, connection state and launcher actions
- dock with running-state indicators for built-in surfaces
- command center for apps, settings, files, tabs, bookmarks and actions
- Stage overview and saved workspaces
- wallpaper, icon-pack and layout personalization
- session restoration after process death or reboot

### Browser — the primary work surface

- native Android browser chrome around the system Chromium WebView engine
- compact horizontal tabs plus a vertical tab overview
- auto-hiding address bar and full-content focus mode
- per-site Mobile, Adaptive and Desktop viewport profiles
- desktop user-agent and wide-viewport controls, with honest compatibility fallback
- page zoom, text scale and fit-to-width presets
- mouse hover, right-click menus and desktop keyboard shortcuts
- bookmarks, history, local session restore, private tabs and site permissions
- file upload, share, print/PDF handoff and pop-up/new-window handling
- persistent download queue integrated with the file shelf
- installable web-app shortcuts that open in minimal-chrome internal windows
- renderer-crash recovery and aggressive suspension of background tabs

The browser does not promise every site will treat Android as a desktop. User-agent switching is only one signal; incompatible sites receive a visible fallback rather than hidden breakage.

### Terminal

- an embedded terminal renderer and PTY-backed Android shell for safe built-in commands
- app-workspace file operations and purpose-built helpers for HTTP, Git, SSH and project tasks where maintainable
- optional Termux Connect for the user's full Linux packages and existing Termux home
- explicit backend badge so the user always knows whether a command runs in the launcher sandbox or Termux
- profiles, tabs, font/line-height controls, extra keys and physical-keyboard mappings
- command history, snippets and “open folder in terminal” integration
- an Environment Doctor for `TMPDIR`/`TEMP`, `PATH`, storage grants, shell availability and Termux connection health

Termux Connect is a bridge, not a fork. A modern Android app cannot safely reproduce arbitrary Termux package execution from a writable app directory while targeting current Android security rules.

### Files, downloads and editor

- launcher workspace plus user-granted folders via Storage Access Framework
- recent files, downloads, favorites, tags and project folders
- background-safe download queue with pause, resume, retry and integrity checks
- preview for common images, video, audio, text and PDF handoff
- share-sheet intake and a persistent asset shelf for creator workflows
- native code/text editor with syntax highlighting, search, tabs and autosave
- drag/drop and “open with” between built-in browser, files, editor and terminal surfaces

### Settings and control center

- Appearance: color, wallpaper, icon style, density, font scale, glass level and motion
- Desktop: dock, menu strip, Stage behavior, gestures, saved workspaces and widgets
- Browser: viewport, user agent, zoom, privacy, downloads, permissions and search
- Terminal: backend, shell profile, font, keys, snippets and Termux connection
- Input: cursor, hover, scroll speed inside the shell, shortcuts and touch alternatives
- Performance: Auto, Smooth and Battery policies with live memory information
- Accessibility: contrast, reduced motion, larger targets and independent text/icon scaling
- System bridges: the appropriate Android panels for connectivity, volume, apps and permissions

### Optional permission-backed additions

- launcher notification center through Android Notification Access
- recent-app ranking through Usage Access
- brightness write-through only after explicit Modify System Settings permission

The base experience must not depend on these optional permissions.

## 5. Workflow presets

### Creator workspace

Browser + download/asset shelf + file preview + notes/editor. It is optimized for gathering references, managing media, moving files between web tools and preserving downloads when the foreground app changes.

### Developer workspace

Terminal + project files + editor + browser preview. It is optimized for Git/SSH, Node or Python work through Termux Connect, web debugging workflows and cloud-agent control. Workspace profiles normalize Android-specific environment differences instead of assuming desktop paths such as `/tmp` exist.

### Focus workspace

One active surface, hidden dock/menu strip, notification suppression inside the launcher and a quick return to the prior workspace.

## 6. Technical architecture

- Kotlin Android application, minimum Android 11.
- Jetpack Compose for adaptive shell UI, with custom Views where browser, terminal or high-frequency rendering needs them.
- Unidirectional state with Kotlin coroutines and Flow.
- Room for indexed local state and DataStore for preferences.
- Android Keystore for local secrets and connection keys.
- WorkManager/foreground work only for user-visible durable jobs.
- Baseline Profiles, Macrobenchmark and Perfetto-driven performance testing.
- Modular Gradle structure so browser or terminal failures cannot destabilize the home shell.

Proposed modules:

```text
app
core:model
core:design
core:platform
core:storage
feature:shell
feature:apps
feature:command
feature:browser
feature:terminal
feature:files
feature:editor
feature:settings
benchmark
```

## 7. Performance contract

Reference-device targets are gates, not aspirations:

- first launcher frame under 900 ms on warm start and under 1.8 s on cold start;
- 60 fps for direct manipulation, with no repeated frame stalls above 32 ms;
- shell-only proportional set size target below 140 MB;
- at most two live WebView renderers on the 4 GB profile; older tabs freeze and restore;
- at most two simultaneously active built-in surfaces in portrait/landscape on the 4 GB profile;
- no continuous wallpaper animation or full-screen real-time blur on the reference profile;
- no permanent background service when the user is idle;
- recover the last stable workspace after Android kills the process.

Exact numbers will be tightened after the first benchmark build is measured on the Galaxy A30.

## 8. Definition of v1.0 done

One release is accepted only when the user can:

1. Install it and set it as Home without ADB or root.
2. Finish setup without technical knowledge.
3. Launch apps and restore the shell reliably.
4. browse a desktop-oriented site with compact chrome and mouse/keyboard shortcuts;
5. download a file, find it, edit text/code and hand it to another app;
6. use the built-in shell and connect an existing Termux environment;
7. switch and restore Creator, Developer and Focus workspaces;
8. personalize scale and appearance without causing unreadable layouts;
9. run the complete golden-path test smoothly on the 4 GB Android 11 reference device;
10. understand every Android limitation from the UI rather than discovering it through failure.
