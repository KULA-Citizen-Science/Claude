# Anagramm-Werkstatt

Eine kleine, komplett offline nutzbare Android-App, mit der man aus einem eingegebenen Begriff oder
Namen **Anagramme bildet, indem man die einzelnen Buchstaben per Drag & Drop hin- und herschiebt und
neu kombiniert**. Die Buchstaben verteilen sich auf zwei Bereiche – eine **Werkbank**, auf der das
Anagramm zusammengebaut wird, und ein **Ablageboard**, auf dem man Buchstaben zwischenparken kann.
Buchstaben lassen sich zwischen beiden Bereichen ziehen (oder per Tippen schnell hinüberschicken), und
mit **Leerzeichen** kann das Anagramm in mehrere Wörter getrennt werden. Gelungene Anagramme lassen
sich lokal unter „Meine Anagramme“ speichern.

## Funktionen

- **Buchstaben laden** – der eingegebene Begriff/Name wird in einzelne Buchstaben-Kacheln zerlegt und
  auf die Werkbank gelegt (Leerzeichen, Ziffern und Satzzeichen werden ignoriert, Umlaute und ß
  bleiben erhalten).
- **Werkbank & Ablageboard** – zwei getrennte Bereiche. Auf der Werkbank wird gebaut, auf dem
  Ablageboard werden gerade nicht benötigte Buchstaben abgelegt. Eine Kachel lässt sich per Drag
  innerhalb eines Bereichs umsortieren **oder** in den anderen Bereich ziehen; die übrigen Kacheln
  gleiten dabei sanft zur Seite. **Tippen** auf eine Kachel schickt sie schnell in den jeweils anderen
  Bereich.
- **Leerzeichen einfügen** – „+ Leerzeichen“ fügt auf der Werkbank ein Trennzeichen ein, das sich wie
  eine Kachel verschieben lässt; ein Tipp darauf entfernt es wieder. So entstehen mehrwortige
  Anagramme (z. B. „Anna Lena“ → „Alan Enna“).
- **Mischen** – ordnet die Buchstaben auf der Werkbank zufällig neu an (und vermeidet dabei nach
  Möglichkeit die gerade gezeigte Reihenfolge).
- **Zurücksetzen** – legt alle Buchstaben in der ursprünglichen Reihenfolge zurück auf die Werkbank
  und leert Ablageboard und Leerzeichen.
- **Statusanzeige** – ein Feld oben zeigt das aktuell gelegte Wort und ob es ein echtes *Anagramm*
  (alle Buchstaben verbraucht, andere Reihenfolge), noch das *Original* oder *unvollständig* ist
  (es liegen noch Buchstaben im Ablageboard).
- **Speichern** – ein vollständiges Anagramm lässt sich in die lokale Liste „Meine Anagramme“
  übernehmen.

## Projektstruktur

- `core/` — reines Kotlin/JVM-Modul ohne Android-Abhängigkeit. Die gesamte Anagramm-Logik liegt hier
  (`Anagrams`): Buchstaben aus der Eingabe extrahieren, Kacheln erzeugen, das Wort buchstabieren,
  Anagramm-Signatur/-Prüfung, Umsortieren und deterministisches Mischen — mit JUnit-Tests.
- `app/` — die Android-App: Jetpack-Compose-UI (`AnagramScreen`), das interaktive Zwei-Zonen-Brett
  mit zonenübergreifendem Drag & Drop (`WorkBoard` — Werkbank + Ablageboard, Leerzeichen), das
  `AnagramViewModel` und die lokale Persistenz der gespeicherten Anagramme über `SharedPreferences`
  (`SavedAnagramStore`). Keine Datenbank, keine Netzwerk- oder sonstigen Laufzeitberechtigungen.

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
      Kachel pro Buchstabe auf der Werkbank; Leerzeichen der Eingabe erzeugen keine Kachel.
- [ ] Eine Kachel innerhalb der Werkbank ziehen; die übrigen Buchstaben rücken zur Seite, beim
      Loslassen sitzt der Buchstabe an der neuen Position, und das Wort oben aktualisiert sich.
- [ ] Eine Kachel von der Werkbank auf das Ablageboard ziehen (und zurück) — der Übergang zwischen den
      beiden Bereichen funktioniert. Ein Tipp auf eine Kachel schickt sie in den jeweils anderen Bereich.
- [ ] „+ Leerzeichen“ tippen — auf der Werkbank erscheint ein Trennzeichen; es lässt sich verschieben,
      und ein Tipp darauf entfernt es wieder. Ergebnis z. B. „Alan Enna“.
- [ ] Solange noch Buchstaben im Ablageboard liegen, zeigt die Statuszeile „Unvollständig“; erst wenn
      alle Buchstaben auf der Werkbank sind und die Reihenfolge abweicht, steht dort „Anagramm“.
- [ ] „Mischen“ ordnet die Werkbank zufällig neu; „Zurücksetzen“ legt alles wieder auf die Werkbank.
- [ ] Bei einem vollständigen Anagramm „Speichern“ tippen — der Eintrag erscheint unter „Meine
      Anagramme“ und ist nach dem Neustart der App noch da. „Löschen“ entfernt ihn wieder.
- [ ] Ein längeres Wort laden und prüfen, dass die Kacheln auf mehrere Zeilen umbrechen und sich auch
      dann sauber ziehen lassen.
