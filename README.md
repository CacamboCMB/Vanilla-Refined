# Vanilla Refined — by Cacambo

[English](docs/README.en.md) · [Serverregeln](docs/SERVER.md) · [Prüfstand](docs/IMPLEMENTATION_STATUS.md)

**Entwicklungskandidat `0.12.3-alpha.1`, keine freigegebene stabile Version.**
Der Quellstand verbindet die Arbeitspakete 0.11.7–0.12.3. Es werden nicht rückwirkend
sieben angeblich getestete Releases erzeugt. Lokale Logiktests sind bestanden;
Minecraft-Build, Produktions-Serverstart und Zwei-Spieler-Abnahme sind davon getrennt.

## Module und Serververhalten

Holzfällen, Farmer, Inventarsortierung, Grabstein und Werkzeugwechsel verwenden
autoritative Serverlogik. Persönliche Einstellungen werden auf dem Server nach
Schema geprüft und pro authentifizierter UUID gespeichert. Der Betreiber kann
Optionen erlauben, verbieten oder vollständig vorgeben. Die Grundeinstellungen
im Client werden dadurch nicht überschrieben.

Shulker-Vorschau, Mausrad-Bedienung, Zoom, Notizen und Minimap bleiben lokale
Funktionen. Ein Dedicated Server lädt keine Benutzeroberfläche, keine Minimap
und keine Client-Mixins. Ohne Mod auf dem Server sind nur die lokalen Funktionen
verfügbar; Inventarsortierung über das neue Protokoll ist dann nicht verfügbar.
Ein unmodifizierter Client aktiviert keine persönliche Automatisierung. Globale
Grabsteinregeln gelten standardmäßig auch für ihn.

## UI und Sprache

Gemeinsame Oberfläche aus 0.11.6, jetzt mit serverabhängigen Sperren und Hinweisen.
Deutsch über `de_de`, Englisch über `en_us`; sonst englischer Mod-Fallback.
`Farmer` heißt in beiden Sprachen Farmer. Eigene Notizen und Wegpunktnamen bleiben
unverändert. Servernachrichten verwenden Übersetzungskomponenten. Ein Client ohne
Mod besitzt diese Übersetzungen nicht; für dessen Grabsteinmeldung wird deshalb
ein englischer Klartext-Fallback gesendet.

## Bauen

Java 25, Gradle 9.6.0, Python 3.10 oder neuer. Buildprofile und Fabric-Abhängigkeiten
stehen in `compatibility/targets.json`. Die vorhandenen Entwicklerwerkzeuge können
verwendet werden; ein Gradle-Wrapper-JAR ist nicht im Quell-Export enthalten.

```sh
gradle --no-daemon -Pminecraft_version=26.3 clean build assembleBundle
# Optional zweites Ziel; separate Artefakte aus derselben Quelle:
gradle --no-daemon -Pminecraft_version=26.2 clean build assembleBundle
```

Ergebnis nach erfolgreichem Build: **eine** Laufzeit-JAR je Buildziel in
`distribution/build/libs`, dazu ein separat gekennzeichnetes Quellarchiv.
Minecraft und Fabric API werden nicht eingebettet. Für Client und Dedicated
Server ist die Laufzeit-JAR desselben Ziels vorgesehen. Eine Mehrversions-Binär-
Kompatibilität wird nicht aus zwei erfolgreichen Kompilierungen abgeleitet.

## Testen und Releases

```sh
python3 tools/test_pure.py
python3 tools/verify_foundation.py
# Nur nach eigener Zustimmung zur Minecraft-EULA:
python3 tools/server_smoke.py --minecraft 26.3 --jar PATH_TO_BUILT_JAR --accept-eula
```

CI kompiliert 26.2/26.3, prüft Metadaten, reale Paketierung und Sprachressourcen.
Der Produktions-Serverstart benötigt die Repository-Variable
`MINECRAFT_EULA_ACCEPTED=true` nach Zustimmung des Betreibers. Ohne Zustimmung
werden keine EULA-Einstellungen stillschweigend geschrieben; die Release-Prüfung
bleibt gesperrt. Ein Starttest ersetzt keine Multiplayer-Abnahme.

Ein passender Alpha-Tag erzeugt nach grünen Prüfungen einen **Release-Entwurf**
mit JARs, Zielmanifesten und SHA-256-Prüfsummen. Die öffentliche Freigabe des
Entwurfs erfolgt nach der Checkliste in `docs/ACCEPTANCE.md`. Stabile Tags sind
in der Pipeline gesperrt. Kein unbekanntes Minecraft-Update wird als getestet
oder für einen späteren Stable-Installer geeignet ausgegeben.

## Installation eines später freigegebenen Builds

Zuerst auf Kopien der Welt/in getrennten Clientprofilen testen. Die elf älteren
`ownmods-*`-JARs dürfen nicht gleichzeitig mit der zusammengefassten JAR geladen
werden. Diese Quelllieferung entfernt und installiert selbst keine Minecraft-
Dateien. Konfigurationsnamen bleiben erhalten; Backups vor einer Umstellung.

MIT-Lizenz. KI-Unterstützung bei der Implementierung ist in `NOTICE.md` ausgewiesen.
Kein offizielles Minecraft-Produkt, keine Verbindung zu Mojang oder Microsoft.
