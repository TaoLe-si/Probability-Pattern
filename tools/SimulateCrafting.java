/*
 * Probability Pattern for AE2 — offline "virtual crafting order" simulation.
 *
 * Simulates the GTNH AE2 695 crafting tree for a processing pattern, exactly as
 * confirmed from the decompiled sources (decompiled/ae2-695):
 *
 *   CraftingTreeNode.request(inv, l, src)
 *     -> pro.request(inv, pro.getTimes(l, outputPerAttempt), src)
 *          getTimes(remaining, stackSize) = ceil(remaining / stackSize)   (vanilla)
 *          request 里：材料 = 每次输入 × 次数；产出记账 = 每次输出 × 次数
 *
 * For a probability pattern our CraftingTreeProcessMixin replaces getTimes with
 * plannedAttempts(ceil(remaining / outputPerAttempt)) so that
 * P(successes >= ceil(N/outputPerAttempt)) >= 1 - alpha, i.e. the machine actually
 * produces at least N with confidence 1 - alpha.
 *
 * This program prints, for various requested amounts, the number of attempts AE2
 * would dispatch ("发配次数"), the materials dispatched and the expected/guaranteed
 * production, comparing the vanilla plan against the probability-compensated plan.
 *
 * Build/run (pure Java, no Minecraft needed):
 *   javac -encoding UTF-8 -d tools-out tools/SimulateCrafting.java src/main/java/com/tz/statpatterns/math/*.java
 *   java -cp tools-out SimulateCrafting
 */
import com.tz.statpatterns.math.ProbabilitySizing;

public class SimulateCrafting {

    public static void main(final String[] args) {
        // ---- probability pattern parameters ----
        final double p = 0.8;                 // single-attempt success probability
        final double alpha = 0.05;            // underproduction risk (95% confidence)
        final long inputPerAttempt = 8;       // inputs consumed per attempt
        final long outputPerAttempt = 1;      // outputs produced per successful attempt
        final int smallSampleLimit = 30;

        System.out.println("=== 概率样板虚拟下单（模拟 GTNH AE2 695 合成树）===");
        System.out.printf("单次成功概率 p=%.2f | 置信度=%.0f%% (alpha=%.2f) | 每次消耗=%d | 每次成功产出=%d%n%n",
                p, (1.0 - alpha) * 100.0, alpha, inputPerAttempt, outputPerAttempt);

        final long[] requests = { 1, 8, 16, 64, 128, 512, 1024 };

        for (final long requested : requests) {
            // required successes = ceil(requested / outputPerAttempt)
            final long requiredSuccesses = (requested + outputPerAttempt - 1) / outputPerAttempt;

            // vanilla: getTimes = ceil(remaining / stackSize)
            final long vanillaTimes = (requested + outputPerAttempt - 1) / outputPerAttempt;

            // probability: planned attempts guaranteeing >= requiredSuccesses successes
            final long probTimes = ProbabilitySizing
                .planAttempts(requiredSuccesses, p, alpha, smallSampleLimit)
                .attempts();

            System.out.println("请求产物 N=" + requested + "（需成功次数=" + requiredSuccesses + "）");
            System.out.printf("  原版计划 : 次数=%d | 发配材料=%d | 产出记账=%d | 期望实际产出≈%d | 期望缺口=%d%n",
                    vanillaTimes,
                    inputPerAttempt * vanillaTimes,
                    outputPerAttempt * vanillaTimes,
                    Math.round(p * vanillaTimes * outputPerAttempt),
                    Math.max(0L, requested - Math.round(p * vanillaTimes * outputPerAttempt)));
            System.out.printf("  概率计划 : 次数=%d | 发配材料=%d | 产出记账=%d | 期望实际产出≈%d | 保证产出≥N? %s (1-α=%.0f%%)%n",
                    probTimes,
                    inputPerAttempt * probTimes,
                    outputPerAttempt * probTimes,
                    Math.round(p * probTimes * outputPerAttempt),
                    ProbabilitySizing.planAttempts(requiredSuccesses, p, alpha, smallSampleLimit)
                        .underproductionRisk() <= alpha,
                    (1.0 - alpha) * 100.0);
            System.out.println();
        }

        System.out.println("说明：");
        System.out.println("  - 发配次数 = AE2 getTimes 返回的尝试次数；发配材料 = 每次输入 × 次数；");
        System.out.println("  - 产出记账 = 每次输出 × 次数（AE2 CPU 工作区按全成功记账）；");
        System.out.println("  - 期望实际产出 = p × 次数 × 每次输出（机器只有 p 概率成功）；");
        System.out.println("  - 概率计划通过 binomial/normal 保证 P(成功≥所需) ≥ 1-α，即实际产出≥N 的置信度；");
        System.out.println("  - 原版计划不补偿概率，期望产出往往不足 N（缺口>0）。");
    }
}
