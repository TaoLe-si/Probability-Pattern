/*
 * Decompiled with CFR 0.152.
 */
package appeng.util;

import java.text.NumberFormat;

public class RoundHelper {
    public static String toRoundedFormattedForm(float number, int n) {
        double precision;
        double roundedNumber = (double)Math.round((double)number * Math.pow(10.0, n)) / Math.pow(10.0, n);
        if (roundedNumber < (precision = 1.0 / Math.pow(10.0, n))) {
            NumberFormat f = NumberFormat.getInstance();
            f.setMaximumFractionDigits(n);
            return "<" + f.format(precision);
        }
        int intNumber = (int)roundedNumber;
        if ((double)intNumber == roundedNumber) {
            return NumberFormat.getInstance().format(intNumber);
        }
        NumberFormat f = NumberFormat.getInstance();
        f.setMaximumFractionDigits(n);
        return f.format(roundedNumber);
    }
}

