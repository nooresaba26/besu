package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.BlockHeader;

public class ParticipationBalanceTracker {

  private final ReputationSelectionConfig config;

  public ParticipationBalanceTracker(final ReputationSelectionConfig config) {
    this.config = config;
  }

  public double participationBalance(final Address validator, final BlockHeader parentHeader) {
    final int consecutiveParticipationCount = 0;
    return Math.pow(config.getDelta(), consecutiveParticipationCount);
  }
}
