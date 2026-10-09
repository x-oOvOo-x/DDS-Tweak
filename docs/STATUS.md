# Development status

## Foundation

- [x] Standalone Fabric client project
- [x] DDS Tweak identity (`dds-tweak`) and simplified package root (`ddstweak`)
- [x] DDS Protocol v1 handshake / custom payload codecs
- [x] Dev / Public snapshot development model
- [x] CI build and guarded Public release workflow
- [x] DDS-server / DDS-Tweak ownership boundary documented
- [x] Dedicated server + client four-combination runtime workflow documented
- [x] Align fake-player client with latest DDS Protocol v1 menu-marker contract
- [x] Restore latest DDS default fake-player UI under the `dds-tweak` resource namespace
- [x] Bundle CozyUI+ adaptation as an optional built-in resource pack, disabled by default
- [x] Fake-player / default UI / CozyUI+ runtime regression test passed
- [x] MaLiLib 0.27.20 required dependency and eager config registration
- [x] `Generic / Visual / Tweaks / DDS / Hotkeys` config foundation
- [x] Editable `Open DDS Tweak Config` hotkey with persistence
- [x] Phase A runtime gate passed: first-open global dropdown, five tabs, hotkey restart persistence, no-DDS join, DDS fake-player/Cozy regression
- [x] Phase A CI gate passed on 1.21.11 (Build #58 before final documentation sync)
- [ ] Expand supported Minecraft targets after 1.21.11 is stable

## MaLiLib foundation contract

Core baseline:

```text
Minecraft 1.21.11
MaLiLib 0.27.20
```

1.21.11 uses Sakura-Ryoko's LTS MaLiLib API (`fi.dy.masa.malilib.*`). DDS Tweak registers an `IInitializationHandler` during Fabric client initialization; `registerModHandlers()` registers the config handler, `Registry.CONFIG_SCREEN` entry and MaLiLib keybind provider. The five DDS Tweak tabs are implemented inside `GuiConfigsBase`, matching the 1.21.11 Tweakeroo/MaLiLib model; there is no `Registry.CONFIG_TAB` contract on this LTS baseline.

The config-screen `ModInfo` contains the screen supplier before any DDS Tweak screen is opened, so DDS Tweak is already present in MaLiLib's upper-right global mod selector on first use.

## Current DDS fake-player contract

```text
protocol 1
channel carpet-dds-addition:network/v1
C2S fake_player_action_c2s
S2C fake_player_open_menu_s2c
layout 1 / 3 / 6 + vanilla GENERIC_9x6 / GENERIC_9x3
```

Client presentation is owned by DDS Tweak:

```text
assets/dds-tweak/...                         default DDS fake-player UI
resourcepacks/dds_fake_player_ui/
└─ assets/dds-tweak/...                      optional CozyUI+ override
```

The default UI uses the current DDS inventory/Ender Chest backgrounds, hover/active button states and action-icon atlas. CozyUI+ remains opt-in through Minecraft's Resource Packs screen and carries its attribution/license files.

Obsolete client `fake_player_sort_c2s`, custom DDS MenuType registration and the legacy `assets/carpet-dds-addition/...` resources are removed from DDS Tweak. Protocol v1 remains unchanged by the MaLiLib foundation work.

## Planned feature set after foundation

1. Taskbar player avatar
2. Stack-count text scaling
3. Disable End flash
4. No Command Confirm
5. DDS Rule configuration GUI
6. Crafter recipe-lock GUI
7. Camera player selector
