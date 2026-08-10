# 更新日志 (Changelog)

本项目遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [1.0.0] - 2026-08-10

首个正式版。整合此前 0.5.x / 0.6.x 的迭代，统一命名、清理代码后正式发布。

### 新增 (Added)

- **统计处理样板**：把「单次尝试」「成功率」和「置信度」一起写进样板，模组会自动算好需要尝试多少次，才能按你指定的置信度凑够目标产出；多层合成链也会逐层独立计算（二项分布 + 正态近似）。
- **有线编码终端**：挂在 AE2 线缆上的编码终端，支持合成 / 切石 / 锻造 / 处理模式；处理模式下编码的就是统计样板。
- **无线编码终端**（需安装 [ae2wtlib](https://github.com/Mari023/AE2WirelessTerminalLibrary)）：随身就能编码、管理统计样板，还支持量子桥和万用终端。
- **升级卡支持**：AE2 能量卡；ae2wtlib 的量子桥卡、磁铁卡。
- **配方查看器联动**：JEI / EMI / REI 都能一键把配方填进终端，成功率也会自动带出来。
- **游戏内指南**（GuideME）：英文和简体中文。
- **多语言**：`en_us` / `zh_cn`。
- **模组图标**：256×256 的模组 Logo。

### 变更 (Changed)

- 模组 ID 从 `probabilitypattern` 改成 **`statpatterns`**（注意：这是破坏性改动，旧存档和已编码样板里的物品 ID 会失效）。
- 显示名保持 **"Probability Pattern for AE2"** 不变。
- 核心类统一改用 `StatPatterns` 前缀（和包名 `com.tz.statpatterns` 保持一致）；跟其他模组联动的集成类保留原名（如 `AE2WTLibIntegration`、`ProbabilityPatternJeiPlugin/EmiPlugin/ReiPlugin`）。
- 整体做了重构清理：删掉死代码（`SP.java`、`DistributionMode` 等）、临时诊断日志和没用到 import，也简化了结果模型。
- 把 JEI/EMI/REI 里三份重复的概率提取逻辑抽成共用的 `StatPatternsExtractor`。

### 依赖 (Dependencies)

- 必需：Minecraft `1.21.1` · NeoForge `≥21.1.169` · Applied Energistics 2 `≥19`
- 可选：ae2wtlib `≥19` · JEI / EMI / REI（任一配方查看器）

### 下载 (Download)

- 发布产物：`build/libs/statpatterns-1.0.0.jar`
