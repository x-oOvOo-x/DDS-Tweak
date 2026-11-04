"""Pure tests for CurseForge preflight and upload payloads (no external HTTP)."""

import io
import json
import sys
import urllib.error
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import upload_curseforge as cf


class CurseForgeUploadTests(unittest.TestCase):
    def test_exact_names_and_approved_duplicate_id(self):
        versions = [
            {"id": 42, "name": "Fabric", "gameVersionTypeID": 68441},
            {"id": 101, "name": "1.21.11", "gameVersionTypeID": 77784},
            {"id": 111, "name": "26.3", "gameVersionTypeID": 88556},
            {"id": 112, "name": "26.3", "gameVersionTypeID": 1},
        ]
        expected = {"Fabric": 42, "1.21.11": 101, "26.3": 111}
        self.assertEqual(cf.resolve_versions(versions, ["1.21.11", "26.3"]), expected)
        self.assertEqual(cf.resolve_versions(versions, ["1.21.11", "26.3"],
                                             {"26.3": 111}), expected)
        with self.assertRaisesRegex(ValueError, "non-Bukkit"):
            cf.resolve_versions(versions, ["26.3"], {"26.3": 112})
        with self.assertRaisesRegex(ValueError, "26.3"):
            cf.resolve_versions(versions, ["26.3"], {"26.3": 999})
        with self.assertRaisesRegex(ValueError, "override"):
            cf.resolve_versions(versions, ["26.3"], {"26.3": -1})
        with self.assertRaisesRegex(ValueError, "26.2"):
            cf.resolve_versions(versions, ["26.2"])
        with self.assertRaisesRegex(ValueError, "Fabric"):
            cf.resolve_versions(versions + [{"id": 45, "name": "Fabric"}], ["1.21.11"])
        with self.assertRaisesRegex(ValueError, "26.3"):
            cf.resolve_versions(versions + [versions[2]], ["26.3"], {"26.3": 111})

    def test_bukkit_duplicates_never_override_minecraft_version_ids(self):
        versions = [
            {"id": 7499, "name": "Fabric", "gameVersionTypeID": 68441},
            {"id": 17045, "name": "26.3", "gameVersionTypeID": 88556},
            {"id": 17046, "name": "26.3", "gameVersionTypeID": 1},
            {"id": 16498, "name": "26.2", "gameVersionTypeID": 86297},
            {"id": 16500, "name": "26.2", "gameVersionTypeID": 1},
            {"id": 16084, "name": "26.1.2", "gameVersionTypeID": 85000},
            {"id": 16085, "name": "26.1.2", "gameVersionTypeID": 1},
            {"id": 14416, "name": "1.21.11", "gameVersionTypeID": 77784},
            {"id": 14417, "name": "1.21.11", "gameVersionTypeID": 1},
            {"id": 9008, "name": "1.18.2", "gameVersionTypeID": 73250},
            {"id": 9016, "name": "1.18.2", "gameVersionTypeID": 1},
        ]
        target = ["26.3", "26.2", "26.1.2", "1.21.11", "1.18.2"]
        expected = {"Fabric": 7499, "26.3": 17045, "26.2": 16498,
                    "26.1.2": 16084, "1.21.11": 14416, "1.18.2": 9008}
        for order in (versions, list(reversed(versions))):
            self.assertEqual(cf.resolve_versions(order, target), expected)
            self.assertEqual(cf.resolve_versions(order, target, {"26.3": 17045}), expected)
        for wrong_id in (17046,):
            with self.assertRaisesRegex(ValueError, "Bukkit"):
                cf.resolve_versions(versions, target, {"26.3": wrong_id})
        with self.assertRaisesRegex(ValueError, "26.2"):
            cf.resolve_versions(
                versions + [{"id": 16499, "name": "26.2", "gameVersionTypeID": 86297}],
                target, {"26.3": 17045},
            )
        no_type = [
            {k: v for k, v in item.items() if k != "gameVersionTypeID"}
            if item["id"] == 17045 else item for item in versions
        ]
        with self.assertRaisesRegex(ValueError, "26.3"):
            cf.resolve_versions(no_type, target, {"26.3": 17045})

    def test_same_type_duplicates_block_entire_batch_before_any_post(self):
        manifest = {"mod_version": "0.1.0", "source_sha": "a" * 40,
                    "active_versions": ["26.3", "26.2"], "artifacts": [
                        {"minecraft": "26.3", "filename": "unused.jar", "sha256": "a" * 64},
                    ]}
        versions = [
            {"id": 7499, "name": "Fabric", "gameVersionTypeID": 68441},
            {"id": 17045, "name": "26.3", "gameVersionTypeID": 88556},
            {"id": 17046, "name": "26.3", "gameVersionTypeID": 1},
            {"id": 16498, "name": "26.2", "gameVersionTypeID": 86297},
            {"id": 16499, "name": "26.2", "gameVersionTypeID": 86297},
        ]
        with tempfile.TemporaryDirectory() as directory:
            with patch.object(cf, "request_json", return_value=versions) as request:
                with self.assertRaisesRegex(ValueError, "26.2"):
                    cf.publish(Path(directory), manifest, "Changes", "token",
                               cf.PROJECT_ID, "release", Path(directory) / "receipt.json",
                               {"26.3": 17045})
                request.assert_called_once_with(cf.API_BASE + "/api/game/versions", "token")

    def test_author_api_http_error_is_attributed_and_token_redacted(self):
        url = cf.API_BASE + "/api/game/versions"
        error = urllib.error.HTTPError(
            url, 400, "Bad Request", {},
            io.BytesIO(b'{"error":"invalid super-secret token"}')
        )
        with patch("urllib.request.urlopen", side_effect=error):
            with self.assertRaisesRegex(RuntimeError, "GET /api/game/versions: HTTP 400") as caught:
                cf.request_json(url, "super-secret")
        self.assertIn("[REDACTED]", str(caught.exception))
        self.assertNotIn("super-secret", str(caught.exception))

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

    def test_missing_or_ambiguous_version_never_posts_a_jar(self):
        manifest = {"mod_version": "0.1.0", "source_sha": "a" * 40,
                    "active_versions": ["26.3", "26.2"], "artifacts": []}
        versions = [
            {"id": 1, "name": "Fabric"},
            {"id": 2, "name": "26.3", "gameVersionTypeID": 20},
            {"id": 3, "name": "26.3", "gameVersionTypeID": 30},
        ]
        with tempfile.TemporaryDirectory() as directory:
            with patch.object(cf, "request_json", return_value=versions) as request:
                with self.assertRaisesRegex(ValueError, "26.3"):
                    cf.publish(Path(directory), manifest, "Changes", "token",
                               cf.PROJECT_ID, "release", Path(directory) / "receipt.json")
                request.assert_called_once_with(cf.API_BASE + "/api/game/versions", "token")

            with patch.object(cf, "request_json", return_value=versions) as request:
                with self.assertRaisesRegex(ValueError, "26.2"):
                    cf.publish(Path(directory), manifest, "Changes", "token",
                               cf.PROJECT_ID, "release", Path(directory) / "receipt.json",
                               {"26.3": 2})
                request.assert_called_once()


if __name__ == "__main__":
    unittest.main()
