/*
 * Probability Pattern for AE2 — interactive offline simulator for the FULL crafting
 * chain, stage by stage. Pure Java (no Minecraft), semantics from decompiled/ae2-695.
 *
 * Stages:
 *   [1] encode           -> simulated pattern NBT
 *   [2] recognize        -> getPatternForItem -> StatisticalPatternDetails
 *   [3] interface        -> addToCraftingList (isReady) / provideCrafting (isActive) — you set
 *                           whether the interface is connected/active, and watch the failure
 *   [4] craftableItems   -> setPatternsFromCraftingMethods(details.getOutputs())
 *   [5] crafting tree    -> CraftingTreeNode.request loop: getTimes(remaining, outputPerAttempt)
 *                           -> times; working-stock produced = outputPer * times
 *   [6] machine run      -> Monte Carlo: actually run "times" attempts with success p and count
 *                           how often production >= N (should be ~= 1 - alpha for the plan)
 *
 * Build/run:
 *   javac -encoding UTF-8 -d tools-out tools/SimulateCrafting.java src/main/java/com/tz/statpatterns/math/*.java
 *   java -cp tools-out SimulateCrafting
 */
import java.util.Random;
import java.util.Scanner;

import com.tz.statpatterns.math.ProbabilitySizing;
import com.tz.statpatterns.math.ProbabilitySizingResult;

public class SimulateCrafting {

    private static final Scanner IN = new Scanner(System.in);
    private static final Random RND = new Random();

    public static void main(final String[] args) {
        banner();
        do {
            System.out.println("---------------- 参数设置（回车=默认） ----------------");
            final double p = askDouble("单次成功概率 p", 0.8);
            final double alpha = askDouble("置信度风险 alpha", 0.05);
            final long inputPer = askLong("每次尝试消耗输入", 8);
            final long outputPer = askLong("每次成功产出", 1);
            final long requested = askLong("ME 终端请求产物 N", 64);
            final boolean ifaceActive = askBool("接口是否已接入激活网格", true);
            final boolean multi = askBool("输入是否也是概率样板产物（多级）", false);
            final int monteCarlo = (int) askLong("Monte Carlo 模拟次数", 2000);

            runChain(p, alpha, inputPer, outputPer, requested, ifaceActive, multi, monteCarlo);
        } while (askBool("是否再来一轮？", true));

        System.out.println("模拟结束。");
    }

    // ==================== 完整链路 ====================

    private static void runChain(final double p, final double alpha, final long inputPer, final long outputPer,
        final long requested, final boolean ifaceActive, final boolean multi, final int monteCarlo) {
        System.out.println("\n==================== 逐阶段模拟 ====================");

        step1Encode(p, alpha);
        final long requiredSuccesses = ceilDiv(requested, outputPer);
        step2Recognize(p, alpha, requiredSuccesses, inputPer, outputPer);
        if (!step3Interface(ifaceActive)) {
            System.out.println("\n>>> 接口未激活 -> provideCrafting 直接 return，craftableItems 为空");
            System.out.println(">>> 终端不显示可合成、无法发起合成。修复: 把接口接入激活网格。");
            return;
        }
        step4Craftables(p, alpha, inputPer, outputPer);
        step5Tree(p, alpha, inputPer, outputPer, requested, multi);
        step6MachineRun(p, alpha, inputPer, outputPer, requested, monteCarlo, multi);
    }

    private static void step1Encode(final double p, final double alpha) {
        System.out.println("\n[1] 编码样板  (ContainerProbabilityPatternTerm.encode, 复刻 695)");
        System.out.println("    -> ProbabilityPatternItem，模拟 NBT:");
        System.out.printf("       in=[9槽含空] out=[产物] crafting=false substitute=false%n");
        System.out.printf("       beSubstitute=false author=玩家%n");
        System.out.printf("       sp_probability=%.4f  sp_alpha=%.4f  sp_alpha95=%s%n", p, alpha, alpha <= 0.05);
        System.out.println("    结果: encode OK（与原版 NBT 一致，仅多 sp_*）");
    }

