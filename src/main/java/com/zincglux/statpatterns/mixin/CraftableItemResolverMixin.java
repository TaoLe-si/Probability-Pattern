/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 zincglux
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
package com.zincglux.statpatterns.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.zincglux.statpatterns.crafting.StatisticalPatternDetails;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.crafting.v2.resolvers.CraftableItemResolver;
import appeng.util.Platform;

/**
 * v2 probability amplification (Spec v2.0 3.4.1, GTNH 695 uses the v2 crafting
 * calculator by default — {@code AEConfig.craftingCalculatorVersion == 2}).
 * <p>
 * The v2 crafting tree computes the number of pattern runs in
 * {@code CraftableItemResolver$CraftFromPatternTask.calculateOneStep} as
 * {@code toCraft = ceilDiv(request.remainingToProcess, matchingOutput.getStackSize())}
 * (non-complex path). This mixin redirects that {@code Platform.ceilDiv} call: for a
 * {@link StatisticalPatternDetails} it replaces the deterministic count with
 * {@code plannedAttempts(requiredSuccesses)}, guaranteeing
 * {@code P(produced >= requested) >= 1 - alpha}.
 * <p>
 * It also fixes the displayed plan ({@code populatePlan}): the amplified run count
 * (e.g. 18 runs for a request of 10 with p=0.8) must only inflate the <em>material</em>
 * request, not the <em>finished-good</em> count shown to the player — the plan shows the
 * requested quantity (10), while the CPU still pushes the amplified number of runs so a
 * probabilistic machine gets enough attempts.
 */
@Mixin(value = CraftableItemResolver.CraftFromPatternTask.class, remap = false)
public abstract class CraftableItemResolverMixin {

    @Shadow
    public ICraftingPatternDetails pattern;

    @Shadow
    public boolean isComplex;

    @Shadow
    protected IAEItemStack matchingOutput;

    @Shadow
    protected long fulfilledAmount;

    /**
     * Redirects the {@code toCraft} computation inside {@code calculateOneStep}:
     * {@code ceilDiv(remainingToProcess, matchingOutput.getStackSize())}. For a
     * probability pattern this returns the amplified attempt count. Only applies on the
     * non-complex path (complex patterns still run one recipe at a time).
     */
    @Redirect(method = "calculateOneStep", at = @At(value = "INVOKE", target = "Lappeng/util/Platform;ceilDiv(JJ)J"))
    private long probabilityPatternTimes(final long remaining, final long stackSize) {
        final long deterministic = Platform.ceilDiv(remaining, stackSize);
        if (this.isComplex || this.pattern == null || !(this.pattern instanceof StatisticalPatternDetails)) {
            return deterministic;
        }
        final StatisticalPatternDetails spd = (StatisticalPatternDetails) this.pattern;
        if (!spd.isProbabilityPattern()) {
            return deterministic;
        }
        final long requiredSuccesses = deterministic <= 0L ? 1L : deterministic;
        return spd.plannedAttempts(requiredSuccesses);
    }

    /**
     * Fixes the displayed finished-good count in the plan: the default shows
     * {@code output * totalCraftsDone} (e.g. 1*18=18), but for a probability pattern the
     * plan must show the requested quantity ({@code fulfilledAmount}=10). The amplified
     * run count still drives material extraction and CPU pushes.
     */
    @Redirect(
        method = "populatePlan",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/storage/data/IAEItemStack;setCountRequestable(J)Lappeng/api/storage/data/IAEStack;"))
    private IAEStack planRequestableCount(final IAEItemStack stack, final long count) {
        if (this.pattern instanceof StatisticalPatternDetails) {
            final long shown = Math.max(0L, this.fulfilledAmount);
            return stack.setCountRequestable(shown);
        }
        return stack.setCountRequestable(count);
    }
}
