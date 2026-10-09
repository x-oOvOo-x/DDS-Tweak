# Building DDS Tweak

## Requirements

- JDK 21
- Gradle 9.6
- Internet access for Fabric/Minecraft dependencies

Each Minecraft target pins its Fabric API build version and runtime version requirement in `versions/<minecraft>/gradle.properties`. Development uses the complete Fabric API artifact for that target. Release JARs must not embed Fabric API modules; players install Fabric API as a normal external dependency.

The repository currently uses a pinned CI Gradle version instead of a checked-in Gradle Wrapper.

## Verify / test / build

```powershell
gradle verifyDDSTweak
gradle test
gradle build --stacktrace
```

Release JAR:

```text
build/libs/dds-tweak-v<version>-mc<minecraft>.jar
```

The `-sources.jar` is not a gameplay release artifact.

## Run client

```powershell
gradle runClient
```

## DDS integration test

Start the canonical DDS development server in another terminal:

```powershell
# Carpet-DDS-Addition-Dev
gradle :1.21.11:runServer --no-parallel
```

Then start DDS Tweak and connect to `localhost`:

```powershell
# DDS-Tweak-Dev
gradle runClient
```

For network/inventory/fake-player changes verify all four combinations: DDS server without Tweak, DDS server with Tweak, Tweak client against a server without DDS, and singleplayer with DDS + Tweak. Build success never replaces runtime validation. See `docs/DEVELOPMENT_WORKFLOW.md`.
