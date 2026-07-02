package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.BlockHeader;

import java.math.BigInteger;

public class ParticipationBalanceTracker {

  private final ReputationSelectionConfig config;
  private final ValidatorContractController contractController;
  private final Address contractAddress;

  public ParticipationBalanceTracker(
      final ReputationSelectionConfig config,
      final ValidatorContractController contractController,
      final Address contractAddress) {
    this.config = config;
    this.contractController = contractController;
    this.contractAddress = contractAddress;
  }

  public double participationBalance(final Address validator, final BlockHeader parentHeader) {
    try {
      final ValidatorContractController.ValidatorStats stats =
          contractController.getValidatorStats(
              parentHeader.getNumber(),
              contractAddress,
              validator);

      final BigInteger count = stats.consecutiveParticipation();

      return Math.pow(config.getDelta(), count.doubleValue());
    } catch (final RuntimeException e) {
      return 1.0;
    }
  }
}