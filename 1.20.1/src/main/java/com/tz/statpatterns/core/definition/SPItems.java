package com.tz.statpatterns.core.definition;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.ItemDefinition;
import com.google.common.base.Preconditions;
import com.tz.statpatterns.SPCreativeTabs;
import com.tz.statpatterns.api.ids.ItemIds;
import com.tz.statpatterns.api.ids.SPCreativeTabIds;
import com.tz.statpatterns.crafting.ProbabilityPatternItem;
import com.tz.statpatterns.item.ProbabilityPatternTerminalItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

import static com.tz.statpatterns.ProbabilityPatternMod.MOD_ID;

public final class SPItems {
    public static final DeferredRegister<Item> DR = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    private static final List<ItemDefinition<?>> ITEMS = new ArrayList<>();

    // Not added to creative tab — blank pattern obtained via terminal mechanics
    public static final ItemDefinition<Item> PROBABILITY_PATTERN = item("Probability Pattern", ItemIds.PROBABILITY_PATTERN, (p) -> new ProbabilityPatternItem(
            p.stacksTo(64)), null);

    // Only initialized when ae2wtlib is present; null otherwise
    @Nullable
    public static final ItemDefinition<ProbabilityPatternTerminalItem> WIRELESS_PROBABILITY_PATTERN_TERMINAL;

    static {
        if (ModList.get().isLoaded("ae2wtlib")) {
            WIRELESS_PROBABILITY_PATTERN_TERMINAL = item("Wireless Probability Pattern Terminal",
                    ItemIds.WIRELESS_PROBABILITY_PATTERN_TERMINAL,
                    ProbabilityPatternTerminalItem::new);
        } else {
            WIRELESS_PROBABILITY_PATTERN_TERMINAL = null;
        }
    }

    private SPItems() {
    }

    static <T extends Item> ItemDefinition<T> item(String name, ResourceLocation id, Function<Item.Properties, T> factory) {
        return item(name, id, factory, SPCreativeTabIds.MAIN);
    }

    /**
     * Lazy ItemDefinition wrapper that works with Forge's DeferredRegister.
     * AE2's ItemDefinition constructor requires the actual Item object, but
     * RegistryObject.get() is not available until registries are populated.
     * We pass Items.AIR as a placeholder and override all methods to resolve
     * the real item lazily from the RegistryObject.
     */
    private static class LazyItemDefinition<T extends Item> extends ItemDefinition<T> {
        private final RegistryObject<T> ro;

        LazyItemDefinition(String name, ResourceLocation id, RegistryObject<T> ro) {
            super(name, id, (T) Items.AIR); // placeholder — never used because all accessors are overridden
            this.ro = ro;
        }

        @Override
        public T m_5456_() {
            return ro.get();
        }

        @Override
        public ItemStack stack() {
            return new ItemStack(ro.get());
        }

        @Override
        public ItemStack stack(int stackSize) {
            return new ItemStack(ro.get(), stackSize);
        }

        @Override
        public GenericStack genericStack(long stackSize) {
            return new GenericStack(AEItemKey.of(ro.get()), stackSize);
        }
    }

    static <T extends Item> ItemDefinition<T> item(String name, ResourceLocation id, Function<Item.Properties, T> factory, @Nullable ResourceKey<CreativeModeTab> group) {
        Item.Properties p = new Item.Properties();
        Preconditions.checkArgument(id.getNamespace().equals(MOD_ID), "Can only registered for AE2");
        RegistryObject<T> ro = DR.register(id.getPath(), () -> factory.apply(p));
        var definition = new LazyItemDefinition<>(name, id, ro);

        if (group != null) {
            SPCreativeTabs.add(definition);
        }
        ITEMS.add(definition);
        return definition;
    }
}
