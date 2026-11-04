# DDS Tweak 多版本适配指南

本文是 DDS Tweak 的长期版本扩展/回填手册。目标是把“能编译”“能启动”“能与对应 DDS 共存”“能正式 handoff”拆成独立门禁，避免每增加一个 Minecraft 版本重新试错。

> 当前 coreVersion 为 1.21.11。版本适配以 `settings.json`、`versions/<mc>/gradle.properties`、preprocess 图和实际构建产物为准，不以 README 中的单一 core 版本代替支持矩阵。

### 旧版增强容器背景与书籍滚轮版本边界（2026-10-20）

- `EnhancedStorageScreen` 与 `UnifiedPlayerInventoryScreen` 的 `render()` 在 **1.20.1 及以前**应先调用原版 `renderBackground()`，再绘制容器槽位和 Tooltip；**1.20.2 起**沿用原版提供的新版背景渲染生命周期，不手动叠加遮罩；**26.x** 保留 `super.extractBackground()`。
- `BookScrollContainerEventHandlerMixin` 的 `ContainerEventHandler#mouseScrolled` 从 **1.20.2** 起使用四个 `double` 参数（鼠标 X/Y、水平/垂直滚动）；1.20.1 及以前使用三个。版本门槛为 `MC >= 12002`，必须检查生成 Mixin callback 签名与实际启动。
- 实测基线：1.20.1 安装 DT 后打开普通箱子和潜影盒时缺少背景遮罩；1.20.4、1.20.6 以及 1.21.1 及以上的相关遮罩已由用户反馈正常。1.20.2 的旧构建在进入游戏前因 BookScroll Mixin 方法签名不匹配而崩溃，遮罩尚无法验收。其他 1.20.1 以下版本仅有源码层面的同类风险，不能列为实机 FAIL。
- 修复验收应覆盖箱子、潜影盒、Provider 背包与末影箱，分别核查受影响旧版本、1.20.2 的启动和遮罩、1.20.4/1.20.6 的视觉回归、26.x extractor；`buildAndGather` 的全版本构建成功不能代替运行时验证。

## 1. 先确认版本属于哪一代

DDS 客户端网络目前分三代：

```text
TWEAK_RAW_CLIENT         <= 1.20.1
DDS_PAYLOAD_READER_MAP   1.20.2 / 1.20.4
DDS_PAYLOAD_CODEC        >= 1.20.6
```

同一代可以批量做兼容基线，但 ownership 仍必须由对应 DDS artifact 明确声明。

下表是历史 F2 的 `DDS_PAYLOAD_CODEC` 回填基线，并非当前 21 个 activeVersions 的完整依赖表；最新范围以 `settings.json` 和各 `versions/<mc>/gradle.properties` 为准：

| 构建 target | 声明支持 Minecraft | Fabric API | MaLiLib | Java | 当前 ownership |
| --- | --- | --- | --- | --- | --- |
| 1.21.11 | 1.21.11 | 0.141.3+1.21.11 | 0.27.20 | 21 | handoff |
| 1.21.10 | 1.21.9–1.21.10 | 0.138.4+1.21.10 | 0.26.8 | 21 | handoff |
| 1.21.8 | 1.21.6–1.21.8 | 0.136.1+1.21.8 | 0.25.7 | 21 | legacy-coexistence |
| 1.21.5 | 1.21.5 | 0.128.2+1.21.5 | 0.24.3 | 21 | legacy-coexistence |
| 1.21.4 | 1.21.4 | 0.119.4+1.21.4 | 0.23.5 | 21 | legacy-coexistence |
| 1.21.3 | 1.21.2–1.21.3 | 0.114.0+1.21.3 | 0.22.8 | 21 | legacy-coexistence |
| 1.21.1 | 1.21–1.21.1 | 0.116.12+1.21.1 | 0.21.10 | 21 | legacy-coexistence |
| 1.20.6 | 1.20.5–1.20.6 | 0.100.8+1.20.6 | 0.19.0 | 21 | legacy-coexistence |

`handoffVersions` 与 `legacyCoexistenceVersions` 以 `settings.json` 为唯一迁移状态合同。**能编译不等于可以 handoff。**

## 2. 本地与 CI 前置

### 2.1 工具链

CI 使用：

```text
Gradle runtime: 9.6
CI JDK:         25
Minecraft target toolchain: per-target Java (1.14.4: 8; 1.21.11: 21; 26.3: 25)
Fabric Loader dependency: per-target gradle.properties (26.3: >=0.19.5)
```

CI 使用 JDK 25 运行 Gradle；生成的 Minecraft 产物遵循各节点自己的 Java toolchain，不能将历史 F2 的 Java 21 要求套用到 26.3。

仓库正式提交 Gradle Wrapper，并固定 Gradle 9.6.0。Windows/PowerShell 标准入口：

```powershell
.\gradlew <task>
```

Linux/macOS：

```bash
./gradlew <task>
```

Wrapper 是仓库合同；不要要求开发机额外安装系统 Gradle，也不要用系统 `gradle` 覆盖 Wrapper 版本。首次运行会下载 Wrapper 指定的 Gradle distribution。

### 2.2 模组前置

DDS Tweak 是纯客户端模组：

- Fabric Loader：必需。
- MaLiLib：**外部硬依赖，不打进 DDS Tweak JAR**。
- Carpet DDS Addition：不是 mod-load 硬依赖；仅 DDS integration / paired runtime 测试需要。
- Fabric API：构建按每 target 绑定版本，只 include 当前实际使用的 Fabric API modules。
- IPN / MiniHUD / ItemScroller 等：兼容对象，不是必需前置。

`runClient` 会从 Gradle 依赖自动带入对应 MaLiLib；不要再向该版本的开发 `mods` 目录手工塞第二份 MaLiLib。

## 3. 新增版本的固定顺序

每次扩版本严格按下面顺序：

```text
1. 确认 DDS 对应版本存在 + 网络代际
2. 新建 versions/<target>/gradle.properties
3. 接入 settings.json
4. 接入 preprocess node + mapping edge
5. 绑定 Fabric API / MaLiLib / Java / loader metadata
6. 处理资源包 pack metadata
7. 先尝试共享源码编译
8. 修真实出现的 API 差异
9. verify + test + buildAndGather
10. runClient 无 DDS
11. 与对应 DDS legacy 组合实测
12. 若 artifact 声明支持版本范围，补测范围边界
13. 最后才做 paired DDS/Tweak ownership cutover
```

不要先猜一批差异并复制源码。让最老 target 的实际编译错误暴露边界，再按架构层级处理。

## 4. 差异应该放在哪里

优先级：

```text
完全相同
→ src/main/java

少量 import / 方法 / descriptor / 输入签名差异
→ preprocess //#if / //$$

同一种差异在多处重复
→ platform/minecraft 或 platform/malilib facade

整个 Mixin / Screen / render / input 实现显著不同
→ versions/<mc>/src/main/java overlay
```

禁止为了“省条件判断”把一整套共享类复制到版本 overlay。

F2 已形成两个真实 facade：

