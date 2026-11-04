# Building DDS Tweak

## Requirements

- Gradle 9.6.0 **via the committed wrapper** (`gradlew.bat` on Windows); do not replace it with a system Gradle command.
- JDK 25 for the full multi-version Gradle/CI build, with the required Java toolchains for individual game targets.
- Network access to download Minecraft, Fabric, MaLiLib and other declared build dependencies.

`settings.json` defines the current 21 active build targets (Minecraft 1.14.4–26.3). Each
`versions/<minecraft>/gradle.properties` defines the matching Minecraft,
Fabric Loader, Fabric API, MaLiLib and Java runtime requirements; **there is no single
Java/Loader/MaLiLib version valid for all targets**.

Examples (verify the property files whenever preparing an installation):

| Target | Java | Fabric Loader constraint | MaLiLib |
| --- | ---: | --- | --- |
| 1.14.4 | 8 | >=0.17.0 | 0.10.0-dev.20+arne.2 |
| 1.21.11 (core) | 21 | >=0.17.0 | 0.27.20 |
| 26.3 | 25 | >=0.19.5 | 0.30.1 |

Fabric API and MaLiLib are **external runtime dependencies** and are not bundled in DT
release JARs. CDA is optional for general client features but required for applicable
CDA integrations. Always match a test instance to the requested target's dependency file.

## Verify / test / build (Windows PowerShell)

```powershell
# Targeted tests while developing
.\gradlew.bat :1.21.11:test --stacktrace

# Final aggregate build (includes root verification and all 21 active targets)
.\gradlew.bat buildAndGather --stacktrace
```

The aggregate task writes current runtime JARs into `build/releases/` after
checking processed metadata, mixins and feature/version boundaries. Do not choose
an arbitrary JAR from `build/libs/` or treat sources/dev/test-only outputs as release JARs.
A successful CI build is not Minecraft client launch or user-interaction evidence.

## Run an isolated client

```powershell
.\gradlew.bat :1.21.11:runClient
# For other versions, first verify target dependencies and use :<target>:runClient.
```

Run against a **separate test instance/world**. For CDA paired checks, start the
matching CDA development server in a separate terminal (example for 1.21.11):

```powershell
# In Carpet-DDS-Addition-Dev
.\gradlew.bat :1.21.11:runServer --no-parallel
```

Check four combinations where applicable: server without DT, DT against a server
without CDA, matched CDA + DT on a dedicated server, and integrated singleplayer.
Record exact source SHAs, MC/Fabric/MaLiLib/CDA versions, JAR SHA256, actual
observations, screenshots/logs and PASS/FAIL/BLOCKED/NOT RUN. Do not promote
compile success to runtime or paired-handoff success.

