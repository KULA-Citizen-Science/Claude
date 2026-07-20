# NextQuest

A tiny Android app for an inattentive ADHD brain with time blindness and executive dysfunction.
Push a button, tell it your next planned activity, and it names — grounded in established,
evidence-graded frameworks — **where that activity is likely to trip you up**, and **one concrete
move past it**. The output is deliberately two short lines; the taxonomy underneath is thorough
(see [`docs/RESEARCH.md`](docs/RESEARCH.md)).

Aesthetics: a loud 90s-homepage layout with cute, nerdy LucasArts-adventure charm — chunky
bevelled VGA panels, a pixel-art mascot, faint CRT scanlines, all hand-drawn in Compose.

## How it works

1. **Home** → one big button.
2. **Choose your quest** → tap a common ADHD-hard archetype (The Boring Admin, The Appointment,
   The Big Project, The Chore You Avoid, The Wind-Down, The Errand, The Reply), or **Other…** and
   flick a few toggles to describe any activity.
3. **Reading** → a point-and-click style *scene*: a quest-tinted starfield room with the quest's
   prop on the wall, the mascot facing a symbolic pixel-art image of the trap (a cracked wall, a
   sprinting alarm clock, a road with no finish line…), and the escape strategy sitting in an
   inventory slot (`USE:`). Below it, the card: `THE TRAP` (one line), a tiny framework tag +
   evidence marker, and `THE ESCAPE` (one line). Star placement is seeded per (quest, trap), so
   every combination gets its own unique picture. **NEXT TRAP** cycles the lower-ranked
   frictions; **NEW QUEST** resets.

The card's evidence marker is honest about tiers: **`✓` peer-reviewed** vs **`~` clinical
heuristic** (Barkley, Marx/Zheng, Safren/Solanto, Gollwitzer are peer-reviewed; Dodson's INCUP and
the "Wall of Awful" are useful heuristics, and labelled as such).

> NextQuest is a nudge tool, **not** medical advice, diagnosis, or treatment.

## Project structure

- `core/` — plain Kotlin/JVM module, **no Android dependency**. The entire taxonomy and reasoning
  engine live here: activity dimensions (`Activity`), the friction taxonomy (`FrictionPoint` /
  `FrictionCatalog`), the evidence-graded framework catalog (`Framework`), the pre-tagged
  archetypes (`Archetypes`), and the ranking engine (`FrictionEngine`). The scene art also lives
  here as data: `art/Sprite` + `art/SceneArt` hold the 13 trap sprites and 13 escape emblems as
  character grids, so tests can verify the art catalog (rectangular, on-palette, full coverage of
  the taxonomy) without a renderer. Fully JUnit-tested.
- `app/` — the Android app: Jetpack Compose UI (`Home` / `Picker` / `Reading`) and the hand-drawn
  retro toolkit (bevelled panels, pixel buttons, mascot, icons). Depends on `:core`.
- `docs/` — the evidence write-up (`RESEARCH.md`) and the source research brief
  (`adhd-task-taxonomy-report.md`).

## Building & testing

`gradle :core:test` passes (18/18 tests) and is the authoritative check on the taxonomy/ranking.
The `:app` module builds successfully (`:app:assembleDebug` verified with SDK platform 34 /
build-tools 34.0.0); runtime behaviour still needs the manual on-device checklist below.

Run the engine tests (no Android SDK needed):

```
./gradlew :core:test
```

Build and install the full app on a machine with Android Studio / the Android SDK:

1. Install [Android Studio](https://developer.android.com/studio) (bundles a JDK + SDK manager),
   or the Android command-line tools + JDK 17.
2. `./gradlew assembleDebug`
3. `./gradlew installDebug`

### Manual verification checklist (needs a device/emulator)

- [ ] Home button opens the picker; system Back exits from Home.
- [ ] Each archetype produces a sensible top trap + strategy + framework tag. Spot-check:
      The Appointment → *Time blindness* (running late); The Big Project → *No finish line*;
      The Chore You Avoid → *Activation*; The Errand → *Transition cost*; The Boring Admin →
      *Wall of Awful*.
- [ ] **Other…** toggles change the reading (e.g. tick "brings dread" → Wall of Awful surfaces).
- [ ] **NEXT TRAP** cycles through the ranked frictions and wraps; **NEW QUEST** returns to the
      picker; Back on a reading returns to the picker.
- [ ] The evidence marker (`✓`/`~`) and tier legend render, and match the framework shown.
- [ ] Retro styling renders: bevelled panels, pixel mascot bobs, scanlines are subtle, monospace
      text is legible on the aubergine background.

### Optional polish

The UI uses the platform **monospace** font (no bundled binary). Dropping a true OFL pixel font —
e.g. Press Start 2P or Silkscreen — into `app/src/main/res/font/` and pointing `Retro.questFont`
at it is a pure visual upgrade.
