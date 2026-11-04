"""Fail closed unless a staged GitHub release contains exactly the validated runtime assets."""

import argparse
import hashlib
import json
from pathlib import Path


def verify(manifest_file: Path, release_file: Path, tag: str) -> int:
    manifest = json.loads(manifest_file.read_text(encoding="utf-8"))
    release = json.loads(release_file.read_text(encoding="utf-8"))
    if isinstance(release, list):
        matches = [item for item in release if item.get("tag_name") == tag]
        if len(matches) != 1:
            raise ValueError("expected exactly one matching Draft release")
        release = matches[0]
    if manifest.get("kind") != "production":
        raise ValueError("formal release requires production manifest")
    if release.get("tag_name") != tag or release.get("draft") is not True:
        raise ValueError("release must be the expected draft tag")
    if release.get("prerelease") is True:
        raise ValueError("formal release must not be a prerelease")

    expected = {item["filename"]: (item["bytes"], item["sha256"]) for item in manifest["artifacts"]}
    if len(expected) != len(manifest["active_versions"]) or not expected:
        raise ValueError("manifest has missing or duplicate active artifacts")
    with manifest_file.open("rb") as stream:
        expected[manifest_file.name] = (manifest_file.stat().st_size, hashlib.file_digest(stream, "sha256").hexdigest())

    assets = release.get("assets", [])
    names = [item["name"] for item in assets]
    if len(names) != len(expected) or set(names) != set(expected):
        raise ValueError(f"release asset inventory mismatch: expected {sorted(expected)}, got {sorted(names)}")
    for asset in assets:
        size, digest = expected[asset["name"]]
        if asset.get("size") != size or asset.get("digest") != f"sha256:{digest}":
            raise ValueError(f"release asset size/SHA256 mismatch: {asset['name']}")
    return len(assets)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--release-json", type=Path, required=True)
    parser.add_argument("--tag", required=True)
    args = parser.parse_args()
    try:
        count = verify(args.manifest, args.release_json, args.tag)
    except (OSError, KeyError, ValueError, TypeError) as exc:
        parser.exit(1, f"Formal release verification FAILED: {exc}\n")
    print(f"Verified {count} assets against manifest; draft release can be published.")


if __name__ == "__main__":
    main()
