/*
 * Decompiled with CFR 0.152.
 */
package appeng.util;

import java.util.regex.Pattern;

public final class UUIDMatcher {
    private static final String UUID_REGEX = "[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}";
    private static final Pattern PATTERN = Pattern.compile("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}");

    public boolean isUUID(CharSequence potential) {
        return PATTERN.matcher(potential).matches();
    }
}

