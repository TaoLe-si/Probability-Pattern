package com.tz.statpatterns;

import appeng.core.definitions.AEItems;
import appeng.core.definitions.ItemDefinition;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public final class SPCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ProbabilityPatternMod.MOD_ID);

    private static final List<ItemDefinition<?>> itemDefs = new ArrayList<>();
    private static final List<Item> rawItems = new ArrayList<>();

    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.probabilitypattern"))
                    .icon(() -> AEItems.PROCESSING_PATTERN.stack())
                    .displayItems((parameters, output) -> {
                        for (var itemDefinition : itemDefs) {
                            output.accept(itemDefinition);
                        }
                        for (var item : rawItems) {
                            output.accept(item);
                        }
                    })
                    .build());

    public static void add(ItemDefinition<?> itemDef) {
        itemDefs.add(itemDef);
    }

    public static void addRaw(Item item) {
        rawItems.add(item);
    }

    private SPCreativeTabs() {
    }
}
