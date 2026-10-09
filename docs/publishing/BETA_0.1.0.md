# DDS Tweak 0.1.0-beta.1 — Store listing and review checklist

This document is **release-specific** to the `beta/0.1.0-beta.1` branch (Minecraft 1.21.11). It is not a development-main feature matrix.

## Project title

DDS Tweak

## Modrinth short summary / CurseForge summary

Client-side quality-of-life tweaks, visual enhancements, and optional Carpet DDS Addition integration for inventory and fake-player controls.

## Project description (English; suitable for Modrinth / CurseForge)

# DDS Tweak

**DDS Tweak** is a client-side Fabric mod that brings configurable interface improvements, visual enhancements, and practical quality-of-life features to Minecraft. It also provides **optional client-side UI integration with Carpet DDS Addition** for compatible servers.

### Visual and interface improvements

- **Gamma Override** — adjust brightness with a configurable value.
- **Stack Count Scaling** — change the size of inventory stack-count text.
- **Compact Status Effects** — display a more compact inventory potion-effect layout.
- **Chunk Center Indicator** — help locate the center of a chunk.
- **Rail Power Placement Guide** — assist with powered-rail placement.
- **Better Snow Coverage** — improve snow visuals on supported blocks.
- **Helpful Hitboxes** — make certain narrow blocks easier to target while holding a matching item.
- **Disable End Flash** and **Hide Block Outline** — optional visual controls.
- **Player Head Window Icon** — optionally show the player avatar as the game window icon.

### Gameplay and convenience

- **Auto Eat** — optionally eat eligible inventory food when hunger drops below a configured threshold.
- **Drop Confirmation** — configure confirmation and item lists for dropping items.
- **Click Through** — configurable interaction handling for certain block and item interfaces.
- **Camera Player Selector** — select players using commands available on the connected server.
- **Book Page Scrolling**, **Quick Mute**, and **Startup Sound** options.
- **Auto Log Cleanup** — optionally remove historical log files *inside the Minecraft game directory*, based on age or count.
- **No Command Confirm** and other small configurable controls.

### Optional Carpet DDS Addition integration

On a compatible server with the appropriate permissions and advertised capabilities, DDS Tweak can provide enhanced interfaces and controls for fake players, inventory management, and Ender Chest workflows. These integrations depend on what the server supports. **DDS Tweak does not grant server permissions, replace server-side logic, or require Carpet DDS Addition for its standalone features.**

An optional **CozyUI+-derived** resource pack is bundled for the fake-player interface and remains disabled until selected by the player.

### Installation

1. Install **Fabric Loader** for Minecraft **1.21.11**.
2. Install compatible versions of **Fabric API** and **MaLiLib**; both are required.
3. Place the matching **DDS Tweak 0.1.0-beta.1** JAR in your client's `mods` folder.
4. Press **Left Alt + V** to open the configuration screen.

**Required versions for this build:** Fabric Loader 0.14.25+, Fabric API 0.141.3+1.21.11+, MaLiLib 0.27.20+, and Java 21+. Use builds compatible with Minecraft 1.21.11.

### Beta notes

This is a Beta release. Not all optional integration scenarios have been independently verified. Functionality may depend on the server, other installed mods, and your permissions. The **Happy Ghast riding speed modifier is not included in this release**.

### Credits and licenses

- Better Snow Coverage adaptation, based on work by Tobias Frischmann / [ToBinio](https://github.com/ToBinio/better-snow-coverage), MIT License.
- Optional fake-player resource pack assets adapted from [CozyUI+](https://github.com/Fogg05/CozyUI-Plus) by 零雾〇五 Fogg05, GPL-3.0.
- DDS Tweak project code declares LGPL-3.0-only. See the accompanying license and third-party notices.

## 中文项目说明（选用；英文应保留）

# DDS Tweak

**DDS Tweak** 是一款 Fabric 客户端模组，提供可配置的界面优化、视觉调整和日常操作增强，并可选与 **Carpet DDS Addition** 配合，在受支持的服务端上提供假人背包、末影箱和相关操作界面。

**主要功能：** 亮度调节、物品堆叠数字缩放、紧凑状态效果、区块中心指示、充能铁轨辅助、积雪视觉改善、细小方块辅助选取、关闭末地闪光、隐藏方块轮廓、窗口玩家头像、自动进食、丢弃确认、穿透交互、Camera 玩家选择器、书页滚轮翻页、快速静音和日志自动清理等。

**安装要求：** Minecraft 1.21.11、Fabric Loader、Fabric API、MaLiLib（Java 21+）。按 `左 Alt + V` 打开配置。无需在普通服务器安装 CDA 即可使用独立客户端功能；只有相应的 DDS 联动界面需要服务端具备兼容能力和权限。

**Beta 说明：** 当前 Beta **不包含 Happy Ghast 骑乘速度调整功能**。部分服务端联动功能依赖服务端支持。内置的 CozyUI+ 衍生假人界面资源包为可选启用。版权信息见英文说明及随包文件。

## Release version metadata

- Version number: `0.1.0-beta.1`
- Version title: `DDS Tweak 0.1.0 Beta 1 — Minecraft 1.21.11`
- Version type: **Beta**
- Minecraft: **1.21.11 only**
- Loader: **Fabric**
- Environment: **client required; dedicated server unsupported**
- Required dependencies: **Fabric API**, **MaLiLib**; Fabric Loader is a prerequisite
- Optional: **Carpet DDS Addition**, only for the integration features it supports
- Java: **21+**
- Project source URL: **do not point to the private Dev repository**; supply a publicly accessible corresponding-source copy before public binary distribution

## Modrinth moderation and legal checklist

- [x] Happy Ghast speed modifier removed from this Beta build
- [x] Beta branch isolated; main retains its feature
- [x] Multi-version CI build passed; Minecraft 1.21.11 JAR produced
- [x] Project owner reported in-game tests passed (exact scenarios unrecorded)
- [x] LGPL license text added to repository and packaged into release JAR by build configuration
- [x] Upstream MIT notices and embedded GPL-3.0 resource-pack license present in source
- [x] Removed private development repo URL from `fabric.mod.json`
- [ ] Provide corresponding source + build scripts at a public URL or in a public source archive when the binary is publicly distributed; verify license coverage of all included content
- [ ] Confirm no-CDA startup and multiplayer capability/permission scenarios if they were not covered by the reported tests
- [ ] Set appropriate **AI-generated text** disclosure for this AI-drafted listing; assess and disclose **AI-generated code/assets** as applicable
- [ ] Assess whether significant human-authored content meets Modrinth's public publication requirement
- [ ] Set **Contains derivative content** disclosure for Better Snow Coverage / CozyUI+ and credit both upstream works
- [ ] Verify actual project icon and screenshots were not generated or modified by generative AI
- [ ] Enter accurate required dependencies on the Modrinth version form
- [ ] Submit for review as Private; public release only after all outstanding legal/disclosure conditions pass

**Scope limitation:** This file prepares listing copy. It does not submit or approve a Modrinth or CurseForge project, independently determine AI authorship, or prove that a runtime scenario absent from the test record passed.
