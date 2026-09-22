# Quellimport und erste echte CI-Abnahme

Diese Lieferung ist **kein Minecraft-Installer** und enthält noch keine kompilierte Mod-JAR.
Die vorhandene funktionierende Installation bleibt unverändert.

## Bereits auf GitHub

Repository: CacamboCMB/Vanilla-Refined (öffentlich).
Branch: feature/server-foundation-0.12.3.
Vorbereitungscommit: `b352f2862946920fce401efc396a8dbb0208f44f`.

Dort liegen vor dem Import nur README, eine Quellimport-Markierung und die
CI-/Release-Workflows. Der komplette Java-Quellstand ist zu diesem Zeitpunkt
noch **nicht** im Repository. main wurde nicht verändert.

## Einmaliger vollständiger Import

Voraussetzungen: Git für Windows im PATH, Internet und ein GitHub-Konto mit
Schreibzugriff auf genau dieses Repository. Git darf zur eigenen Anmeldung
auffordern. Keine Tokens, Passwörter oder Anmeldedaten in Chat/Quelltext eintragen.
Für den Import sind weder Java, Gradle noch Python erforderlich.

Die separat gelieferte Datei `VanillaRefined-0.12.3-alpha.1-GitHub-Import.ps1`
enthält dieses vollständige Quellpaket. Sie verifiziert dessen SHA-256, entpackt
es in einen neuen Arbeitsordner und ruft `tools/Publish-Sources.ps1` auf.
Alternativ kann dieses Skript direkt aus dem entpackten ZIP gestartet werden:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\Publish-Sources.ps1
```

Das Skript prüft alle im SOURCE-MANIFEST aufgeführten Dateien, klont einen neuen
Arbeitsordner und verlangt den oben genannten Ausgangscommit. Bei zwischenzeitlichen
Änderungen am Remote-Branch bricht es ab, statt diese zu überschreiben. Es überträgt
die Dateien mit einem normalen Commit/Push auf den Feature-Branch, nie per Force-Push.
Nur die ausdrücklich benannte Vorbereitungsmarkierung `SOURCE_IMPORT_REQUIRED.md`
wird entfernt. main wird nicht verändert, kein Release erstellt, keine Mod installiert.
Mit `-NoPush` entsteht ausschließlich ein lokaler Commit zur Prüfung.

Nach erfolgreichem Push: https://github.com/CacamboCMB/Vanilla-Refined/actions

## Server-Starttest und Minecraft-EULA

Der Produktions-Server-Starttest wird nur nach ausdrücklicher eigener Zustimmung
zur Minecraft-EULA ausgeführt. Nach dieser Zustimmung in GitHub unter
Settings → Secrets and variables → Actions → Variables die Repository-Variable
`MINECRAFT_EULA_ACCEPTED` mit dem Wert `true` anlegen.
Die Variable muss vor dem CI-Lauf gesetzt sein oder der Lauf wird anschließend
neu gestartet. Ohne Zustimmung bleibt dieser Test gesperrt; das ist kein Beleg
für einen Java-Fehler. Die Pipeline akzeptiert die EULA nicht stillschweigend.

Die Build-Matrix prüft Minecraft 26.2 und 26.3 separat. Diagnose-Artefakte werden
auch bei Fehlern gesichert. `candidate-*`-Artefakte nach dem Kompilieren sind
noch keine Freigabe. Erst nach erfolgreichem realem Serverstart werden
`validated-*`-Artefakte für Alpha-Release-Entwürfe erzeugt.

Eine Zwei-Spieler-Abnahme inklusive Inventar, Tod/Respawn, Regeln, Reconnect,
Schutzmod-Anbindung und UI bleibt gemäß ACCEPTANCE.md erforderlich.
Bis dahin keine stabile Version veröffentlichen oder produktive Welten umstellen.
