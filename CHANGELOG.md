# 更新日志 (Changelog)

本项目遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [1.0.0] - 2026-08-10

首个正式发布版本。在历史迭代（0.5.x / 0.6.x）的基础上完成命名统一、代码清理与打包发布。

### 新增 (Added)

- **统计处理样板（Statistical Processing Pattern）**：编码「单次尝试」及其「成功率」与「置信度」，模组自动规划足够尝试次数，以选定置信度产出目标数量；合成树支持逐层独立计算（二项分布 + 正态近似）。
- **有线编码终端**：AE2 线缆附着终端，支持合成 / 切石 / 锻造 / 处理模式；处理模式下编码统计样板。
- **无线编码终端**（需 [ae2wtlib](https://github.com/Mari023/AE2WirelessTerminalLibrary)）：随身编码与管理统计样板，支持量子桥与万用终端。
- **升级卡支持**：AE2 能量卡；ae2wtlib 量子桥卡、磁铁卡。
- **配方查看器集成**：JEI / EMI / REI 一键填充配方并自动提取成功率。
- **游戏内指南**（GuideME）：英文与简体中文。
- **多语言**：`en_us` / `zh_cn`。
- **模组图标**：256×256 模组 logo。

### 变更 (Changed)

- 模组 ID 由 `probabilitypattern` 改为 **`statpatterns`**（破坏性变更：旧存档/已编码样板中的物品 ID 将失效）。
- 显示名保持 **"Probability Pattern for AE2"**。
- 核心类命名统一为 `StatPatterns` 前缀（与包名 `com.tz.statpatterns` 一致）；与其他模组联动的集成类保留原名（`AE2WTLibIntegration`、`ProbabilityPatternJeiPlugin/EmiPlugin/ReiPlugin` 等）。
- 代码整体重构：清理死代码（`SP.java`、`DistributionMode` 等）、临时诊断日志、未使用 import，简化结果模型。
- 抽取共享概率提取工具 `StatPatternsExtractor`，去除 JEI/EMI/REI 三处重复逻辑。

### 依赖 (Dependencies)

- 必需：Minecraft `1.21.1` · NeoForge `≥21.1.169` · Applied Energistics 2 `≥19`
- 可选：ae2wtlib `≥19` · JEI / EMI / REI（任一配方查看器）

### 下载 (Download)

- 发布产物：`build/libs/statpatterns-1.0.0.jar`
