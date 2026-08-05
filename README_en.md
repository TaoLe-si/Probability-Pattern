# Probability Pattern for AE2

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-blue?logo=minecraft)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1.169-orange)
![AE2](https://img.shields.io/badge/AE2-19.2.17-green)
![Java](https://img.shields.io/badge/Java-21-red)
![License](https://img.shields.io/badge/License-LGPL%20v3-blue)

> **🇨🇳 中文版本**: [README.md](README.md)

**Probability Pattern for AE2** is a NeoForge addon for [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2) on Minecraft 1.21.1. It adds probability-based encoded patterns for machines with random output rates (e.g. GT byproducts, magic mod probability crafting).

---

## How It Works

Probability patterns model random output as a **binomial distribution**: each attempt succeeds with probability p, with a target of N outputs. The system computes the required attempts so that the risk of underproduction is ≤ α.

| Parameter | Meaning | Default |
|-----------|---------|---------|
| p | Single-attempt success probability | 0.8 (80%) |
| α | Significance level / acceptable underproduction risk | 0.05 (5%) |
| N | Target output quantity | Determined by AE2 crafting request |

### Calculation Strategy

- **Small batches (N ≤ 30)**: Exact binomial lower-tail probability, cumulative
- **Large batches (N > 30)**: Normal approximation N(np, np(1-p)), single-tailed z-test

### Chain Crafting

Probability patterns support chain crafting — intermediate product quantities propagate up the crafting tree, with each level independently computing its required attempts.

---

## Technical Implementation

### Mixin Injection Layer

Two mixins intercept AE2''s crafting calculation:

| Mixin | Role |
|-------|------|
| `CraftingServiceMixin` | Wraps `IGrid` as `PGrid` during `beginCraftingCalculation`, so `getCraftingService()` returns `PCraftingService` |
| `CraftingTreeNodeMixin` | Captures the actual requested amount at each tree node, injects `forRequest(total)` into `StatisticalPatternDetails` for per-level probability sizing |

### Proxy Layer

- **PGrid** — Wraps AE2''s `IGrid`, intercepts `getService(ICraftingService.class)` to return `PCraftingService`
- **PCraftingService** — Delegating wrapper around `ICraftingService`, transparent to AE2''s crafting system

### Pattern System

- **EncodedStatisticalPattern** — Persistent data component (`inputsPerAttempt`, `output`, `successProbability`, `alpha`, `smallSampleLimit`), serialized via Codec for NBT and network sync
- **StatisticalPatternDetails** — Extends `AEProcessingPattern`; scales inputs by total computed attempts in `getInputs()`; `forRequest(total)` creates per-request instances
- **ProbabilityPatternItem** — Custom `EncodedPatternItem`; blank patterns suppress invalid pattern tooltips

---

## Usage

### 1. Get the Terminal

Take the **Probability Pattern Encoding Terminal** from the "AE2 Probability Patterns" creative tab, place it, and open.

### 2. Encode a Pattern

1. Place **per-attempt** input samples in the input grid
2. Place the target output in the output slot
3. Insert a blank `probability_pattern`
4. Set the success probability in the probability field (e.g. `0.8` for 80%)
5. Press the encode button

### 3. JEI Integration

Drag recipes directly from JEI into the terminal. If the recipe class has a `successProbability` / `probability` / `chance` method or field, JEI auto-extracts and fills the probability.

---

## Mod Info

| Item | Value |
|------|-------|
| Mod ID | `probabilitypattern` |
| Name | Probability Pattern for AE2 |
| Version | 0.1.0 |
| Package | `com.tz.statpatterns` |

### Dependencies

| Dependency | Version |
|------------|---------|
| Minecraft | 1.21.1 |
| NeoForge | ≥ 21.1.169 |
| Applied Energistics 2 | ≥ 19.2.17 |
| JEI (optional) | ≥ 19.27 |

---

## Project Structure

```
src/main/java/com/tz/statpatterns/
├── ProbabilityPatternMod.java          # Mod entry point
├── SPCreativeTabs.java                 # Creative tab
├── api/ids/
│   ├── BlockIds.java                   # Block IDs
│   ├── Components.java                 # Data component registration
│   ├── ItemIds.java                    # Item IDs
│   └── SPCreativeTabIds.java           # Tab IDs
├── client/
│   ├── ProbabilityPatternClient.java   # Client registration
│   └── ProbabilityPatternTerminalScreen.java  # Encoding UI with probability field
├── core/
│   └── SP.java                         # Core constants
├── core/definition/
│   ├── SPBlockEntities.java            # Block entity registration
│   ├── SPBlocks.java                   # Block registration
│   ├── SPItems.java                    # Item registration
│   ├── SPMenus.java                    # Menu registration
│   └── SPParts.java                    # Cable part registration
├── crafting/
│   ├── EncodedStatisticalPattern.java  # Probability pattern data component
│   ├── ProbabilityPatternItem.java     # Probability pattern item
│   └── StatisticalPatternDetails.java  # AE2 pattern details with probability scaling
├── init/
│   └── InitCapabilityProviders.java    # Capability registration
├── integration/jei/
│   └── ProbabilityPatternJeiPlugin.java  # JEI integration (drag & auto-extract probability)
├── math/
│   ├── DistributionMode.java           # Distribution mode enum
│   ├── ProbabilitySizing.java          # Core algorithm (binomial & normal approximation)
│   └── ProbabilitySizingResult.java    # Computation result
├── mixin/
│   ├── CraftingServiceMixin.java       # Intercepts beginCraftingCalculation, injects PGrid
│   └── CraftingTreeNodeMixin.java      # Intercepts tree nodes, injects forRequest total
├── network/
│   ├── PCraftingService.java           # ICraftingService proxy
│   ├── PGrid.java                      # IGrid proxy
│   └── PGridNode.java                  # IGridNode proxy
├── part/
│   └── ProbabilityPatternTerminalPart.java  # Cable-attached terminal part
└── terminal/
    └── ProbabilityPatternTerminalMenu.java  # Terminal menu logic (encode & probability sync)
```

---

## Build

Requires **Java 21**.

```powershell
.\gradlew.bat build
```

Output: `build/libs/probabilitypattern-0.1.0.jar`.

---

## License

This project is open-source under [GNU LGPLv3](LICENSE). As a derivative work of AE2, it follows AE2''s license.