```text
platform/minecraft/MinecraftCompat
→ Identifier / ResourceLocation
→ NBT getIntOr / getStringOr

platform/malilib/MalilibCompat
→ 老 MaLiLib Config 构造器
→ FileUtils / JsonUtils 文件 API
```

## 5. 当前已确认的 Minecraft API 边界

### 5.1 Identifier / ResourceLocation

当前需要三段处理：

```text
>= 1.21.11
Identifier.fromNamespaceAndPath(...)

1.21.1–1.21.10
ResourceLocation.fromNamespaceAndPath(...)

<= 1.20.6
new ResourceLocation(namespace, path)
```

不要只条件编译构造表达式而保持错误的返回类型/import；类型名本身也是版本边界。

### 5.2 CompoundTag 默认值 getter

```text
>= 1.21.5
getIntOr / getStringOr

< 1.21.5
contains + getInt/getString
```

重复调用必须走 `MinecraftCompat.intOr/stringOr`，不要把条件散到 network/QCA/Fake Player 各处。

### 5.3 Slot#getNoItemIcon

```text
>= 1.21.4
ResourceLocation/Identifier

< 1.21.4
Pair<atlas, icon>
```

这是返回类型变化，适合局部 preprocess。

### 5.4 GUI matrix

```text
>= 1.21.6
Matrix3x2fStack
pushMatrix / popMatrix

< 1.21.6
PoseStack
pushPose / popPose
```

涉及缩放/平移时同时检查参数维度，例如旧 `scale(x,y,z)`。

### 5.5 EditBox

`EditBox#setCentered(true)` 当前只在较新的目标可用；F2 以 `MC >= 1.21.8` 条件启用。旧版使用轻量 `LegacyCenteredEditBox` 兼容层：

- 按当前文本实际宽度计算水平偏移；
- 只在渲染时临时平移 EditBox；
- 点击时用同一偏移反算鼠标坐标，保证光标位置与视觉位置一致；
- 保留原始 EditBox 的完整命中区域；
- 不使用反射，也不复制整套 Screen。

这类“API 不存在但只是视觉行为差异”的问题优先做小型兼容层，不要为一个方法复制整页 GUI。

### 5.5a Silent Auto Eat 背包预览渲染边界（2026-10-24）

`InventoryScreen.extractRenderState(LivingEntity)` 在 1.21.11 和
26.1.2/26.2/26.3 暴露可复用的实体 `EntityRenderState` 消费边界。
`InventoryPreviewMixin` 只对当前 `LocalPlayer` 的背包预览
`ArmedEntityRenderState` 进行 `ItemModelResolver` 视觉投影，保留真实副手交易。
`common.gradle processResources` 必须在低于 1.21.11 的目标物理移除该 Mixin；
其他屏幕或世界实体不受影响。验收需覆盖编译后的类、目标最终 Mixin JSON、
左右主手、第三人称预览及一/三人称游戏世界不受影响，且不同实际 GUI API
版本必须启动测试；构建绿不代表完成运行验收。

### 5.5b Silent Auto Eat 渲染方法边界（2026-10-26）

- 1.21.10 及更旧版本：`AbstractContainerScreen.renderSlot` / `renderTooltip`，旧版 Tooltip 暂保留可组合 getter 包装，仍需运行时验证。
- 快捷栏及背包副手视觉（1.20.1 专项）：原先新增的 `ItemInHandLayer` 投影源码虽然编译，但 `common.gradle` 仍在 `mcVersion < 1_21_11` 时从最终 `dds-tweak.mixins.json` 删除 `InventoryPreviewMixin`，因此 1.20.1 实际从未加载该预览适配器。现仅在 1.20.1 与 1.21.11+ 保留该 Mixin，并在 `processResources` 校验每版本的生成 JSON；1.20.1 必须重新验证背包副手槽图标与 3D 人物预览、真实背包及关闭 GUI 后恢复，不能以构建成功代替实机成功。1.14.4 日志则已证明能够触发但 `SWAP(button=40)` 后苹果仍在来源槽且副手为空；只对 1.14.4 用受限的三次原版 `PICKUP`（来源槽→动态定位的副手槽→来源槽）代替交换，严格校验空光标与槽位权限。其他活动版本不改交换策略，1.14.4 的客户端实测仍需确认。
- 1.21.11：`Gui.renderItemHotbar`，容器 `renderSlot` / `renderTooltip`。
- 26.1.2：`Gui.extractItemHotbar`，容器 `extractSlot` / `extractTooltip`。
- 26.2 与 26.3：`Hud.extractItemHotbar`，容器 `extractSlot` / `extractTooltip`。

跨 21 个活动目标的 `verifySilentAutoEatVanillaTargets` 由现有 `buildAndGather` 统一运行。它读取 Minecraft 的实际 class 字节码，检查目标方法及必要的局部存储和读取调用，并将报告保存到对应版本的 `build/reports/silent-auto-eat/vanilla-targets.json`。这属于静态字节码验证，**不能替代实际客户端的 Mixin 注入、Tooltip、手部/快捷栏及 QuickCraft/IPN/ItemScroller 联调**。

### 5.6 输入事件与 preprocess 标记

1.21.9 起 GUI 键鼠输入使用新的 `KeyEvent` / `MouseButtonEvent` 路径；旧版本继续使用 `key/scan/modifiers` 与 `x/y/button` 签名。Screen 内属于局部差异，使用 preprocess。

ReplayMod preprocessor 的条件代码必须以 `//$$` 开头；`//$` 只是普通 Java 注释。这个错误尤其危险，因为缺少 override 通常不会导致编译失败。例如旧版 `mouseClicked(double,double,int)` 若误写成 `//$`：

```text
源码能编译
→ Screen 能正常渲染
→ hover/高亮正常
→ 旧版 mouseClicked override 实际不存在
→ 点击完全无效
```

因此遇到“能 hover 但点不动”时，先检查预处理后的目标源码是否真的生成了输入 override，再查网络层：

```powershell
Get-Content .\versions\1.21.4\build\preprocessed\main\java\ddstweak\integration\dds\fakeplayer\screen\DdsFakePlayerScreen.java |
  Select-String "mouseClicked|LegacyCenteredEditBox|RenderType"
```

`//$$` 同样适用于旧版 import、blit fallback、方法声明和类声明；不能只检查主逻辑行。

### 5.7 GUI texture blit / RenderType / RenderPipelines

GUI texture blit 不是“两段式”边界，而是三段：

```text
>= 1.21.6
RenderPipelines.GUI_TEXTURED

1.21.2–1.21.5
RenderType::guiTextured

<= 1.21.1
旧 GuiGraphics.blit(ResourceLocation, ...)
```

1.21.2 开始 `GuiGraphics.blit` 的 GUI texture 路径引入 `RenderType` 参数；1.21.6 再切到 `RenderPipeline`。因此不能把所有 `<1.21.6` 版本都落回旧 direct blit。该错误可以通过编译，却会在 1.21.2–1.21.5 出现“菜单/Slot 仍可操作，但背景、按钮等纹理不渲染”的运行时症状。

### 5.8 End flash

