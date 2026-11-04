# Client configuration layout

DDS Tweak keeps its own client settings in the **Minecraft instance** directory:

```text
.minecraft/config/dds-tweak/
├── settings.json
├── camera-command-profiles.json
├── camera-destinations.json
└── camera-quick-commands.json
```

These are local client files, not the repository's `config/` contracts and not CDA server rules.

| Old file in `config/` | Current file in `config/dds-tweak/` |
| --- | --- |
| `dds-tweak.json` | `settings.json` |
| `dds-tweak-camera-command-profiles.json` | `camera-command-profiles.json` |
| `dds-tweak-camera-destinations.json` | `camera-destinations.json` |
| `dds-tweak-camera-quick-commands.json` | `camera-quick-commands.json` |

Each file migrates independently on its first access. An existing new file takes precedence: no old file is merged into or allowed to overwrite it. If only the old file exists, DT validates that it is a JSON object and copies its original bytes to the new location. Existing Camera `.bak` files are also copied when no new backup exists. The old files remain untouched to support manual rollback to an older DT version.

Migration does not rename any JSON keys, change Camera profile schema, alter local/global overrides, or modify the configured keybinds. Newly created files use only the new directory. If a stored JSON file is invalid or cannot be read, DT reports an error instead of silently replacing it with defaults. Resolve any conflicts by backing up both files and choosing the desired copy manually; the newer path always wins automatically.

**Cross-version caveat:** Older DT releases continue writing the old paths and will not see changes made to the new files. Switching back and forth can therefore create independent configurations. Back up the full `config/` directory before downgrading or manually reconciling edits. No automatic cleanup or deletion of old files is performed.

## CDA Undo/Redo shortcuts

All active DDS Tweak Minecraft targets expose two configurable MaLiLib shortcuts in the DDS Tweak **Client** tab: **CDA Undo** (`LEFT_CONTROL,Z` by default) and **CDA Redo** (`LEFT_CONTROL,LEFT_ALT,Z` by default). They persist as `Hotkeys.ddsUndo` and `Hotkeys.ddsRedo` in `config/dds-tweak/settings.json`. Players can rebind, clear, or reset each shortcut. The primary labels are localized (CDA 撤销 / CDA 重做 in Simplified Chinese), while `ddsUndo` / `ddsRedo` are shown separately underneath only when **Show Original Config Names** is enabled. If both shortcuts are configured identically, both are disabled until the conflict is resolved.

The client requires creative mode, no open GUI, DT feature ownership, and a CDA server advertising both Undo and Redo input-takeover capabilities. DT forwards only existing `undo_c2s` and `redo_c2s` requests and never performs a client-local undo; CDA remains authoritative. With a co-installed unmarked legacy CDA owning client input, the old CDA key handler remains in control, so changing the DT bindings has no effect on those legacy shortcuts. The DT hardcoded keyboard Mixin is removed to prevent double handling.
