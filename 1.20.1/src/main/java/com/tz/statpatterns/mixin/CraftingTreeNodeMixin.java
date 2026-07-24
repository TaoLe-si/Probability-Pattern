package com.tz.statpatterns.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEKey;
import appeng.crafting.CraftingTreeNode;
import com.tz.statpatterns.crafting.StatisticalPatternDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;

@Mixin(value = CraftingTreeNode.class, remap = false)
public abstract class CraftingTreeNodeMixin {

    @Shadow
    private long amount;

    @Unique
    private long probabilityPattern$totalRequested;

    /**
     * Intercept request() just before buildChildPatterns() is called.
     * Capture the remaining requestedAmount (after inventory extraction).
     */
    @Inject(
            method = "request",
            at = @At(value = "INVOKE",
                    target = "Lappeng/crafting/CraftingTreeNode;buildChildPatterns()V"))
    private void beforeBuildChildPatterns(
            appeng.crafting.inv.CraftingSimulationState inv,
            long requestedAmount,
            appeng.api.stacks.KeyCounter containerItems,
            CallbackInfo ci) {
        this.probabilityPattern$totalRequested = requestedAmount * this.amount;
    }

    @Redirect(
            method = "buildChildPatterns",
            at = @At(value = "INVOKE",
                    target = "Lappeng/api/networking/crafting/ICraftingService;getCraftingFor(Lappeng/api/stacks/AEKey;)Ljava/util/Collection;"))
    private Collection<IPatternDetails> wrapPatternsForNode(ICraftingService service, AEKey whatToCraft) {
        var patterns = service.getCraftingFor(whatToCraft);
        if (this.probabilityPattern$totalRequested <= 0) {
            return patterns;
        }
        var result = new ArrayList<IPatternDetails>(patterns.size());
        for (var p : patterns) {
            if (p instanceof StatisticalPatternDetails spd) {
                result.add(spd.forRequest(this.probabilityPattern$totalRequested));
            } else {
                result.add(p);
            }
        }
        return result;
    }
}
