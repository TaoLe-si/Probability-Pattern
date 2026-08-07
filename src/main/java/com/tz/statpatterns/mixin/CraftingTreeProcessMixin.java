/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.tz.statpatterns.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tz.statpatterns.crafting.StatisticalPatternDetails;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.crafting.CraftingTreeProcess;
import cpw.mods.fml.common.FMLLog;

/**
 * Makes AE2's crafting tree run probability patterns enough times.
 * <p>
 * This is the late-phase Mixin equivalent of the 1.21.1 version's
 * {@code CraftingTreeNodeMixin} (and the earlier ASM coremod used in this port).
 * It injects at the head of {@code CraftingTreeProcess.getTimes(long, long)} — the
 * exact spot where AE2 decides how many times to run a processing pattern to produce
 * {@code remaining} output. For a probability pattern we replace the deterministic
 * {@code ceil(remaining/output)} with the binomial / normal-approximation attempt
 * plan from {@code ProbabilitySizing}, guaranteeing
 * P(produced &gt;= remaining) &gt;= 1 - alpha.
 * <p>
 * AE2 is a regular mod (not a coremod), so this is registered as a <b>late</b> mixin
 * via {@link LateMixinLoader} (requires UniMixins / GTNHMixins late phase).
 * <p>
 * remap = false: CraftingTreeProcess is a mod class (not obfuscated), so Mixin's
 * annotation processor must not try to resolve its method/field names to SRG names.
 */
@Mixin(value = CraftingTreeProcess.class, remap = false)
public abstract class CraftingTreeProcessMixin {

    @Shadow
    private ICraftingPatternDetails details;

    @Shadow
    private boolean limitQty;

    @Shadow
    private boolean fullSimulation;

    @Inject(method = "getTimes", at = @At("HEAD"), cancellable = true)
    private void probabilityPatternTimes(final long remaining, final long stackSize,
        final CallbackInfoReturnable<Long> cir) {
        // AE2 short-circuits getTimes to 1 when it is only simulating the job or running a
        // quantity-limited request. Overriding those phases with a huge planned attempt
        // count would multiply the requested quantities many-fold, so keep AE2's behavior.
        if (this.limitQty || this.fullSimulation) {
            return;
        }
        if (this.details instanceof StatisticalPatternDetails) {
            final StatisticalPatternDetails spd = (StatisticalPatternDetails) this.details;
            if (spd.isProbabilityPattern()) {
                // 695's CraftingTreeProcess.getTimes(remaining, stackSize) is called with
                // remaining = target output amount and stackSize = output per attempt, and
                // returns ceil(remaining / stackSize) = how many successful attempts we need.
                // Plan for that many successes (not the raw output amount), otherwise every
                // attempt that yields > 1 output would over-plan and the requested quantity
                // would be wrong.
                final long requiredSuccesses = remaining <= 0 ? 1L : (remaining + stackSize - 1) / stackSize;
                final long times = spd.plannedAttempts(requiredSuccesses);
                FMLLog.info(
                    "[ProbabilityPattern] CraftingTreeProcess.getTimes intercepted: remaining=%d stackSize=%d success=%d times=%d p=%.3f alpha=%.3f",
                    remaining,
                    stackSize,
                    requiredSuccesses,
                    times,
                    spd.successProbability(),
                    spd.alpha());
                cir.setReturnValue(times);
            }
        }
    }
}
