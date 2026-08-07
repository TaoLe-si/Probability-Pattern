/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 */
package appeng.crafting.v2.resolvers;

import appeng.api.storage.data.IAEStack;
import appeng.crafting.v2.CraftingContext;
import appeng.crafting.v2.CraftingRequest;
import appeng.crafting.v2.resolvers.CraftingTask;
import java.util.List;
import javax.annotation.Nonnull;

@FunctionalInterface
public interface CraftingRequestResolver<StackType extends IAEStack<StackType>> {
    @Nonnull
    public List<CraftingTask> provideCraftingRequestResolvers(@Nonnull CraftingRequest<StackType> var1, @Nonnull CraftingContext var2);
}