DDS Tweak 的 End flash Mixin 只在存在对应渲染类/API 的版本启用。旧 target 在 `processResources` 阶段物理移除不可用 Mixin 条目。

不要只让 Java 类“编译成空壳”却仍把不存在的 target 留在 mixin json。

Minecraft 26.3 在 `SkyRenderer#renderEndFlash` 的方法参数开头新增 `RenderPass`，此前 26.1.2、26.2 使用 `PoseStack, float, float, float`。DT 此处只需要在 HEAD 取消该方法，不读取参数，因此 `@Inject` callback 必须只接收 `CallbackInfo`，避免不必要的目标参数签名耦合。保留 `cancellable=true` 和实际注入要求；不得用 `require=0` 掩盖运行时未命中。

这类签名变化必须增加实际 26.3 启动、进入世界、天空渲染 smoke。仅编译或源码级 contract 通过不足以证明运行时注入成功。

### 5.9 NoCommandConfirm

“确认命令执行”不是所有 1.21.x 都有的原版机制。DDS Tweak 以 **MC >= 1.21.6** 作为功能存在边界：

```text
>= 1.21.6
→ 保留 feature.nocommandconfirm.ClientPacketListenerMixin
→ Tweaks 页显示 noCommandConfirm

< 1.21.6
→ processResources 物理移除该 Mixin
→ Tweaks 页不显示该选项
→ feature Mixin 不会进入运行时，配置项也不进入 Tweaks 列表
```

典型错误：

```text
InvalidInjectionException:
could not find any targets matching 'openCommandSendConfirmationWindow'
```

这不是“Mixin 名字没映射好”的默认结论；先确认目标原版功能是否在该 Minecraft 版本真实存在。1.21.4 就属于“原版机制尚不存在”，不能通过 `require=0` 掩盖，也不应硬找一个并不存在的替代注入点。

### 5.10 Stack-count 渲染边界

Stack count 的原版拆分点是 **1.21.2**：

```text
>= 1.21.2
GuiGraphics.renderItemCount(...)
→ 可在 HEAD 注入，只替换数量文字并 cancel 该私有子步骤

<= 1.21.1
数量文字仍内嵌在 renderItemDecorations(...)
→ 不能继续注入 renderItemCount（方法不存在）
→ 也不能 cancel 整个 renderItemDecorations，否则耐久条/冷却等装饰会一起丢失
→ 只 Redirect 其中的 drawString 数量文字调用
```

旧版 Redirect 使用 vanilla 已计算好的文字左上角，按 `x + font.width(text)`、`y + font.lineHeight` 还原右下锚点后缩放，因此只改变数量文字，不改变耐久条、冷却和其他 item decoration。

典型运行时错误：

```text
InvalidInjectionException:
could not find any targets matching 'renderItemCount' in GuiGraphics
```

如果构建阶段只出现 `Unable to determine descriptor for @Inject target method` 警告而仍然 green，不代表该 Mixin 可运行；最老 target 必须实际 `runClient`。

### 旧版配置滑块超宽纹理兼容（MC 1.14.4–1.20.1）

MaLiLib 旧版 `WidgetSlider` 使用 `textures/gui/widgets.png` 的定宽按钮图集。窗口变宽时，背景纹理的 UV 读取可能超出原按钮范围而出现缺失。DT 仅在自身配置界面的旧版本滑块中采用安全的分段背景纹理绘制；X/Y、宽高、文字、滑块柄位置、回调以及 MaLiLib 的鼠标按下/释放行为保持不变。不影响其他模组的 MaLiLib 界面。

从 MC 1.20.2 起，MaLiLib 使用 GUI Sprite 绘制，不能直接套用旧图集补丁。应分别检查大/小窗口、不同 GUI 缩放、各类型滑块、拖动/重置、切换输入模式及保存后的表现。构建与单测不能替代实机视觉验证。

## 6. MaLiLib 版本差异

1.20.6 / MaLiLib 0.19 与新 LTS 的主要差异已经实际遇到：

```text
new ConfigBoolean(name, value)
new ConfigHotkey(name, key)
Config*.apply(prefix)

FileUtils.getConfigDirectoryAsPath()
FileUtils.createDirectoriesIfMissing(...)
JsonUtils.parseJsonFileAsPath(...)
JsonUtils.writeJsonToFileAsPath(...)
```

这些在 0.19 不完整存在，因此统一通过 `MalilibCompat`。

原则：

- 业务层不直接判断 MaLiLib 版本。
- 旧 API 若只是构造/文件读写差异，放 facade。
- 不为旧版本复制整个 `Configs`。
- 配置适配后必须重新测：首次打开 MaLiLib、五 Tab、Hotkey 保存、重启持久化。

### 6.1 全局 Config Screen selector

当前项目采用下面的实际边界：

```text
MC <= 1.20.6
→ 当前 target 使用 MaLiLib 0.19.x
→ 无原生 Registry.CONFIG_SCREEN / 全局 selector
→ DDS Tweak 启用 legacy compatibility

MC >= 1.21
→ 当前 target 使用的 MaLiLib 版本均已有原生 config-screen registry/switcher
→ 使用 MaLiLib 原生实现
→ DDS Tweak 不注入 LegacyConfigSelectorMixin
```

因此当前 active versions 中只有 `1.20.6` target 需要 legacy selector；以后若加入 1.20.4/1.20.1 等更老 target，也归入 legacy 路径，但要按对应 MaLiLib API 重新适配。这个边界是**项目依赖策略**，不是 Minecraft 自身能力边界。MaLiLib 历史上的早期 1.21 构建并非全部带全局 selector；当前项目的 1.21/1.21.1 target 固定使用 MaLiLib 0.21.10，因此直接走 native path。

1.20.6 legacy registry 当前内置：

```text
MaLiLib
DDS Tweak
Litematica       optional fallback adapter
MiniHUD          optional fallback adapter
Tweakeroo        optional fallback adapter
Item Scroller    optional fallback adapter
```

同时公开 Fabric entrypoint `dds-tweak:config_screen`。第三方模组实现 `ddstweak.api.config.LegacyConfigScreenProvider` 后可主动注册配置 Screen；mod id 从 entrypoint 所属 `ModContainer` 获取，provider 不能冒充其他模组。主动注册优先于内置 fallback adapter；重复注册、provider 异常、反射入口不匹配全部 fail closed，只记录 WARN，不影响基础配置 GUI 启动。

MagicLib 只可作为实现参考，不是依赖，也不自动加入 legacy registry。

## 7. 资源包 / 数据包版本

### 7.1 本项目当前踩坑的是“资源包”，不是数据包

Fake Player CozyUI+ 是内置 **resource pack**：

```text
resourcepacks/dds_fake_player_ui/pack.mcmeta
```

DDS Tweak 当前没有内置 data pack。资源包和数据包的 `pack_format` **不是同一个版本号体系**；未来若增加 data pack，必须单独维护 `data_pack_format`，不得复用本节数值。

### 7.2 当前资源包生成矩阵

源码中的 `pack.mcmeta` 不能作为多版本最终产物直接复用。构建时由 `common.gradle/processResources` 按 target 重写：

