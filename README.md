# Stylus Notes

A simple, local-only Android notetaking app built for handwriting with a stylus (developed
against the Motorola Stylus 5G, 2024). Draw freeform ink with pressure-sensitive strokes on an
infinite canvas (two-finger pan and pinch-zoom, with a Fit button to jump back to all your ink),
switch between a white or black canvas, pick from an ink color palette that stays legible on
either background, and export a note as a PNG or PDF sized for handwriting recognition by an LLM
(e.g. uploading to Claude for HTR). Exports crop to the bounding box of the ink plus a margin,
not the screen.

## Project structure

- `core/` — plain Kotlin/JVM module, no Android dependency. Stroke smoothing, undo/redo, and the
  WCAG contrast math behind the ink color palette all live here, with JUnit tests.
- `app/` — the Android app: Room persistence, the custom `InkCanvasView` that handles stylus
  input, Compose UI (note list + editor), and PNG/PDF export.

## Building

This repo was scaffolded in a sandbox with no Android SDK and no network access to
`dl.google.com`, so only `:core` (plain Kotlin/JVM) could actually be compiled and tested here —
`gradle :core:test` passes (16/16 tests). The `:app` module needs the Android SDK to build and has
**not** been compiled in this environment; review it accordingly before treating it as verified.

To build the full app on a machine with Android Studio / the Android SDK installed:

1. Install [Android Studio](https://developer.android.com/studio) (bundles a compatible JDK and
   lets you install SDK platforms/build-tools through the SDK Manager), or install the Android
   SDK command-line tools and JDK 17 yourself.
2. Open this directory in Android Studio, or from the command line:
   ```
   ./gradlew assembleDebug
   ```
3. Run the full test suite (core + app unit tests):
   ```
   ./gradlew test
   ```
4. Install on a device/emulator:
   ```
   ./gradlew installDebug
   ```

## Manual verification checklist (needs a real device)

Stylus behavior — pressure, tilt, palm rejection, latency — can't be verified without a physical
stylus, so these need to be checked by hand on the Motorola Stylus 5G (or another Android stylus
device) after building and installing the app:

- [ ] Draw a stroke with the stylus; confirm it renders smoothly and width visibly varies with
      pressure.
- [ ] Rest a palm on the screen while writing; confirm the palm doesn't produce marks or interrupt
      the stroke (palm rejection).
- [ ] Erase a stroke: with an active stylus, use the barrel button (or dedicated eraser tip); with
      a passive stylus, use the ⌫ eraser toggle in the editor toolbar.
- [ ] Undo/redo a few strokes and confirm the canvas updates correctly each time.
- [ ] Toggle the canvas background between white and black; confirm ink drawn with "Adaptive"
      color flips to stay visible on both.
- [ ] Pick each accent ink color from the palette and confirm it's readable on both a white and a
      black canvas.
- [ ] Create a note, add strokes, leave the editor (back button), reopen the note, and confirm the
      strokes persisted (autosave).
- [ ] Rename a note — tap its title in the editor top bar, and use Rename on a note card in the
      list — and confirm the new title shows in both places after reopening the app.
- [ ] Pan with two fingers and pinch-zoom in/out; write while zoomed in and confirm the ink lands
      where the pen touches. Tap Fit and confirm the view returns to showing all ink. Reopen the
      note and confirm it opens fitted to the ink.
- [ ] Write beyond one screenful (pan, keep writing), export, and confirm the export contains all
      of it — the exported page is the ink's bounding box, not the screen.
- [ ] Export a note as PNG and as PDF; open each file and confirm the ink is legible — this is the
      format that gets shared to an LLM (e.g. Claude) for handwriting recognition, so legibility
      here is the actual acceptance bar.
- [ ] Delete a note from the note list and confirm it's gone after reopening the app.

### How stylus input and palm rejection work

`InkCanvasView` accepts two kinds of pointers:

- **Active stylus** (`MotionEvent.TOOL_TYPE_STYLUS` / `TOOL_TYPE_ERASER`): always draws, with
  pressure-sensitive width, barrel-button erase, and strict palm rejection — while a stylus
  stroke is active, every other pointer is swallowed.
- **Passive/capacitive stylus** (reported by Android as `TOOL_TYPE_FINGER` — this is what the
  built-in pen on the Moto G Stylus 2024/2025 and earlier is; Motorola only switched to an
  active pen in the 2026 model): tool type can't distinguish pen from palm, so palm rejection
  falls back to contact size. Contacts smaller than ~8 mm draw; larger ones are ignored, and an
  in-progress stroke whose contact grows past ~11 mm is discarded as a palm.

**Canvas navigation:** notes live on an infinite canvas. Two small contacts at once (two
fingertips) pan the view, and moving them apart/together pinch-zooms around the gesture's focal
point; a stroke just started by the first finger is discarded when the second lands, since the
gesture was navigation, not writing. Large (palm) contacts never join navigation. With an active
stylus, the pen keeps absolute priority — finger gestures are ignored while a pen stroke is in
progress. The editor's Fit button zooms back out to show all ink (notes also reopen fitted).

Consequences of the passive path to be aware of: a deliberate small fingertip contact can also
draw (indistinguishable from a passive pen tip); pressure-sensitive width is flat because
passive pens report no meaningful pressure; and the toolbar ⌫ eraser toggle exists because a
passive pen has no barrel button or eraser tip. On devices whose touchscreen doesn't report
contact size at all, size-based rejection is disabled and small/large contacts all draw.

In a stock Android Studio emulator a host mouse click is reported as a finger touch, so it will
draw via the passive path; the emulator's stylus/S-Pen input simulation under Extended Controls
(where available) exercises the active-stylus path instead.
