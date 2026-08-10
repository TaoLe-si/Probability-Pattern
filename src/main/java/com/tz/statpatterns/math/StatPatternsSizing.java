
/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tz.statpatterns.math;

public final class StatPatternsSizing {
    private StatPatternsSizing() {
    }

    public static StatPatternsSizingResult planAttempts(long targetSuccesses, double successProbability,
            double alpha, int smallSampleLimit) {
        validate(targetSuccesses, successProbability, alpha);

        if (successProbability == 1.0) {
            return new StatPatternsSizingResult(targetSuccesses, targetSuccesses);
        }

        if (targetSuccesses <= smallSampleLimit) {
            return exactBinomialPlan(targetSuccesses, successProbability, alpha);
        }

        return normalApproximationPlan(targetSuccesses, successProbability, alpha);
    }

    private static StatPatternsSizingResult exactBinomialPlan(long targetSuccesses, double p, double alpha) {
        var attempts = Math.max(targetSuccesses, (long) Math.ceil(targetSuccesses / p));
        while (binomialLowerTail(attempts, p, targetSuccesses - 1) > alpha) {
            attempts++;
        }
        return new StatPatternsSizingResult(targetSuccesses, attempts);
    }

    private static StatPatternsSizingResult normalApproximationPlan(long targetSuccesses, double p, double alpha) {
        var z = inverseStandardNormal(1.0 - alpha);
        var attempts = Math.max(targetSuccesses, (long) Math.ceil(targetSuccesses / p));
        while (normalZ(targetSuccesses, attempts, p) < z) {
            attempts++;
        }
        return new StatPatternsSizingResult(targetSuccesses, attempts);
    }

    private static double binomialLowerTail(long attempts, double p, long maxSuccesses) {
        if (maxSuccesses < 0) {
            return 0.0;
        }
        if (maxSuccesses >= attempts) {
            return 1.0;
        }
        if (p == 1.0) {
            return maxSuccesses >= attempts ? 1.0 : 0.0;
        }

        var q = 1.0 - p;
        var probability = Math.pow(q, attempts);
        var sum = probability;
        for (long successes = 1; successes <= maxSuccesses; successes++) {
            probability *= ((attempts - successes + 1.0) / successes) * (p / q);
            sum += probability;
        }
        return Math.min(1.0, sum);
    }

    private static double normalZ(long targetSuccesses, long attempts, double p) {
        var mean = attempts * p;
        var variance = attempts * p * (1.0 - p);
        return (mean - targetSuccesses) / Math.sqrt(variance);
    }

    private static double inverseStandardNormal(double probability) {
        if (!(probability > 0.0 && probability < 1.0)) {
            throw new IllegalArgumentException("Probability must be in (0, 1).");
        }

        var a = new double[] {
                -3.969683028665376e+01, 2.209460984245205e+02,
                -2.759285104469687e+02, 1.383577518672690e+02,
                -3.066479806614716e+01, 2.506628277459239e+00
        };
        var b = new double[] {
                -5.447609879822406e+01, 1.615858368580409e+02,
                -1.556989798598866e+02, 6.680131188771972e+01,
                -1.328068155288572e+01
        };
        var c = new double[] {
                -7.784894002430293e-03, -3.223964580411365e-01,
                -2.400758277161838e+00, -2.549732539343734e+00,
                4.374664141464968e+00, 2.938163982698783e+00
        };
        var d = new double[] {
                7.784695709041462e-03, 3.224671290700398e-01,
                2.445134137142996e+00, 3.754408661907416e+00
        };

        var low = 0.02425;
        var high = 1.0 - low;
        double q;
        double r;

        if (probability < low) {
            q = Math.sqrt(-2.0 * Math.log(probability));
            return (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5])
                    / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0);
        }
        if (probability > high) {
            q = Math.sqrt(-2.0 * Math.log(1.0 - probability));
            return -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5])
                    / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0);
        }

        q = probability - 0.5;
        r = q * q;
        return (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q
                / (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1.0);
    }

    private static void validate(long targetSuccesses, double successProbability, double alpha) {
        if (targetSuccesses <= 0) {
            throw new IllegalArgumentException("Target successes must be positive.");
        }
        if (!(successProbability > 0.0 && successProbability <= 1.0)) {
            throw new IllegalArgumentException("Success probability must be in (0, 1].");
        }
        if (!(alpha > 0.0 && alpha < 1.0)) {
            throw new IllegalArgumentException("Alpha must be in (0, 1).");
        }
    }
}
