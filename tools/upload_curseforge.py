"""Upload hash-verified DDS Tweak runtime JARs to the CurseForge Author API.

No upload is attempted until the complete artifact inventory and every game
version have passed preflight. POST failures are not retried: a lost response
could otherwise produce duplicate CurseForge files.
"""

import argparse
import json
import os
import re
import urllib.error
import urllib.request
import uuid
from pathlib import Path

from validate_release_artifacts import validate

API_BASE = "https://minecraft.curseforge.com"
PROJECT_ID = 1734610


def verified_manifest(root: Path, directory: Path, manifest_path: Path) -> dict:
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    version = manifest.get("mod_version", "")
    if not re.fullmatch(r"\d+\.\d+\.\d+", version):
        raise ValueError(f"CurseForge formal publication requires X.Y.Z, got {version!r}")
    if manifest.get("kind") != "production":
        raise ValueError("Only a production manifest may be uploaded")
    verified = validate(root, directory, manifest["source_sha"], production=True)
    if manifest != verified:
        raise ValueError("Release manifest no longer matches the runtime JARs and source")
    return manifest


def resolve_versions(available: list, minecraft_versions: list) -> dict:
    """Resolve exact names; refuse unavailable or ambiguous CurseForge IDs."""
    result = {}
    for name in ["Fabric", *minecraft_versions]:
        matches = [entry["id"] for entry in available
                   if entry.get("name") == name and isinstance(entry.get("id"), int)]
        if len(matches) != 1:
            raise ValueError(f"CurseForge version {name!r}: expected one exact ID, found {len(matches)}")
        result[name] = matches[0]
    return result


def metadata_for(artifact: dict, version: str, changelog: str, ids: dict, release_type: str) -> dict:
    return {
        "changelog": changelog,
        "changelogType": "markdown",
        "displayName": f"DDS Tweak {version} (Minecraft {artifact['minecraft']})",
        "gameVersions": [ids[artifact["minecraft"]], ids["Fabric"]],
        "releaseType": release_type,
        "isMarkedForManualRelease": False,
        "relations": {"projects": [
            {"slug": "fabric-api", "type": "requiredDependency"},
            {"slug": "malilib", "type": "requiredDependency"},
        ]},
    }


def multipart(metadata: dict, filename: str, data: bytes) -> tuple:
    boundary = "dds-tweak-" + uuid.uuid4().hex
    separator = ("--" + boundary + "\r\n").encode()
    body = b"".join([
        separator,
        b'Content-Disposition: form-data; name="metadata"\r\nContent-Type: application/json\r\n\r\n',
        json.dumps(metadata, ensure_ascii=False).encode("utf-8"), b"\r\n",
        separator,
        f'Content-Disposition: form-data; name="file"; filename="{filename}"\r\n'.encode("ascii"),
        b"Content-Type: application/java-archive\r\n\r\n",
        data, b"\r\n",
        ("--" + boundary + "--\r\n").encode(),
    ])
    return body, f"multipart/form-data; boundary={boundary}"


def request_json(url: str, token: str, data=None, content_type=None):
    headers = {"X-Api-Token": token, "Accept": "application/json"}
    if content_type:
        headers["Content-Type"] = content_type
    request = urllib.request.Request(url, data=data, headers=headers,
                                     method="POST" if data is not None else "GET")
    try:
        with urllib.request.urlopen(request, timeout=90) as response:
            return json.load(response)
    except urllib.error.HTTPError as exc:
        raise RuntimeError(f"CurseForge Author API rejected request (HTTP {exc.code})") from exc


def publish(directory: Path, manifest: dict, changelog: str,
            token: str, project_id: int, release_type: str, receipt: Path) -> dict:
    versions = request_json(f"{API_BASE}/api/game/versions", token)
    if not isinstance(versions, list):
        raise ValueError("CurseForge returned an unexpected game version inventory")
    ids = resolve_versions(versions, manifest["active_versions"])
    records = []
    result = {"schema": 1, "project_id": project_id, "version": manifest["mod_version"],
              "source_sha": manifest["source_sha"], "files": records}
    receipt.parent.mkdir(parents=True, exist_ok=True)

    for item in manifest["artifacts"]:
        file = directory / item["filename"]
        meta = metadata_for(item, manifest["mod_version"], changelog, ids, release_type)
        body, content_type = multipart(meta, file.name, file.read_bytes())
        response = request_json(f"{API_BASE}/api/projects/{project_id}/upload-file",
                                token, body, content_type)
        file_id = response.get("id") if isinstance(response, dict) else None
        if not isinstance(file_id, int) or file_id <= 0:
            raise RuntimeError(f"CurseForge did not confirm a file ID for {file.name}")
        records.append({"minecraft": item["minecraft"], "filename": file.name,
                        "sha256": item["sha256"], "curseforge_file_id": file_id})
        receipt.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
        print(f"CurseForge accepted {file.name} as file {file_id}", flush=True)
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--directory", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--changelog", type=Path, required=True)
    parser.add_argument("--receipt", type=Path, required=True)
    parser.add_argument("--project-id", type=int, default=PROJECT_ID)
    parser.add_argument("--release-type", choices=("release", "beta", "alpha"), default="release")
    parser.add_argument("--dry-run", action="store_true", help="check local files without HTTP or token")
    args = parser.parse_args()
    try:
        manifest = verified_manifest(args.root, args.directory, args.manifest)
        if args.project_id != PROJECT_ID:
            raise ValueError("Unexpected CurseForge project ID for DDS Tweak")
        changelog = args.changelog.read_text(encoding="utf-8").strip()
        if not changelog:
            raise ValueError("CurseForge changelog must not be empty")
        if args.dry_run:
            print(f"Local preflight passed for {len(manifest['artifacts'])} JARs; no HTTP requests sent")
            return
        token = os.environ.get("CURSEFORGE_API_TOKEN", "")
        if not token:
            raise ValueError("CURSEFORGE_API_TOKEN secret is required")
        publish(args.directory, manifest, changelog, token,
                args.project_id, args.release_type, args.receipt)
    except (OSError, ValueError, KeyError, RuntimeError) as exc:
        parser.exit(1, f"CurseForge publication FAILED: {exc}\n")


if __name__ == "__main__":
    main()
