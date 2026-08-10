# Probability Pattern for AE2 — 概率样板

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-blue?logo=minecraft)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1.169-orange)
![AE2](https://img.shields.io/badge/AE2-19.2.17-green)
![Java](https://img.shields.io/badge/Java-21-red)
![License](https://img.shields.io/badge/License-LGPL%20v3-blue)

> **English version**: [README_en.md](README_en.md)

**Probability Pattern for AE2** 是 [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2) 的 NeoForge 扩展模组，为 Minecraft 1.21.1 增加概率样板功能。适用于有随机产出率的机器（如 GT 的副产、魔法模组的概率合成等）。

---

## 工作原理

概率样板将随机产出建模为**二项分布**：每次尝试以概率 p 成功，目标产出 N 个。系统计算所需尝试次数，使得"产出不足"的风险 ≤ α。

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

### Mixin 注入层

通过 Mixin 在 AE2 合成计算期间介入：

| Mixin | 作用 |
|-------|------|
| `CraftingServiceMixin` | `@Overwrite beginCraftingCalculation`，将 AE2 的 `CraftingCalculation` 替换为 `StatPatternsCraftingCalculation` |

### 合成树

- **StatPatternsCraftingCalculation** — 继承 AE2 `CraftingCalculation`，在合成计算期间跟踪整体成功概率
- **StatPatternsCraftingTreeNode** — 合成树节点，通过 `StatisticalPatternDetails.forRequest()` 按请求量缩放概率规模
- **StatPatternsCraftingTreeProcess** — 合成树节点处理，汇总各子层成功概率

### 样板系统

- **EncodedStatisticalPattern** — 持久化数据记录（`inputsPerAttempt`、`output`、`successProbability`、`alpha`、`smallSampleLimit`），通过 `Components` 以 NBT 序列化（1.20.1 Forge 无 DataComponent/Codec）
- **StatisticalPatternDetails** — 实现 AE2 `IPatternDetails`，在 `getInputs()` 时按概率计算后的总尝试次数缩放输入量；`forRequest(total)` 创建指定请求量的实例
- **StatPatternsPatternItem** — 自定义 `EncodedPatternItem`，空白样板不显示无效的 pattern tooltip

---

## 使用方法

### 1. 获取终端

在创造模式标签页 "AE2 概率样板" 中取出**概率样板编码终端**，放置并打开。

### 2. 编码样板

1. 将**单次尝试**的输入样本放入输入格
2. 将目标产物放入输出槽
3. 放入空白 `stat_pattern`
4. 在终端的概率输入框中设置成功率（如 `0.8` 即 80%）
5. 点击编码按钮

### 3. JEI 集成

支持从 JEI 配方直接拖拽到终端。如果配方类包含 `successProbability` / `probability` / `chance` 方法或字段，JEI 会自动提取成功率并填入。

---

## 模组信息

| 项目 | 值 |
|------|-----|
| Mod ID | `statpatterns` |
| 名称 | Probability Pattern for AE2 |
| 版本 | 0.4.5 |
| 包名 | `com.tz.statpatterns` |

### 依赖

| 依赖 | 版本 |
|------|------|
| Minecraft | 1.20.1 |
| Forge / NeoForge | ≥ 47 |
| Applied Energistics 2 | ≥ 15 |
| JEI（可选） | ≥ 15.20 |

---

## 项目结构

```
src/main/java/com/tz/statpatterns/
├── StatPatternsMod.java                  # Mod 入口
├── StatPatternsCreativeTabs.java         # 创造模式标签页
├── api/ids/
│   ├── Components.java                   # NBT 序列化助手
│   ├── ItemIds.java                      # 物品/部件 ID
│   └── StatPatternsCreativeTabIds.java   # 标签页 ID
├── client/
│   ├── StatPatternsClient.java           # 客户端注册
│   ├── StatPatternsTerminalScreen.java   # 编码界面（含概率输入框）
│   └── WirelessStatPatternsTerminalScreen.java  # 无线终端界面
├── core/
│   └── StatPatterns.java                 # 核心工具（ResourceLocation 构造）
├── core/definition/
│   ├── StatPatternsItems.java            # 物品注册
│   ├── StatPatternsMenus.java            # 菜单注册
│   └── StatPatternsParts.java            # 线缆部件注册
├── crafting/
│   ├── EncodedStatisticalPattern.java    # 概率样板数据记录
│   ├── StatPatternsCraftingCalculation.java  # 合成计算（概率跟踪）
│   ├── StatPatternsCraftingTreeNode.java     # 合成树节点
│   ├── StatPatternsCraftingTreeProcess.java  # 合成树处理
│   ├── StatPatternsPatternDecoder.java   # 样板解码器
│   ├── StatPatternsPatternItem.java      # 概率样板物品
│   └── StatisticalPatternDetails.java    # AE2 样板详情（概率缩放）
├── integration/
│   ├── ae2wtlib/AE2WTLibIntegration.java # ae2wtlib 联动（升级卡 / 量子桥）
│   └── jei/ProbabilityPatternJeiPlugin.java  # JEI 集成（配方拖拽 & 概率自动提取）
├── item/
│   └── StatPatternsTerminalItem.java     # 手持无线终端物品
├── math/
│   ├── DistributionMode.java             # 分布模式枚举
│   ├── ProbabilitySizing.java            # 核心算法（二项分布 & 正态近似）
│   └── ProbabilitySizingResult.java      # 计算结果
├── mixin/
│   ├── CraftingServiceMixin.java         # 拦截 beginCraftingCalculation，注入 StatPatterns 计算
│   └── CraftingTreeNodeMixin.java        # 占位（已被 StatPatternsCraftingTree* 替代）
├── part/
│   ├── StatPatternsEncodingLogic.java    # 编码逻辑（概率 / alpha 参数）
│   └── StatPatternsTerminalPart.java     # 线缆附着终端部件
└── terminal/
    ├── StatPatternsTerminalMenu.java     # 终端菜单逻辑（编码 & 概率同步）
    ├── StatPatternsTerminalMenuHost.java # 菜单宿主（量子桥支持）
    └── WirelessStatPatternsTerminalMenu.java # 无线终端菜单
```

---

## 构建

需要 **Java 21**。

---

## 许可证

本项目基于 [GNU LGPLv3](LICENSE) 开源。作为 AE2 的衍生作品，遵循 AE2 的许可协议。
