/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.gtnewhorizon.gtnhlib.util.parsing.MathExpressionParser
 *  com.gtnewhorizon.gtnhlib.util.parsing.MathExpressionParser$Context
 */
package appeng.util.calculators;

import com.gtnewhorizon.gtnhlib.util.parsing.MathExpressionParser;

public class Calculator {
    private static final MathExpressionParser.Context ctx = new MathExpressionParser.Context().setEmptyValue(0.0).setErrorValue(Double.NaN);

    public static double conversion(String expression) {
        double result = MathExpressionParser.parse((String)expression, (MathExpressionParser.Context)ctx);
        if (ctx.wasSuccessful()) {
            return result;
        }
        return Double.NaN;
    }
}

