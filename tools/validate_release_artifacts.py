"""Validate every active DDS Tweak runtime JAR and write a SHA256 manifest."""

import argparse
import hashlib
import json
from pathlib import Path
from zipfile import BadZipFile, ZipFile

DEPENDENCIES = {
    "minecraft": "minecraft_dependency",
    "fabricloader": "loader_dependency",
    "fabric-api": "fabric_api_dependency",
    "java": "java_dependency",
    "malilib": "malilib_dependency",
}


def dependencies_for(mc):
    result = dict(DEPENDENCIES)
    if tuple(map(int, mc.split("."))) < (1, 19, 2):
        result["fabric"] = result.pop("fabric-api")
    return result


def properties(path):
    result = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith(("#", "!")) and "=" in line:
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def has_pre_cutover_marker(value):
    if isinstance(value, dict):
        return "pre_cutover_test" in value or any(has_pre_cutover_marker(item) for item in value.values())
    if isinstance(value, list):
        return any(has_pre_cutover_marker(item) for item in value)
    return False


def validate(root, directory, source_sha, production=False):
    settings = json.loads((root / "settings.json").read_text(encoding="utf-8"))
    active = settings["activeVersions"]
    if not active or len(active) != len(set(active)):
        raise ValueError("activeVersions must be nonempty and unique")
    version = properties(root / "gradle.properties")["mod_version"]
    names = {v: f"dds-tweak-v{version}-mc{v}.jar" for v in active}
    actual = {p.name for p in directory.iterdir() if p.is_file() and p.suffix.lower() == ".jar"}
    expected = set(names.values())
    if actual != expected:
        raise ValueError(f"runtime JAR inventory mismatch: missing={sorted(expected - actual)}, extra={sorted(actual - expected)}")

    artifacts = []
    for mc in active:
        props = properties(root / "versions" / mc / "gradle.properties")
        if props["minecraft_version"] != mc:
            raise ValueError(f"{mc}: minecraft_version does not match node")
        file = directory / names[mc]
        try:
            with ZipFile(file) as archive:
                if archive.testzip() is not None:
                    raise ValueError(f"{file.name}: corrupt ZIP member")
                members = set(archive.namelist())
                required = {"fabric.mod.json", "dds-tweak.mixins.json", "ddstweak/DDSTweak.class",
                            "LICENSE", "THIRD_PARTY_NOTICES.md",
                            "resourcepacks/dds_fake_player_ui/NOTICE.txt",
                            "resourcepacks/dds_fake_player_ui/LICENSE-CozyUI-Plus.txt"}
                if not required <= members:
                    raise ValueError(f"{file.name}: missing runtime members {sorted(required - members)}")
                if any(m.startswith("META-INF/jars/") and "fabric-" in m.lower() for m in members):
                    raise ValueError(f"{file.name}: embedded Fabric API is forbidden")
                meta = json.loads(archive.read("fabric.mod.json"))
        except (BadZipFile, KeyError, json.JSONDecodeError) as exc:
            raise ValueError(f"{file.name}: invalid runtime archive: {exc}") from exc
        if meta.get("id") != "dds-tweak" or meta.get("version") != version or meta.get("environment") != "client":
            raise ValueError(f"{file.name}: module id, version or environment mismatch")
        if meta.get("mixins") != ["dds-tweak.mixins.json"]:
            raise ValueError(f"{file.name}: unexpected mixin configuration")
        if production and has_pre_cutover_marker(meta):
            raise ValueError(f"{file.name}: pre-cutover marker forbidden in production")
        if meta.get("license") != "LGPL-3.0-only":
            raise ValueError(f"{file.name}: unexpected license metadata")
        if production and meta.get("contact", {}).get("sources") != "https://github.com/x-oOvOo-x/DDS-Tweak":
            raise ValueError(f"{file.name}: public source URL is missing")
        dependencies = dependencies_for(mc)
        alternate_api = "fabric-api" if "fabric" in dependencies else "fabric"
        if alternate_api in meta.get("depends", {}):
            raise ValueError(f"{file.name}: incorrect alternate Fabric API mod ID")
        for key, prop in dependencies.items():
            if meta.get("depends", {}).get(key) != props[prop]:
                raise ValueError(f"{file.name}: dependency {key} differs from {prop}")
        with file.open("rb") as stream:
            digest = hashlib.file_digest(stream, "sha256").hexdigest()
        artifacts.append({"minecraft": mc, "filename": file.name, "bytes": file.stat().st_size, "sha256": digest,
                          "depends": {key: props[prop] for key, prop in dependencies.items()}})
    return {"schema": 1, "kind": "production" if production else "compatibility", "source_sha": source_sha,
            "module": "dds-tweak", "mod_version": version, "active_versions": active, "artifacts": artifacts}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parent.parent)
    parser.add_argument("--directory", type=Path, required=True)
    parser.add_argument("--source-sha", required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--production", action="store_true", help="reject pre-cutover marker and label manifest as production")
    args = parser.parse_args()
    if len(args.source_sha) != 40 or any(c not in "0123456789abcdef" for c in args.source_sha.lower()):
        parser.error("--source-sha must be a 40-character Git SHA")
    try:
        result = validate(args.root, args.directory, args.source_sha.lower(), production=args.production)
    except (OSError, ValueError, KeyError) as exc:
        parser.exit(1, f"Release validation FAILED: {exc}\n")
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Verified {len(result['artifacts'])} runtime JARs; manifest: {args.manifest}")


if __name__ == "__main__":
    main()
