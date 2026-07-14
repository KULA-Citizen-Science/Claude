# Shadow Routines

An ADHD-friendly Android app that externalizes the *soft aspects* around a task — the
time-buffers, communication steps, logistics, self-care, and follow-ups that are easy to
forget — and gives a **micro-reward** when you finish: the whole screen flips like a card and
shows a quote drawn from your own local Markdown corpus.

Built native (Kotlin + Jetpack Compose), fully **local-first**: all task, routine, and quote
data lives on-device (Room/SQLite + DataStore). No network is used for core functionality.

## What it does (v0)

- **Shadow routines.** Pick a "core task type" — Meeting, Workshop planning, Commute, Deep work
  block, Errand, Daily routine — and the app opens a checklist of often-forgotten soft steps,
  grouped into Time buffers / Communication / Logistics / Self-care / Follow-ups.
- **Complete → reward.** Finishing a routine flips the screen around its horizontal axis and
  shows *"You just completed: [task] / Never forget: [quote]"*, with author/source and a
  favorite toggle. Tap anywhere to flip back.
- **Novel quotes.** Rewards are chosen from your Markdown corpus with a keyword-relevance +
  anti-repetition engine, so quotes stay fresh instead of repeating (details below).
- **Gentle gamification.** The home screen tracks routines completed and unique quotes seen.
  No penalties, no pressure.

## The quote corpus

Quotes are plain Markdown, one or more per file, each with light front-matter:

```
---
id: woolf-room-1
author: Virginia Woolf
source: A Room of One's Own
tags: [work, solitude, creativity, time]
---
"A woman must have money and a room of her own if she is to write fiction."
```

- `id` is optional — if omitted it is derived from the filename + a content hash, stable across
  re-scans (so per-quote "seen"/favorite state survives a rebuild).
- `tags` accepts an inline list (`[a, b]`), a bare comma list, or a multi-line `- item` list.
- A starter corpus (~30 quotes) ships in `app/src/main/assets/quotes/`. You can also **import
  from a folder** on the device (overflow menu → *Import quotes from folder…*, via the Storage
  Access Framework) or **rescan the bundled corpus**.

### Importing prose (books, notes)

You can also point the importer at plain prose `.md` files that have **no** per-quote
front-matter. Prose is broken up **one quote per sentence**, and each quote must:

- be at most **130 characters** (`MarkdownQuoteParser.DEFAULT_MAX_QUOTE_LENGTH`) — longer
  sentences are dropped, not truncated;
- **start at a real sentence boundary** (a capital, digit or opening quote), which discards the
  mid-sentence fragments left behind when a book breaks a sentence across a page;
- not be a heading or running page-label (`# ...`, or a short mostly-ALL-CAPS line like
  `ARCS OF COHERENCE 173`) — these are removed even between paragraphs.

Footnote markers (superscripts, or digits stuck to a sentence's end) are stripped, and a `>`
blockquote is kept whole even if long. Curated front-matter quotes are always taken exactly as
written.

**Attribution:** put a single front-matter block at the very top of a prose file and its
`author` / `source` / `tags` apply to every quote pulled from that file, so a sideloaded book
shows its source on the reward card:

```
---
author: Steven Pinker
source: The Sense of Style
tags: [writing]
---
<the whole book as prose…>
```

### How a reward quote is chosen (novelty)

Selection lives in the pure-Kotlin `:core` module (`QuoteEngine`) and is fully unit-tested:

1. Build keywords from the task title/description + the template's context tags.
2. Keyword-search the corpus (Room FTS on-device; an in-memory index in tests) for a candidate
   set, ranking tag hits above body hits.
3. Exclude anything in a persisted **recently-seen buffer** (last 50 by default); if that empties
   the set, relax **oldest-first** rather than lifting the whole buffer.
4. Weighted-random pick, favouring higher relevance and lower seen-count.
5. **Fallbacks:** no keyword hits → draw from a shuffled **deck** of the whole corpus (every
   quote appears once before any repeat); tiny corpus → relax gracefully.

The recently-seen buffer and deck position are persisted (DataStore) so novelty survives app
restarts. Repetition is strongly biased against, never hard-forbidden.

## Project structure

- `core/` — plain Kotlin/JVM module, no Android dependency. The domain model (routine templates,
  soft-aspect categories), the Markdown quote parser, keyword extraction, relevance scoring, and
  the whole quote-selection / anti-repetition engine live here, with JUnit tests.
- `app/` — the Android app: Room persistence (routines, checklist snapshots, quotes + an FTS4
  table), DataStore for novelty state, SAF corpus import, and the Compose UI including the
  full-screen flip reward.

Keeping the selection logic in `:core` means the novelty policy is testable without a device or
the Android SDK.

## Building

Requires the Android SDK (platform 34, build-tools 34.0.0) and JDK 17.

```
./gradlew assembleDebug     # build the debug APK
./gradlew test              # :core unit tests (+ any :app unit tests)
./gradlew installDebug      # install on a device/emulator
```

If you don't have Android Studio, install the SDK command-line tools, then:

```
export ANDROID_HOME=$HOME/android-sdk
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

## Manual verification checklist (needs a device/emulator)

- [ ] Launch the app; the home screen lists the six routine templates and a stats row.
- [ ] Tap a template (e.g. *Meeting*); confirm a grouped checklist opens and progress updates as
      you check items.
- [ ] Tap **Complete & reward me**; confirm the screen flips around the horizontal axis to a
      full-screen reward showing the task title and a quote, then flips back on tap.
- [ ] Complete several routines in a row; confirm quotes don't immediately repeat.
- [ ] Toggle the ♥ favorite on a reward; complete again and confirm state is coherent.
- [ ] Overflow menu → *Rescan bundled corpus*; confirm the snackbar reports the quote count.
- [ ] Overflow menu → *Import quotes from folder…*; pick a folder with `.md` files and confirm
      the count, then complete a routine and confirm imported quotes can appear.
- [ ] Force-stop and reopen; confirm recently-seen quotes still aren't repeating (novelty state
      persisted).

## Roadmap (deferred past v0)

- Calendar-based triggers (suggest a template from an upcoming event) — the trigger seam is
  manual-only in v0.
- Gamification *unlocks* (themes/accents) beyond the current counters.
- Custom/edited templates, a quote browser / favorites screen, and an auto-return timeout on the
  reward.
