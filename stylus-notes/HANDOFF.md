# Handoff: first real build of the `:app` module

This scaffold lives on `claude/android-stylus-notes-app-xz97wj` and `main`, which point at the
same commit — whichever branch your session started on, you have the complete v1 scaffold of the
Stylus Notes app. It was written in a sandbox where `dl.google.com` (Google's Maven repo and
Android SDK host) was unreachable, so:

- `:core` (pure Kotlin/JVM) **compiles and passes all 16 unit tests** (`gradle :core:test`).
- `:app` (the Android module) **has never been compiled**. It was hand-reviewed only. Expect a
  handful of first-compile errors; fixing them is the main job of this session.

See `README.md` for the feature overview and the manual on-device test checklist.

## Prerequisite: environment network access

The cloud environment must allow `dl.google.com` (Network access → **Custom**, add
`dl.google.com`, keep "include default package managers" checked — or use **Full**). Verify from
inside the session before doing anything else:

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

# Point Gradle at it (either works):
echo "sdk.dir=$ANDROID_HOME" > local.properties   # already gitignored
```

## Building

```bash
./gradlew assembleDebug      # falls back: preinstalled `gradle` (8.14.x) if wrapper download fails
./gradlew test               # :core (16 tests) + any :app unit tests
```

The wrapper is pinned to Gradle 8.9 and downloads from `services.gradle.org` (allowlisted), but
the download redirects through GitHub release assets; if that fails in the sandbox, just use the
preinstalled system `gradle` — 8.14.x satisfies AGP 8.6's minimum (8.7).

## Known risk spots to check first (in likely order of trouble)

1. **Room + KSP + Kotlin 2.0**: versions are Room 2.6.1 / KSP 2.0.21-1.0.28. If the KSP round
   fails, either add `ksp.useKSP2=false` to `gradle.properties` or bump Room to a 2.7.x version
   that officially supports Kotlin 2.0.
2. **`kotlinOptions { jvmTarget }`** in `app/build.gradle.kts` is deprecated under Kotlin 2.0 —
   fine as a warning; migrate to `compilerOptions` if it errors.
3. **ViewModel factory imports** in `app/src/main/java/com/kula/stylusnotes/ui/StylusNotesNavHost.kt`
   (`androidx.lifecycle.viewmodel.initializer` / `viewModelFactory`) — confirm they resolve from
   `lifecycle-viewmodel-compose`; if not, add `androidx.lifecycle:lifecycle-viewmodel-ktx`.
4. **Material3 experimental APIs** (`TopAppBar`, etc.) — `@OptIn(ExperimentalMaterial3Api::class)`
   annotations exist where I expected them to be needed, but the compiler is the referee.
5. **Compose BOM 2024.10.01 vs Kotlin 2.0.21** — should be compatible via the
   `org.jetbrains.kotlin.plugin.compose` plugin; if the Compose compiler complains, align the BOM
   to whatever it suggests.

Anything beyond mechanical fixes (signature changes, dependency bumps) should preserve the
behavior documented in `README.md` — especially: stylus-only input with palm rejection, adaptive
ink color, both-background-legible palette (validated by `:core` tests — keep them green), and
exports always rendered on white.

## Definition of done for the build session

- [ ] `./gradlew assembleDebug` succeeds
- [ ] `./gradlew test` fully green (the 16 `:core` tests must stay passing)
- [ ] `app/build/outputs/apk/debug/app-debug.apk` exists; report its path and size
- [ ] All fixes committed and pushed — prefer `claude/android-stylus-notes-app-xz97wj`, but if the
      session's git proxy only allows pushing to the session's own working branch, push there and
      report the branch name
- [ ] Reply lists any behavior-relevant changes made beyond mechanical compile fixes

On-device verification (pressure, palm rejection, latency) still needs the physical Motorola
Stylus 5G afterwards — that part can't be automated; checklist is in `README.md`.
