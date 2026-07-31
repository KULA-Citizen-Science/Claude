# Anagramm-Werkstatt

Eine kleine, komplett offline nutzbare Android-App, mit der man aus einem eingegebenen Begriff oder
Namen **Anagramme bildet, indem man die einzelnen Buchstaben per Drag & Drop hin- und herschiebt und
neu kombiniert**. Man tippt ein Wort ein, die Buchstaben werden zu verschiebbaren Kacheln, und beim
Ziehen einer Kachel rücken die anderen zur Seite und das oben angezeigte Wort aktualisiert sich live.
Gelungene Anagramme lassen sich lokal unter „Meine Anagramme“ speichern.

## Funktionen

- **Buchstaben laden** – der eingegebene Begriff/Name wird in einzelne Buchstaben-Kacheln zerlegt
  (Leerzeichen, Ziffern und Satzzeichen werden ignoriert, Umlaute und ß bleiben erhalten).
- **Hin- und herschieben** – jede Kachel lässt sich ziehen; die übrige Reihe gleitet sanft mit, sodass
  eine Lücke an der Zielposition entsteht. Beim Loslassen rastet der Buchstabe dort ein.
- **Mischen** – ordnet alle Buchstaben zufällig neu an (und vermeidet dabei nach Möglichkeit die
  gerade gezeigte Reihenfolge).
- **Zurücksetzen** – stellt die ursprüngliche Reihenfolge des geladenen Begriffs wieder her.
- **Statusanzeige** – ein Feld oben zeigt das aktuell gelegte Wort und ob es ein echtes *Anagramm*
  (gleiche Buchstaben, andere Reihenfolge) oder noch das *Original* ist.
- **Speichern** – ein echtes Anagramm lässt sich in die lokale Liste „Meine Anagramme“ übernehmen.

## Projektstruktur

- `core/` — reines Kotlin/JVM-Modul ohne Android-Abhängigkeit. Die gesamte Anagramm-Logik liegt hier
  (`Anagrams`): Buchstaben aus der Eingabe extrahieren, Kacheln erzeugen, das Wort buchstabieren,
  Anagramm-Signatur/-Prüfung, Umsortieren und deterministisches Mischen — mit JUnit-Tests.
- `app/` — die Android-App: Jetpack-Compose-UI (`AnagramScreen`), das interaktive Kachelbrett mit
  Drag-&-Drop-Neuordnung (`LetterTilesBoard`), das `AnagramViewModel` und die lokale Persistenz der
  gespeicherten Anagramme über `SharedPreferences` (`SavedAnagramStore`). Keine Datenbank, keine
  Netzwerk- oder sonstigen Laufzeitberechtigungen.

## Bauen

Voraussetzung ist das Android SDK (Platform 34, Build-Tools 34.0.0) und JDK 17. In einer Cloud-Session
ohne Android Studio installiert man die Command-line-Tools und das SDK so:

```bash
export ANDROID_HOME=$HOME/android-sdk
mkdir -p $ANDROID_HOME/cmdline-tools
curl -fLo /tmp/clt.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q /tmp/clt.zip -d $ANDROID_HOME/cmdline-tools
mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
echo "sdk.dir=$ANDROID_HOME" > local.properties   # ist bereits in .gitignore
```

Anschließend:

```bash
./gradlew :core:test          # reine Logik-Tests (JVM)
./gradlew assembleDebug       # baut app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug        # auf ein angeschlossenes Gerät / einen Emulator installieren
```

Der Gradle-Wrapper ist auf 8.9 gepinnt; ein vorinstalliertes `gradle` (8.7+) tut es ebenfalls.

## Manuelle Prüfung auf dem Gerät

Die eigentliche Drag-Interaktion braucht einen Touchscreen und lässt sich nur von Hand prüfen:

- [ ] Einen Begriff (z. B. „Anna Lena“) eingeben und „Buchstaben laden“ tippen — es erscheint eine
      Kachel pro Buchstabe; Leerzeichen erzeugen keine Kachel.
- [ ] Eine Kachel ziehen und an eine andere Stelle schieben; die übrigen Buchstaben rücken zur Seite,
      und beim Loslassen sitzt der Buchstabe an der neuen Position. Das Wort oben aktualisiert sich.
- [ ] Sobald die Reihenfolge vom Original abweicht, wechselt die Anzeige von „Original“ auf „Anagramm“.
- [ ] „Mischen“ ordnet die Buchstaben zufällig neu; „Zurücksetzen“ stellt das Ausgangswort wieder her.
- [ ] Bei einem echten Anagramm „Speichern“ tippen — der Eintrag erscheint unter „Meine Anagramme“ und
      ist nach dem Neustart der App noch da. „Löschen“ entfernt ihn wieder.
- [ ] Ein längeres Wort laden und prüfen, dass die Kacheln auf mehrere Zeilen umbrechen und sich auch
      dann sauber ziehen lassen.