    private static void step2Recognize(final double p, final double alpha, final long requiredSuccesses,
        final long inputPer, final long outputPer) {
        System.out.println("\n[2] 样板识别  (ProbabilityPatternItem.getPatternForItem)");
        System.out.println("    -> StatisticalPatternDetails extends PatternHelper");
        System.out.printf("       p=%.3f alpha=%.3f isProbabilityPattern=%s%n", p, alpha, p < 1.0);
        System.out.printf("       outputs[产物 x%d]  inputs[每次%d]%n", outputPer, inputPer);
        System.out.println("    结果: 识别成功（非 null）");
    }

    private static boolean step3Interface(final boolean active) {
        System.out.println("\n[3] 放入 ME 接口  (DualityInterface)");
        System.out.printf("    addToCraftingList 需要 gridProxy.isReady()   = %s%n", active);
        System.out.printf("    provideCrafting   需要 gridProxy.isActive()  = %s%n", active);
        System.out.println("    -> 满足后 details 进入 craftingList 并 addCraftingOption");
        return active;
    }

    private static void step4Craftables(final double p, final double alpha, final long inputPer,
        final long outputPer) {
        System.out.println("\n[4] craftableItems 注册  (CraftingGridCache.setPatternsFromCraftingMethods)");
        System.out.println("    遍历 details.getOutputs() -> copy+reset+setCraftable -> craftableItems");
        System.out.printf("    craftableItems[产物]  （每 %d 输入 -> %d 产出，单次概率 %.2f）%n", inputPer, outputPer, p);
        System.out.println("    ME 终端此时应显示产物可合成（绿色）");
    }

    private static void step5Tree(final double p, final double alpha, final long inputPer, final long outputPer,
        final long requested, final boolean multi) {
        System.out.println("\n[5] 合成树 / 发配  (CraftingTreeNode.request 循环)");
        System.out.println("    while (剩余 > 0) { pro.request(inv, pro.getTimes(剩余, outputPer), src) }");

        final long requiredSuccesses = ceilDiv(requested, outputPer);

        // 原版：getTimes = ceil(remaining/outputPer)，一次请求次数=requiredSuccesses
        final long vanillaTimes = requiredSuccesses;
        final long vanillaWork = outputPer * vanillaTimes;
        final long vanillaExpected = Math.round(p * vanillaTimes * outputPer);

        // 概率：getTimes = plannedAttempts(requiredSuccesses)
        final ProbabilitySizingResult sizing = ProbabilitySizing.planAttempts(requiredSuccesses, p, alpha, 30);
        final long probTimes = sizing.attempts();
        final long probWork = outputPer * probTimes;

        System.out.printf("    请求 N=%-5d 需成功=%d 每次输入=%d 每次产出=%d%n", requested, requiredSuccesses, inputPer,
            outputPer);
        System.out.println("    ------------------------------------------------------");
        System.out.printf("    原版计划 : getTimes=%d 次 | 工作区记账产出=%d | 发配材料=%d%n",
            vanillaTimes, vanillaWork, inputPer * vanillaTimes);
        System.out.printf("    概率计划 : getTimes=%d 次 | 工作区记账产出=%d | 发配材料=%d%n",
            probTimes, probWork, inputPer * probTimes);
        System.out.println("    ------------------------------------------------------");
        System.out.printf("    原版期望实际产出≈%d（缺口 %d）; 概率保证 P(产出≥N)≥%.0f%%%n",
            vanillaExpected, Math.max(0L, requested - vanillaExpected), (1.0 - alpha) * 100.0);

        if (multi) {
            final long childRequest = inputPer * probTimes;
            final ProbabilitySizingResult child = ProbabilitySizing.planAttempts(childRequest, p, alpha, 30);
            System.out.printf("    多级: 子样板需求=输入×次数=%d，子样板发配 %d 次、材料 %d%n",
                childRequest, child.attempts(), inputPer * child.attempts());
        }
    }

