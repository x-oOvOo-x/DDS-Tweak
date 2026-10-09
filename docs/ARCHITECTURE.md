# DDS Tweak 架构

## 1. 定位与职责

DDS Tweak 是纯客户端 Fabric 模组，负责客户端 tweak 与 Carpet DDS Addition 的客户端增强层。Carpet DDS Addition 始终负责规则、权限、真实状态和动作执行；DDS Tweak 不实现服务端玩法逻辑。

```text
Carpet DDS Addition                    DDS Tweak
├─ Rules / server gameplay             ├─ Client entrypoint
├─ authority / validation              ├─ Screens / input / rendering
├─ real inventory / Ender Chest        ├─ client prediction
├─ authoritative actions               ├─ DDS Protocol client
└─ DDS Protocol server        <----->  └─ optional DDS integrations
```

没有 DDS Tweak 时 DDS 服务端功能必须仍可使用；没有 DDS 服务端时 DDS Tweak 必须正常进服并安静关闭 DDS 集成功能。

## 2. 长期代码结构

```text
src/main/java/ddstweak/
├─ DDSTweak.java                       Fabric 客户端薄入口
├─ DDSTweakReference.java              MOD_ID / MOD_NAME / MOD_INFO
├─ DDSTweakInitHandler.java            MaLiLib 客户端基础设施注册
├─ config/                             配置聚合 / Hotkey provider
├─ gui/                                MaLiLib 配置界面
├─ feature/<feature>/                  独立纯客户端功能
├─ integration/dds/<feature>/          需要 DDS 服务端配合的客户端功能
├─ event/                              input/render/tick/world 分发
├─ network/                            transport / handshake / capability
├─ platform/minecraft/                 MC API 跨版本边界
├─ platform/malilib/                   MaLiLib API 跨版本边界
└─ mixin/                              窄 hook，不堆业务
```

约束：入口和初始化 Handler 只组装基础设施；feature/integration 持有业务；network 不反向承载 GUI/feature 业务；Mixin 只定位原版执行点并委托业务层。

## 3. DDS Protocol v1 合同

跨仓重构不得顺手修改：

```text
protocol: 1
channel: carpet-dds-addition:network/v1
hello_c2s
hello_s2c
fake_player_action_c2s
fake_player_open_menu_s2c
```

客户端 HELLO 同时声明 C2S 与 S2C capability。服务端在原版 OpenScreen 前发送 `fake_player_open_menu_s2c`；DDS Tweak 按当前连接维护有界 FIFO，将 marker 与紧随其后的原版 OpenScreen 配对。

```text
layout 1 = inventory / GENERIC_9x6
layout 3 = Ender Chest 3 rows / GENERIC_9x3
layout 6 = Ender Chest 6 rows / GENERIC_9x6
```

旧 `fake_player_sort_c2s` 和自注册 fake-player MenuType 不再属于当前合同。


### 3.1 Client transport substrate（Phase C0）

DDS Tweak 的 generic `network/` 只负责 transport / HELLO / capability / connection state，不持有 Fake Player、QCA、Undo 的 packet 常量或业务状态。

```text
DDSTweak
├─ pure client features
└─ DdsIntegrationBootstrap
   ├─ fakeplayer registers C2S/S2C + reset hook
   ├─ future QCA registers C2S/S2C + reset hook
   └─ future Undo registers C2S + reset hook
             ↓
      DdsClientCapabilityRegistry
             ↓
        DdsClientNetwork
```

`canSend(id)` 的客户端条件是：

```text
local feature registered C2S
AND
current server HELLO advertises that C2S
```

Ordered S2C（例如 open-menu marker）在网络接收顺序中立即进入 feature state；普通 UI 更新才异步切回 client thread。

### 3.2 Local ownership 与 wire bridge

Phase C-1 已证明三类网络代际：

```text
TWEAK_RAW_CLIENT         <= 1.20.1
DDS_PAYLOAD_READER_MAP   1.20.2 / 1.20.4
DDS_PAYLOAD_CODEC        >= 1.20.6
```

1.21.11 已完成 C1 paired handoff：DDS artifact 发布 loader-safe ownership marker，本地 DDS + Tweak 组合解析为 `HANDOFF_DDS_PAYLOAD_CODEC`。未带有效 marker 的旧 DDS 仍保持 `LEGACY_COMPAT`。

1.21.11 DDS artifact 的 `fabric.mod.json custom` 显式声明：

```text
dds:network_ownership
contract = 1
logical_client_owner = tweak
wire_mode = DDS_PAYLOAD_CODEC
```

同 JVM handoff 模式下 DDS 保留唯一 codec registrar，Tweak 保留唯一 logical client HELLO/session/dispatch，并通过 bridge 使用 DDS Payload class。DDS 仍然不是 Tweak 的 Fabric hard dependency。


## 4. 客户端与资源所有权

