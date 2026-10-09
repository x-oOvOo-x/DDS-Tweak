#!/usr/bin/env python3
import argparse
import json
import os
from pathlib import Path

RELATIVE_CONTRACT = Path("config/dds-client-ownership-contract.json")
CANONICAL_SOURCE = "x-oOvOo-x/Carpet-DDS-Addition-Dev:config/dds-client-ownership-contract.json"


def canonical_text(path: Path) -> str:
    data = json.loads(path.read_text(encoding="utf-8"))
    if data.get("schema") != 1 or data.get("canonicalSource") != CANONICAL_SOURCE:
        raise SystemExit(f"Invalid DDS ownership contract: {path}")
    return json.dumps(data, ensure_ascii=False, indent=2) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Sync DDS Tweak's pinned ownership-contract snapshot from a local DDS checkout."
    )
    parser.add_argument(
        "--dds-repo",
        type=Path,
        default=Path(os.environ.get("DDS_DEV_REPO", "../Carpet-DDS-Addition-Dev")),
        help="Path to the Carpet-DDS-Addition-Dev checkout.",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Only verify that the pinned snapshot matches the DDS canonical contract.",
    )
    args = parser.parse_args()

    root = Path(__file__).resolve().parents[1]
    source = args.dds_repo.resolve() / RELATIVE_CONTRACT
    target = root / RELATIVE_CONTRACT

    if not source.is_file():
        raise SystemExit(f"DDS canonical contract not found: {source}")

    expected = canonical_text(source)
    actual = canonical_text(target) if target.is_file() else ""

    if args.check:
        if actual != expected:
            raise SystemExit(
                "DDS ownership contract snapshot is stale. "
                "Run tools/sync-dds-client-ownership-contract.py with the matching DDS checkout."
            )
        print("DDS ownership contract snapshot matches the canonical DDS contract.")
        return 0

    if actual == expected:
        print("DDS ownership contract snapshot is already current.")
        return 0

    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(expected, encoding="utf-8")
    print(f"Updated {target} from {source}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
