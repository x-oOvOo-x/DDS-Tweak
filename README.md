# DDS Tweak

DDS Tweak is a client-side Fabric mod for practical Minecraft tweaks and optional integration with Carpet DDS Addition.

## Runtime dependencies

DDS Tweak requires:

- Fabric Loader
- Fabric API
- MaLiLib

Fabric API is an external runtime dependency and is not bundled inside the DDS Tweak JAR. Carpet DDS Addition and other supported integrations remain optional.

## Current target

- Minecraft 1.21.11
- Fabric Loader 0.19.3
- Java 21
- DDS Protocol v1

The project is client-only. Carpet DDS Addition remains authoritative for server state, permissions, fake-player inventories and actions. DDS Tweak may provide client UI and interaction enhancements when the server advertises compatible DDS protocol capabilities.

## Beta publication

The `beta/0.1.0-beta.1` branch is a separate Minecraft 1.21.11 Beta candidate for the initial Modrinth review. It excludes the Happy Ghast riding speed modifier; the development `main` branch retains that feature. Do not merge the Beta branch into `main`.

Publication copy, version notes and checklist: `docs/publishing/BETA_0.1.0.md`.

## Licensing and upstream material

DDS Tweak's project metadata declares LGPL-3.0-only; see `LICENSE`.
The optional CozyUI+-derived resource pack is GPL-3.0; see its embedded license and notice.
Better Snow Coverage-derived portions are credited in `THIRD_PARTY_NOTICES.md` (MIT).

An accessible corresponding-source distribution must accompany public binary distribution as required by the applicable licenses. A private development repository URL is not a public source link.

## Development

- Architecture: `docs/ARCHITECTURE.md`
- Development workflow: `docs/DEVELOPMENT_WORKFLOW.md`
- Build instructions: `BUILDING.md`
- Release workflow: `docs/RELEASE_WORKFLOW.md`

The canonical Java package root is `ddstweak` and the Fabric mod id is `dds-tweak`.
