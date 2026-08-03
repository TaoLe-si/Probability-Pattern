package com.tz.statpatterns.terminal;

import appeng.api.networking.IGridNode;
import appeng.core.definitions.AEItems;
import appeng.menu.slot.RestrictedInputSlot;
import com.tz.statpatterns.core.definition.SPMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import appeng.helpers.IPatternTerminalMenuHost;
import de.mari_023.ae2wtlib.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.wut.ItemWUT;

/**
 * Wireless variant of the probability pattern terminal menu.
 * Includes singularity slot for Quantum Bridge Card and WUT support.
 */
public class WirelessProbabilityPatternTerminalMenu extends ProbabilityPatternTerminalMenu {

    private final ProbabilityPatternTerminalMenuHost wtHost;

    public WirelessProbabilityPatternTerminalMenu(int containerId, Inventory playerInventory,
            @Nullable IPatternTerminalMenuHost host) {
        this(SPMenus.WIRELESS_PROBABILITY_PATTERN_TERMINAL, containerId, playerInventory, host);
    }

    public WirelessProbabilityPatternTerminalMenu(MenuType<?> menuType, int containerId,
            Inventory playerInventory, @Nullable IPatternTerminalMenuHost host) {
        super(menuType, containerId, playerInventory, host);

        this.wtHost = (ProbabilityPatternTerminalMenuHost) host;

        // Singularity slot for Quantum Bridge Card
        this.addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.QE_SINGULARITY,
                wtHost.getSingularityInventory(), 0), AE2wtlibSlotSemantics.SINGULARITY);
    }

    public IGridNode getGridNode() {
        return wtHost.getActionableNode();
    }

    public boolean isWUT() {
        return wtHost.getItemStack().getItem() instanceof ItemWUT;
    }

    public ProbabilityPatternTerminalMenuHost getWTHost() {
        return wtHost;
    }

    @Override
    protected ItemStack transferStackToMenu(ItemStack stack) {
        if (stack.is(AEItems.QUANTUM_ENTANGLED_SINGULARITY.stack().getItem())
                || stack.is(AEItems.SINGULARITY.stack().getItem())) {
            for (var slot : slots) {
                if (slot.mayPlace(stack)) {
                    slot.safeInsert(stack);
                    return ItemStack.EMPTY;
                }
            }
            return ItemStack.EMPTY;
        }
        return super.transferStackToMenu(stack);
    }
}
