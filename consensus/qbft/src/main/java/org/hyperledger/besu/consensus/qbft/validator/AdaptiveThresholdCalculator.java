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

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.BlockHeader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AdaptiveThresholdCalculator {

  private final ValidatorMetricsProvider metricsProvider;
  private final NetworkPercentileCalculator percentileCalculator;

  public AdaptiveThresholdCalculator(final ValidatorMetricsProvider metricsProvider) {
    this.metricsProvider = metricsProvider;
    this.percentileCalculator = new NetworkPercentileCalculator();
  }

  public NetworkMetricsSnapshot snapshot(
      final Collection<Address> validators, final BlockHeader parentHeader) {

    final List<Double> uptimes = new ArrayList<>();
    final List<Double> successRates = new ArrayList<>();
    final List<Double> failureRates = new ArrayList<>();

    for (final Address validator : validators) {
      uptimes.add(metricsProvider.uptime(validator, parentHeader));
      successRates.add(metricsProvider.successRate(validator, parentHeader));
      failureRates.add(metricsProvider.failureRate(validator, parentHeader));
    }

    return new NetworkMetricsSnapshot(uptimes, successRates, failureRates);
  }

  public AdaptiveThresholds calculate(final NetworkMetricsSnapshot snapshot) {
    final double uptimeThreshold = percentileCalculator.percentile(snapshot.uptimes(), 25.0);

    final double successThreshold = percentileCalculator.percentile(snapshot.successRates(), 25.0);

    final double failureThreshold = percentileCalculator.percentile(snapshot.failureRates(), 75.0);

    return new AdaptiveThresholds(uptimeThreshold, successThreshold, failureThreshold);
  }
}
