# Handoff: Shadow Routines

ADHD-friendly routine + quote-reward Android app. Kotlin + Jetpack Compose, local-first
(Room + DataStore), no network for core features. See `README.md` for the feature overview and
the manual on-device test checklist.

## Module layout

- `:core` — pure Kotlin/JVM, **no Android dependency**, fully unit-tested. Contains the domain
  model, Markdown quote parser, keyword extractor, relevance scoring, and the quote-selection /
  anti-repetition engine (`QuoteEngine`, `Deck`, `RecentlySeen`).
- `:app` — Android app: Room (routines, checklist snapshots, quotes + FTS4), DataStore novelty
  state, SAF import, Compose UI + the full-screen flip reward.

## Build status in this environment

This session had the Android SDK installed (`platform-34`, `build-tools;34.0.0`) and network to
`dl.google.com`, so **both modules were built here**:

- `:core:test` — green (36 tests: parser, keyword extraction, in-memory index, deck/recently-seen
  buffer, and the selection engine including a "no repetition within the buffer window" property
  test).
- `:app:assembleDebug` — produced `app/build/outputs/apk/debug/app-debug.apk`.

To rebuild from scratch (fresh session without the SDK):

```bash
export ANDROID_HOME=$HOME/android-sdk
mkdir -p $ANDROID_HOME/cmdline-tools
curl -fLo /tmp/clt.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q /tmp/clt.zip -d $ANDROID_HOME/cmdline-tools
mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
echo "sdk.dir=$ANDROID_HOME" > local.properties   # gitignored

gradle :app:assembleDebug :core:test
```

The preinstalled system `gradle` (8.14.x) satisfies AGP 8.6's minimum; the wrapper is pinned to
Gradle 8.9 if you prefer `./gradlew`.

## Key design decisions (confirmed with the product owner)

- **Corpus store/search:** Room + FTS4 (`quotes_fts`), parse-on-change, rebuild on rescan.
- **Anti-repetition:** hybrid — keyword relevance → recently-seen buffer exclusion (relaxed
  oldest-first) → shuffled full-corpus deck fallback. State persisted in DataStore.
- **Triggers (v0):** manual only (`I'm about to…`). Calendar/location deferred.
- **Corpus import:** bundled `assets/quotes/*.md` + one-shot SAF folder import.

## Where things live (orientation for the next change)

- Selection algorithm + novelty: `core/.../quote/QuoteEngine.kt`, `AntiRepetition.kt`,
  `QuoteIndex.kt`, `QuoteScoring.kt`. **This is the tested heart — keep the tests green.**
- Corpus parsing: `core/.../quote/MarkdownQuoteParser.kt`.
- Routine templates (the six seeded task types): `core/.../routine/SeedTemplates.kt`.
- Room schema: `app/.../data/db/` (entities, DAOs, `AppDatabase`). Schema JSON is exported to
  `app/schemas/` — bump `version` and add a migration for any entity change.
- Reward flip: `app/.../ui/flip/FlipRewardHost.kt` + `ui/reward/RewardCard.kt`.
- Wiring: `app/.../di/AppContainer.kt`, `ShadowRoutinesApp.kt`, `MainActivity.kt`.

## Known limitations / good first follow-ups

- The Markdown parser treats a body line that is exactly `---` as a fence (documented in the
  parser KDoc). Fine for short quotes.
- SAF import is one-shot (content copied into Room); the folder uri is not persisted.
- On-device behavior (the flip animation feel, SAF picker, snackbars) still needs a manual pass
  on a device/emulator — see the checklist in `README.md`.
