/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 zincglux
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.zincglux.statpatterns.math;

/**
 * Core probability sizing algorithm (binomial / normal-approximation statistics).
 * <p>
 * Spec v2.0 ch.4: models a probabilistic machine as a binomial process — each attempt
 * succeeds with probability {@code p}; running the machine {@code n} times yields
 * {@code X ~ Binomial(n, p)} successes. Finds the smallest {@code n} such that
 * {@code P(X >= k) >= 1 - alpha} where {@code k} is the required number of successes.
 * <p>
 * - Small samples (k &lt;= {@code smallSampleLimit}, default 30): exact binomial lower
 * tail, summed term by term with the recurrence {@code P(X=i) = P(X=i-1) * (n-i+1)/i * p/(1-p)}.
 * - Large samples: normal approximation {@code X ~ N(mu, sigma^2)}, one-tailed z-test.
 */
public final class ProbabilitySizing {

    private ProbabilitySizing() {}

    /**
     * Compute the smallest number of attempts {@code n} so that the probability of
     * producing at least {@code targetSuccesses} successes is {@code >= 1 - alpha}.
     *
     * @param targetSuccesses    required number of successes k (&gt; 0)
     * @param successProbability single-attempt success probability p in (0, 1]
     * @param alpha              significance level in (0, 1)
     * @param smallSampleLimit   max k for the exact binomial algorithm
     * @return the sizing result (never null)
     */
    public static ProbabilitySizingResult planAttempts(final long targetSuccesses, final double successProbability,
        final double alpha, final int smallSampleLimit) {
        validate(targetSuccesses, successProbability, alpha);

        // Deterministic machine: every attempt succeeds, so n = k (Spec v2.0 4.7).
        if (successProbability == 1.0) {
            return new ProbabilitySizingResult(
                targetSuccesses,
                targetSuccesses,
                successProbability,
                alpha,
                DistributionMode.BINOMIAL,
                0.0);
        }

        if (targetSuccesses <= smallSampleLimit) {
            return exactBinomialPlan(targetSuccesses, successProbability, alpha);
        }

        return normalApproximationPlan(targetSuccesses, successProbability, alpha);
    }

    /**
     * Exact binomial plan: start from n = ceil(k/p) and increment until the lower tail
     * {@code P(X < k) = P(X <= k-1)} drops at or below alpha.
     */
    private static ProbabilitySizingResult exactBinomialPlan(final long targetSuccesses, final double p,
        final double alpha) {
        long attempts = Math.max(targetSuccesses, (long) Math.ceil(targetSuccesses / p));
        while (binomialLowerTail(attempts, p, targetSuccesses - 1) > alpha) {
            attempts++;
        }
        return new ProbabilitySizingResult(
            targetSuccesses,
            attempts,
            p,
            alpha,
            DistributionMode.BINOMIAL,
            binomialLowerTail(attempts, p, targetSuccesses - 1));
    }

    /**
     * Normal-approximation plan: find the smallest n with z = (mu - k)/sigma &gt;= z_{1-alpha}.
     */
    private static ProbabilitySizingResult normalApproximationPlan(final long targetSuccesses, final double p,
        final double alpha) {
        final double z = inverseStandardNormal(1.0 - alpha);
        long attempts = Math.max(targetSuccesses, (long) Math.ceil(targetSuccesses / p));
        while (normalZ(targetSuccesses, attempts, p) < z) {
            attempts++;
        }
        return new ProbabilitySizingResult(
            targetSuccesses,
            attempts,
            p,
            alpha,
            DistributionMode.NORMAL_APPROXIMATION,
            normalUnderproductionRisk(targetSuccesses, attempts, p));
    }

    /**
     * Exact lower tail P(X &lt;= maxSuccesses) for Binomial(attempts, p), computed with the
     * recurrence P(X=0) = (1-p)^n, P(X=i) = P(X=i-1) * (n-i+1)/i * p/(1-p).
     */
    private static double binomialLowerTail(final long attempts, final double p, final long maxSuccesses) {
        if (maxSuccesses < 0) {
            return 0.0;
        }
        if (maxSuccesses >= attempts) {
            return 1.0;
        }
        if (p == 1.0) {
            return maxSuccesses >= attempts ? 1.0 : 0.0;
        }

        final double q = 1.0 - p;
        double probability = Math.pow(q, attempts);
        double sum = probability;
        for (long successes = 1; successes <= maxSuccesses; successes++) {
            probability *= ((attempts - successes + 1.0) / successes) * (p / q);
            sum += probability;
        }
        return Math.min(1.0, sum);
    }

    /** P(X &lt; k) under the normal approximation = Phi(-z). */
    private static double normalUnderproductionRisk(final long targetSuccesses, final long attempts, final double p) {
        return normalCdf(-normalZ(targetSuccesses, attempts, p));
    }

    /** z-score of k successes after {@code attempts} trials: (mu - k) / sigma. */
    private static double normalZ(final long targetSuccesses, final long attempts, final double p) {
        final double mean = attempts * p;
        final double variance = attempts * p * (1.0 - p);
        return (mean - targetSuccesses) / Math.sqrt(variance);
    }

    /** Standard normal CDF Phi(x) = 0.5 * (1 + erf(x / sqrt(2))). */
    private static double normalCdf(final double x) {
        return 0.5 * (1.0 + erf(x / Math.sqrt(2.0)));
    }

    /**
     * Error function erf(x) via the Abramowitz &amp; Stegun 7.1.26 approximation
     * (maximum error 1.5e-7), as required by Spec v2.0 4.4.
     */
    private static double erf(final double x) {
        final double sign = Math.signum(x);
        final double ax = Math.abs(x);
        final double t = 1.0 / (1.0 + 0.3275911 * ax);
        final double y = 1.0
            - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t
                * Math.exp(-ax * ax);
        return sign * y;
    }

    /**
     * Inverse standard normal (quantile function) via the Peter Acklam algorithm with
     * rational minimax approximations (Spec v2.0 4.5). Input must lie in (0, 1).
     */
    private static double inverseStandardNormal(final double probability) {
        if (!(probability > 0.0 && probability < 1.0)) {
            throw new IllegalArgumentException("Probability must be in (0, 1).");
        }

        final double[] a = new double[] { -3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02,
            1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00 };
        final double[] b = new double[] { -5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02,
            6.680131188771972e+01, -1.328068155288572e+01 };
        final double[] c = new double[] { -7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00,
            -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00 };
        final double[] d = new double[] { 7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00,
            3.754408661907416e+00 };

        final double low = 0.02425;
        final double high = 1.0 - low;
        final double q;
        final double r;

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

    /**
     * Parameter validation (Spec v2.0 4.7): targetSuccesses &gt; 0, 0 &lt; p &le; 1, 0 &lt; alpha &lt; 1.
     */
    private static void validate(final long targetSuccesses, final double successProbability, final double alpha) {
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
