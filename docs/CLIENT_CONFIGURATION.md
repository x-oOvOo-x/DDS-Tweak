# Client configuration layout

DDS Tweak keeps its own client settings in the **Minecraft instance** directory:

```text
.minecraft/config/dds-tweak/
├── settings.json
├── camera-command-profiles.json
├── camera-destinations.json
├── camera-quick-commands.json
├── carpet-rules.json
├── carpet-presets.json
├── carpet-presets-transfer.json
└── carpet-exports/
    └── <exported-plan>.json
```

These are local client files, not the repository's `config/` contracts and not CDA server rules.

Rule-plan **Import → Browse** opens the operating system's native file picker by default: Minecraft 1.15.2 through 26.2 use LWJGL TinyFD, while 26.3 uses SDL3's asynchronous native dialog. Minecraft 1.14.4 has no bundled TinyFD and uses a cancellable lightweight Windows Forms helper on Windows (without dynamic C# compilation). A native API/launch failure falls back to DT's in-game JSON browser; dismissing the system dialog without choosing a file does **not** trigger a fallback. The starting directory is `config/dds-tweak/carpet-exports/`, and the path field starts empty unless the user enters or selects a file. Cancelling the import view releases DT's pending UI state and discards stale callbacks; depending on the OS, an already-open native dialog may still need to be dismissed using its own Cancel button. No external PowerShell process is started on 1.15.2+; the 1.14.4 compatibility helper is the only exception. Users can still enter a local JSON file path manually; `carpet-presets-transfer.json` remains available for explicit/manual imports.

| Old file in `config/` | Current file in `config/dds-tweak/` |
| --- | --- |
| `dds-tweak.json` | `settings.json` |
| `dds-tweak-camera-command-profiles.json` | `camera-command-profiles.json` |
| `dds-tweak-camera-destinations.json` | `camera-destinations.json` |
| `dds-tweak-camera-quick-commands.json` | `camera-quick-commands.json` |
| `dds-tweak-carpet-rules.json` | `carpet-rules.json` |
| `dds-tweak-carpet-presets.json` | `carpet-presets.json` |
| `dds-tweak-carpet-presets-transfer.json` | `carpet-presets-transfer.json` |
| `dds-tweak-carpet-exports/*.json` | `carpet-exports/*.json` |

Only DT-owned client data is in scope. Carpet/CDA server rules, MaLiLib preferences and other mods' files stay in their original locations. The `carpet-exports/` directory receives copies of existing DT JSON exports once, only when the new directory does not yet exist; non-JSON files, symlinks and unrelated folders are not imported. An existing new directory takes precedence, so deleting a migrated export does not automatically restore it.

Each file migrates independently on its first access. The legacy preset exchange file is only an optional import suggestion, so it is copied byte-for-byte even when its JSON is malformed; actual imports still validate their content and reject invalid files. An existing new file takes precedence: no old file is merged into or allowed to overwrite it. If only the old file exists, DT validates that it is a JSON object and copies its original bytes to the new location. Existing Camera `.bak` files are also copied when no new backup exists. The old files remain untouched to support manual rollback to an older DT version.

Migration does not rename any JSON keys, change Camera profile schema, alter local/global overrides, or modify the configured keybinds. Newly created files use only the new directory. If a stored JSON file is invalid or cannot be read, DT reports an error instead of silently replacing it with defaults. Resolve any conflicts by backing up both files and choosing the desired copy manually; the newer path always wins automatically.

**Cross-version caveat:** Older DT releases continue writing the old paths and will not see changes made to the new files. Switching back and forth can therefore create independent configurations. Back up the full `config/` directory before downgrading or manually reconciling edits. No automatic cleanup or deletion of old files is performed.

## CDA Undo/Redo shortcuts

All active DDS Tweak Minecraft targets expose two configurable MaLiLib shortcuts in the DDS Tweak **Client** tab: **CDA Undo** (`LEFT_CONTROL,Z` by default) and **CDA Redo** (`LEFT_CONTROL,LEFT_ALT,Z` by default). They persist as `Hotkeys.ddsUndo` and `Hotkeys.ddsRedo` in `config/dds-tweak/settings.json`. Players can rebind, clear, or reset each shortcut. The primary labels are localized (CDA 撤销 / CDA 重做 in Simplified Chinese), while `ddsUndo` / `ddsRedo` are shown separately underneath only when **Show Original Config Names** is enabled. If both shortcuts are configured identically, both are disabled until the conflict is resolved.

The client requires creative mode, no open GUI, DT feature ownership, and a CDA server advertising both Undo and Redo input-takeover capabilities. DT forwards only existing `undo_c2s` and `redo_c2s` requests and never performs a client-local undo; CDA remains authoritative. With a co-installed unmarked legacy CDA owning client input, the old CDA key handler remains in control, so changing the DT bindings has no effect on those legacy shortcuts. The DT hardcoded keyboard Mixin is removed to prevent double handling.
