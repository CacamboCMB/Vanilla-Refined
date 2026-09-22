# Vanilla Refined 0.12.3-alpha.1 — Foundation candidate

Consolidated implementation candidate for UI/localization, logical-server authority,
personal preferences within server policy, client/server separation, one target
runtime JAR and validated GitHub build/release automation.

The release workflow creates a DRAFT PRERELEASE only after the entire 26.2/26.3 build
matrix, metadata/package checks and production-server smoke checks pass. Candidate
build artifacts available from CI before smoke completion are not release-approved.

This does not prove two-player gameplay, protection-mod interoperability, item-loss
safety in every Minecraft failure path, or a shared binary across future versions.
Complete docs/ACCEPTANCE.md before publishing. The suite JAR replaces the eleven
old ownmods runtime JARs; do not load both forms together. Back up world/config data
and test in a separate instance first. Fabric Loader and matching Fabric API remain
required. No world or client configuration is overwritten by this source delivery.
