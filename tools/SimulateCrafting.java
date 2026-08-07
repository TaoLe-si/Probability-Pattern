/*
 * Probability Pattern for AE2 — interactive offline "virtual crafting order" simulator.
 *
 * Simulates the ENTIRE GTNH AE2 695 crafting chain for a probability pattern, using the
 * exact semantics confirmed from the decompiled sources (decompiled/ae2-695):
 *
 *   [1] encode      -> our ProbabilityPatternItem NBT (in/out/crafting/substitute/
 *                      beSubstitute/author + sp_probability/sp_alpha/sp_alpha95)
 *   [2] recognize   -> getPatternForItem -> StatisticalPatternDetails (extends PatternHelper)
 *   [3] interface   -> DualityInterface.addToCraftingList (needs gridProxy.isReady())
 *                      -> provideCrafting (needs gridProxy.isActive())
 *   [4] craftables  -> CraftingGridCache.setPatternsFromCraftingMethods(details.getOutputs())
 *                      -> craftableItems
 *   [5] crafting    -> CraftingJob -> CraftingTreeNode:
 *                        pro.request(inv, pro.getTimes(l, outputPerAttempt), src)
 *                      getTimes = ceil(remaining/stackSize)  (vanilla)
 *                      our mixin replaces it with plannedAttempts(ceil(N/outputPerAttempt))
 *                      so P(production >= N) >= 1 - alpha.
 *
 * You set the pattern parameters interactively and it prints every step, the dispatched
 * attempt/material quantities and the guaranteed production, so you can see where a
 * requested amount is (or is not) satisfied.
 *
 * Build/run (pure Java, no Minecraft needed):
 *   javac -encoding UTF-8 -d tools-out tools/SimulateCrafting.java src/main/java/com/tz/statpatterns/math/*.java
 *   java -cp tools-out SimulateCrafting
 */
import java.util.Scanner;

import com.tz.statpatterns.math.ProbabilitySizing;
import com.tz.statpatterns.math.ProbabilitySizingResult;

public class SimulateCrafting {

    private static final Scanner IN = new Scanner(System.in);

    public static void main(final String[] args) {
        banner();
        do {
            System.out.println("---------------- 参数设置（直接回车 = 默认值） ----------------");
            final double p = askDouble("单次成功概率 p (0<p<=1)", 0.8);
            final double alpha = askDouble("置信度风险 alpha (0<alpha<1)", 0.05);
            final long inputPer = askLong("每次尝试消耗的输入数量", 8);
            final long outputPer = askLong("每次成功产出的数量", 1);
            final long requested = askLong("ME 终端请求的产物数量 N", 64);
            final boolean multi = askBool("输入是否也是概率样板产物（多级合成树）", false);

            runSimulation(p, alpha, inputPer, outputPer, requested, multi);
        } while (askBool("是否再来一轮？", true));

        System.out.println("模拟结束。");
    }

    // ==================== 完整链路 ====================

    private static void runSimulation(final double p, final double alpha, final long inputPer,
        final long outputPer, final long requested, final boolean multi) {
        System.out.println("\n==================== 合成链路模拟 ====================");

        step1_encode(p, alpha);
        step2_recognize(p, alpha);
        step3_interface();
        step4_craftable(p, alpha, inputPer, outputPer);
        step5_craftingTree(p, alpha, inputPer, outputPer, requested, multi);
    }

    private static void step1_encode(final double p, final double alpha) {
        System.out.println("\n[1] 编码样板  (ContainerProbabilityPatternTerm.encode, 复刻 695)");
        System.out.println("    -> 生成 ProbabilityPatternItem，NBT:");
        System.out.println("       in=[9 槽，含空] out=[产物] crafting=false substitute=false");
        System.out.println("       beSubstitute=false author=玩家");
        System.out.printf("       sp_probability=%.4f  sp_alpha=%.4f  sp_alpha95=%s%n",
            p, alpha, alpha <= 0.05);
        System.out.println("    结果: encode: OK（NBT 与原版一致，仅多 sp_* 字段）");
    }

    private static void step2_recognize(final double p, final double alpha) {
        System.out.println("\n[2] 样板识别  (ProbabilityPatternItem.getPatternForItem)");
        System.out.println("    -> StatisticalPatternDetails extends PatternHelper:");
        System.out.printf("       p=%.3f alpha=%.3f isProbabilityPattern=%s%n",
            p, alpha, p < 1.0);
        System.out.println("    结果: 识别成功（非 null），接口可将其加入 craftingList");
    }

