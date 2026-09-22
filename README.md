# Vanilla Refined — by Cacambo

Vorbereitung für den Entwicklungskandidaten **0.12.3-alpha.1** (Arbeitspakete 0.11.7–0.12.3).

**Dieser Branch ist noch kein vollständiger Quellimport und keine veröffentlichte Mod.** Siehe `SOURCE_IMPORT_REQUIRED.md`.

Die CI-/Release-Workflows sind vorbereitet. Der vollständige Java-Quellstand einschließlich Server-Policy,
persönlicher Einstellungen, Client-/Server-Trennung, Distributionsprojekt und Tests wird mit dem
beigefügten Quellimport-Skript übertragen. `main` bleibt bis zur Prüfung unverändert.

CI ist für Minecraft 26.2 und 26.3 eingerichtet. Ein Produktions-Serverstart benötigt nach eigener
Zustimmung zur Minecraft-EULA die Repository-Variable `MINECRAFT_EULA_ACCEPTED=true`.
Release-Tags erzeugen erst nach erfolgreichen Prüfungen einen Alpha-Release-Entwurf.
Ein Starttest ersetzt keine Zwei-Spieler-Abnahme; stabile Veröffentlichung bleibt gesperrt.
