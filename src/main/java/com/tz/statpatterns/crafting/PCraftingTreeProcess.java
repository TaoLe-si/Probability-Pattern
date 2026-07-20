package com.tz.statpatterns.crafting;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.CraftBranchFailure;
import appeng.crafting.CraftingTreeNode;
import appeng.crafting.inv.CraftingSimulationState;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class PCraftingTreeProcess {

    private final PCraftingTreeNode parent;
    final IPatternDetails details;
    private final PCraftingCalculation job;
    // Use linked hashmap to ensure deterministic ordering of subcrafts
    private final Map<PCraftingTreeNode, Long> nodes = new LinkedHashMap<>();
    boolean possible = true;
    private boolean containerItems;
    /**
     * If true, we perform this pattern by 1 at the time. This ensures that container items or outputs get reused when
     * possible.
     */
    private boolean limitQty;

    public PCraftingTreeProcess(ICraftingService cc, PCraftingCalculation job,
                                IPatternDetails details,
                                PCraftingTreeNode craftingTreeNode) {
        this.parent = craftingTreeNode;
        this.details = details;
        this.job = job;

        updateLimitQty();

        final IPatternDetails.IInput[] inputs = this.details.getInputs();
        for (int x = 0; x < inputs.length; ++x) {
            var input = inputs[x];
            var firstInput = input.getPossibleInputs()[0];
            this.nodes.put(new PCraftingTreeNode(cc, job, firstInput.what(), firstInput.amount(), this, x),
                    input.getMultiplier());
        }
    }

    boolean notRecursive(IPatternDetails details) {
        return this.parent == null || this.parent.notRecursive(details);
    }

    /**
     * Check if this pattern has one of its outputs as input. If that's the case, update {@code limitQty} to make sure
     * we simulate this pattern one by one. Also check for container items.
     */
    private void updateLimitQty() {
        // TODO: consider checking substitute inputs as well?
        for (IPatternDetails.IInput input : details.getInputs()) {
            var primaryInput = input.getPossibleInputs()[0];
            boolean isAnInput = false;

            for (var output : details.getOutputs()) {
                if (output.what().matches(primaryInput)) {
                    isAnInput = true;
                    break;
                }
            }

            if (isAnInput) {
                this.limitQty = true;
            }

            if (input.getRemainingKey(primaryInput.what()) != null) {
                this.limitQty = this.containerItems = true;
            }
        }
    }

    boolean limitsQuantity() {
        return this.limitQty;
    }

    void request(CraftingSimulationState inv, long times)
            throws CraftBranchFailure, InterruptedException {
        this.job.handlePausing();

        var containerItems = this.containerItems ? new KeyCounter() : null;

        // request and remove inputs...
        for (var entry : this.nodes.entrySet()) {
            entry.getKey().request(inv, entry.getValue() * times, containerItems);
        }

        // by now we must have succeeded, otherwise an exception would have been thrown by request() above

        // add container items
        if (containerItems != null) {
            for (var stack : containerItems) {
                inv.insert(stack.getKey(), stack.getLongValue(), Actionable.MODULATE);
                inv.addStackBytes(stack.getKey(), stack.getLongValue(), 1);
            }
        }

        // add crafting results..
        for (var out : this.details.getOutputs()) {
            inv.insert(out.what(), out.amount() * times, Actionable.MODULATE);
        }

        inv.addCrafting(details, times);
        inv.addBytes(times);
    }

    long getNodeCount() {
        long tot = 0;

        for (PCraftingTreeNode node : this.nodes.keySet()) {
            tot += node.getNodeCount();
        }

        return tot;
    }

    long getOutputCount(AEKey what) {
        long tot = 0;

        for (var is : this.details.getOutputs()) {
            if (what.matches(is)) {
                tot += is.amount();
            }
        }

        return tot;
    }

    boolean hasMultiplePaths() {
        for (var entry : nodes.entrySet()) {
            if (entry.getKey().hasMultiplePaths()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Count the number of statistical (probability) patterns in this process and all sub-processes.
     */
    int countStatisticalPatterns() {
        int count = details instanceof StatisticalPatternDetails ? 1 : 0;
        for (PCraftingTreeNode node : nodes.keySet()) {
            count += node.countProbabilityPatterns();
        }
        return count;
    }

    /**
     * Pre-scan: count statistical patterns available for a given item, without building the full tree.
     * Follows ALL patterns (both statistical and non-statistical) to correctly count statistical
     * patterns at any depth in the crafting chain.
     * Uses a visited set to avoid infinite recursion.
     */
    static int countStatisticalPatternsFor(ICraftingService cc, AEKey what, Set<AEKey> visited) {
        if (!visited.add(what)) return 0;
        if (cc.canEmitFor(what)) return 0;

        int count = 0;
        for (var p : cc.getCraftingFor(what)) {
            if (p instanceof StatisticalPatternDetails) {
                count++;
            }
            // Recurse into ALL patterns' inputs (not just statistical ones)
            // to find statistical patterns deeper in the chain
            for (var input : p.getInputs()) {
                var firstInput = input.getPossibleInputs()[0].what();
                count += countStatisticalPatternsFor(cc, firstInput, visited);
            }
        }
        return count;
    }
}

