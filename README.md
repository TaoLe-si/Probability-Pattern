# Probability Pattern for AE2 — 1.7.10 移植版

本分支（`1.7.10`）是 [Probability Pattern for AE2](https://github.com/TaoLe-si/Probability-Pattern)（Minecraft 1.21.1 / NeoForge）向 **Minecraft 1.7.10 / Forge / Applied Energistics 2 (rv3)** 的移植。

## 功能

- **概率样板（Processing Pattern (Probability)）**：为 AE2 增加概率合成样板。样板编码「每次尝试的输入」「目标产物」与「单次成功概率 p」，并保证「总产出 ≥ 目标」的置信度 ≥ 1 − α。
  - 小批量（N ≤ 30）：精确二项分布左尾概率，逐次累加。
  - 大批量（N > 30）：正态近似 N(np, np(1−p))，单尾 z 检验。
- **ME 概率样板编码终端（part）**：贴附在 ME 线缆上的终端，用于编码 / 管理概率样板，带概率输入框与 95% / 99% 置信度切换。
- **合成拦截（ASM Coremod）**：向 `appeng.crafting.CraftingTreeProcess.getTimes` 注入概率规模计算，使合成树在请求 N 个产物时按二项 / 正态模型运行足够次数（替代 1.21.1 版使用的 Mixin）。

## 目录

- `src/main/java/com/tz/statpatterns/` — 模组源码
  - `math/` — 概率规模计算（二项分布 / 正态近似）
  - `crafting/` — `ProbabilityPatternItem`、`StatisticalPatternDetails`、`EncodedStatisticalPattern`
  - `part/` — `ProbabilityPatternTerminalPart`
  - `container/` — `ContainerProbabilityPatternTerm`
  - `client/gui/` — `GuiProbabilityPatternTerm`
  - `coremod/` — ASM 合成拦截核心
  - `network/` — 客户端 → 服务器报文
- `src/main/resources/` — 语言、贴图、元数据

## 构建

> ⚠️ 1.7.10 的 ForgeGradle 1.2 / MCP 工具链需要 **JDK 8**，较新 JDK 无法工作。

1. 安装 JDK 8，并确保 `JAVA_HOME` 指向它。
2. 将编译好的 AE2 rv3（1.7.10）jar 放入 `libs/`（例如 `libs/appliedenergistics2-rv3.jar`）。
3. 执行：

```bash
gradlew setupDecompWorkspace --refresh-dependencies
gradlew build
```

产物位于 `build/libs/probabilitypattern-<version>.jar`。

### 开发运行

```bash
gradlew setupDecompWorkspace
gradlew eclipse   # 或 gradlew idea
gradlew runClient
```

## 与 1.21.1 版的差异（移植说明）

| 1.21.1 (NeoForge) | 1.7.10 (Forge) |
| --- | --- |
| Mixin 注入 `CraftingTreeNode` | ASM Coremod 注入 `CraftingTreeProcess.getTimes` |
| `IPatternDetails` + DataComponent/Codec | `ICraftingPatternDetails` + ItemStack NBT |
| `EncodedPatternItem` | `ProbabilityPatternItem implements ICraftingPatternItem` |
| `PatternEncodingTermMenu/Screen` | 基于 `ContainerMEMonitorable` / `GuiMEMonitorable` 重实现 |
| 无线终端（依赖 ae2wtlib） | 未移植（1.7.10 无 ae2wtlib） |
| GuideME 手册 | 未移植 |
| JEI / EMI / REI 集成 | 未移植 |

## 许可证

LGPL-3.0（与 AE2 rv3 一致）。见 `LICENSE`。
