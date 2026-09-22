# Vollständiger Quellimport noch erforderlich

Dieser Feature-Branch enthält zunächst die CI-/Release-Vorbereitung, nicht die vollständige Mod.
Der vollständige Entwicklungskandidat 0.12.3-alpha.1 wird als geprüftes Quellpaket mit Importskript geliefert.
Das Importskript verifiziert Dateiprüfsummen, kontrolliert den Ausgangscommit und überträgt den vollständigen Stand ohne Force-Push.
Es entfernt anschließend genau diese Vorbereitungsmarkierung. main bleibt unverändert.

Bis dahin sind weder ein Minecraft-Build noch eine Serverfreigabe dieses Branches behauptet.
Pure Java-Tests des lokalen Kandidaten: 2090 Foundation-Assertions (davon 2000 randomisierte Policy-Samples),
84 Timber-/Patchtests insgesamt sowie 51213 Layout-Geometriefälle. Kein Multiplayer-Test.