客户端 Screen、快捷键、渲染、输入、客户端预测、IPN/MiniHUD/MaLiLib 等第三方兼容和 UI 资源归 DDS Tweak。DDS 只保留对应服务端执行和协议定义。

资源 namespace 与网络 namespace 分离：

```text
DDS Tweak mod/resource namespace: dds-tweak
DDS Protocol v1 channel:          carpet-dds-addition:network/v1
```

默认假人 UI 使用 `assets/dds-tweak/textures/gui/...`。可选 CozyUI+ 内置资源包使用相同 `assets/dds-tweak/...` 路径覆盖默认 UI，激活类型为 `NORMAL`，默认不启用；资源包只改变视觉，不改变 packet、菜单 layout、槽位或服务端状态。CozyUI+ 的 NOTICE 和 GPL-3.0 许可文本必须随资源包保留。

## 5. MaLiLib 与客户端生命周期

MaLiLib 是 DDS Tweak 的必需外部依赖；DDS Tweak 不依赖 Tweakeroo。1.21.11 core 基线固定使用 Sakura-Ryoko MaLiLib `0.27.20` LTS，API package 为 `fi.dy.masa.malilib.*`。

初始化必须 eager registration：

```text
Fabric client init
  ↓
DDSTweak.onInitializeClient()
  ↓
InitializationHandler.registerInitializationHandler(...)
  ↓
DDSTweakInitHandler.registerModHandlers()
  ├─ ConfigManager.registerConfigHandler(...)
  ├─ Registry.CONFIG_SCREEN.registerConfigScreenFactory(...)
  ├─ InputEventHandler.getKeybindManager().registerKeybindProvider(...)
  └─ HotkeyCallbacks.init()
```

`DDSTweakReference.MOD_INFO` 在初始化前即携带 `DDSTweakConfigScreen` supplier，因此用户第一次打开任意 MaLiLib 配置界面时，DDS Tweak 已经存在于右上角全局模组下拉。

### 5.1 Legacy config-screen compatibility

全局配置页切换能力按**当前项目实际依赖矩阵**分两条路径：

```text
MC <= 1.20.6
→ 当前绑定 MaLiLib 0.19.x，没有原生 Registry.CONFIG_SCREEN
→ DDS Tweak 启用 LegacyConfigScreenRegistry + LegacyConfigSelectorMixin

MC >= 1.21
→ 当前各 target 绑定的 MaLiLib 均已有原生 CONFIG_SCREEN / config switcher
→ 只使用 MaLiLib 原生 registry
→ 不加载 legacy selector Mixin
```

这是一条项目适配边界，不表示 Minecraft 版本本身提供该能力。MaLiLib 历史上较早的 1.21 构建也曾缺少全局 selector，但这些版本不属于 DDS Tweak 当前依赖矩阵；当前 1.21/1.21.1 target 使用 MaLiLib 0.21.10。

旧版兼容层结构：

```text
ddstweak.api.config
├─ LegacyConfigScreenProvider
└─ LegacyConfigScreenRegistrar
        ↓ Fabric entrypoint: dds-tweak:config_screen
LegacyConfigScreenRegistry
├─ MaLiLib
├─ DDS Tweak
├─ third-party providers
└─ fallback adapters
   ├─ Litematica
   ├─ MiniHUD
   ├─ Tweakeroo
   └─ Item Scroller
        ↓
LegacyConfigSelectorMixin
→ 只增强 GuiConfigsBase 页面
```

第三方 provider 的 mod id 由其 Fabric `ModContainer` 决定，不能冒充其他模组；主动注册优先于内置 fallback adapter。重复注册、provider 异常或 optional adapter 入口变化必须 fail closed，不得阻止 DDS Tweak / MaLiLib 启动。MagicLib 不是依赖，也不作为 legacy selector 的内置条目。

以后若继续增加 1.20.4、1.20.1 或更老 target，仍沿用“没有原生 CONFIG_SCREEN 就走 legacy compatibility”的架构，但必须重新验证对应 MaLiLib 的 `GuiBase`、`GuiConfigsBase`、`WidgetDropDownList` 和 Minecraft `Screen` API；不能假定 1.20.6 实现原样可编译。

1.21.11 LTS 不以 `Registry.CONFIG_TAB` 注册页签。DDS Tweak 的五个页签按同版本 Tweakeroo 模式由 `GuiConfigsBase` 页面自身管理：

```text
[ Generic ] [ Visual ] [ Tweaks ] [ DDS ] [ Hotkeys ]
```

禁止等“第一次打开自身设置 / 第一次按快捷键 / 第一次连接 DDS”后才注册。

生命周期按客户端语义处理：client init 负责基础设施；world/server join 建立会话态；server switch/disconnect 清理 DDS capability、menu marker、camera target 和规则缓存；不得将服务端会话态跨服务器保留。

## 6. Tweakeroo 借鉴边界

