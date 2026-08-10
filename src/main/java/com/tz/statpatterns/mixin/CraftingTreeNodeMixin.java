/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tz.statpatterns.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEKey;
import appeng.crafting.CraftingTreeNode;
import com.tz.statpatterns.crafting.StatPatternsDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collection;

@Mixin(CraftingTreeNode.class)
public abstract class CraftingTreeNodeMixin {

    @Shadow
    private long amount;

    @Unique
    private long probabilityTotalRequested;

    /**
     * Capture requestedAmount *just before* buildChildPatterns() is called.
     * At this point requestedAmount has been reduced by any items extracted
     * from inventory, so this reflects the amount that actually needs crafting.
     */
    @ModifyVariable(
            method = "request",
            at = @At(value = "INVOKE",
                    target = "Lappeng/crafting/CraftingTreeNode;buildChildPatterns()V"),
            argsOnly = true)
    private long captureRequested(long requestedAmount) {
        this.probabilityTotalRequested = requestedAmount * this.amount;
        return requestedAmount;
    }

    @Redirect(
            method = "buildChildPatterns",
            at = @At(value = "INVOKE",
                    target = "Lappeng/api/networking/crafting/ICraftingService;getCraftingFor(Lappeng/api/stacks/AEKey;)Ljava/util/Collection;"))
    private Collection<IPatternDetails> wrapPatternsForNode(ICraftingService service, AEKey whatToCraft) {
        var patterns = service.getCraftingFor(whatToCraft);
        if (this.probabilityTotalRequested <= 0) {
            return patterns;
        }
        var result = new ArrayList<IPatternDetails>(patterns.size());
        for (var p : patterns) {
            if (p instanceof StatPatternsDetails spd) {
                result.add(spd.forRequest(this.probabilityTotalRequested));
            } else {
                result.add(p);
            }
        }
        return result;
    }
}