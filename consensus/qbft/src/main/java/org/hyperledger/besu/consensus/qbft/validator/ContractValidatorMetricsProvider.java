package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.BlockHeader;

public class ContractValidatorMetricsProvider implements ValidatorMetricsProvider {

  private final ValidatorMetricsProvider fallback;

  public ContractValidatorMetricsProvider(final ValidatorMetricsProvider fallback) {
    this.fallback = fallback;
  }

  @Override
  public double uptime(final Address validator, final BlockHeader parentHeader) {
    // TODO: read observedBlocks and onlineBlocks from ReputationManager.getValidatorStats(address)
    return fallback.uptime(validator, parentHeader);
  }

  @Override
  public double successRate(final Address validator, final BlockHeader parentHeader) {
    // TODO: read successfulVotes and participatedRounds from ReputationManager.getValidatorStats(address)
    return fallback.successRate(validator, parentHeader);
  }

  @Override
  public double failureRate(final Address validator, final BlockHeader parentHeader) {
    // TODO: read unsuccessfulVotes and participatedRounds from ReputationManager.getValidatorStats(address)
    return fallback.failureRate(validator, parentHeader);
  }
}