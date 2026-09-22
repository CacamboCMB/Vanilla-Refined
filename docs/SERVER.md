# Serverregeln und persönliche Einstellungen

## Installation / Zuständigkeit

Auf dem Fabric-Server: die geprüfte Vanilla-Refined-JAR des Minecraft-Ziels und
passende Fabric API. Für persönliches Menü, Einstellungen und lokale Funktionen
benötigt der Spieler dieselbe Protokollgeneration im Client. Ein Fabric-freier
Client kann auf einem ansonsten Vanilla-kompatiblen Fabric-Server spielen; dies
hängt auch von dessen anderen Mods ab. Vanilla Refined fügt keine neuen Blöcke,
Items oder Registry-Einträge hinzu. Der reale Vanilla-Client-Beitritt steht auf
der Abnahmeliste und ist noch kein nachgewiesenes Testergebnis.

| Modul | Server führt aus | Individuell |
|---|---|---|
| Holzfällen | Ja | Persönliche Äxte/Optionen innerhalb der Policy |
| Farmer | Ja | Pflanzen, Aktivierung, Bedienung innerhalb der Policy |
| Inventarsortierung | Ja | Sortierart/Optionen; Server liest eigenes Menü/Inventar |
| Werkzeugwechsel | Ja | Qualität, Verzauberung, Automatik innerhalb der Policy |
| Grabstein | Ja | Standard Dedicated: global vorgegeben; umstellbar |
| Shulker, Mausrad, Zoom, Notizen, Minimap | Keine eigene Weltlogik | Lokal; Mausrad nutzt normale, servergeprüfte Minecraft-Menüaktionen |

## Dateien und Rechte

Dedicated-Policy: `config/vanilla_refined/server-policy.properties`.
Singleplayer/LAN-Policy: `<Welt>/vanilla_refined/server-policy.properties`.
Persönliche Serverkopien: `<Welt>/vanilla_refined/players/<UUID>.settings`.
Client-Konfiguration bleibt in den bisherigen OwnMods-Dateien.

Die Policy wird beim ersten Start erzeugt, danach alle 100 Server-Ticks auf eine
geänderte Datei geprüft. Nur Dateisystemzugriff des Betreibers kann globale Regeln
ändern. Es gibt **kein** Client-Paket zum Ändern globaler Regeln und keinen
ungesicherten Ingame-Adminbefehl. Ungültige Änderungen werden verworfen; die letzte
gültige Policy bleibt aktiv. Beim Start mit einer ungültigen Policy sind alle
serverseitigen Funktionen ausgeschaltet, ohne die Datei zu überschreiben.

Das vollständige Dateischema wird aus den registrierten Modulen erzeugt.
`server-policy.example.properties` wurde aus den tatsächlichen Java-Schemas generiert
und mit demselben Decoder validiert. Bei Axtschlüsseln ist der Doppelpunkt in
Properties-Dateien als `\:` escaped; diese Schreibweise beibehalten. Beispiel
zum Bearbeiten einzelner Zeilen in der generierten Datei:

```properties
schemaVersion=1
ownmods_timber.mode=PERSONAL
ownmods_timber.rule.axe.minecraft\:diamond_axe=FORCE_FALSE
ownmods_timber.rule.axe.minecraft\:netherite_axe=FORCE_FALSE
ownmods_timber.rule.consume_saplings=FORCE_TRUE
ownmods_toolswap.allowed.policy=best,same
ownmods_gravestone.mode=SERVER_LOCKED
ownmods_gravestone.server.bool.enabled=true
```

Die vollständige erzeugte Datei enthält alle weiteren Optionen. Fehlende Werte
werden nicht als unsichere Freigabe interpretiert; ungültige Schlüssel werden abgelehnt.

PERSONAL: Spieler entscheidet, soweit Boolean-Regel/Choice-Whitelist es erlaubt.
SERVER_LOCKED: Die `.server.bool.*` und `.server.choice.*`-Werte gelten für alle.
DISABLED: Modul aus, unabhängig von persönlichen Einstellungen.
Boolean-Regeln: PERSONAL, FORCE_TRUE, FORCE_FALSE. Ein gesperrtes Modul wird nicht
durch ein persönliches `enabled=true` entsperrt.

Default Dedicated: Timber/Farmer/Inventarsortierung/Werkzeugwechsel PERSONAL;
Grabstein SERVER_LOCKED. Ressourcenverbrauch fürs Nachpflanzen von Bäumen wird
serverseitig erzwungen. Die Client-Defaults bleiben erhalten.
Default Singleplayer: persönliche Regeln, auch beim Grabstein.

## Protokoll / Sicherheitsgrenzen

Server → Client: Protokollversion, Revisionsnummer, Fähigkeiten, Serverregeln und
effektiv gültige Werte ausschließlich des Empfängers.
Client → Server: eigene Optionswerte, Revisions- und monotone Anfrage-ID. Die UUID
kommt **nicht** aus dem Paket. Unbekannte Module, Optionen, Choice-Tokens, doppelte
Schlüssel, übergroße Nachrichten und ungültige Bool-Werte werden abgelehnt.

Sortieranfragen enthalten nur Menü-ID, Menürevision, Auswahl Inventar/Kiste und
Protokoll-/Anfrage-ID. Der Server prüft Lebenszustand, Zuschauerstatus, geöffnetes
Menü, Gültigkeit, Cursor-Stack, unterstützten Menütyp und serverseitige Policy.
Es werden keine vom Client behaupteten Item-Stacks, Kontostände oder Slotinhalte
übernommen. Inventaraktionen besitzen Snapshot-/Rollback-Prüfungen.

Nach Disconnect werden aktive Sitzungen verworfen. Gespeicherte persönliche Werte
aktivieren einen unmodifizierten Client beim nächsten Beitritt nicht automatisch.
Neue persönliche Automatisierung erfordert einen erfolgreichen Handshake.

## Schutzsysteme / Grenzen

Vanilla-Berechtigungen, Weltgrenze und geladene Chunks werden geprüft. Der
`WorldMutationGuard` bietet einen Veto-Hook für Claim-/Regionsmods. **Das ist keine
automatische Kompatibilitätsgarantie mit beliebigen Schutzmods.** Gerade Grabstein-
und Nachpflanz-Platzierungen brauchen einen passenden Hook plus Integrationstest.
Bei verweigerter Grabsteinplatzierung bleibt die Vanilla-Todesbehandlung aktiv.
Andere Spieler dürfen nicht stillschweigend durch globale Clientdateien beeinflusst
werden. Zwei-Client-, Konflikt-, Tod-/Respawn- und Regionsmod-Tests sind vor einer
Serverfreigabe Pflicht.
