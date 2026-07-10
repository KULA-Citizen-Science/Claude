# Stylus Notes

A simple, local-only Android notetaking app built for handwriting with a stylus (developed
against the Motorola Stylus 5G, 2024). Draw freeform ink with pressure-sensitive strokes, switch
between a white or black canvas, pick from an ink color palette that stays legible on either
background, and export a note as a PNG or PDF sized for handwriting recognition by an LLM (e.g.
uploading to Claude for HTR).

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
- [ ] Use the stylus's barrel button (or a dedicated eraser tip, if the stylus has one) to erase a
      stroke.
- [ ] Undo/redo a few strokes and confirm the canvas updates correctly each time.
- [ ] Toggle the canvas background between white and black; confirm ink drawn with "Adaptive"
      color flips to stay visible on both.
- [ ] Pick each accent ink color from the palette and confirm it's readable on both a white and a
      black canvas.
- [ ] Create a note, add strokes, leave the editor (back button), reopen the note, and confirm the
      strokes persisted (autosave).
- [ ] Export a note as PNG and as PDF; open each file and confirm the ink is legible — this is the
      format that gets shared to an LLM (e.g. Claude) for handwriting recognition, so legibility
      here is the actual acceptance bar.
- [ ] Delete a note from the note list and confirm it's gone after reopening the app.

### Note on emulator testing without a physical stylus

`InkCanvasView` only draws for `MotionEvent.TOOL_TYPE_STYLUS` / `TOOL_TYPE_ERASER` — plain finger
or mouse taps are intentionally ignored (that's the palm-rejection mechanism). A host mouse click
in a stock Android Studio emulator is normally reported as a finger touch, so it won't draw. To
test in an emulator without physical hardware, use the emulator's stylus/S-Pen input simulation
under Extended Controls (where available), rather than a plain mouse click.
