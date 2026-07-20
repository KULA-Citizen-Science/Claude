# Handoff: first real build of the NextQuest `:app` module

This branch (`claude/adhd-activity-predictor-7skuzj`) repurposes the shared Gradle/Compose
scaffold into **NextQuest** — an ADHD next-activity friction oracle (see `README.md`). It was
written in a sandbox where `dl.google.com` (Google's Maven repo and Android SDK host) was
unreachable, so:

- `:core` (pure Kotlin/JVM) **compiles and passes all 18 unit tests** (`gradle :core:test`). This
  is the whole reasoning engine + taxonomy, and it is the authoritative correctness check.
- `:app` (the Android module, Compose only — no Room/KSP) **has never been compiled**. It was
  hand-reviewed only. Expect a handful of first-compile errors; fixing them is the main job of this
  session.

## Prerequisite: environment network access

The cloud environment must allow `dl.google.com` (Network access → **Custom**, add
`dl.google.com`, keep "include default package managers" checked — or use **Full**). Verify:

```bash
curl -sS -o /dev/null -w "%{http_code}\n" https://dl.google.com/android/repository/repository2-1.xml
# 200 = good; 000/403 = network policy still blocks it, stop and tell the user
```

## Installing the Android SDK (no Android Studio in cloud sessions)

```bash
export ANDROID_HOME=$HOME/android-sdk
mkdir -p $ANDROID_HOME/cmdline-tools
curl -fLo /tmp/clt.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q /tmp/clt.zip -d $ANDROID_HOME/cmdline-tools
mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH

yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "sdk.dir=$ANDROID_HOME" > local.properties   # already gitignored
```

## Building

```bash
gradle :core:test           # no SDK needed — use the preinstalled system gradle (8.14.x)
./gradlew assembleDebug      # needs the Android SDK + dl.google.com for AGP
```

Note: the Gradle **wrapper** download (`services.gradle.org` → GitHub release asset) is blocked in
this sandbox, so use the preinstalled system `gradle` (8.14.x satisfies AGP 8.6). For `:core` in
isolation, `gradle :core:test --configure-on-demand` avoids configuring the Android module.

## Known risk spots to check first (in likely order of trouble)

1. **`kotlinOptions { jvmTarget }`** in `app/build.gradle.kts` is deprecated under Kotlin 2.0 —
   fine as a warning; migrate to `compilerOptions` if it errors.
2. **Compose Foundation drawing APIs** used in `ui/components/` (`drawBehind`, `drawWithContent`,
   `DrawScope.drawArc/drawPath`, `Modifier.offset`, `collectIsPressedAsState`) — all stable in the
   pinned Compose BOM (2024.10.01), but the compiler is the referee.
3. **`enableEdgeToEdge` / `BackHandler` / `safeDrawingPadding`** — from activity-compose and
   foundation-layout; confirm they resolve.
4. **Compose BOM 2024.10.01 vs Kotlin 2.0.21** — compatible via
   `org.jetbrains.kotlin.plugin.compose`; if the Compose compiler complains, align the BOM.

Anything beyond mechanical fixes should preserve behaviour documented in `README.md` and
`docs/RESEARCH.md` — especially the friction→framework→strategy mappings and the evidence tiers,
which are locked down by the `:core` tests (keep them green).

## Definition of done for the build session

- [ ] `./gradlew assembleDebug` succeeds
- [ ] `./gradlew test` fully green (the 18 `:core` tests must stay passing)
- [ ] `app/build/outputs/apk/debug/app-debug.apk` exists; report its path and size
- [ ] All fixes committed and pushed to `claude/adhd-activity-predictor-7skuzj` (or, if the git
      proxy only allows the session's own working branch, push there and report the branch name)
- [ ] Reply lists any behaviour-relevant changes made beyond mechanical compile fixes

On-device verification uses the checklist in `README.md`.
