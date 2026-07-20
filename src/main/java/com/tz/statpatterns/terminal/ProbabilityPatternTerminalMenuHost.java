package com.tz.statpatterns.terminal;

import java.util.function.BiConsumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.helpers.IPatternTerminalLogicHost;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.menu.ISubMenu;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.parts.encoding.PatternEncodingLogic;

import com.tz.statpatterns.api.ids.Components;
import com.tz.statpatterns.item.ProbabilityPatternTerminalItem;

public class ProbabilityPatternTerminalMenuHost extends WirelessTerminalMenuHost<ProbabilityPatternTerminalItem>
        implements IPatternTerminalMenuHost, IPatternTerminalLogicHost {

    private final PatternEncodingLogic logic;
    private boolean isLoading = false;

    public ProbabilityPatternTerminalMenuHost(ProbabilityPatternTerminalItem item, Player player,
            ItemMenuHostLocator locator,
            BiConsumer<Player, ISubMenu> returnToMainMenu) {
        super(item, player, locator, returnToMainMenu);
        this.logic = new PatternEncodingLogic(this);
        loadFromItem();
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
     * Load the pattern encoding logic state from the item stack's data component.
     */
    private void loadFromItem() {
        ItemStack stack = getItemStack();
        CompoundTag tag = stack.get(Components.PATTERN_LOGIC_STATE);
        if (tag != null) {
            isLoading = true;
            try {
                logic.readFromNBT(tag, getPlayer().level().registryAccess());
            } finally {
                isLoading = false;
            }
        }
    }

    /**
     * Save the pattern encoding logic state to the item stack's data component.
     */
    private void saveToItem() {
        ItemStack stack = getItemStack();
        CompoundTag tag = new CompoundTag();
        logic.writeToNBT(tag, getPlayer().level().registryAccess());
        stack.set(Components.PATTERN_LOGIC_STATE, tag);
    }
}