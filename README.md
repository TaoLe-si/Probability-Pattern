# Probability Pattern for AE2 — 1.7.10（GTNH）

为 **Applied Energistics 2 (rv3, GTNH 分支)** 增加「概率合成样板」的模组。

- **modid**: `statpatterns`
- **根包**: `com.zincglux.statpatterns`
- **当前版本**: `v1.0.0`（由 git tag 派生）
- **作者**: zincglux
- **依赖**: Forge 10.13.4.1614 / AE2 rv3-beta-695-GTNH / UniMixins（运行时 Mixin）

概率样板编码「每次尝试的输入」「目标产物」与「单次成功概率 p」，并保证「总产出 ≥ 目标数量」的置信度 ≥ 1 − α。当 ME 合成树请求 N 个产物时，合成拦截使机器按二项 / 正态模型运行足够多次，从而保证成功率。

---

## 功能

- **概率样板物品（Encoded Probability Pattern）**
  - 继承 AE2 的 `ItemEncodedPattern`，复用其渲染（Shift 显示成品）、Shift 右键清空等机制。
  - 概率参数（p、α、95%/99% 置信度）以 `sp_*` NBT 键随样板保存，与原版 `in/out` 编码格式兼容。
- **ME 概率样板编码终端（part）**
  - 贴附在 ME 线缆上的终端部件，带概率输入框与 95% / 99% 置信度切换按钮。
  - 固定为处理模式（processing pattern），不提供合成模式。
  - 中键（或左键点击缺失可合成物品）调整编码格 / 存储区物品数量、打开原版合成数量 GUI。
- **合成概率放大（UniMixins late-phase Mixin）**
  - 只保留 **`CraftableItemResolverMixin`**：GTNH 695 默认使用 v2 合成计算器（`AEConfig.craftingCalculatorVersion == 2`），该 Mixin 重定向 `CraftableItemResolver$CraftFromPatternTask.calculateOneStep` 的 `Platform.ceilDiv`，对概率样板返回 `plannedAttempts(k)`，并修正计划显示的成品数量。
  - 概率规模算法见 `math/ProbabilitySizing`：小样本（k ≤ 30）精确二项分布左尾；大样本正态近似单尾 z 检验。
- **NEI / NEE 配方填充**
  - 终端 GUI 上的 NEI "?" 按钮由 **NEE（NotEnoughEnergistics）** 的 `NEEPatternTerminalHandler` 提供填充（处理配方、GT 机器配方等），按模组 `IRecipeProcessor` 解析输入与产物，绕开 AE 原生 `findMatchingRecipe` 校验。NEE 为可选依赖（未安装时仅无处理配方填充）。

---

## 构建

本项目使用 GTNH 构建约定：

- `com.gtnewhorizons.gtnhconvention` / `gtnhsettingsconvention` **2.0.27**
- **Gradle 9.3.1**，需要 **JDK 25**（见 `gradle.properties` 的 `org.gradle.java.home`）
- 代理 `127.0.0.1:7890`（见 `gradle.properties` 的 `systemProp.*`）

```bash
gradlew clean spotlessApply build
```

产物位于 `build/libs/statpatterns-<version>.jar`（发布版 reobf jar，部署用非 `-dev` / `-sources`）。

> **版本号由 git tag 派生**：`modVersion` 仅作记录。发布时打 tag：
> ```bash
> git tag v1.0.0
> gradlew build        # 产出 statpatterns-v1.0.0.jar
> ```

### 依赖

| 依赖 | 用途 | 版本 |
| --- | --- | --- |
| Applied Energistics 2 | 必选（`required-after:appliedenergistics2`） | rv3-beta-695-GTNH（`libs/appliedenergistics2-rv3-beta-695-GTNH-dev.jar` 编译） |
| UniMixins | 运行时必选（late-phase Mixin 加载器） | GTNH 实例自带 |
| NotEnoughItems | 运行时（NEI "?" 按钮） | 2.8.44-GTNH（编译 2.7.91-GTNH） |
| NotEnoughEnergistics | 可选（推荐，处理配方填充） | 1.7.14 |

---


## 使用

1. 把概率样板终端（ME Probability Pattern Terminal）贴到 ME 线缆上，放入物品与流体/输入配方。
2. 在终端里输入单次成功概率 p（如 0.8），选择置信度 95%（α=0.05）或 99%（α=0.01），点击编码。
3. 编码后的概率样板放入样板供应器 / 接口，ME 合成请求 N 个产物时，合成树会自动放大运行次数。
4. 中键点击编码格物品可调整输入数量；NEI 配方页点击 "?"（NEE 提供）可一键填充。

---

## 许可证

LGPL-3.0，见 `LICENSE`。

