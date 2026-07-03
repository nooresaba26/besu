package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.BlockHeader;

public class ReputationScoreCalculator {

  private final ReputationSelectionConfig config;
  private final ValidatorMetricsProvider metricsProvider;
  private final ParticipationBalanceTracker participationBalanceTracker;
  private final Blockchain blockchain;

  public ReputationScoreCalculator(
      final ReputationSelectionConfig config,
      final ValidatorMetricsProvider metricsProvider,
      final ParticipationBalanceTracker participationBalanceTracker,
      final Blockchain blockchain) {
    this.config = config;
    this.metricsProvider = metricsProvider;
    this.participationBalanceTracker = participationBalanceTracker;
    this.blockchain = blockchain;
  }

  public double calculateScore(final Address validator, final BlockHeader parentHeader) {
    final double timeDecayedScore = calculateTimeDecayedScore(validator, parentHeader);

    final double participationBalance =
        participationBalanceTracker.participationBalance(validator, parentHeader);

    return clamp(timeDecayedScore * participationBalance);
  }

  public boolean passesThresholds(final Address validator, final BlockHeader parentHeader) {
  final double uptime = metricsProvider.uptime(validator, parentHeader);
  final double successRate = metricsProvider.successRate(validator, parentHeader);
  final double failureRate = metricsProvider.failureRate(validator, parentHeader);

  return uptime >= config.getUptimeThreshold()
      && successRate >= config.getSuccessThreshold()
      && failureRate <= config.getFailureThreshold();
}

  private double calculateTimeDecayedScore(final Address validator, final BlockHeader parentHeader) {
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

  private double clamp(final double value) {
    return Math.max(0.0, Math.min(1.0, value));
  }
  public double uptime(final Address validator, final BlockHeader parentHeader) {
  return metricsProvider.uptime(validator, parentHeader);
}

public double successRate(final Address validator, final BlockHeader parentHeader) {
  return metricsProvider.successRate(validator, parentHeader);
}

public double failureRate(final Address validator, final BlockHeader parentHeader) {
  return metricsProvider.failureRate(validator, parentHeader);
}
}