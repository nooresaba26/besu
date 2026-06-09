package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.BlockHeader;

public class ReputationScoreCalculator {

  private final ReputationSelectionConfig config;
  private final ParticipationBalanceTracker participationBalanceTracker;

  public ReputationScoreCalculator(
      final ReputationSelectionConfig config,
      final ParticipationBalanceTracker participationBalanceTracker) {
    this.config = config;
    this.participationBalanceTracker = participationBalanceTracker;
  }

  public double calculateScore(final Address validator, final BlockHeader parentHeader) {
    final double uptime = 1.0;
    final double successRate = 1.0;
    final double failureRate = 0.0;

    final double baseScore =
        config.getAlpha() * uptime
            + config.getBeta() * successRate
            + config.getGamma() * (1.0 - failureRate);

    final double timeDecayedScore = baseScore;

    final double participationBalance =
        participationBalanceTracker.participationBalance(validator, parentHeader);

    return clamp(timeDecayedScore * participationBalance);
  }

  private double clamp(final double value) {
    return Math.max(0.0, Math.min(1.0, value));
  }
}
