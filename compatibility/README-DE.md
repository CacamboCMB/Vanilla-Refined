# Kompatibilitätsprofile

`targets.json` trennt feste Build-Abhängigkeiten von offenen Laufzeitbedingungen.
Die Minecraft-Profile 26.2 und 26.3 sind für 0.12.3-alpha.1 **nicht freigegebene
Prüfziele**. Lokale Prüfungen sind in `LOCAL-VALIDATION.json` protokolliert.

Eine akzeptierte Versionsbedingung beweist weder ABI-Kompatibilität noch korrektes
Gameplay. CI muss vollständigen Build, echte Distributions-JAR und Produktions-
Serverstart prüfen. Die Mehrspieler-Abnahme erfolgt zusätzlich nach
`docs/ACCEPTANCE.md`. Unterschiedliche Buildziele erzeugen getrennte Artefakte;
die Lauffähigkeit derselben JAR auf beiden Versionen wird hier nicht behauptet.

Diese Lieferung ist Quellcode, kein Minecraft-Installer. Keine Originalwelten,
Notizen, Kartendaten oder installierten Mods werden verändert.
