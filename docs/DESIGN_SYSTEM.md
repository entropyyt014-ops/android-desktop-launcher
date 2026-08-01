# Design System — Liquid Graphite

## Direction

Liquid Graphite borrows the calm hierarchy, spatial confidence and direct manipulation associated with macOS, then combines it with the user's preferred premium SaaS depth and controlled violet glow. It must feel designed, not themed.

We borrow patterns, not Apple assets, trademarks or exact component copies.

## Visual language

- Base: near-black graphite with slightly warm elevated surfaces.
- Accent: deep violet used for focus, active state and restrained ambient glow.
- Text: soft white, never pure white across large areas.
- Depth: borders, tonal elevation and small shadows before blur.
- Glass: translucent only where it clarifies layers; cached or opaque substitutes on the 4 GB profile.
- Texture: extremely subtle grain to avoid sterile vector-flat surfaces.
- Icons: original or permissively licensed rounded stroke icons with filled active variants.
- Type: Inter for interface text and JetBrains Mono for terminal/code.

## Interaction grammar

- Every motion explains origin, destination or state change.
- Open actions grow from their launcher source; minimize actions return toward the dock.
- The active Stage is visually dominant; background surfaces lose contrast before they lose scale.
- Hover appears only when a pointing device exists.
- Touch never depends on hover or right-click.
- Context menus open from the pointer on mouse and from anchored bottom sheets on touch.
- Keyboard focus is always visible.

## Motion budgets

- micro response: 90–140 ms;
- panel/window transition: 180–260 ms;
- workspace transition: 260–360 ms;
- use spring motion only for direct manipulation and dock response;
- reduced-motion mode replaces scale/blur transitions with short fades;
- background animation stops when the shell loses focus.

## Adaptive chrome

### Portrait

- 24–30 dp compact menu strip, optionally hidden;
- dock centered above navigation insets;
- one primary Stage and an edge shelf for secondary context;
- browser omnibox collapses to a thin URL/title pill during content focus.

### Landscape / physical input

- persistent compact menu strip;
- dock may move to the left edge when vertical space is more valuable;
- two-tile and floating layouts for built-in surfaces;
- browser tabs become denser and mouse-first controls appear.

## Performance variants

The same design system has three renderer policies, selected automatically and manually overrideable:

| Policy | Treatment |
|---|---|
| Auto | Device-aware balance; default |
| Smooth | Minimal live blur, reduced particles, strict two-surface limit |
| Battery | Opaque surfaces, shorter fades, no wallpaper motion, lower background refresh |

These are settings, not app editions. Layout and capabilities remain consistent.

## Accessibility

- independent interface, text, icon, dock and cursor scaling;
- minimum 44 dp touch targets even when visual icons are smaller;
- contrast-safe text over wallpaper through adaptive scrims;
- reduced transparency and reduced motion options;
- no information encoded only by glow or color.
