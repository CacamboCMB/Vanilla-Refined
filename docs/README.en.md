# Vanilla Refined — by Cacambo

**0.12.3-alpha.1 is a source candidate, not a stable or multiplayer-approved release.**
See `IMPLEMENTATION_STATUS.md` for performed versus pending checks.

Five gameplay modules (Timber, Farmer, Inventory Sort, Gravestone and Tool Swap)
execute on the logical server. The authenticated connection determines the player
UUID. Personal preferences cannot modify server policy. Policies support PERSONAL,
SERVER_LOCKED and DISABLED, per-boolean locks and choice allowlists.
Client-only Shulker Preview, Mouse Wheel input, Zoom, Notes and Minimap stay local.
The same target distribution is intended for client and dedicated server; a server
must not initialize client entrypoints or client mixins.

Build with Java 25, Gradle 9.6.0 and Python 3.10+: `gradle clean build assembleBundle`.
Select a reviewed target with `-Pminecraft_version=26.2` or `26.3`. One runtime JAR
is produced per target. Minecraft and Fabric API are separate dependencies. Unknown
future versions are not validated merely because metadata permits them.

The CI pipeline runs a target matrix, pure production-code tests, source/translation
checks, aggregate JAR checks and a temporary production server startup/shutdown.
The last step requires the maintainer's explicit EULA consent via the repository
variable `MINECRAFT_EULA_ACCEPTED=true`. It does not simulate player gameplay.
Matching alpha/beta/rc tags create a draft prerelease only after validation passes.
Stable publication remains blocked pending reviewed two-client acceptance.

German and English UI resources are included. Other selected languages use English
mod fallback; user-written notes and waypoint names are preserved. See `SERVER.md`
for policy format, persistence and protection-integration limitations.