| Target / 范围 | 当前资源包 metadata |
| --- | --- |
| 1.21.11 | `min_format=75, max_format=75` |
| 1.21.9–1.21.10 | `min_format=69, max_format=69` |
| 1.21.8 target（覆盖 1.21.6–1.21.8） | `pack_format=64, supported_formats=[63,64]` |
| 1.21.5 | `pack_format=55` |
| 1.21.4 | `pack_format=46` |
| 1.21.2–1.21.3 | `pack_format=42` |
| 1.21–1.21.1 | `pack_format=34` |
| 1.20.5–1.20.6 | `pack_format=32` |

1.21.9+ 使用新 `min_format/max_format` 结构；更老目标仍需要 `pack_format`。

### 7.3 典型故障

若日志出现：

```text
Couldn't load pack metadata
No key pack_format
Missing metadata in pack dds-tweak:dds_fake_player_ui
```

优先检查**构建产物**，不是只看源码：

```powershell
Get-Content .\versions\1.21.8\build\resources\main\resourcepacks\dds_fake_player_ui\pack.mcmeta
```

源码 `pack.mcmeta` 可以是 core 模板，但最终 `build/resources/main` 必须符合当前 target。

资源包解析失败后出现的 `getDevice() before it was initialized` 等异常可能只是启动中断后的次生错误；先处理日志中的第一个确定性错误。

## 8. 构建与测试命令

以下命令以仓库固定的 Gradle Wrapper 为标准。

### 8.1 静态合同

```powershell
.\gradlew verifyDDSTweak
.\gradlew test
```

### 8.2 功能修改与 contract test 同步

版本适配中的功能、依赖或边界一旦改变，对应 contract test 必须在同一个 PR 同步修改。尤其包括：

```text
preprocess / Mixin 启用边界
MaLiLib / Fabric / Java 依赖边界
public/legacy integration API
optional adapter 列表与优先级
resource pack metadata
DDS capability / ownership / wire mode
默认配置和持久化合同
```

测试应验证“必须成立的行为合同”，不要无必要地绑定私有 helper 或等价 guard 的字面写法。若生产行为有意变化导致旧测试失败，应更新测试以表达新合同，并同步修改本文件或架构/功能文档；不能为了旧字符串断言而恢复已经废弃的实现。

每次功能修改后的最小检查：

```text
[ ] 生产代码与版本边界一致
[ ] 对应 contract test 已新增/更新
[ ] ARCHITECTURE / VERSION_ADAPTATION / 功能计划中相关描述已同步
[ ] verifyDDSTweak + test + build 通过
[ ] 涉及 GUI/Mixin/网络/第三方集成时完成相应 runtime gate
```

### 8.3 全版本构建

```powershell
.\gradlew buildAndGather --stacktrace
```

成功后检查：

```text
build/releases/
```

JAR 数量必须等于 `settings.json.activeVersions` 数量。

### 8.4 单 target 客户端

```powershell
.\gradlew :1.21.11:runClient
.\gradlew :1.21.10:runClient
.\gradlew :1.21.8:runClient
.\gradlew :1.21.5:runClient
.\gradlew :1.21.4:runClient
.\gradlew :1.21.3:runClient
.\gradlew :1.21.1:runClient
.\gradlew :1.20.6:runClient
```

每个 target 使用独立 `run/<target>`，不要让不同版本共用同一个开发运行目录。

### 8.5 对应 DDS 服务端

在 `Carpet-DDS-Addition-Dev`：

```powershell
.\gradlew :<target>:runServer --no-parallel
```

例如：

```powershell
.\gradlew :1.21.8:runServer --no-parallel
.\gradlew :1.21.4:runServer --no-parallel
.\gradlew :1.20.6:runServer --no-parallel
```

客户端连接 `localhost`。

### 8.6 F2 代表性 runtime gate

兼容基线至少测试：

```text
1.21.8 → F2 高版本边界        PASS（2026-09-20）
1.21.4 → 中间 API 边界        PASS
1.20.6 → 最老 + MaLiLib 0.19  PASS（当前 head 完整 legacy selector 矩阵通过）
```

1.20.6 当前 head 已完成 legacy selector 全矩阵实测：MaLiLib ↔ DDS Tweak、Litematica / MiniHUD / Tweakeroo / Item Scroller fallback adapters、MagicLib 共存以及第三方 provider 均通过。selector runtime gate 已关闭；后续进入 paired DDS ownership cutover。

每个版本检查：

- 启动无 Mixin apply error。
- MaLiLib 配置可打开，DDS Tweak 可见。
- Fake Player inventory / Ender Chest。
- QCA ABSORB / EXTRACT：legacy-coexistence 阶段先验证普通容器路径；Fake Player mirror 内的 QCA 需单独记录。旧 DDS QCA 客户端可能无法识别 Tweak-owned mirror slot，这一项在 paired handoff 后必须转为强制通过。
- 打开 → 操作 → 关闭 → 再打开。
- 退出服务器 → 重连。
- 无 duplicate payload / duplicate screen / 双输入处理。
- 无 DDS 时纯客户端功能仍正常。

Undo/Redo 只在 DDS 实际支持的版本测试；当前服务端实现以 `MC >= 1.21.9` 为边界，不要要求 1.21.8 及以下出现 Undo/Redo。

## 9. “一个 JAR 覆盖多个 MC 版本”的额外门禁

`runClient` 只运行 build target 本身。例如：

```text
:1.21.10:runClient
只证明 1.21.10 runtime
不能自动证明同一个 JAR 在 1.21.9 runtime 正常
```

因此只要 `minecraft_dependency` 声明范围，正式发布前必须补实际范围边界测试：

```text
1.21.10 artifact → 至少实际测 1.21.9 + 1.21.10
1.21.8 artifact  → 至少实际测 1.21.6 + 1.21.8
1.21.3 artifact  → 至少实际测 1.21.2 + 1.21.3
1.21.1 artifact  → 至少实际测 1.21 + 1.21.1
1.20.6 artifact  → 至少实际测 1.20.5 + 1.20.6
```

可以使用真实 Fabric 测试实例安装 CI JAR；不要把“同 target 编译成功”当成范围内所有版本已验证。

同理，必须确认该 target 使用的 MaLiLib artifact 自己也允许声明的 Minecraft 范围，不能只看 Maven 坐标能解析。

## 10. Ownership / handoff 门禁

新增版本必须先保持 legacy：

```text
Tweak compatibility
→ build green
→ runtime green with current DDS
→ paired DDS/Tweak PR
→ DDS 发布 ownership marker
→ DDS 物理裁掉 logical-client Mixins
→ Tweak 移入 handoffVersions
→ paired runtime 再验证
→ 才允许合并
```

禁止因为 Tweak 已能启动就提前：

- 修改 `handoffVersions`；
- 让 DDS 发布 `logical_client_owner=tweak`；
- 移除 DDS client hook。

