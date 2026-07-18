package com.tz.statpatterns.item;

import java.util.function.DoubleSupplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.phys.BlockHitResult;

import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.ItemMenuHostLocator;

import com.tz.statpatterns.core.definition.SPMenus;
import com.tz.statpatterns.terminal.ProbabilityPatternTerminalMenuHost;

public class ProbabilityPatternTerminalItem extends WirelessTerminalItem {
    private static final double POWER_CAPACITY = 1600000.0;

    public ProbabilityPatternTerminalItem(Properties props) {
        super(() -> POWER_CAPACITY, props);
    }

    @Override
    public MenuType<?> getMenuType() {
        return SPMenus.PROBABILITY_PATTERN_TERMINAL.get();
    }

    @Nullable
    @Override
    public ProbabilityPatternTerminalMenuHost getMenuHost(Player player, ItemMenuHostLocator locator,
            @Nullable BlockHitResult hitResult) {
        return new ProbabilityPatternTerminalMenuHost(this, player, locator,
                (p, sm) -> openFromInventory(p, locator, true));
    }
}