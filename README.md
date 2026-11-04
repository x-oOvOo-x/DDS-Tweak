# DDS Tweak

DDS Tweak is a client-side Fabric mod for practical Minecraft tweaks and optional integration with Carpet DDS Addition.

## Runtime dependencies

DDS Tweak requires:

- Fabric Loader
- Fabric API
- MaLiLib

Fabric API is an external runtime dependency and is not bundled inside the DDS Tweak JAR. Carpet DDS Addition and other supported integrations remain optional.

## Current target

- Core development target: Minecraft 1.21.11
- Active build targets: 21 versions, from Minecraft 1.14.4 through 26.3; not every feature is available on every version (see [version capabilities](docs/VERSION_CAPABILITY_BOUNDARIES.md)).
- Fabric Loader, MaLiLib and Java requirements depend on the selected Minecraft target; refer to `versions/<minecraft>/gradle.properties` and [BUILDING.md](BUILDING.md). For example, 1.21.11 requires Java 21, while 26.3 requires Java 25 and Fabric Loader >=0.19.5.
- DDS Protocol v1

The project is client-only. Carpet DDS Addition remains authoritative for server state, permissions, fake-player inventories and actions. DDS Tweak may provide client UI and interaction enhancements when the server advertises compatible DDS protocol capabilities.

## Mod Menu integration

Mod Menu is **optional**. When it is installed for the current Minecraft version,
open **Mods → DDS Tweak → Configure** to open the existing MaLiLib settings screen.
Closing the screen returns to Mod Menu. With Mod Menu absent, DDS Tweak still
starts normally and its existing MaLiLib config selector and `Left Alt + V`
configuration shortcut remain available. No server mod or CDA integration is needed.

## Client configuration

DDS Tweak stores its local client settings under `config/dds-tweak/` in the Minecraft instance. Existing root-level DT configs are copied on first use and preserved for rollback. See [Client configuration](docs/CLIENT_CONFIGURATION.md).

## Development

- Tab fake-player groups: `docs/features/tab-fake-player-groups.md`
- Build instructions: `BUILDING.md`

The canonical Java package root is `ddstweak` and the Fabric mod id is `dds-tweak`.
