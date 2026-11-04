"""Pure tests for CurseForge preflight and upload payloads (no external HTTP)."""

import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import upload_curseforge as cf


class CurseForgeUploadTests(unittest.TestCase):
    def test_resolves_every_mc_target_and_fabric_before_upload(self):
        version_types = [
            {"id": 10, "name": "Minecraft 1.21", "slug": "minecraft-1-21"},
            {"id": 20, "name": "Minecraft 26.3", "slug": "minecraft-26-3"},
            {"id": 30, "name": "Bukkit 26.3", "slug": "bukkit-26-3"},
        ]
        versions = [
            {"id": 42, "name": "Fabric"},
            {"id": 101, "name": "1.21.11", "gameVersionTypeID": 10},
            {"id": 111, "name": "26.3", "gameVersionTypeID": 20},
            {"id": 112, "name": "26.3", "gameVersionTypeID": 30},
        ]
        self.assertEqual(
            cf.resolve_versions(versions, ["1.21.11", "26.3"], version_types),
            {"Fabric": 42, "1.21.11": 101, "26.3": 111},
        )
        with self.assertRaisesRegex(ValueError, "26.2"):
            cf.resolve_versions(versions, ["1.21.11", "26.2"], version_types)
        with self.assertRaisesRegex(ValueError, "expected one exact ID"):
            cf.resolve_versions(
                versions + [{"id": 99, "name": "Fabric"}], ["26.3"], version_types
            )
        with self.assertRaisesRegex(ValueError, "26.3"):
            cf.resolve_versions(
                versions + [{"id": 113, "name": "26.3", "gameVersionTypeID": 20}],
                ["26.3"], version_types,
            )
        with self.assertRaisesRegex(ValueError, "26.3"):
            cf.resolve_versions(versions, ["26.3"], version_types[:1])

    def test_metadata_has_exact_version_and_required_dependencies(self):
        ids = {"Fabric": 42, "26.3": 11}
        meta = cf.metadata_for({"minecraft": "26.3"}, "0.1.0", "Changes",
                               ids, "release")
        self.assertEqual(meta["gameVersions"], [11, 42])
        self.assertEqual(meta["releaseType"], "release")
        self.assertEqual({p["slug"] for p in meta["relations"]["projects"]},
                         {"fabric-api", "malilib"})

    def test_multipart_includes_jar_and_metadata_without_reencoding(self):
        contents = b"\x00\xffPK\x03\x04"
        payload, content_type = cf.multipart({"gameVersions": [11, 42]}, "mod.jar", contents)
        self.assertIn("multipart/form-data; boundary=", content_type)
        self.assertIn(b'"gameVersions": [11, 42]', payload)
        self.assertIn(b'filename="mod.jar"', payload)
        self.assertIn(contents, payload)

    def test_manifest_drift_and_dev_version_fail_before_any_network(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest_path = root / "manifest.json"
            expected = {"kind": "production", "mod_version": "0.1.0",
                        "source_sha": "a" * 40, "artifacts": []}
            manifest_path.write_text(json.dumps(expected))
            with patch.object(cf, "validate", return_value=expected):
                self.assertEqual(cf.verified_manifest(root, root, manifest_path), expected)
            with patch.object(cf, "validate", return_value={**expected, "artifacts": [1]}):
                with self.assertRaisesRegex(ValueError, "no longer matches"):
                    cf.verified_manifest(root, root, manifest_path)
            manifest_path.write_text(json.dumps({**expected, "mod_version": "0.1.0-dev"}))
            with patch.object(cf, "validate") as verifier:
                with self.assertRaisesRegex(ValueError, "X.Y.Z"):
                    cf.verified_manifest(root, root, manifest_path)
                verifier.assert_not_called()

    def test_missing_mc_version_never_posts_a_jar(self):
        manifest = {"mod_version": "0.1.0", "source_sha": "a" * 40,
                    "active_versions": ["26.3", "26.2"], "artifacts": []}
        with tempfile.TemporaryDirectory() as directory:
            inventories = [
                [{"id": 1, "name": "Fabric"},
                 {"id": 2, "name": "26.3", "gameVersionTypeID": 20}],
                [{"id": 20, "name": "Minecraft 26.3", "slug": "minecraft-26-3"}],
            ]
            with patch.object(cf, "request_json", side_effect=inventories) as request:
                with self.assertRaisesRegex(ValueError, "26.2"):
                    cf.publish(Path(directory), manifest, "Changes", "token",
                               cf.PROJECT_ID, "release", Path(directory) / "receipt.json")
                self.assertEqual(request.call_count, 2)


if __name__ == "__main__":
    unittest.main()
