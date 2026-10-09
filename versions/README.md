# DDS Tweak version nodes

Each `versions/<mc>/gradle.properties` owns that target's Minecraft, Fabric API, MaLiLib and Java metadata.

Compatibility priority:

1. shared `src/main/java`;
2. narrow preprocess directives for small API/signature differences;
3. shared `platform/minecraft` or `platform/malilib` facade once the same difference repeats;
4. `versions/<mc>/src/main/java` overlay for large version-specific Mixin, screen, input, rendering or third-party-compat implementations.

Do not copy unchanged shared classes into a version overlay. `versions/mainProject` is the preprocess source anchor and is independent from `coreVersion`.
