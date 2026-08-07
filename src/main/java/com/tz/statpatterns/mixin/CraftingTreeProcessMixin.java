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
 * Injects at the head of {@code CraftingTreeProcess.getTimes(long, long)} (GTNH 695):
 * for a probability pattern the deterministic {@code ceil(remaining/outputPerAttempt)}
 * is replaced by the binomial / normal-approximation attempt plan from
 * {@code ProbabilitySizing}, guaranteeing P(produced &gt;= remaining) &gt;= 1 - alpha.
 * <p>
 * AE2 loads after Mixin's early phase, so this is registered as a late mixin via
 * {@link com.tz.statpatterns.LateMixinLoader}.
 * <p>
 * remap = false: CraftingTreeProcess is a mod class (not obfuscated).
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
        // AE2 returns 1 while simulating the job or running a quantity-limited request;
        // overriding those phases would multiply the requested quantities, so keep it.
        if (this.limitQty || this.fullSimulation) {
            return;
        }
        if (this.details instanceof StatisticalPatternDetails) {
            final StatisticalPatternDetails spd = (StatisticalPatternDetails) this.details;
            if (spd.isProbabilityPattern()) {
                // getTimes(remaining, stackSize): remaining = target output amount,
                // stackSize = output per attempt, so the required successes are
                // ceil(remaining / stackSize).
                final long requiredSuccesses = remaining <= 0 ? 1L : (remaining + stackSize - 1) / stackSize;
                final long times = spd.plannedAttempts(requiredSuccesses);
                FMLLog.info(
                    "[ProbabilityPattern] getTimes intercepted: remaining=%d stackSize=%d success=%d times=%d p=%.3f alpha=%.3f",
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