legacy-coexistence 还有一个额外约束：同一客户端 JVM 内旧 DDS 与 DDS Tweak 都可能发送 HELLO，而服务端的 connection-scoped capability 状态按最近一次 HELLO 更新。旧 DDS 已不实现 Fake Player action C2S，因此它的 HELLO 不包含 `fake_player_action_c2s`。如果它在 Tweak 之后再次发送 HELLO，会造成“按钮仍能 hover/高亮、客户端也认为服务端支持 action，但服务端按 client advertisement 静默拒绝 action”的假成功状态。为消除该时序竞争，Tweak 在 `LEGACY_COMPAT` 下发送任一 feature C2S 前先重申一次合并后的 HELLO；HELLO 与 feature packet 通过同一连接按顺序发送，服务端先更新 capability 再处理 feature packet。 这是 legacy-coexistence 的协议防御，不应直接当成 UI 点击故障的默认根因；本次 1.21.4“hover 正常但点击无效”最终确认是旧版输入分支的 preprocess 标记错误。

paired handoff 必测：

- HELLO 只发送/处理一次。
- codec registrar 不重复。
- Fake Player GUI 不重复打开。
- QCA 不双重取消/预测。
- 支持版本上的 Undo/Redo 一次按键只执行一次。
- reconnect 后无 stale session/menu marker。

## 11. 常见报错与判断

| 现象 | 通常原因 | 处理 |
| --- | --- | --- |
| `ConfigBoolean/ConfigHotkey` 构造器不存在 | 老 MaLiLib API | 放入 `MalilibCompat` |
| `FileUtils/JsonUtils ...AsPath` 不存在 | 老 MaLiLib 文件 API | `MalilibCompat` |
| `Identifier/ResourceLocation` 找不到或返回类型不兼容 | MC 命名/API 边界 | `MinecraftCompat` + 必要 preprocess |
| `getIntOr/getStringOr` 不存在 | NBT API 边界 | `MinecraftCompat.intOr/stringOr` |
| `getNoItemIcon` override 返回类型不兼容 | Slot API 变化 | 局部 preprocess |
| `PoseStack cannot be converted to Matrix3x2fStack` | GUI matrix 变化 | 局部 preprocess |
| 1.21.2–1.21.5 Fake Player 可操作但背景/按钮透明 | GUI texture blit 错走旧 direct overload | 使用 `RenderType::guiTextured`；1.21.6+ 才用 `RenderPipelines.GUI_TEXTURED` |
| Fake Player 按钮可 hover/高亮但点击完全无效 | 旧版 `mouseClicked(double,double,int)` preprocess 分支未真正生成，常见原因是 `//$` 单 `$` 标记 | 先查 `build/preprocessed`；确认存在旧签名 override 和 `//$$` 标记，再查网络层 |
| `EditBox.setCentered` 不存在 / 数字在旧版左偏 | GUI API 变化 | `>=1.21.8` 用原生 `setCentered(true)`；旧版走 `LegacyCenteredEditBox` |
| `No key pack_format` / `Missing metadata in pack` | 资源包格式结构不匹配 | 检查生成后的 `pack.mcmeta` |
| `Unable to determine descriptor for @Inject target` | Mixin target/descriptor 跨版本可疑 | 不能只看 build；必须 runClient 验证 |
| 1.20.6/1.21.1 启动时报 `renderItemCount` target not found | `renderItemCount` 直到 1.21.2 才拆出，旧版数量文字仍在 `renderItemDecorations` | `>=1.21.2` 注入 `renderItemCount`；`<=1.21.1` Redirect `renderItemDecorations` 内的 count `drawString`，不要 cancel 整个装饰方法 |
| `openCommandSendConfirmationWindow` target not found | 目标原版机制在旧版本不存在 | 按版本物理移除 Mixin，并隐藏无效配置；当前边界为 <1.21.6 |
| optional MiniHUD/IPN/ItemScroller class loading WARN | 可选模组未安装 | 若无 Mixin apply failure 通常可忽略 |
| CI run `cancelled` | 同分支连续 push 被 concurrency 取消 | 只看最新 head SHA 的 run |
| contract test 在 facade 重构后失败但所有 target 已编译 | 测试硬匹配旧源码实现 | 改为验证合同/默认值/facade，不恢复旧实现 |
| `:1.20.6:compileJava` 报 `box` 等变量不存在，且变量声明位于 preprocess 分支 | 旧版声明仍是 `//$` 普通注释，分支没有激活 | 查预处理产物和源文件字面标记；必须是 `//$$` |
| `compileTestJava` 突然出现“字符文字的行结尾不合法”等大量语法错误 | 自动补丁/字符串拼接破坏了测试源码 | 先打开实际提交后的测试文件，不要从后续连锁语法错误反推业务代码 |
| 自动脚本本想写入 `//$$`，提交后却变成 `//$` | JavaScript `String.replace` replacement string 会把 `$$` 解释成单个字面 `$` | 写 preprocess 标记时用 replacement callback/直接拼接，并提交后重新读取源码确认 |
| Windows checkout/clean 出现 `Filename too long` | Runner 清理 Loom/Gradle 深层缓存路径失败 | 若后续 checkout/build 继续执行，不要当成当前失败根因；真正失败看第一条 task ERROR，可另行启用 Git longPaths |
| 启动日志后面出现一串渲染/设备异常 | 前面已有资源/Mixin初始化失败 | 从第一条确定性 ERROR 往前排查 |

## 12. Mixin 警告的处理原则

`compileJava` 通过并不保证 Mixin 可以在目标版本运行。

出现：

```text
Unable to determine descriptor for @Inject target method
```

必须至少做一次对应 target `runClient`。若运行时出现：

```text
Mixin apply failed
InvalidInjectionException
NoSuchMethodError
ClassNotFoundException（目标为必需类）
```

则必须：

1. 核对目标方法是否改名/删掉；
2. 调 descriptor / ordinal / args；
3. 若该功能在旧版不存在，按版本物理移除 Mixin；
4. 只有差异显著时才上 overlay。

不能把所有 Mixin WARN 统一当成“可忽略”。

## 13. CI 判断

工作流会对同一 PR 使用 concurrency：

```text
新 push
→ 前一个 run cancelled
→ 最新 head SHA run 接管
```

所以连续修复时大量 cancelled 是正常现象。

F2 兼容 PR 最低绿灯：

```text
verifyDDSTweak
+ all active target compile/package/remap
+ canonical tests
+ buildAndGather
+ rolling prerelease publish
```

但 CI green 仍不能替代 runtime gate。

## 14. 建议的排查顺序

遇到旧版本启动/构建失败时按以下顺序，避免被后续噪声误导：

```text
1. 第一条 compile ERROR / runtime ERROR
2. target 的 Minecraft/Fabric API/MaLiLib 坐标
3. preprocess 生成源码（先确认条件分支/override/import 是否真的存在）
4. GUI 问题拆成 texture/render、geometry/hover、input override、transport/server 四层验证
5. build/resources/main 中生成资源
6. Mixin target / descriptor
7. contract/test 源码本身是否被自动补丁破坏
8. optional-mod WARN / Windows 清理 WARN
9. 后续连锁异常
```

必要时清理后单 target 重跑：

```powershell
.\gradlew clean
.\gradlew :<target>:runClient --stacktrace
```

