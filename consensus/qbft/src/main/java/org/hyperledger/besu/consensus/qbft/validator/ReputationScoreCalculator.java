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

public class ReputationScoreCalculator {

  private final ReputationSelectionConfig config;
  private final ValidatorMetricsProvider metricsProvider;
  private final ParticipationBalanceTracker participationBalanceTracker;

  public ReputationScoreCalculator(
      final ReputationSelectionConfig config,
      final ValidatorMetricsProvider metricsProvider,
      final ParticipationBalanceTracker participationBalanceTracker) {
    this.config = config;
    this.metricsProvider = metricsProvider;
    this.participationBalanceTracker = participationBalanceTracker;
  }

  public double calculateScore(final Address validator, final BlockHeader parentHeader) {
    final double uptime = metricsProvider.uptime(validator, parentHeader);
    final double successRate = metricsProvider.successRate(validator, parentHeader);
    final double failureRate = metricsProvider.failureRate(validator, parentHeader);

    final double baseScore =
        config.getAlpha() * uptime
            + config.getBeta() * successRate
            + config.getGamma() * (1.0 - failureRate);

    final double participationBalance =
        participationBalanceTracker.participationBalance(validator, parentHeader);

    return clamp(baseScore * participationBalance);
  }

  private double clamp(final double value) {
    return Math.max(0.0, Math.min(1.0, value));
  }
}
