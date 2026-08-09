# Probability Pattern for AE2 — 概率样板（1.7.10）

![Minecraft](https://img.shields.io/badge/Minecraft-1.7.10-blue?logo=minecraft)
![Forge](https://img.shields.io/badge/Forge-10.13.4.1614-orange)
![AE2](https://img.shields.io/badge/AE2-rv3--beta--695--GTNH-green)
![Java](https://img.shields.io/badge/Java-8-red)
![License](https://img.shields.io/badge/License-LGPL%20v3-blue)

**Probability Pattern for AE2** 是 [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2)（GTNH 分支，rv3-beta-695）的 Forge 扩展模组，为 Minecraft 1.7.10 增加概率样板功能。适用于有随机产出率的机器（如 GT 的副产物、魔法模组的概率合成等）。

---

## 工作原理

概率样板将随机产出建模为**二项分布**：每次尝试以概率 p 成功，目标产出 N 个。系统计算所需尝试次数，使"产出不足"的风险 ≤ α。

| 参数 | 含义 | 默认值 |
|------|------|--------|
| p | 单次尝试成功率 | 0.8 (80%) |
| α | 显著性水平 / 可接受的少产风险 | 0.05 (5%) |
| N | 目标产出数量 | 由 AE2 合成请求决定 |

### 计算策略

- **小批量（N ≤ 30）**：精确二项分布左尾概率，逐次累加
- **大批量（N > 30）**：正态近似 N(np, np(1-p))，采用单尾 z 检验

### 链式合成

概率样板支持链式合成——中间产物的需求量会沿着合成树向上传播，每一层独立计算所需的尝试次数。

---

## 技术实现

### Mixin 注入层（UniMixins 晚相位）

通过一个 Mixin 在 AE2 v2 合成计算器中介入：

| Mixin | 作用 |
|-------|------|
| `CraftableItemResolverMixin` | `@Redirect` `CraftableItemResolver$CraftFromPatternTask.calculateOneStep` 的 `Platform.ceilDiv`，对概率样板返回 `plannedAttempts(k)`，并修正 `populatePlan` 中显示的成品数量 |

AE2 是普通 mod，加载晚于 Mixin 早期相位，故经 `LateMixinLoader` 走 GTNHMixins 的 **late 相位**。

### NEI / NEE 配方填充

- 终端 GUI 的 NEI "?" 按钮由 **NEE（NotEnoughEnergistics）** 的 `NEEPatternTerminalHandler` 提供填充，按各模组 `IRecipeProcessor` 解析输入与产物（GT 机器配方等），绕开 AE 原生 `findMatchingRecipe` 校验。
- NEE 为可选依赖（未安装时仅无处理配方填充）。

### 样板系统

- **EncodedStatisticalPattern** — 概率样板的 NBT 编解码（`in`/`out` + `sp_probability`/`sp_alpha`/`sp_alpha95`/`sp_smallSampleLimit`），与 AE2 原版 `in/out` 编码格式兼容
- **StatisticalPatternDetails** — 实现 `ICraftingPatternDetails`，固定为处理样板（`isCraftable()=false`），`plannedAttempts(k)` 按二项 / 正态模型计算所需尝试次数
- **ProbabilityPatternItem** — 继承 AE2 `ItemEncodedPattern`，复用其渲染（Shift 显示成品）、Shift 右键清空等机制

---

## 使用方法

### 1. 获取终端

用 **ME 样板终端**（Pattern Terminal）与 **工程处理器**（Engineering Processor）**无序合成** **ME 概率样板终端**，贴附到 ME 线缆上并打开。

### 2. 编码样板

1. 将**单次尝试**的输入样本放入输入格
2. 将目标产物放入输出槽
3. 放入空白样板（AE2 空白样板即可）
4. 在终端的概率输入框中设置成功率（如 `0.8` 即 80%），并选择 95% / 99% 置信度
5. 点击编码按钮

### 3. NEI 集成

支持从 NEI 配方页点击 "?"（NEE 提供）一键填充输入与产物，包括 GT 机器等处理配方；中键点击编码格 / 存储区物品可调整数量或触发合成。

---

## 模组信息

| 项目 | 值 |
|------|-----|
| Mod ID | `statpatterns` |
| 名称 | AE2 Probability Pattern |
| 版本 | 1.0.0 |
| 包名 | `com.zincglux.statpatterns` |

### 依赖

| 依赖 | 版本 |
|------|------|
| Minecraft | 1.7.10 |
| Forge | ≥ 10.13.4.1614 |
| Applied Energistics 2 | rv3-beta-695-GTNH |
| UniMixins（运行时） | GTNH 自带 |
| NotEnoughItems（运行时） | 2.8.44-GTNH |
| NotEnoughEnergistics（可选） | ≥ 1.7.14 |

---

## 项目结构

```
src/main/java/com/zincglux/statpatterns/
├── ProbabilityPatternMod.java              # Mod 入口（注册 / NEI / NEE 集成）
├── LateMixinLoader.java                    # UniMixins 晚相位 Mixin 加载器
├── container/
│   ├── ContainerProbabilityPatternTerm.java        # 编码终端容器（编码逻辑）
│   └── ContainerProbabilityPatternValueAmount.java # 数量调整容器
├── crafting/
│   ├── EncodedStatisticalPattern.java      # 概率样板 NBT 编解码
│   ├── ProbabilityPatternItem.java         # 概率样板物品
│   └── StatisticalPatternDetails.java      # AE2 样板详情（概率缩放）
├── handler/
│   └── ProbabilityPatternGuiHandler.java   # GUI 处理器
├── item/
│   └── ItemProbabilityPatternTerminal.java # 终端部件物品
├── math/
│   ├── ProbabilitySizing.java              # 核心算法（二项分布 & 正态近似）
│   └── ProbabilitySizingResult.java        # 计算结果
├── mixin/
│   └── CraftableItemResolverMixin.java     # v2 概率放大（calculateOneStep 重定向）
├── network/
│   ├── ProbabilityPatternNetwork.java      # 网络通道注册
│   ├── ProbabilityPatternPacket*.java      # 概率设置报文
│   ├── ProbabilityPatternValueSetPacket*.java    # 数量调整报文
│   ├── PacketProbabilityPatternAutoCraft*.java   # 自动合成报文
│   └── ProbabilityPatternServerTaskQueue.java    # 服务器任务队列
└── part/
    └── ProbabilityPatternTerminalPart.java # 线缆附属终端部件（概率状态持久化）
src/main/java/appeng/client/gui/implementations/
├── GuiProbabilityPatternTerm.java          # 编码界面（含概率输入框）
└── GuiProbabilityPatternValueAmount.java   # 数量调整界面
```

---

## 构建

需要 **JDK 25** 与 **Gradle 9.3.1**（gtnhconvention 2.0.27）构建，MC 运行时由 gtnhgradle 自动使用 JDK 8。

```bash
gradlew clean spotlessApply build
```

产物位于 `build/libs/statpatterns-<version>.jar`；版本号由 **git tag**（如 `v1.0.0`）派生。

---

## 许可证

本项目基于 [GNU LGPLv3](LICENSE) 开源。作为 AE2 的衍生作品，遵循 AE2 的许可协议。


