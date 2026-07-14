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
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.BlockHeader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReputationScoreCalculator implements ValidatorMetricsProvider {
  private static final Logger LOG = LoggerFactory.getLogger(ReputationScoreCalculator.class);
  private final ReputationSelectionConfig config;
  private final ValidatorMetricsProvider metricsProvider;
  private final ParticipationBalanceTracker participationBalanceTracker;
  private final Blockchain blockchain;
  private final ReputationScoreHistoryStore reputationScoreHistoryStore;

  public ReputationScoreCalculator(
      final ReputationSelectionConfig config,
      final ValidatorMetricsProvider metricsProvider,
      final ParticipationBalanceTracker participationBalanceTracker,
      final Blockchain blockchain,
      final ReputationScoreHistoryStore reputationScoreHistoryStore) {

    this.config = config;
    this.metricsProvider = metricsProvider;
    this.participationBalanceTracker = participationBalanceTracker;
    this.blockchain = blockchain;
    this.reputationScoreHistoryStore = reputationScoreHistoryStore;
  }

  public void storeCurrentBaseScores(
      final Iterable<Address> validators,
      final BlockHeader parentHeader,
      final FuzzyWeights fuzzyWeights) {

    final long scoreBlockNumber = parentHeader.getNumber();

    for (final Address validator : validators) {
      if (reputationScoreHistoryStore.get(validator, scoreBlockNumber).isPresent()) {
        LOG.debug(
            "Base reputation already stored: validator={} block={}", validator, scoreBlockNumber);
        continue;
      }

      final double baseScore = calculateBaseScore(validator, parentHeader, fuzzyWeights);

      final long scaledScore = ReputationScoreScale.fromDouble(baseScore);

      reputationScoreHistoryStore.save(validator, scoreBlockNumber, scaledScore);
      LOG.info(
          "Stored base reputation: validator={} block={} score={} scaledScore={}",
          validator,
          scoreBlockNumber,
          baseScore,
          scaledScore);
    }
  }

  public double calculateScore(final Address validator, final BlockHeader parentHeader) {
    final double timeDecayedScore = calculateTimeDecayedScore(validator, parentHeader);

    final double participationBalance =
        participationBalanceTracker.participationBalance(validator, parentHeader);

    return clamp(timeDecayedScore * participationBalance);
  }

  public boolean passesThresholds(
      final Address validator,
      final BlockHeader parentHeader,
      final AdaptiveThresholds thresholds) {

    final double uptime = metricsProvider.uptime(validator, parentHeader);
    final double successRate = metricsProvider.successRate(validator, parentHeader);
    final double failureRate = metricsProvider.failureRate(validator, parentHeader);

    return uptime >= thresholds.uptimeThreshold()
        && successRate >= thresholds.successThreshold()
        && failureRate <= thresholds.failureThreshold();
  }

  private double calculateTimeDecayedScore(
      final Address validator, final BlockHeader parentHeader) {
    double weightedScoreSum = 0.0;
    double weightSum = 0.0;

    for (int d = 0; d <= config.getTimeDecayWindow(); d++) {
      final long blockNumber = parentHeader.getNumber() - d;

      if (blockNumber < 0) {
        break;
      }

      final BlockHeader historicalHeader =
          blockchain.getBlockHeader(blockNumber).orElse(parentHeader);

      final double weight = Math.pow(config.getLambda(), d);
      final double baseScore = calculateBaseScore(validator, historicalHeader);

      weightedScoreSum += weight * baseScore;
      weightSum += weight;
    }

    if (weightSum == 0.0) {
      return 0.0;
    }

    return weightedScoreSum / weightSum;
  }

  private double calculateBaseScore(final Address validator, final BlockHeader parentHeader) {
    final double uptime = metricsProvider.uptime(validator, parentHeader);
    final double successRate = metricsProvider.successRate(validator, parentHeader);
    final double failureRate = metricsProvider.failureRate(validator, parentHeader);

    return clamp(
        config.getAlpha() * uptime
            + config.getBeta() * successRate
            + config.getGamma() * (1.0 - failureRate));
  }

  private double calculateBaseScore(
      final Address validator, final BlockHeader parentHeader, final FuzzyWeights fuzzyWeights) {

    final double uptime = metricsProvider.uptime(validator, parentHeader);

    final double successRate = metricsProvider.successRate(validator, parentHeader);

    final double failureRate = metricsProvider.failureRate(validator, parentHeader);

    return clamp(
        fuzzyWeights.alpha() * uptime
            + fuzzyWeights.beta() * successRate
            + fuzzyWeights.gamma() * (1.0 - failureRate));
  }

  private double clamp(final double value) {
    return Math.max(0.0, Math.min(1.0, value));
  }

  @Override
  public double uptime(final Address validator, final BlockHeader parentHeader) {
    return metricsProvider.uptime(validator, parentHeader);
  }

  @Override
  public double successRate(final Address validator, final BlockHeader parentHeader) {
    return metricsProvider.successRate(validator, parentHeader);
  }

  @Override
  public double failureRate(final Address validator, final BlockHeader parentHeader) {
    return metricsProvider.failureRate(validator, parentHeader);
  }

  ReputationScoreHistoryStore getReputationScoreHistoryStore() {
    return reputationScoreHistoryStore;
  }
}
