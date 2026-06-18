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