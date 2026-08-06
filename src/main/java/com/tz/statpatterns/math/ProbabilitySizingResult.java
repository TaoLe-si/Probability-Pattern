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

/**
 * Result of a probability sizing calculation.
 * (1.7.10 port of the {@code record ProbabilitySizingResult} from the 1.21.1 version.)
 */
public final class ProbabilitySizingResult
{
	private final long targetSuccesses;
	private final long attempts;
	private final double successProbability;
	private final double alpha;
	private final DistributionMode distribution;
	private final double underproductionRisk;

	public ProbabilitySizingResult( final long targetSuccesses, final long attempts, final double successProbability, final double alpha, final DistributionMode distribution, final double underproductionRisk )
	{
		this.targetSuccesses = targetSuccesses;
		this.attempts = attempts;
		this.successProbability = successProbability;
		this.alpha = alpha;
		this.distribution = distribution;
		this.underproductionRisk = underproductionRisk;
	}

	public long targetSuccesses()
	{
		return this.targetSuccesses;
	}

	public long attempts()
	{
		return this.attempts;
	}

	public double successProbability()
	{
		return this.successProbability;
	}

	public double alpha()
	{
		return this.alpha;
	}

	public DistributionMode distribution()
	{
		return this.distribution;
	}

	public double underproductionRisk()
	{
		return this.underproductionRisk;
	}
}
