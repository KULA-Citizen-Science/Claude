# Anagramm-Werkstatt

Eine kleine, komplett offline nutzbare Android-App, mit der man aus einem eingegebenen Begriff oder
Namen **Anagramme bildet, indem man die einzelnen Buchstaben per Drag & Drop hin- und herschiebt und
neu kombiniert**. Die Oberfläche besteht aus zwei Bereichen: oben das **Ablageboard** als Vorrat der
noch freien Buchstaben, darunter die **Werkbank**, auf der das Anagramm zusammengesetzt wird. Nach dem
Laden liegen alle Buchstaben im Vorrat; von dort zieht man sie auf die Werkbank und ordnet sie
beliebig um. Mit **Leerzeichen** lässt sich das Ergebnis in mehrere Wörter trennen, und gelungene
Anagramme werden lokal unter „Meine Anagramme“ gespeichert.

## Funktionen

- **Buchstaben laden** – der eingegebene Begriff/Name wird in einzelne Buchstaben-Kacheln zerlegt und
  ins Ablageboard gelegt (Leerzeichen, Ziffern und Satzzeichen werden ignoriert, Umlaute und ß bleiben
  erhalten). Die Anzeige erfolgt durchgehend in Großbuchstaben.
- **Ablageboard (oben) & Werkbank (unten)** – eine Kachel lässt sich per Drag innerhalb eines Bereichs
  umsortieren **oder** in den anderen Bereich ziehen; die übrigen Kacheln gleiten dabei sanft zur
  Seite. **Tippen** auf einen Buchstaben schickt ihn schnell in den jeweils anderen Bereich.
- **Leerzeichen einfügen** – „+ Leerzeichen“ setzt auf der Werkbank ein Trennzeichen, das sich wie
  eine Kachel verschieben lässt. Zum Entfernen zieht man es ins Ablageboard; ein Tipp darauf bewirkt
  bewusst nichts, damit ein gesetzter Wortabstand nicht versehentlich verloren geht. So entstehen
  mehrwortige Anagramme (z. B. „ANNA LENA“ → „ALAN ENNA“).
- **Mischen** – ordnet die Buchstaben auf der Werkbank zufällig neu an (und vermeidet dabei nach
  Möglichkeit die gerade gezeigte Reihenfolge).
- **Zurücksetzen** – legt alle Buchstaben zurück ins Ablageboard und leert Werkbank und Leerzeichen.
- **Statusanzeige** – ein Feld oben zeigt das aktuell gelegte Wort und ob es ein echtes *Anagramm*
  (alle Buchstaben auf der Werkbank, andere Reihenfolge), noch das *Original* oder *unvollständig* ist
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
      Kachel pro Buchstabe im Ablageboard (oben), die Werkbank darunter ist leer; Leerzeichen der
      Eingabe erzeugen keine Kachel, und alle Kacheln zeigen Großbuchstaben.
- [ ] Eine Kachel aus dem Ablageboard auf die Werkbank ziehen (und zurück) — der Übergang zwischen den
      beiden Bereichen funktioniert. Ein Tipp auf einen Buchstaben schickt ihn in den anderen Bereich.
- [ ] Eine Kachel innerhalb der Werkbank ziehen; die übrigen Buchstaben rücken zur Seite, beim
      Loslassen sitzt der Buchstabe an der neuen Position, und das Wort oben aktualisiert sich.
- [ ] „+ Leerzeichen“ tippen — auf der Werkbank erscheint ein Trennzeichen; es lässt sich verschieben,
      ein Tipp darauf bewirkt nichts, und ins Ablageboard gezogen verschwindet es. Ergebnis z. B.
      „ALAN ENNA“.
- [ ] Solange noch Buchstaben im Ablageboard liegen, zeigt die Statuszeile „Unvollständig“; erst wenn
      alle Buchstaben auf der Werkbank sind und die Reihenfolge abweicht, steht dort „Anagramm“.
- [ ] „Mischen“ ordnet die Werkbank zufällig neu; „Zurücksetzen“ legt alles zurück ins Ablageboard.
- [ ] Bei einem vollständigen Anagramm „Speichern“ tippen — der Eintrag erscheint unter „Meine
      Anagramme“ und ist nach dem Neustart der App noch da. „Löschen“ entfernt ihn wieder.
- [ ] Ein längeres Wort laden und prüfen, dass die Kacheln auf mehrere Zeilen umbrechen und sich auch
      dann sauber ziehen lassen.
