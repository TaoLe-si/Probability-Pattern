/*
 * Decompiled with CFR 0.152.
 */
package appeng.crafting;

import appeng.api.storage.data.IAEItemStack;

public class CraftBranchFailure
extends RuntimeException {
    private static final long serialVersionUID = 654603652836724823L;
    private final IAEItemStack missing;

    public CraftBranchFailure(IAEItemStack what, long howMany) {
        super("Failed: " + what.getItem().getUnlocalizedName() + " x " + howMany);
        this.missing = what.copy();
        this.missing.setStackSize(howMany);
    }

    public IAEItemStack getMissing() {
        return this.missing;
    }
}