    private static void step6MachineRun(final double p, final double alpha, final long inputPer,
        final long outputPer, final long requested, final int monteCarlo, final boolean multi) {
        System.out.println("\n[6] 机器实际运行（Monte Carlo 真实跑概率机器）");

        final long requiredSuccesses = ceilDiv(requested, outputPer);
        final long vanillaTimes = requiredSuccesses;
        final ProbabilitySizingResult sizing = ProbabilitySizing.planAttempts(requiredSuccesses, p, alpha, 30);
        final long probTimes = sizing.attempts();

        // 原版方案：跑 vanillaTimes 次，统计实际产出>=N 的比例
        final int vanillaSatisfied = runMachine(vanillaTimes, p, outputPer, requested, monteCarlo);
        // 概率方案：跑 probTimes 次
        final int probSatisfied = runMachine(probTimes, p, outputPer, requested, monteCarlo);

        System.out.printf("    每次模拟: 跑机器 %d 次尝试（每次 p=%.2f 成功，每次成功产出 %d）%n", probTimes, p, outputPer);
        System.out.printf("    重复 %d 次统计实际产出>=N=%d 的比例:%n", monteCarlo, requested);
        System.out.printf("      原版计划 (%d 次尝试): %.1f%% 满足  (期望≈%.0f%%)%n",
            vanillaTimes, 100.0 * vanillaSatisfied / monteCarlo, p * 100.0);
        System.out.printf("      概率计划 (%d 次尝试): %.1f%% 满足  (目标≥%.0f%%)%n",
            probTimes, 100.0 * probSatisfied / monteCarlo, (1.0 - alpha) * 100.0);
    }

    private static int runMachine(final long attempts, final double p, final long outputPer, final long requested,
        final int trials) {
        int satisfied = 0;
        for (int t = 0; t < trials; t++) {
            long successes = 0;
            for (long a = 0; a < attempts; a++) {
                if (RND.nextDouble() < p) {
                    successes++;
                }
            }
            if (successes * outputPer >= requested) {
                satisfied++;
            }
        }
        return satisfied;
    }

    // ==================== 输入辅助 ====================

    private static double askDouble(final String prompt, final double def) {
        System.out.print(prompt + " [" + def + "]: ");
        final String line = IN.nextLine().trim();
        if (line.isEmpty()) {
            return def;
        }
        try {
            return Double.parseDouble(line);
        } catch (final NumberFormatException e) {
            return def;
        }
    }

    private static long askLong(final String prompt, final long def) {
        System.out.print(prompt + " [" + def + "]: ");
        final String line = IN.nextLine().trim();
        if (line.isEmpty()) {
            return def;
        }
        try {
            return Long.parseLong(line);
        } catch (final NumberFormatException e) {
            return def;
        }
    }

    private static boolean askBool(final String prompt, final boolean def) {
        System.out.print(prompt + " [" + (def ? "y/N" : "N/y") + "]: ");
        final String line = IN.nextLine().trim().toLowerCase();
        if (line.isEmpty()) {
            return def;
        }
        if (line.startsWith("y")) {
            return true;
        }
        if (line.startsWith("n")) {
            return false;
        }
        return def;
    }

    private static long ceilDiv(final long a, final long b) {
        return (a + b - 1) / b;
    }

    private static void banner() {
        System.out.println("=============================================================");
        System.out.println("  概率样板 · 逐阶段虚拟下单模拟（GTNH AE2 695 语义）");
        System.out.println("  来源: decompiled/ae2-695 | 概率计算: ProbabilitySizing");
        System.out.println("  回车使用默认值，Ctrl+C 退出");
        System.out.println("=============================================================");
    }
}
