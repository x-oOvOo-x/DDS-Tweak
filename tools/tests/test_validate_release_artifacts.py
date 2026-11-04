"""Contract tests for the P5 artifact validator, using tiny synthetic JARs."""

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

SCRIPT = Path(__file__).resolve().parents[1] / "validate_release_artifacts.py"
spec = importlib.util.spec_from_file_location("validate_release_artifacts", SCRIPT)
validator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(validator)


class ReleaseArtifactTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.out = self.root / "out"
        self.out.mkdir()
        (self.root / "settings.json").write_text(json.dumps({"activeVersions": ["1.14.4", "1.21.11", "26.3"]}))
        (self.root / "gradle.properties").write_text("mod_version=0.1.0-dev\n")
        for mc in ["1.14.4", "1.21.11", "26.3"]:
            folder = self.root / "versions" / mc
            folder.mkdir(parents=True)
            props = {"minecraft_version": mc, "minecraft_dependency": mc,
                     "loader_dependency": ">=0.14.25", "fabric_api_dependency": ">=0.1",
                     "malilib_dependency": ">=0.2", "java_dependency": "21"}
            (folder / "gradle.properties").write_text("\n".join(f"{k}={v}" for k, v in props.items()))
            self.make_jar(mc, props)

    def make_jar(self, mc, props, omitted=(), **overrides):
        meta = {"id": "dds-tweak", "version": "0.1.0-dev", "environment": "client",
                "mixins": ["dds-tweak.mixins.json"], "license": "LGPL-3.0-only",
                "contact": {"sources": "https://github.com/x-oOvOo-x/DDS-Tweak"},
                "depends": {key: props[prop] for key, prop in validator.dependencies_for(mc).items()}}
        meta.update(overrides)
        name = self.out / f"dds-tweak-v0.1.0-dev-mc{mc}.jar"
        members = {
            "fabric.mod.json": json.dumps(meta), "dds-tweak.mixins.json": "{}",
            "ddstweak/DDSTweak.class": b"synthetic",
            "LICENSE": "LGPL-3.0-only", "THIRD_PARTY_NOTICES.md": "MIT notice",
            "resourcepacks/dds_fake_player_ui/NOTICE.txt": "CozyUI+",
            "resourcepacks/dds_fake_player_ui/LICENSE-CozyUI-Plus.txt": "GPL-3.0-only",
        }
        with ZipFile(name, "w") as jar:
            for path, content in members.items():
                if path not in omitted:
                    jar.writestr(path, content)
        return name

    def test_valid_all_active_and_digest(self):
        result = validator.validate(self.root, self.out, "a" * 40)
        self.assertEqual(len(result["artifacts"]), 3)
        self.assertEqual(result["active_versions"], ["1.14.4", "1.21.11", "26.3"])
        self.assertEqual(len(result["artifacts"][0]["sha256"]), 64)

    def test_missing_active_target(self):
        (self.out / "dds-tweak-v0.1.0-dev-mc26.3.jar").unlink()
        with self.assertRaisesRegex(ValueError, "missing"):
            validator.validate(self.root, self.out, "a" * 40)

    def test_reject_extra_source_dev_stale_and_unknown(self):
        for extra in ("dds-tweak-v0.1.0-dev-mc26.3-sources.jar", "dds-tweak-v0.1.0-dev-mc26.3-dev.jar",
                      "dds-tweak-v0.1.0-dev-mc1.21.10.jar", "other.jar"):
            f = self.out / extra
            f.write_bytes(b"bad")
            with self.assertRaisesRegex(ValueError, "extra"):
                validator.validate(self.root, self.out, "a" * 40)
            f.unlink()

    def test_wrong_metadata_and_dependency(self):
        props = validator.properties(self.root / "versions" / "26.3" / "gradle.properties")
        self.make_jar("26.3", props, id="unexpected")
        with self.assertRaisesRegex(ValueError, "module id"):
            validator.validate(self.root, self.out, "a" * 40)
        self.make_jar("26.3", props, depends={**{k: props[p] for k, p in validator.dependencies_for("26.3").items()}, "java": "25"})
        with self.assertRaisesRegex(ValueError, "dependency java"):
            validator.validate(self.root, self.out, "a" * 40)

    def test_corrupt_archive(self):
        (self.out / "dds-tweak-v0.1.0-dev-mc26.3.jar").write_bytes(b"not-a-jar")
        with self.assertRaisesRegex(ValueError, "invalid runtime archive"):
            validator.validate(self.root, self.out, "a" * 40)

    def test_production_manifest_and_pre_cutover_rejection(self):
        manifest = validator.validate(self.root, self.out, "a" * 40, production=True)
        self.assertEqual(manifest["kind"], "production")
        props = validator.properties(self.root / "versions" / "26.3" / "gradle.properties")
        self.make_jar("26.3", props, custom={"pre_cutover_test": True})
        with self.assertRaisesRegex(ValueError, "pre-cutover marker"):
            validator.validate(self.root, self.out, "a" * 40, production=True)
        self.assertEqual(validator.validate(self.root, self.out, "a" * 40)["kind"], "compatibility")

    def test_production_rejects_nested_pre_cutover_marker(self):
        props = validator.properties(self.root / "versions" / "26.3" / "gradle.properties")
        self.make_jar("26.3", props, custom={
            "dds:network_ownership": {"contract": 1, "pre_cutover_test": True}
        })
        with self.assertRaisesRegex(ValueError, "pre-cutover marker"):
            validator.validate(self.root, self.out, "a" * 40, production=True)

    def test_legacy_fabric_mod_id_and_alternate_rejected(self):
        result = validator.validate(self.root, self.out, "a" * 40)
        self.assertIn("fabric", result["artifacts"][0]["depends"])
        self.assertNotIn("fabric-api", result["artifacts"][0]["depends"])
        props = validator.properties(self.root / "versions" / "1.14.4" / "gradle.properties")
        deps = {key: props[prop] for key, prop in validator.dependencies_for("1.14.4").items()}
        deps["fabric-api"] = deps["fabric"]
        self.make_jar("1.14.4", props, depends=deps)
        with self.assertRaisesRegex(ValueError, "Fabric API"):
            validator.validate(self.root, self.out, "a" * 40)

    def test_missing_license_or_notice_is_rejected(self):
        props = validator.properties(self.root / "versions" / "26.3" / "gradle.properties")
        for member in ("LICENSE", "THIRD_PARTY_NOTICES.md",
                       "resourcepacks/dds_fake_player_ui/NOTICE.txt"):
            self.make_jar("26.3", props, omitted=(member,))
            with self.assertRaisesRegex(ValueError, "missing runtime members"):
                validator.validate(self.root, self.out, "a" * 40)

    def test_production_rejects_private_source_url(self):
        props = validator.properties(self.root / "versions" / "26.3" / "gradle.properties")
        self.make_jar("26.3", props, contact={"sources": "https://github.com/x-oOvOo-x/DDS-Tweak-Dev"})
        with self.assertRaisesRegex(ValueError, "public source"):
            validator.validate(self.root, self.out, "a" * 40, production=True)

    def test_duplicate_active_versions(self):
        (self.root / "settings.json").write_text(json.dumps({"activeVersions": ["26.3", "26.3"]}))
        with self.assertRaisesRegex(ValueError, "unique"):
            validator.validate(self.root, self.out, "a" * 40)


if __name__ == "__main__":
    unittest.main()
