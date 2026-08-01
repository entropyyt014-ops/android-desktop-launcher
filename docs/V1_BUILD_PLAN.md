# Single-Release Build Plan

There will be one public product target: `v1.0`. The stages below are internal build gates. We do not publish a succession of stripped-down launcher editions.

## Gate 0 — Foundation and benchmark harness

- create the modular Android project and CI;
- set Android 11 as the reference floor and current Android as the target;
- define design tokens, adaptive sizing and navigation contracts;
- add unit, screenshot, startup, macrobenchmark and lint infrastructure;
- create a repeatable Galaxy A30 benchmark checklist.

Exit: a signed debug APK opens a blank responsive shell, reports measured device profile and produces benchmark output.

Status: CI passed and the debug APK opened on the physical Galaxy A30. The first install also confirmed that the diagnostic scaffold must not remain the product Home screen; Gate 1 removes it.

## Gate 1 — Golden-path shell

- Home-role onboarding;
- desktop surface, app library, dock and compact menu strip;
- command center and settings skeleton;
- portrait Stage and landscape two-tile manager;
- touch, mouse, keyboard and session-state foundations.

Exit: the launcher can safely become Home, launch apps, survive process death and remain smooth on the reference profile.

Current slice: setup assistant, Home-role request, real desktop/menu bar/Dock, installed-app library, Command Center, Control Center, Settings, persistent Dock pins and portrait/landscape Stage windows. Two-tile management and full process-death device evidence remain Gate 1 work.

## Gate 2 — Browser vertical slice

- browser window and tab model;
- desktop/adaptive/mobile site profiles;
- compact and auto-hiding browser chrome;
- shortcuts, mouse context, downloads, file upload and permission handling;
- tab suspension, renderer-loss recovery and session restore.

Exit: a desktop-oriented web workflow can be completed without falling back to Chrome for ordinary use.

## Gate 3 — Work tools

- file hub, user-granted folders, previews and asset shelf;
- durable download queue;
- native code/text editor;
- built-in PTY shell;
- Termux detection, permission flow and Connect integration;
- browser/files/editor/terminal handoffs.

Exit: download a project or asset, inspect/edit it, run an applicable command and reopen the workspace after restart.

## Gate 4 — Complete desktop experience

- saved Creator, Developer and Focus workspaces;
- Android widgets and web-app shortcuts;
- full personalization and adaptive setup calibration;
- control-center panels and system deep links;
- optional notifications and usage integrations;
- import/export and recovery/reset flow.

Exit: all v1.0 capabilities are reachable, understandable and reversible without technical setup.

## Gate 5 — Hardening and release

- Galaxy A30 memory, jank, battery and long-session testing;
- Android 11 through current-version compatibility matrix;
- accessibility, keyboard-only and touch-only QA;
- browser compatibility and unsafe-download tests;
- terminal boundary and command-injection review;
- dependency/license audit, privacy disclosure and threat model;
- release signing, reproducible CI artifact and rollback procedure.

Exit: every definition-of-done item in the product blueprint passes and the release candidate has no critical defect.

## First implementation milestone

The first code milestone is one coherent route, not isolated mock screens:

```text
Install → adaptive setup → choose as Home → open desktop → launch built-in browser
→ load a desktop-profile page → open file shelf → open built-in terminal → restart → restore workspace
```

Visual polish and measurement begin in this milestone; they are not postponed to the end.

## Repository workflow

- `main` remains releasable.
- Work happens in small `feat/`, `fix/` and `perf/` branches.
- Every pull request includes its device/API test evidence.
- Performance budgets are CI-visible and regressions block merging.
- APK release artifacts are created only by the signed release workflow.