不要为了排查一个版本先修改 ownership。

## 15. F2 / 1.21.4 回填实战复盘

这次 1.21.4 回填暴露的问题具有代表性，后续扩版本优先按这里复用排查方法。

| 阶段现象 | 最终确认原因 | 以后怎么查 |
| --- | --- | --- |
| Fake Player 菜单 Slot 可操作，但背景/按钮纹理透明 | 1.21.2–1.21.5 的 GUI texture blit 需要 `RenderType::guiTextured`，不能直接走 <=1.21.1 的旧 overload | GUI blit 至少按 `<=1.21.1 / 1.21.2–1.21.5 / >=1.21.6` 三段验证 |
| 按钮会 hover/高亮，但点击完全无效 | 旧版 `mouseClicked(double,double,int)` 行误写成 `//$`，没有被 preprocess 激活 | hover 正常时先看目标版本的 preprocessed Screen 是否真的含 mouseClicked override |
| 排查一度转向 menu / HELLO / capability | 这些路径存在独立兼容风险，但本次 runtime 复测没有支持它们作为点击故障根因 | 把判断当成可证伪假设；每次修复都用同一 runtime 症状复测，失败就撤销“已定位”结论 |
| 间隔数字如 `12` 在旧版明显左偏 | 老版本没有 `EditBox#setCentered(true)` | 视觉小差异用轻量兼容层；不要复制整个 Screen |
| 修改发送 API 后 canonical test 失败 | 合同测试硬匹配旧 helper 名称/调用路径 | 测试验证行为合同和边界，不要过度绑定内部实现 |
| 新增 preprocess 回归测试反而失败 | 测试自己检查了错误 marker；后续自动补丁又破坏测试源码 | test failure 先判断是生产代码、合同变化还是测试本身错误 |
| 居中兼容分支在 1.20.6 报 `box` 未定义 | 自动补丁把预期的 `//$$` 写成了 `//$` | 自动编辑 preprocess 代码后必须“写入 → 重新 fetch → 字面确认” |
| Runner 出现大量 `Filename too long` | Windows 清理缓存的噪声，不是该轮真实失败 | 以 Gradle 首个失败 task / 首条 compiler error 为准 |

### 15.1 GUI 故障按四层拆开

不要把“按钮不工作”整体当成一个问题。固定拆成：

```text
A. texture/render：背景、图标是否画出来
B. geometry/hover：鼠标命中是否正确
C. input override：mouseClicked/keyPressed 是否真的进入目标 Screen
D. transport/server：C2S 是否发送、服务端是否接受并执行
```

本次 1.21.4 的两个典型症状：

```text
纹理透明但 Slot 能操作
→ 输入/容器不一定坏
→ 先查 render API

hover 正常但 click 无效
→ 几何命中基本成立
→ 先查 input override
→ 不要第一时间跳到网络
```

### 15.2 preprocess 代码的提交门禁

任何新增或修改的条件代码，提交前至少完成：

```text
[ ] 源文件中的条件边界正确
[ ] 被注释分支使用 //$$，不是 //$
[ ] 最老 target 的 build/preprocessed 中该分支实际展开
[ ] 最老 target compileJava 成功
[ ] 若是 GUI 输入/渲染，至少做一次 runtime
```

如果通过脚本修改源码，尤其是 JavaScript `String.replace`，不要直接把 `$$` 放进 replacement string；优先使用 callback 返回完整文本。提交后重新 fetch 文件确认字面内容，不能只相信脚本返回“成功”。

### 15.3 CI / runtime 的判定纪律

本轮已经验证：**build green、test green、runtime green 是三种不同证据**。

- 编译成功不能证明旧版输入 override 已存在；缺少 override 本身可以合法编译。
- contract test 失败不能自动证明生产实现错误；测试也可能过期或被补丁破坏。
- CI green 不能证明 GUI 真正可用；1.21.4 的纹理和点击问题都需要实机才能暴露。
- runtime 复测推翻前一假设后，应更新文档/issue，避免把已否定的推测继续写成“通常原因”。

本次最终状态：Build #354 通过，1.21.4 实机按钮与文字居中复测通过。 1.20.6 在 StackCountScale Mixin 边界修复后也已完成本轮实机启动/功能复测。

1.20.6 首次 `runClient` 随后暴露了新的 Mixin API 边界：`renderItemCount` 在该版本不存在。该问题与 Fake Player 无关，属于 StackCountScale 的原版方法拆分差异；因此最老边界 runtime gate 仍然必要。

Legacy MaLiLib selector 首次 runtime 又暴露了一个继承层级边界：MaLiLib 0.19 的 `GuiConfigsBase` **没有声明 `initGui()`**，该方法声明在父类 `GuiBase`。因此不能把 Mixin 直接 target 到 `GuiConfigsBase#initGui`；正确做法是注入 `GuiBase#initGui`，运行时再用 `instanceof GuiConfigsBase` 限定只增强配置页。随后又确认 MaLiLib 0.19 自身的 `MaLiLibConfigGui` 也是标准 `GuiConfigsBase`，因此基础安装（MaLiLib + DDS Tweak）就应显示至少两个 selector 条目，不再把“只有 DDS Tweak 时隐藏”作为正常基线。MiniHUD `1.20.6-031.0` 与 Item Scroller `1.20.6-0.23.0` 的 `GuiConfigs()` 无参入口及 `malilib >=0.19.0` 依赖也已核实，可安全纳入 optional adapter。

## 16. 版本适配完成定义

一个版本只有同时满足以下条件，才算“兼容完成”：

```text
[ ] version metadata 正确
[ ] dependency 可解析
[ ] preprocess graph 正确
[ ] 关键条件分支在最老 target 的 build/preprocessed 中展开正确
[ ] 资源包 metadata 正确
[ ] compile/package/remap 成功
[ ] contract tests 成功
[ ] runClient 成功
[ ] MaLiLib 配置成功
[ ] 无 DDS runtime 成功
[ ] 对应 DDS legacy/handoff runtime 成功
[ ] Fake Player / QCA 等该版本实际支持功能成功
[ ] reconnect 成功
[ ] 声明版本范围的最低/最高端实际验证
[ ] paired ownership gate（若本阶段要 handoff）成功
```

只完成前六项叫“构建兼容”，不能写成“版本适配完成”。


### 17. 1.21.8 paired handoff：QCA 拖动回归结果

1.21.8 paired handoff 最终实测通过右键拖动 ABSORB / EXTRACT。当前确认的必要修复是：`ContainerScreenInputMixin` 在 `MC < 1.21.9` 的旧输入签名必须使用 ReplayMod preprocessor 的 `//$$` 标记；合同测试已锁定 `keyPressed / mouseClicked / mouseDragged / mouseReleased` 四个注入点。

诊断运行中确认：

```text
protocol=true
schema=true
takeover=true
TARGET_STORAGE / ENDER_STORAGE / VIEWER_INVENTORY role 正常
eligible=true
gesture-start 可进入
mouseDragged 可进入
ABSORB / EXTRACT 实机正常
```

