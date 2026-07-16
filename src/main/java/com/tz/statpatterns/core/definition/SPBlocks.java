package com.tz.statpatterns.core.definition;

import appeng.block.AEBaseBlock;
import appeng.block.AEBaseBlockItem;
import appeng.core.definitions.BlockDefinition;
import appeng.core.definitions.ItemDefinition;
import com.google.common.base.Preconditions;
import com.tz.statpatterns.SPCreativeTabs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;

import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import static com.tz.statpatterns.ProbabilityPatternMod.MOD_ID;
import static com.tz.statpatterns.api.ids.BlockIds.PROBABILITY_PATTERN_PROVIDER;

public final class SPBlocks {
    public static final DeferredRegister.Blocks DR = DeferredRegister.createBlocks(MOD_ID);

    private SPBlocks() {
        // Prevent instantiation
    }

    private static <T extends Block> BlockDefinition<T> block(String englishName, ResourceLocation id,
                                                              Supplier<T> blockSupplier) {
        return block(englishName, id, blockSupplier, null);
    }

    private static <T extends Block> BlockDefinition<T> block(
            String englishName,
            ResourceLocation id,
            Supplier<T> blockSupplier,
            @Nullable BiFunction<Block, Item.Properties, BlockItem> itemFactory) {
        Preconditions.checkArgument(id.getNamespace().equals(MOD_ID));

        // Create block and matching item
        var deferredBlock = DR.register(id.getPath(), blockSupplier);
        var deferredItem = SPItems.DR.register(id.getPath(), () -> {
            var block = deferredBlock.get();
            var itemProperties = new Item.Properties();
            if (itemFactory != null) {
                var item = itemFactory.apply(block, itemProperties);
                if (item == null) {
                    throw new IllegalArgumentException("BlockItem factory for " + id + " returned null");
                }
                return item;
            } else if (block instanceof AEBaseBlock) {
                return new AEBaseBlockItem(block, itemProperties);
            } else {
                return new BlockItem(block, itemProperties);
            }
        });

        var itemDef = new ItemDefinition<>(englishName, deferredItem);
        SPCreativeTabs.add(itemDef);
        BlockDefinition<T> definition = new BlockDefinition<>(englishName, deferredBlock, itemDef);

        //BLOCKS.add(definition);

        return definition;

    }
}
