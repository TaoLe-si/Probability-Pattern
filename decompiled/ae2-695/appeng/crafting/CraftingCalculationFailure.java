/*
 * Decompiled with CFR 0.152.
 */
package appeng.crafting;

import appeng.api.storage.data.IAEItemStack;

public class CraftingCalculationFailure
extends RuntimeException {
    private static final long serialVersionUID = 654603652836724823L;
    private final IAEItemStack missing;

    public CraftingCalculationFailure(IAEItemStack what, long howMany) {
        super("this should have been caught!");
        this.missing = what.copy();
        this.missing.setStackSize(howMany);
    }
}

