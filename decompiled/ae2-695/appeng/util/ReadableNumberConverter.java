/*
 * Decompiled with CFR 0.152.
 */
package appeng.util;

import appeng.util.ISlimReadableNumberConverter;
import appeng.util.IWideReadableNumberConverter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;

public final class ReadableNumberConverter
extends Enum<ReadableNumberConverter>
implements ISlimReadableNumberConverter,
IWideReadableNumberConverter {
    public static final /* enum */ ReadableNumberConverter INSTANCE = new ReadableNumberConverter();
    private static final int DIVISION_BASE = 1000;
    private static final char[] ENCODED_POSTFIXES;
    private final Format format;
    private static final /* synthetic */ ReadableNumberConverter[] $VALUES;

    public static ReadableNumberConverter[] values() {
        return (ReadableNumberConverter[])$VALUES.clone();
    }

    public static ReadableNumberConverter valueOf(String name) {
        return Enum.valueOf(ReadableNumberConverter.class, name);
    }

    private ReadableNumberConverter() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        symbols.setGroupingSeparator(',');
        DecimalFormat format = new DecimalFormat(".#;0.#");
        format.setDecimalFormatSymbols(symbols);
        format.setRoundingMode(RoundingMode.DOWN);
        this.format = format;
    }

    @Override
    public String toSlimReadableForm(long number) {
        return this.toReadableFormRestrictedByWidth(number, 3);
    }

    private String toReadableFormRestrictedByWidth(long number, int width) {
        String slimResult;
        assert (number >= 0L);
        String numberString = Long.toString(number);
        int numberSize = numberString.length();
        if (numberSize <= width) {
            return numberString;
        }
        long base = number;
        double last = base * 1000L;
        int exponent = -1;
        String postFix = "";
        while (numberSize > width) {
            last = base;
            numberSize = Long.toString(base /= 1000L).length() + 1;
            postFix = String.valueOf(ENCODED_POSTFIXES[++exponent]);
        }
        String withPrecision = this.format.format(last / 1000.0) + postFix;
        String withoutPrecision = Long.toString(base) + postFix;
        String string = slimResult = withPrecision.length() <= width ? withPrecision : withoutPrecision;
        assert (slimResult.length() <= width);
        return slimResult;
    }

    @Override
    public String toWideReadableForm(long number) {
        return this.toReadableFormRestrictedByWidth(number, 4);
    }

    private static /* synthetic */ ReadableNumberConverter[] $values() {
        return new ReadableNumberConverter[]{INSTANCE};
    }

    static {
        $VALUES = ReadableNumberConverter.$values();
        ENCODED_POSTFIXES = "kMGTPE".toCharArray();
    }
}

