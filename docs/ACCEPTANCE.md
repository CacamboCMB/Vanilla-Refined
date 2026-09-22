# Abnahme vor öffentlicher Nutzung

**Keine dieser Zeilen ist durch einen reinen Logiktest automatisch erledigt.**
In `validation/acceptance-template.json` Ergebnis, konkreten Build-SHA und Nachweise
festhalten. Nur mit kopierten Testwelten arbeiten; Originalinventare nicht riskieren.

## Build/Start
- [ ] 26.2: kompletter Gradle-Build, API- und Paketprüfung, echter Dedicated-Start/Stopp.
- [ ] 26.3: dieselben Prüfungen; tatsächliche erzeugte JAR, nicht nur Entwicklungs-Classpath.
- [ ] Client mit derselben Ziel-JAR starten; Ressourcen laden, Hauptmenü/Pausemenü öffnen.
- [ ] Zweiter unabhängiger Client tritt einem Dedicated Server bei, Disconnect/Reconnect.
- [ ] Client ohne Vanilla Refined tritt bei; kein unerwartetes persönliches Holzfällen.
- [ ] Client mit Mod auf Vanilla-Server: lokale Module nutzbar; Serverfunktionen klar gesperrt.

## Zwei Spieler / Richtlinien
- [ ] A nutzt andere Timber-Äxte als B; B bleibt beim Ändern von A unverändert.
- [ ] Verbotene Axt nicht nutzbar, auch bei manipuliertem lokalen Configfile.
- [ ] Persönlich aus bleibt aus; SERVER_LOCKED kann nicht vom Client überstimmt werden.
- [ ] Betreiber editiert Policy; Revisionswechsel synchronisiert beide Spieler.
- [ ] Ungültige Policyänderung beschädigt weder Datei noch letzte gültige Einstellungen.
- [ ] Falsche Revisions-/Anfrage-ID, Wiederholungen und Spam lösen keine Inventaraktion aus.
- [ ] Integrierter Server und LAN: keine parallelen Client-/Server-Mapzugriffe und kein Zustandsleck beim Weltwechsel.

## Inventar / Welt
- [ ] Sortieren erhält Menge, Verzauberungen, Namen, Komponenten und Shulker-Inhalte.
- [ ] Zwei Spieler sehen dieselbe Kiste nach aufeinanderfolgenden Sortieranfragen konsistent.
- [ ] Kein Sortieren bei geschlossenem/veraltetem Menü, Cursor-Stack oder ungültigem Zugriff.
- [ ] Farmer: Linksklick/Rechtsklick, Saatgutverbrauch, Sneak-Bypass, verbotene Pflanzen.
- [ ] Tool Swap: Bruch nach Vanilla-Durability, Verzauberung, Auswahlqualität, Disconnect.
- [ ] Grabstein: Tod, Wasser/Lava/Void, keepInventory, fehlende Ressourcen, voller Inhalt.
- [ ] Grabstein verbindet sich nie mit fremder bestehender Kiste; Fehlerpfad dupliziert nichts.
- [ ] Claim-/Spawn-Schutz für Fällen, Replant und Grabstein mit tatsächlichem Schutzmod geprüft.

## UI / Sprache
- [ ] Deutsch, Englisch und eine dritte Sprache mit englischem Mod-Fallback.
- [ ] Änderungen der Minecraft-Sprache im laufenden Client; nächste Menüöffnung korrekt.
- [ ] Pause-Button, Modübersicht, jede Einstellung und Weltkarte M auf kleinen/großen GUI-Skalen.
- [ ] Gesperrte Optionen lesbar erklärt; keine abgeschnittenen Tabs oder unerkennbaren Icons.
- [ ] Lokale Notizen/Kartendaten/Eigennamen und frühere Einstellungen erhalten.

Erst anschließend Release-Entwurf prüfen und als **Alpha/Prerelease** veröffentlichen.
Stabilfreigabe benötigt zusätzlich dokumentierte längere Nutzungs-/Fehlerpfadtests
und eine überprüfte Änderung der bewusst restriktiven Release-Regeln.
