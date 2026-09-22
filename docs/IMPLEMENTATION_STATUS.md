# Implementierungsstand / ehrliche Prüfgrenzen

Quelle: wiederhergestellter Nutzerexport + kumulativer Patch 0.11.6-alpha.
Der abgebrochene Vorgängerversuch mit angeblich 51 Tests war nicht als Quellstand
wiederauffindbar. Diese Implementierung wurde neu erstellt und separat geprüft.

| Arbeitspaket | Stand im Quellcode | Noch erforderlicher Nachweis |
|---|---|---|
| 0.11.7 | Gemeinsame UI übernommen, servergesperrte Felder, 655 DE/EN-Schlüssel geprüft | Minecraft-Screenshots/Sprachwechsel/GUI-Skalen |
| 0.11.8 | Keine Client-Imports in main; clientseitige Entry-/Mixin-Deklarationen getrennt | Echter Client-/Dedicated-Start |
| 0.11.9 | Payloads, validierte persönliche Werte, Policy-Modi, UUID-Storage, Reload, Grenzen | Zwei-Client-Netzwerk-/Reconnect-Abnahme |
| 0.12.0 | Timber/Farmer/Sort/Grabstein/Tool Swap serverseitig integriert | Kompletter Minecraft-Build und Spieltests |
| 0.12.1 | Eine aggregierte Ziel-JAR aus elf Modulen, keine Minecraft/Fabric-Bibliotheken eingebettet | Prüfung der tatsächlich gebauten JAR |
| 0.12.2 | CI-Konfiguration und sicherer Repository-Import vorbereitet | Vollständigen Quellstand übertragen und CI ausführen |
| 0.12.3 | Prüfgebundene Alpha-Release-Entwürfe, SHA-256-Zielmanifeste | Erfolgreiche Zielmatrix + Serverstart; manuelle Veröffentlichung |

## Lokal ausgeführt

- Echte pure Java-Produktionsklassen: **2.090 Foundation-Assertions**, darin
  **2.000 randomisierte Policy-Samples** (nicht 2.000 Multiplayer-Simulationen).
- Bestehende Timber-/Transaktionstests: **84 bestanden**, einschließlich der
  39 Patchtests. Die 39 werden nicht zusätzlich zu den 84 addiert.
- **51.213 Layout-Geometriefälle**; keine gerenderten Ingame-Screenshots.
- Strukturprüfung: elf Module, sechs gemeinsame und sieben Client-Entry-Points,
  Client-Mixin-Abgrenzung, **655 Übersetzungsschlüssel je Sprache**.

## Hier nicht ausgeführt

Die Umgebung enthält JDK 21 für reine Tests, aber keine herunterladbare vollständige
Minecraft-/Fabric-/JDK-25-Toolchain. Weder ein kompletter 26.2/26.3-Build noch ein
echter Serverstart oder Multiplayer-Spieltest wird deshalb als bestanden markiert.
Die CI ist so eingerichtet, dass fehlende Voraussetzungen keinen grünen Release
vortäuschen. JDK-21-Syntax-/Logikprüfungen ersetzen keine Java-25-/Minecraft-Kompilierung.

`compatibility/targets.json` kennzeichnet beide Zielprofile weiterhin als Kandidaten.
Die Versionsbereiche sind bewusst nicht gleichbedeutend mit Freigaben. Ein einzelner
Dist-JAR-Build, der unter zwei Versionen läuft, wurde ebenfalls nicht nachgewiesen.
