# NextQuest — web app (PWA)

The same app as the Android version, as a static installable web app: plain HTML/CSS/JS, **no build
step, no dependencies, no backend**. Everything runs in the browser and works offline once loaded.

## Run it locally

```
cd web
python3 -m http.server 8080      # or: npm run serve
# open http://localhost:8080
```

Any static server works. Service workers need `http://localhost` or HTTPS, so opening `index.html`
straight from disk works for the app but not for offline/install.

## Install on a phone

Open the hosted URL in Chrome (Android) or Safari (iOS) → menu → **Add to Home screen**. It launches
full-screen with its own icon, and works offline after the first visit.

## Hosting

The folder is host-agnostic: all paths are relative and routes are hash-based (`#/pick`,
`#/read/<quest>/<trap>`), so it works from a subpath (GitHub Pages, Codeberg Pages, any static
bucket) with no server rewrites. Upload the contents of `web/`.

### GitHub Pages (automated)

`.github/workflows/pages.yml` publishes the app. On each push to `main` that touches `web/` (or
when run manually from the Actions tab) it runs the web tests, stages only the runtime files (no
tests or tools), checks that everything the service worker precaches is present
(`tools/check-precache.mjs`), and deploys.

One-time setup: repo **Settings → Pages → Build and deployment → Source: GitHub Actions**. The site
then lives at `https://<owner>.github.io/<repo>/`. Pages only deploys from branches the
`github-pages` environment allows (by default just `main`), so merge a feature branch to `main`
before expecting it to publish.

Moving to another host later: the workflow is the only GitHub-specific piece. Delete it and upload
the same staged files (`index.html style.css manifest.webmanifest sw.js js/ icons/`).

When you change any file, bump `CACHE` in `sw.js` so installed copies pick up the update.

## How it's built

| File | Role |
|------|------|
| `js/engine.js` | Port of the Kotlin `:core` domain: activity dimensions, evidence-tiered frameworks, the 13-trap catalog with scoring, archetypes, ranking engine |
| `js/art.js` | **Generated** from the Kotlin `SceneArt` sprite grids by `tools/gen-art.py` — don't edit |
| `js/render.js` | Canvas rendering: pixel sprites, mascot, quest icons, seeded starfield |
| `js/app.js` | Hash-routed UI: Home / Picker / Reading |
| `style.css` | The cozy-VGA / LucasArts look: bevelled panels, scanlines, pixel buttons |
| `sw.js`, `manifest.webmanifest`, `icons/` | Offline shell and install metadata (`tools/make-icons.cjs` renders the PNGs) |

## Keeping web and Android in sync

The engine exists twice (Kotlin and JS), so drift is guarded by tests:

- `web/test/engine.test.mjs` — port of the Kotlin engine/catalog/art tests.
- `web/test/golden.txt` — the web engine's ranked traps for all 7 archetypes **and all 128 "Other…"
  toggle combinations**. `core/.../WebParityTest.kt` checks the Kotlin engine against the same file.

```
cd web && npm test                 # JS tests (Node 20+)
gradle :core:test                  # Kotlin tests, including the parity check
```

If you change scoring, trap order or the "Other…" mapping on one side: update the other side, then
regenerate the golden with `npm run golden`. If you change sprites in `SceneArt.kt`, run
`npm run art`. Trap/strategy *wording* is not covered by the golden (only ids and ranking), so keep
copy edits mirrored by hand.
