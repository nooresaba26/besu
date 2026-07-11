/*
 * Copyright contributors to Besu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.consensus.qbft.validator;

import java.util.List;
import java.util.Map;

/**
 * Calculates one network-level alpha, beta, and gamma using fuzzy logic.
 *
 * <p>Low uptime increases alpha.
 *
 * <p>Low success rate increases beta.
 *
 * <p>High failure rate increases gamma.
 */
public class FuzzyWeightCalculator {

  private static final int DEFUZZIFICATION_STEPS = 1000;
  private static final double DEFAULT_WEIGHT = 1.0 / 3.0;

  private final NetworkPercentileCalculator percentileCalculator;

  public FuzzyWeightCalculator() {
    this.percentileCalculator = new NetworkPercentileCalculator();
  }

  public FuzzyWeights calculate(final NetworkMetricsSnapshot snapshot) {
    validateSnapshot(snapshot);

    final List<Double> uptimes = snapshot.uptimes();
    final List<Double> successRates = snapshot.successRates();
    final List<Double> failureRates = snapshot.failureRates();

    final double averageUptime = average(uptimes);
    final double averageSuccess = average(successRates);
    final double averageFailure = average(failureRates);

    final double uptimeP25 = percentileCalculator.percentile(uptimes, 25.0);
    final double uptimeP50 = percentileCalculator.percentile(uptimes, 50.0);
    final double uptimeP75 = percentileCalculator.percentile(uptimes, 75.0);

    final double successP25 = percentileCalculator.percentile(successRates, 25.0);
    final double successP50 = percentileCalculator.percentile(successRates, 50.0);
    final double successP75 = percentileCalculator.percentile(successRates, 75.0);

    final double failureP25 = percentileCalculator.percentile(failureRates, 25.0);
    final double failureP50 = percentileCalculator.percentile(failureRates, 50.0);
    final double failureP75 = percentileCalculator.percentile(failureRates, 75.0);

    final double uptimeLow = trapezoidalMembership(averageUptime, 0.0, 0.0, uptimeP25, uptimeP50);
    final double uptimeMedium =
        triangularMembership(averageUptime, uptimeP25, uptimeP50, uptimeP75);
    final double uptimeHigh = trapezoidalMembership(averageUptime, uptimeP50, uptimeP75, 1.0, 1.0);

    final double successLow =
        trapezoidalMembership(averageSuccess, 0.0, 0.0, successP25, successP50);
    final double successMedium =
        triangularMembership(averageSuccess, successP25, successP50, successP75);
    final double successHigh =
        trapezoidalMembership(averageSuccess, successP50, successP75, 1.0, 1.0);

    final double failureLow =
        trapezoidalMembership(averageFailure, 0.0, 0.0, failureP25, failureP50);
    final double failureMedium =
        triangularMembership(averageFailure, failureP25, failureP50, failureP75);
    final double failureHigh =
        trapezoidalMembership(averageFailure, failureP50, failureP75, 1.0, 1.0);

    final double alphaRaw =
        defuzzify(
            Map.of(
                OutputLabel.HIGH, uptimeLow,
                OutputLabel.MEDIUM, uptimeMedium,
                OutputLabel.LOW, uptimeHigh));

    final double betaRaw =
        defuzzify(
            Map.of(
                OutputLabel.HIGH, successLow,
                OutputLabel.MEDIUM, successMedium,
                OutputLabel.LOW, successHigh));

    final double gammaRaw =
        defuzzify(
            Map.of(
                OutputLabel.LOW,
                failureLow,
                OutputLabel.HIGH,
                Math.max(failureMedium, failureHigh)));

    final double total = alphaRaw + betaRaw + gammaRaw;

    if (!Double.isFinite(total) || total <= 0.0) {
      return new FuzzyWeights(DEFAULT_WEIGHT, DEFAULT_WEIGHT, DEFAULT_WEIGHT);
    }

    return new FuzzyWeights(alphaRaw / total, betaRaw / total, gammaRaw / total);
  }

  private void validateSnapshot(final NetworkMetricsSnapshot snapshot) {
    if (snapshot == null) {
      throw new IllegalArgumentException("Network metrics snapshot cannot be null");
    }

    if (snapshot.uptimes().isEmpty()
        || snapshot.successRates().isEmpty()
        || snapshot.failureRates().isEmpty()) {
      throw new IllegalArgumentException("Network metric lists cannot be empty");
    }
  }

  private double average(final List<Double> values) {
    return values.stream().mapToDouble(this::clamp).average().orElse(0.0);
  }

  private double triangularMembership(
      final double input, final double a, final double b, final double c) {

    final double x = clamp(input);
    final double left = clamp(a);
    final double middle = clamp(b);
    final double right = clamp(c);

    if (x < left || x > right) {
      return 0.0;
    }

    if (x == middle) {
      return 1.0;
    }

    if (middle > left && x < middle) {
      return clamp((x - left) / (middle - left));
    }

    if (right > middle && x > middle) {
      return clamp((right - x) / (right - middle));
    }

    if (middle == left && x <= middle) {
      return 1.0;
    }

    if (right == middle && x >= middle) {
      return 1.0;
    }

    return 0.0;
  }

  private double trapezoidalMembership(
      final double input, final double a, final double b, final double c, final double d) {

    final double x = clamp(input);
    final double pointA = clamp(a);
    final double pointB = clamp(b);
    final double pointC = clamp(c);
    final double pointD = clamp(d);

    if (x < pointA || x > pointD) {
      return 0.0;
    }

    if (x >= pointB && x <= pointC) {
      return 1.0;
    }

    if (x < pointB) {
      if (pointB == pointA) {
        return 1.0;
      }

      return clamp((x - pointA) / (pointB - pointA));
    }

    if (x > pointC) {
      if (pointD == pointC) {
        return 1.0;
      }

      return clamp((pointD - x) / (pointD - pointC));
    }

    return 0.0;
  }

  private double defuzzify(final Map<OutputLabel, Double> activations) {
    double weightedSum = 0.0;
    double membershipSum = 0.0;

    for (int step = 0; step <= DEFUZZIFICATION_STEPS; step++) {
      final double outputValue = (double) step / DEFUZZIFICATION_STEPS;
      double aggregatedMembership = 0.0;

      for (final Map.Entry<OutputLabel, Double> entry : activations.entrySet()) {
        final double activation = clamp(entry.getValue());
        final double outputMembership = outputMembership(entry.getKey(), outputValue);

        aggregatedMembership =
            Math.max(aggregatedMembership, Math.min(activation, outputMembership));
      }

      weightedSum += outputValue * aggregatedMembership;
      membershipSum += aggregatedMembership;
    }

    if (membershipSum == 0.0) {
      return DEFAULT_WEIGHT;
    }

    return weightedSum / membershipSum;
  }

  private double outputMembership(final OutputLabel label, final double value) {

    return switch (label) {
      case LOW -> triangularMembership(value, 0.0, 0.0, 0.5);
      case MEDIUM -> triangularMembership(value, 0.0, 0.5, 1.0);
      case HIGH -> triangularMembership(value, 0.5, 1.0, 1.0);
    };
  }

  private double clamp(final double value) {
    if (!Double.isFinite(value)) {
      return 0.0;
    }

    return Math.max(0.0, Math.min(1.0, value));
  }

  private enum OutputLabel {
    LOW,
    MEDIUM,
    HIGH
  }
}
