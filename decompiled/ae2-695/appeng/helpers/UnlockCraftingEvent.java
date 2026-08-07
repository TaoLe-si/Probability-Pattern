/*
 * Decompiled with CFR 0.152.
 */
package appeng.helpers;

import appeng.api.config.LockCraftingMode;

public enum UnlockCraftingEvent {
    PULSE(LockCraftingMode.LOCK_UNTIL_PULSE),
    RESULT(LockCraftingMode.LOCK_UNTIL_RESULT);

    private final LockCraftingMode correspondingMode;

    private UnlockCraftingEvent(LockCraftingMode mode) {
        this.correspondingMode = mode;
    }

    public boolean matches(LockCraftingMode mode) {
        return this.correspondingMode == mode;
    }
}