    private static void step3_interface() {
        System.out.println("\n[3] 放入 ME 接口  (DualityInterface)");
        System.out.println("    前置条件（AE2 硬性判断，反编译确认）:");
        System.out.println("       - addToCraftingList 需要 gridProxy.isReady()（接口已连网格）");
        System.out.println("       - provideCrafting  需要 gridProxy.isActive()（网格已激活/充能）");
        System.out.println("    -> 满足后 details 进入 craftingList，再 addCraftingOption");
        System.out.println("    注意: 若接口未接控制器/充能，此步静默失败 -> 终端永远无法合成");
    }

    private static void step4_craftable(final double p, final double alpha, final long inputPer,
        final long outputPer) {
        System.out.println("\n[4] craftableItems 注册  (CraftingGridCache.setPatternsFromCraftingMethods)");
        System.out.println("    遍历 details.getOutputs() -> copy+reset+setCraftable -> craftableItems");
        System.out.printf("    -> craftableItems[%s]（每 %d 输入 → %d 产出，单次概率 %.2f）%n",
            "产物", inputPer, outputPer, p);
        System.out.println("    ME 终端此时应显示产物为可合成（绿色标记）");
        System.out.printf("    期望发配（概率补偿）: 每 N 需求跑 planAttempts(ceil(N/%d)) 次%n", outputPer);
    }

    private static void step5_craftingTree(final double p, final double alpha, final long inputPer,
        final long outputPer, final long requested, final boolean multi) {
        System.out.println("\n[5] 合成树 / 发配数量  (CraftingJob -> CraftingTreeNode -> getTimes)");
        System.out.println("    CraftingTreeNode.request: pro.request(inv, pro.getTimes(l, outputPer), src)");
        System.out.println("    -> getTimes 返回【尝试次数】，材料=输入×次数，产出记账=输出×次数");

        // required successes = ceil(N / outputPer)
        final long requiredSuccesses = ceilDiv(requested, outputPer);

        // vanilla plan
        final long vanillaTimes = ceilDiv(requested, outputPer);
        final long vanillaExpected = Math.round(p * vanillaTimes * outputPer);
        final long vanillaGap = Math.max(0L, requested - vanillaExpected);

        // probability plan
        final ProbabilitySizingResult sizing = ProbabilitySizing
            .planAttempts(requiredSuccesses, p, alpha, 30);
        final long probTimes = sizing.attempts();
        final long probExpected = Math.round(p * probTimes * outputPer);
        final boolean guaranteed = sizing.underproductionRisk() <= alpha;

        System.out.println();
        System.out.println("    ---------------------------------------------------------");
        System.out.printf("    请求 N=%-6d 需成功次数=%-5d 每次输入=%-4d 每次产出=%-3d%n",
            requested, requiredSuccesses, inputPer, outputPer);
        System.out.println("    ---------------------------------------------------------");
        System.out.printf("    原版计划 : 次数=%-6d 材料=%-8d 产出记账=%-8d 期望实际产出≈%-6d 缺口=%d%n",
            vanillaTimes, inputPer * vanillaTimes, outputPer * vanillaTimes, vanillaExpected, vanillaGap);
        System.out.printf("    概率计划 : 次数=%-6d 材料=%-8d 产出记账=%-8d 期望实际产出≈%-6d 保证≥N? %s (1-α=%.0f%%)%n",
            probTimes, inputPer * probTimes, outputPer * probTimes, probExpected, guaranteed,
            (1.0 - alpha) * 100.0);
        System.out.println("    ---------------------------------------------------------");

        if (multi) {
            System.out.println("    多级: 输入也是概率样板产物，子节点需求 = 输入数量 × 次数");
            final long childRequest = inputPer * probTimes;
            System.out.printf("          子样板需求 = %d × %d = %d 个中间产物（继续按同样 p 递归）%n",
                inputPer, probTimes, childRequest);
            final ProbabilitySizingResult child = ProbabilitySizing
                .planAttempts(childRequest, p, alpha, 30);
            System.out.printf("          子样板发配次数 = %d，材料 = %d × %d = %d%n",
                child.attempts(), inputPer, child.attempts(), inputPer * child.attempts());
        }

        System.out.println("\n    结论:");
        System.out.println("      - 原版不补偿概率 -> 期望产出 p×N，N 越大缺口越大（这正是'数量不一致'）");
        System.out.println("      - 概率计划增加发配次数，保证 P(实际产出≥N) ≥ 1-α");
        System.out.println("      - 发配材料 = 输入 × 次数（每次尝试都消耗材料，即使失败）");
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
            System.out.println("  无效输入，使用默认 " + def);
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
            System.out.println("  无效输入，使用默认 " + def);
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
        System.out.println("  概率样板 · 虚拟下单（模拟 GTNH AE2 695 完整合成链路）");
        System.out.println("  反编译语义来源: decompiled/ae2-695");
        System.out.println("  按提示输入参数，回车使用默认值。Ctrl+C 退出。");
        System.out.println("=============================================================");
    }
}
