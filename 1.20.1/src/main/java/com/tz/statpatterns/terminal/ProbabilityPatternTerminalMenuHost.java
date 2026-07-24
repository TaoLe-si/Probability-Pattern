package com.tz.statpatterns.terminal;

import java.util.function.BiConsumer;

import com.tz.statpatterns.api.ids.Components;
import com.tz.statpatterns.part.ProbabilityPatternEncodingLogic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.inventories.InternalInventory;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.menu.ISubMenu;
import appeng.parts.encoding.PatternEncodingLogic;

import de.mari_023.ae2wtlib.terminal.WTMenuHost;

/**
 * Menu host for the handheld probability pattern terminal.
 * Extends WTMenuHost for ae2wtlib Quantum Bridge support.
 */
public class ProbabilityPatternTerminalMenuHost extends WTMenuHost
        implements IPatternTerminalMenuHost, IPatternTerminalLogicHost {
    private final ProbabilityPatternEncodingLogic logic;
    private boolean isLoading = false;

    public ProbabilityPatternTerminalMenuHost(Player player, Integer slot, ItemStack stack,
            BiConsumer<Player, ISubMenu> returnToMainMenu) {
        super(player, slot, stack, returnToMainMenu);
        this.logic = new ProbabilityPatternEncodingLogic(this);
        loadFromItem();
    }

    public double getProbability() {
        return logic.getProbability();
    }

    public boolean isAlpha95() {
        return logic.isAlpha95();
    }

    public void setProbability(double probability) {
        logic.setProbability(probability);
    }

    public void setAlpha95(boolean value) {
        logic.setAlpha95(value);
    }

    @Override
    public PatternEncodingLogic getLogic() {
        return logic;
    }

    @Override
    public Level getLevel() {
        return getPlayer().level();
    }

    @Override
    public void markForSave() {
        if (!isLoading) {
            saveToItem();
        }
    }

    /**
     * Load the pattern encoding logic state from the item stack's NBT.
     */
    private void loadFromItem() {
        CompoundTag tag = Components.readPatternLogicState(getItemStack());
        if (tag != null) {
            isLoading = true;
            try {
                logic.readFromNBT(tag);
            } finally {
                isLoading = false;
            }
        }
    }

    /**
     * Save the pattern encoding logic state to the item stack's NBT.
     */
    private void saveToItem() {
        ItemStack stack = getItemStack();
        CompoundTag tag = new CompoundTag();
        logic.writeToNBT(tag);
        Components.writePatternLogicState(stack, tag);
    }

    public InternalInventory getSingularityInventory() {
        return getSubInventory(WTMenuHost.INV_SINGULARITY);
    }
}
