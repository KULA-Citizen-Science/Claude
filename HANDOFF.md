# Handoff: building the NextQuest `:app` module

This branch (`claude/adhd-activity-predictor-7skuzj`) repurposes the shared Gradle/Compose
scaffold into **NextQuest** — an ADHD next-activity friction oracle (see `README.md`).

**Status:** `:core` (pure Kotlin/JVM) compiles and passes all 18 unit tests (`gradle :core:test`)
— this is the whole reasoning engine + taxonomy and the authoritative correctness check. `:app`
**builds successfully**: `:app:assembleDebug` produced a working 9.1 MB debug APK with zero
compile fixes (SDK platform 34, build-tools 34.0.0, system Gradle 8.14.3, JDK 21). What remains
unverified is on-device behaviour — see the manual checklist in `README.md`.

The steps below document how to set up the SDK in a fresh cloud session.

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

Any future change should preserve the behaviour documented in `README.md` and
`docs/RESEARCH.md` — especially the friction→framework→strategy mappings and the evidence tiers,
which are locked down by the `:core` tests (keep them green: `gradle test` must stay 18/18).

On-device verification uses the checklist in `README.md`.
