# KULA — Android apps

This repository holds **two independent, self-contained Android app projects**, side by side in
their own directories. Neither depends on the other; each has its own Gradle build, wrapper, and
docs. Keeping them in separate folders means merging any branch can never overwrite the other app.

| Directory | App | What it is |
|-----------|-----|------------|
| [`shadow-routines/`](shadow-routines/) | **Shadow Routines** | ADHD-friendly routine support: checklists of often-forgotten "soft aspects" around a task, with a full-screen flip reward showing a quote from a local Markdown corpus. |
| [`stylus-notes/`](stylus-notes/) | **Stylus Notes** | Local-only handwriting notetaking for a stylus: pressure-sensitive ink on an infinite canvas, with PNG/PDF export. |

Each app is built from inside its own directory, e.g.:

```bash
cd shadow-routines      # or: cd stylus-notes
./gradlew assembleDebug
./gradlew test
```

See each directory's own `README.md` and `HANDOFF.md` for feature details, build/SDK setup, and
the manual on-device test checklists.

## History

Both apps began life as the root project on their own branches
(`claude/android-stylus-notes-app-xz97wj` for Stylus Notes; `claude/adhd-routine-quote-rewards-*`
for Shadow Routines). They were reorganized into subdirectories so both can live together on one
branch without one replacing the other.