曾出现一次“标记修复后仍失败”的中间复测，但诊断提交本身只增加日志，没有改变 QCA 手势语义；当前证据不足以把该次失败归因于生产代码。不要把“旧构建/旧 JAR/未完整重启”等环境因素写成既定根因。临时 `QCA trace` 日志在确认通过后已移除。


### 17.1 DT #160：原版普通存储菜单安全槽位的 QCA 右键拖动（2026-10-20）

P6 在 1.20.1 报告普通容器区不能启动 QCA 右键存取/拖动，而玩家背包槽位可用。DT PR #161（已合入 `main`，配对实机矩阵待补齐）恢复 CDA 原版的精确 `ChestMenu.class` 路径，再依容器覆盖审查补充以下**精确原版菜单类**；只修复容器 GUI 内的 QCA 操作，不新增可随身打开的 `MenuKind`：

| 菜单类 | 涵盖的原版普通存储容器 | 边界 |
| --- | --- | --- |
| `ChestMenu` | 箱子、木桶及部分实体存储菜单 | 沿用 CDA 原先显式支持的路径 |
| `HopperMenu` | 漏斗、漏斗矿车 | 仅正常存储槽位 |
| `DispenserMenu` | 投掷器、发射器 | 仅正常存储槽位 |
| `ShulkerBoxMenu` | 放置的潜影盒 | 遵守 `Slot.mayPlace` / 服务端潜影盒限制，不允许潜影盒套娃 |

四种菜单类均在 DT 最低目标 1.14.4 至最高目标 26.3 的对应源码/映射中存在，当前无需新增 ReplayMod `//#if` 版本条件。播放器自背包槽位、末影箱原有回退路径保留。优先级仍为：`DdsQcaSlotRoleProvider` 显式角色 → 已识别的原生 Provider 决策 → 查看者背包 → 上述精确原版菜单；不得通过 `instanceof AbstractContainerMenu`、`Container`、`SimpleContainer`、菜单标题或任意子类扩权。四种菜单都要求目标槽位在当前 `menu.slots` 内，且通过既有 `safeSourceSlot`（可取出、已有物品可放回、CONTROL/DISABLED 排除）。CDA 服务端始终重新验证 `containerId`、规则、菜单、槽位、权限和携带物。

**暂不加入同一白名单**：`FurnaceMenu` / `BlastFurnaceMenu` / `SmokerMenu`、`BrewingStandMenu`、`CrafterMenu`、`HorseInventoryMenu` 等含工作输出/燃料/配方/禁用/装备语义的菜单。必须先逐槽核实 CDA 服务端直写是否保留相应规则，再决定按版本实施；尤其 `CrafterMenu` 不能在 1.14.4～1.20.2 的源码中无条件引用。特殊 GCA/Org/DT 镜像菜单继续优先使用各自角色和所有权校验。无传统 `AbstractContainerScreen` 槽位 GUI 的容器不属本修复。

共享代码触及 DT 21 个 activeVersions，但截至此记录仅 1.20.1 有原始运行复现证据。自动测试包括全部四类原版菜单的正反例以及非法菜单类型、非当前成员槽和不安全槽；代码侧的静态合同不能代替 GUI 交互验证。最终提交需执行一次全版本 `buildAndGather`，并在至少 1.14.4、1.20.1（Legacy）、1.20.4（Reader Map）、1.21.11 与 26.3（Codec）分别核对新菜单类型的单次右键/连续拖动、ABSORB / EXTRACT、空/满槽、禁用规则、端到端同步、断线重连，以及 CDA 拒绝时是否误触发原版右键或重复处理。每个测试项都要记录 DT/CDA 具体产物与客户端/服务端日志；未实测版本标记 NOT RUN。

本修复不改 CDA 服务端生产代码、网络协议、QCA 规则、客户端配置/语言项或 P6 paired handoff、P7/P8 与 ownership 契约，不恢复已删除的 Ender 三个批量按钮。


### 18. 1.20.6 commented-branch mapping names

ReplayMod Preprocessor remaps active source tokens between Minecraft mappings, but code stored behind `//$$` is inactive text until that version branch is materialized. Therefore a legacy-only branch must use the target-era mapped class name when the symbol itself changed across versions.

The 1.20.6 fake-player equipment-slot icon override is the concrete example:

```text
core/newer source type: net.minecraft.resources.Identifier
1.20.6 legacy override: net.minecraft.resources.ResourceLocation
```

Using `Pair<Identifier,Identifier>` inside the commented legacy branch compiles in source form but fails after 1.20.6 preprocessing because that literal identifier is not remapped. The F2 contract now locks the legacy override to `ResourceLocation`.

### 19. Build target 与 Fabric 运行时版本范围必须分离

`minecraft_version` 只表示该 JAR 的编译目标，不能直接作为 Fabric Loader 的运行时依赖范围。对于一个 JAR 覆盖多个相邻 Minecraft 版本的节点，`fabric.mod.json` 必须展开 `minecraft_dependency`。

例如 1.21.10 构建节点：

```text
minecraft_version=1.21.10
minecraft_dependency=>=1.21.9 <=1.21.10
```

其产物必须声明：

```json
"minecraft": ">=1.21.9 <=1.21.10"
```

而不能声明为精确的 `1.21.10`。否则代码虽然按 1.21.10 编译且计划覆盖 1.21.9，Fabric Loader 仍会在 1.21.9 启动前直接拒绝加载。

同一规则适用于 1.21.8（1.21.6–1.21.8）、1.21.3（1.21.2–1.21.3）、1.21.1（1.21–1.21.1）和 1.20.6（1.20.5–1.20.6）节点。构建合同会校验处理后的 `fabric.mod.json` 与各节点的 `minecraft_dependency` 完全一致。


## 20. F2 DDS_PAYLOAD_CODEC handoff 最终状态

2026-09-21 完成 paired runtime 矩阵：

```text
1.21.9  PASS  真实 1.21.9 客户端 + 1.21.10 artifact
1.21.8  PASS
1.21.4  PASS
1.20.6  PASS
```

因此当前 DDS Tweak 的 DDS_PAYLOAD_CODEC 构建节点全部进入 handoff：

```text
1.21.11
1.21.10  （artifact 覆盖 1.21.9–1.21.10）
1.21.8
1.21.5
1.21.4
1.21.3
1.21.1
1.20.6
```

`legacyCoexistenceVersions` 当前为空。这里的版本集合表示 DDS Tweak 的构建节点，不额外列出 1.21.9，因为 1.21.9 由 1.21.10 artifact 的运行时范围覆盖。

本阶段不包含 DDS_PAYLOAD_READER_MAP 世代；1.20.4 / 1.20.2 的 ownership 迁移必须作为后续独立阶段处理。


## Silent Auto Eat presentation adapter rule

Silent Auto Eat keeps one shared transaction state machine and one shared presentation projection across all active targets. Minecraft-version-specific code is limited to the thin render adapter that supplies an already-resolved `ItemStack` to that projection.

Adapter priority:

