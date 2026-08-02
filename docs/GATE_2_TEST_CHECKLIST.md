# Gate 2 Browser physical-device checklist

Reference device: Samsung Galaxy A30, Android 11, 4 GB RAM.

## Open and navigate

- Open **Browse** from the Dock and Command Center.
- Load an HTTPS page and verify title, progress, back, forward, stop and reload.
- Enter a phrase without a domain and verify it becomes a Google search.
- Open two tabs, switch between them and confirm both retain their pages.
- Open a third tab and confirm Stage remains responsive under the two-WebView budget.

## Profiles and sessions

- Switch one site among Desktop, Adaptive and Mobile profiles.
- Reopen the site and confirm its profile is remembered.
- Force-stop Stage, reopen it and confirm tab URLs, titles, bookmarks and history restore.
- Close a tab and verify **Reopen closed tab**.
- Trigger focus mode and exit through the compact URL pill.

## Web integration

- Use a webpage file-upload control and select a document through Android's picker.
- Start a real download and verify Android DownloadManager progress and completion.
- Open and share a completed download from the Stage download shelf.
- Open a `mailto:` or another external-app link and confirm Stage asks before leaving.
- Visit a site requesting camera, microphone or location and confirm both the Stage prompt and Android permission prompt appear.
- Confirm an invalid HTTPS certificate is never bypassed.

## Recovery and input

- Use `Ctrl/Meta + L`, `T`, `W`, `Shift + T`, `F` and `Tab` with a physical keyboard if available.
- Use Alt + Left/Right for navigation.
- Rotate while a page is open and confirm the active tab survives.
- Keep several tabs open for ten minutes and record any renderer recovery message, crash, frozen page or severe jank.

Report a failure with the URL, active profile, tab count, orientation and a screenshot or screen recording.