借鉴薄入口、独立初始化 Handler、Config/Hotkey/Input/Render/Tick/WorldLoad 分层、Feature Toggle 与参数分离、Hotkey 一等配置、MaLiLib 原生配置 UI。

不照搬 Tweakeroo 的长期 Minecraft 版本分支模型，也不把所有 feature 业务持续集中到巨型 `Configs`、`Callbacks`、`InputHandler`；DDS Tweak 的每项功能优先收敛在 `feature/<name>` 或 `integration/dds/<name>`。

## 7. Core Version 与多版本

```text
coreVersion = 1.21.11
```

`coreVersion` 只表示新功能设计、实现、联调和规范验证主版本；`preprocessMainProject` 是源码转换图的锚点，两者必须独立记录。Phase F0 两者暂时同为 1.21.11，仅因为当前版本图只有 1.21.10 ↔ 1.21.11；扩展完整版本图时允许重新选择 preprocess anchor，而不改变 coreVersion。

DDS Tweak 长期采用单 main 多版本，不使用长期 Minecraft 版本分支：

```text
src/main/java/                       公共源码，默认首选
        │
        ├─ 小差异：preprocess        //#if / //$$，只处理局部签名/API 差异
        ├─ 重复跨版本差异：platform  minecraft / malilib facade，避免条件散落
        └─ 大差异：versions/<mc>/src  Mixin target、Screen/render/input 整体变化时直接版本 overlay

versions/<mc>/gradle.properties      每版本 Minecraft/Fabric/MaLiLib/Java 元数据
versions/mapping-*.txt               preprocess 符号映射
versions/mainProject                 preprocess 实际锚点
settings.json                        core/anchor/active/ownership 矩阵
common.gradle                        每版本构建策略
build.gradle                         版本图、聚合任务、跨版本合同
```

选择规则：

1. 能完全共享就留在 `src/main/java`。
2. 只有少量 import、方法名、参数或 descriptor 差异时，用 preprocess。
3. 同一种差异开始在多个调用点/多个版本重复时，收敛到 `platform/minecraft` 或 `platform/malilib`。
4. Mixin target、GUI/input/render 实现整体不同，或条件分支会显著破坏可读性时，使用 `versions/<mc>/src/main/java` overlay；不强求一份 Java 文件覆盖所有版本。
5. 版本 overlay 只放真正不同的实现，禁止复制整棵公共源码。
6. 每个版本显式绑定依赖；MaLiLib 坐标由该版本节点持有，不由根项目猜测。
7. DDS ownership 与“能否构建”分离。新版本先完成 Tweak build/runtime 等价验证，再通过 paired DDS/Tweak checkpoint 改 handoff marker。

Phase F0 只启用 1.21.11 + 1.21.10：

```text
1.21.11 → handoffVersions
1.21.10 → legacyCoexistenceVersions
```

因此 F0 即使生成 1.21.10 Tweak JAR，也不会声明 DDS 1.21.10 已完成 client ownership handoff。F1 通过配对实机门禁后才修改 DDS 1.21.10 marker/cutout。

## 8. Mixin 与依赖

Mixin 只捕获原版调用点并进入 feature/integration/network 层；descriptor、target、ordinal、cancellable 属于兼容合同，修改必须 runtime test。第三方兼容必须 fail-safe。

MaLiLib 不打进 DDS Tweak JAR。MaLiLib 升级属于兼容改动，必须单独验证：启动注册、右上角全局下拉、五 Tab、Hotkey 编辑/持久化、无 DDS 服务器行为和现有 DDS fake-player integration。

## 9. 身份

```text
project: DDS-Tweak
mod id:  dds-tweak
package: ddstweak
entry:   ddstweak.DDSTweak
mixin:   dds-tweak.mixins.json
```

`gradle verifyDDSTweak` 检查身份、MaLiLib foundation 与 Protocol v1 不变量。

Phase A 详细设计见 `docs/superpowers/specs/2026-09-16-malilib-config-foundation-design.md`。


### D0 QCA / Undo client ownership

DDS Tweak now contains self-contained QCA Enhanced Client and Undo/Redo shortcut implementations, but D0 deliberately keeps legacy DDS client ownership when a local unmarked DDS is installed.

```text
TWEAK_STANDALONE
    -> Tweak QCA/Undo Mixins enabled
    -> input takeover only when server HELLO has c2s_availability_schema=1

LEGACY_COMPAT
    -> Tweak QCA/Undo Mixins disabled
    -> legacy DDS QCA/Undo client remains owner
    -> Tweak Fake Player remains active

HANDOFF_*
    -> Tweak QCA/Undo Mixins enabled
    -> C1 will remove/disable the corresponding DDS logical client paths
```

QCA slot eligibility is Tweak-owned. Fake Player mirrors expose neutral QCA roles so main inventory, hotbar and Ender storage can use ABSORB/EXTRACT without importing DDS server-side `DdsDirectSlotAccess` / `DdsInventoryOwnerView`.
