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

通过两个 Mixin 在 AE2 合成计算期间介入：

| Mixin | 作用 |
|-------|------|
| `CraftingServiceMixin` | 在 `beginCraftingCalculation` 时将 `IGrid` 包装为 `PGrid`，使 `getCraftingService()` 返回 `PCraftingService` |
| `CraftingTreeNodeMixin` | 在合成树的每个节点捕获实际请求量，为 `StatisticalPatternDetails` 注入 `forRequest(total)`，使每层独立计算概率规模 |

### 代理层

- **PGrid** — 包装 AE2 的 `IGrid`，拦截 `getService(ICraftingService.class)` 返回 `PCraftingService`
- **PCraftingService** — 委托模式包装 `ICraftingService`，保持与 AE2 合成系统的透明对接

### 样板系统

- **EncodedStatisticalPattern** — 持久化数据组件（`inputsPerAttempt`、`output`、`successProbability`、`alpha`、`smallSampleLimit`），通过 Codec 支持 NBT 序列化与网络同步
- **StatisticalPatternDetails** — 继承 `AEProcessingPattern`，在 `getInputs()` 时按概率计算后的总尝试次数缩放输入量；`forRequest(total)` 创建指定请求量的实例
- **ProbabilityPatternItem** — 自定义 `EncodedPatternItem`，空白样板不显示无效的 pattern tooltip

---

## 使用方法

### 1. 获取终端

在创造模式标签页 "AE2 概率样板" 中取出**概率样板编码终端**，放置并打开。

### 2. 编码样板

1. 将**单次尝试**的输入样本放入输入格
2. 将目标产物放入输出槽
3. 放入空白 `probability_pattern`
4. 在终端的概率输入框中设置成功率（如 `0.8` 即 80%）
5. 点击编码按钮

### 3. JEI 集成

支持从 JEI 配方直接拖拽到终端。如果配方类包含 `successProbability` / `probability` / `chance` 方法或字段，JEI 会自动提取成功率并填入。

---

## 模组信息

| 项目 | 值 |
|------|-----|
| Mod ID | `probabilitypattern` |
| 名称 | Probability Pattern for AE2 |
| 版本 | 0.1.0 |
| 包名 | `com.tz.statpatterns` |

### 依赖

| 依赖 | 版本 |
|------|------|
| Minecraft | 1.21.1 |
| NeoForge | ≥ 21.1.169 |
| Applied Energistics 2 | ≥ 19.2.17 |
| JEI（可选） | ≥ 19.27 |

---

## 项目结构

```
src/main/java/com/tz/statpatterns/
├── ProbabilityPatternMod.java          # Mod 入口
├── SPCreativeTabs.java                 # 创造模式标签页
├── api/ids/
│   ├── BlockIds.java                   # 方块 ID
│   ├── Components.java                 # 数据组件注册
│   ├── ItemIds.java                    # 物品 ID
│   └── SPCreativeTabIds.java           # 标签页 ID
├── client/
│   ├── ProbabilityPatternClient.java   # 客户端注册
│   └── ProbabilityPatternTerminalScreen.java  # 编码界面（含概率输入框）
├── core/
│   └── SP.java                         # 核心常量
├── core/definition/
│   ├── SPBlockEntities.java            # 方块实体注册
│   ├── SPBlocks.java                   # 方块注册
│   ├── SPItems.java                    # 物品注册
│   ├── SPMenus.java                    # 菜单注册
│   └── SPParts.java                    # 线缆部件注册
├── crafting/
│   ├── EncodedStatisticalPattern.java  # 概率样板数据组件
│   ├── ProbabilityPatternItem.java     # 概率样板物品
│   └── StatisticalPatternDetails.java  # AE2 样板详情（概率缩放）
├── init/
│   └── InitCapabilityProviders.java    # Capability 注册
├── integration/jei/
│   └── ProbabilityPatternJeiPlugin.java  # JEI 集成（配方拖拽 & 概率自动提取）
├── math/
│   ├── DistributionMode.java           # 分布模式枚举
│   ├── ProbabilitySizing.java          # 核心算法（二项分布 & 正态近似）
│   └── ProbabilitySizingResult.java    # 计算结果
├── mixin/
│   ├── CraftingServiceMixin.java       # 拦截 beginCraftingCalculation，注入 PGrid
│   └── CraftingTreeNodeMixin.java      # 拦截合成树节点，注入 forRequest 总量
├── network/
│   ├── PCraftingService.java           # ICraftingService 代理
│   ├── PGrid.java                      # IGrid 代理
│   └── PGridNode.java                  # IGridNode 代理
├── part/
│   └── ProbabilityPatternTerminalPart.java  # 线缆附着终端部件
└── terminal/
    └── ProbabilityPatternTerminalMenu.java  # 终端菜单逻辑（编码 & 概率同步）
```

---

## 构建

需要 **Java 21**。

---

## 许可证

本项目基于 [GNU LGPLv3](LICENSE) 开源。作为 AE2 的衍生作品，遵循 AE2 的许可协议。