1. transform the final render/tooltip `ItemStack` value or local;
2. otherwise use a composable value/operation hook available on that target;
3. do not use an exclusive `@Redirect` of `Slot#getItem()` or `Inventory#getItem()` as the container-render presentation owner.

The exact target method may change by API generation (`renderSlot`, `extractSlot`, or later equivalents). Build success alone is insufficient: each distinct render generation needs a representative runtime/mixin smoke, and third-party coexistence must be tested where a compatible package exists.

## 21. ClickThrough 告示牌交互边界

ClickThrough 的墙牌逻辑按“有效前景操作优先、无操作再穿透”处理。目标解析与 Vanilla 交互执行保持分离：DT 只决定本次 `BlockHitResult` 指向墙牌还是其支撑方块，不自行执行上蜡、染色、发光或命令。

版本边界：

```text
<= 1.19.4
→ 没有放置后的双面文本重编辑协议
→ 染色走旧 SignBlockEntity color API
→ 1.17+ 发光/去发光走旧 glowing-text API
→ click command 由旧 Component 文本检测后保留原始墙牌目标

1.20 – 1.21.11
→ SignApplicator + SignText
→ 正反面仍使用 boolean
→ SignApplicator.canApplyToSign(text, player)

26.1 – 26.2
→ SignApplicator 方法新增 ItemStack 参数
→ 正反面仍使用 boolean
→ 染料颜色改从 ItemStack 的 DataComponents.DYE 获取

>= 26.3
→ 正反面改为 SignTextSlot
→ SignApplicator 继续接收 ItemStack
```

Shift 只作为 ClickThrough 的“不要穿透”修饰：返回原始墙牌目标，不发送 `RELEASE_SHIFT_KEY/PRESS_SHIFT_KEY` 伪造服务器潜行状态。1.20+ 在 Vanilla 本身允许的条件下进入文本编辑；旧版本不凭空增加原版不存在的放置后编辑协议。

## DT #114 — 四现代版本 Provider 玩家菜单适配门禁（2026-10-10）

新增的 GCA/Org 无 CDA 控制仅针对 **1.21.11、26.1.2、26.2、26.3**，其余 17 个构建 target 保持原行为：

- **普通 Carpet 控制**：四节点不再把客户端 Brigadier 树作为标准 `/player` 指令的权限判断，服务端实际验证目标与权限；独立 `attack/use stop` 仍要求真实语法。未知 FAKE 身份不自动禁用普通按钮，离线目标始终不能控制。
- **账号安全**：GCA 优先本次右键 GameProfile + 同连接/容器绑定；无点击来源时需在线账号完整精确匹配或已加载实体完整显示名唯一关联。任意 `假的`、`bot_`、`_Bot` 等显示前后缀不得被剥离成命令目标。不同身份来源冲突必须拒绝发送。
- **Org 末影箱**：潜行右键玩家打开 3 排容器；必须以玩家点击 + 菜单 ID + 结构化账号标记关联，不接管普通箱子。顶部仅潜行/退出按钮，Ender 三个批量按钮不恢复。
- **输入差异**：26.3 采用 SDL 鼠标键值；1.21.11、26.1.2、26.2 采用该代既有事件映射。运行中调整攻击/使用 tick 可从 CONTINUOUS 切换为 INTERVAL；本地输入编辑不依赖命令树。
- **协议兼容**：CDA 缺席时普通菜单照常可用，QCA=N/A；有 CDA 时专有 RPC 继续进行能力、UUID、FAKE、严格 Session/容器验证，不能从显示名推导。
- **验证口径**：`verifyDDSTweak` / `buildAndGather` 证明 21 版本预处理、编译和产物；不能以 1.21.11 的 JUnit 覆盖或单一版本用户实测代替另外三个现代版本的运行记录。合并、P4 联调及 Final Cutover 是独立门禁。

当前行为的唯一实施合同与风险清单参见 [PLAYER_INVENTORY_DT114.md](PLAYER_INVENTORY_DT114.md)，不要把 #114 历史评论中的早期探索方案当成现行权限规则。

## Optional Mod Menu config-screen API

DDS Tweak implements the optional Fabric `modmenu` entrypoint, backed by the
existing `DDSTweakConfigScreen`. No additional DT configuration keys or UI
pages are introduced, and Mod Menu is not a required runtime dependency.

- 1.14.4: Mod Menu 1.7.18 (Modrinth version `leKKCpqK`), legacy
  `io.github.prospector.modmenu.api.ModMenuApi#getConfigScreenFactory`.
- 1.15.2: Mod Menu 1.10.6, early Prospector package with
  `getModConfigScreenFactory`.
- 1.16.5–1.21.11 and 26.x: Terraformers `com.terraformersmc.modmenu.api`
  with version-specific compile-only dependencies in `common.gradle`.
- 26.x uses unmapped `compileOnly`; older mapped targets use Loom
  `modCompileOnly`. No Mod Menu JAR is nested into DDS Tweak.

Regression checklist: with Mod Menu installed, open DDS Tweak from its
Configure button, change a setting, close/Esc back to Mods, reopen, and check
persistence. Without Mod Menu, client startup, the existing `Left Alt + V`
shortcut, and MaLiLib global config switching must still work. Test at least
1.14.4, 1.15.2, 1.16.5, 1.21.11, and 26.3 at runtime; run `buildAndGather`
for the full supported target set before merge. Running the static contract
test or compiling a target is not proof of its in-game navigation behavior.


- **Silent Auto Eat 背包 3D 人物预览（2026-10-27 新适配候选）：**此前仅 1.20.1 与 1.21.11+ 有专用投影，现按 **1.14.4–1.21.1：`ItemInHandLayer`**、**1.21.3：`PlayerRenderer` + `PlayerRenderState`**、**1.21.4–1.21.10：`ArmedEntityRenderState` + `ItemModelResolver`**、**1.21.11+：`InventoryScreen.extractRenderState`** 四个 API 代际实现目标分支。仅修改背包 3D 预览的视觉副手/选中主手，其他 HUD/槽位/第一人称逻辑沿用旧实现，任何版本都不能直接修改 Inventory 或服务端权威。生成的 21 个最终 `dds-tweak.mixins.json` 必须包含注册；新增 ASM 目标检查仅证明方法结构。**请勿把多版本编译当作渲染效果一致的运行证据**：各 API 边界需检查启动无 Mixin 失败、副手图标/背包人物/快捷栏/第一人称显示和取消后恢复，兼测左右主手与非空副手；用户未确认前保留 Draft。


## 2026-11-03 release-audit fixes

Creative QCA transactions now require the original connection/player/Screen/Menu identity before applying replies or continuing queues; the active Creative screen menu is checked directly (it need not equal player.containerMenu). Camera normalizes ROOT before taking a world-folder key on 1.16+; pre-1.16 level IDs remain. Protected offhand Auto Eat filters unavailable inventory staging before choosing a source, keeping hotbar fallback available on older nodes. Quick Mute stores internal restore metadata without adding editable config entries. All existing targets remain; staging/ownership/protocol boundaries are unchanged. See [candidate checks](RELEASE_AUDIT_2026-11-03.md) and [release procedure](RELEASE_WORKFLOW.md).
