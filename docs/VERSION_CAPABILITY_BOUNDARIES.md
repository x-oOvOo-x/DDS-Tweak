# Version Capability Boundaries

DDS Tweak supports many Minecraft generations from one canonical source tree. A successful compile is not enough: each target JAR must contain only features that the target Minecraft version can actually implement.

## Rule

A feature with a hard Minecraft-version capability boundary must be registered in `config/version-capability-boundaries.json`.

For every target below the feature's minimum supported version:

1. **No configuration entry** — the config field and option must not be compiled into the target.
2. **No feature implementation classes** — the feature package must be excluded from that target's Java source set.
3. **No Mixin classes** — feature Mixins must be excluded from the target source set.
4. **No Mixin registration** — the processed `dds-tweak.mixins.json` must not reference the feature.
5. **No final-JAR residue** — the release JAR must not contain classes under the registered feature/Mixin prefixes.
6. **No empty compatibility stubs as a substitute** — an empty class that only makes an unsupported target compile does not count as a clean boundary.

For every target at or above the minimum supported version, the inverse is required: the registered config key, Java packages and Mixins must be present. This prevents a capability from silently disappearing on a version where it is supposed to work.

Shared canonical source files may contain ReplayMod preprocessor branches, but unsupported target projects must exclude the complete feature package before compilation. The boundary is therefore defined by the target artifact, not by whether a shared source file happens to exist in the repository.

## What is not a capability boundary

A feature that is valid on both old and new Minecraft versions but needs different hooks is an **implementation-generation split**, not an unavailable feature.

Example: Helpful Hitboxes uses different outline hooks before and after Minecraft 1.16. Both versions implement the feature, so the build should select exactly one implementation rather than remove the whole feature.

## Current hard boundaries

| Feature | Minimum Minecraft | Unsupported targets must omit |
| --- | --- | --- |
| `chatTooltipOcclusion` | 1.15.2 | config, feature package, Mixin package and Mixin registrations; 1.14.4 lacks the command-suggestion overlay used for occlusion |
| `compactStatusEffects` | 1.21.1 | config, feature package, Mixin package and Mixin registrations; 1.21.1 and 1.21.2+ renderer generations are selected separately |
| `noCommandConfirm` | 1.21.6 | config, feature package, Mixin package and Mixin registration |
| `disableEndFlash` | 1.21.9 | config, feature package, Mixin package and Mixin registrations |
| `betterSnowCoverage` | 1.21.11 | config and feature package; rendering uses Fabric model/renderer APIs instead of renderer-specific Mixins |

## Adding a new version-limited feature

Before implementation:

1. Determine the first Minecraft version that exposes the required game mechanism/API.
2. Add one entry to `config/version-capability-boundaries.json`.
3. Gate the feature's config field/option with the same ReplayMod version condition.
4. Put implementation and Mixins under dedicated package prefixes listed by the contract.
5. Run `buildAndGather`.

`buildAndGather` is responsible for checking both sides of every registered boundary against the processed Mixin config and the final release JAR. A future feature such as a UI or render tweak that cannot exist on older targets must follow this contract instead of introducing no-op compatibility classes.
