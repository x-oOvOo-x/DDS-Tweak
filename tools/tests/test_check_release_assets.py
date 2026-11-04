"""Verify strict release draft checks without any GitHub writes."""

import hashlib
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "check_release_assets.py"
spec = importlib.util.spec_from_file_location("check_release_assets", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class PublicReleaseTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        root = Path(self.tmp.name)
        self.manifest_file = root / "public-release-manifest.json"
        self.release_file = root / "release.json"
        self.tag = "v1.2.3"
        self.manifest = {
            "kind": "production", "active_versions": ["1.21.11", "26.3"],
            "artifacts": [
                {"filename": "dds-tweak-v1.2.3-mc1.21.11.jar", "bytes": 11, "sha256": "a" * 64},
                {"filename": "dds-tweak-v1.2.3-mc26.3.jar", "bytes": 22, "sha256": "b" * 64},
            ],
        }
        self.save()

    def save(self):
        self.manifest_file.write_text(json.dumps(self.manifest), encoding="utf-8")
        entries = self.manifest["artifacts"]
        self.release = {
            "tag_name": self.tag, "draft": True, "prerelease": False,
            "assets": [
                {"name": x["filename"], "size": x["bytes"], "digest": f"sha256:{x['sha256']}"}
                for x in entries
            ] + [{
                "name": self.manifest_file.name,
                "size": self.manifest_file.stat().st_size,
                "digest": "sha256:" + hashlib.sha256(self.manifest_file.read_bytes()).hexdigest(),
            }],
        }
        self.release_file.write_text(json.dumps(self.release), encoding="utf-8")

    def verify(self):
        self.release_file.write_text(json.dumps(self.release), encoding="utf-8")
        return module.verify(self.manifest_file, self.release_file, self.tag)

    def test_exact_draft_succeeds(self):
        self.assertEqual(self.verify(), 3)

    def test_reject_missing_and_extra_asset(self):
        self.release["assets"].pop()
        with self.assertRaisesRegex(ValueError, "inventory mismatch"):
            self.verify()
        self.release["assets"].append({"name": "sources.jar", "size": 1, "digest": "sha256:" + "c" * 64})
        with self.assertRaisesRegex(ValueError, "inventory mismatch"):
            self.verify()

    def test_reject_bad_digest_or_size(self):
        self.release["assets"][0]["digest"] = "sha256:" + "0" * 64
        with self.assertRaisesRegex(ValueError, "size/SHA256"):
            self.verify()
        self.release["assets"][0]["digest"] = "sha256:" + "a" * 64
        self.release["assets"][0]["size"] = 10
        with self.assertRaisesRegex(ValueError, "size/SHA256"):
            self.verify()

    def test_reject_wrong_tag_published_or_prerelease(self):
        self.release["tag_name"] = "v9.9.9"
        with self.assertRaisesRegex(ValueError, "expected draft"):
            self.verify()
        self.release["tag_name"] = self.tag
        self.release["draft"] = False
        with self.assertRaisesRegex(ValueError, "expected draft"):
            self.verify()
        self.release["draft"] = True
        self.release["prerelease"] = True
        with self.assertRaisesRegex(ValueError, "must not be a prerelease"):
            self.verify()

    def test_draft_from_release_list(self):
        self.release_file.write_text(json.dumps([
            {"tag_name": "v0.0.1", "draft": True, "assets": []}, self.release
        ]), encoding="utf-8")
        self.assertEqual(module.verify(self.manifest_file, self.release_file, self.tag), 3)

    def test_reject_compat_manifest(self):
        self.manifest["kind"] = "compatibility"
        self.save()
        with self.assertRaisesRegex(ValueError, "production manifest"):
            self.verify()


if __name__ == "__main__":
    unittest.main()
