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

A mixin intercepts AE2's crafting calculation:

| Mixin | Role |
|-------|------|
| `CraftingServiceMixin` | `@Overwrite beginCraftingCalculation` to replace AE2's `CraftingCalculation` with `StatPatternsCraftingCalculation` |

### Crafting Tree

- **StatPatternsCraftingCalculation** — Extends AE2 `CraftingCalculation`, tracks overall success probability during calculation
- **StatPatternsCraftingTreeNode** — Tree node; scales probability sizing per request via `StatisticalPatternDetails.forRequest()`
- **StatPatternsCraftingTreeProcess** — Tree process node; aggregates child success probabilities

### Pattern System

- **EncodedStatisticalPattern** — Persistent data record (`inputsPerAttempt`, `output`, `successProbability`, `alpha`, `smallSampleLimit`), serialized to NBT via `Components` (no DataComponent/Codec in Forge 1.20.1)
- **StatisticalPatternDetails** — Implements AE2 `IPatternDetails`; scales inputs by total computed attempts in `getInputs()`; `forRequest(total)` creates per-request instances
- **StatPatternsPatternItem** — Custom `EncodedPatternItem`; blank patterns suppress invalid pattern tooltips

---

## Usage

### 1. Get the Terminal

Take the **Probability Pattern Encoding Terminal** from the "AE2 Probability Patterns" creative tab, place it, and open.

### 2. Encode a Pattern

1. Place **per-attempt** input samples in the input grid
2. Place the target output in the output slot
3. Insert a blank `stat_pattern`
4. Set the success probability in the probability field (e.g. `0.8` for 80%)
5. Press the encode button

### 3. JEI Integration

Drag recipes directly from JEI into the terminal. If the recipe class has a `successProbability` / `probability` / `chance` method or field, JEI auto-extracts and fills the probability.

---

## Mod Info

| Item | Value |
|------|-------|
| Mod ID | `statpatterns` |
| Name | Probability Pattern for AE2 |
| Version | 0.4.5 |
| Package | `com.tz.statpatterns` |

### Dependencies

| Dependency | Version |
|------------|---------|
| Minecraft | 1.20.1 |
| Forge / NeoForge | ≥ 47 |
| Applied Energistics 2 | ≥ 15 |
| JEI (optional) | ≥ 15.20 |

---

## Project Structure

```
src/main/java/com/tz/statpatterns/
├── StatPatternsMod.java                  # Mod entry point
├── StatPatternsCreativeTabs.java         # Creative tab
├── api/ids/
│   ├── Components.java                   # NBT serialization helpers
│   ├── ItemIds.java                      # Item/part IDs
│   └── StatPatternsCreativeTabIds.java   # Tab IDs
├── client/
│   ├── StatPatternsClient.java           # Client registration
│   ├── StatPatternsTerminalScreen.java   # Encoding UI with probability field
│   └── WirelessStatPatternsTerminalScreen.java  # Wireless terminal UI
├── core/
│   └── StatPatterns.java                 # Core helpers (ResourceLocation)
├── core/definition/
│   ├── StatPatternsItems.java            # Item registration
│   ├── StatPatternsMenus.java            # Menu registration
│   └── StatPatternsParts.java            # Cable part registration
├── crafting/
│   ├── EncodedStatisticalPattern.java    # Probability pattern data record
│   ├── StatPatternsCraftingCalculation.java  # Crafting calculation (probability tracking)
│   ├── StatPatternsCraftingTreeNode.java     # Crafting tree node
│   ├── StatPatternsCraftingTreeProcess.java  # Crafting tree process
│   ├── StatPatternsPatternDecoder.java   # Pattern decoder
│   ├── StatPatternsPatternItem.java      # Probability pattern item
│   └── StatisticalPatternDetails.java    # AE2 pattern details with probability scaling
├── integration/
│   ├── ae2wtlib/AE2WTLibIntegration.java # ae2wtlib integration (upgrade cards / quantum bridge)
│   └── jei/ProbabilityPatternJeiPlugin.java  # JEI integration (drag & auto-extract probability)
├── item/
│   └── StatPatternsTerminalItem.java     # Handheld wireless terminal item
├── math/
│   ├── DistributionMode.java             # Distribution mode enum
│   ├── ProbabilitySizing.java            # Core algorithm (binomial & normal approximation)
│   └── ProbabilitySizingResult.java      # Computation result
├── mixin/
│   ├── CraftingServiceMixin.java         # Intercepts beginCraftingCalculation
│   └── CraftingTreeNodeMixin.java        # Placeholder (replaced by StatPatternsCraftingTree*)
├── part/
│   ├── StatPatternsEncodingLogic.java    # Encoding logic (probability / alpha)
│   └── StatPatternsTerminalPart.java     # Cable-attached terminal part
└── terminal/
    ├── StatPatternsTerminalMenu.java     # Terminal menu logic (encode & probability sync)
    ├── StatPatternsTerminalMenuHost.java # Menu host (quantum bridge support)
    └── WirelessStatPatternsTerminalMenu.java # Wireless terminal menu
```

---

## Build

Requires **Java 21**.

```powershell
.\gradlew.bat build
```

Output: `build/libs/statpatterns-0.1.0.jar`.

---

## License

This project is open-source under [GNU LGPLv3](LICENSE). As a derivative work of AE2, it follows AE2''s license.
