package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.BlockHeader;

import java.math.BigInteger;

public class ContractValidatorMetricsProvider implements ValidatorMetricsProvider {

  private final ValidatorContractController contractController;
  private final Address contractAddress;
  private final ValidatorMetricsProvider fallback;

  public ContractValidatorMetricsProvider(
      final ValidatorContractController contractController,
      final Address contractAddress,
      final ValidatorMetricsProvider fallback) {
    this.contractController = contractController;
    this.contractAddress = contractAddress;
    this.fallback = fallback;
  }

  @Override
  public double uptime(final Address validator, final BlockHeader parentHeader) {
    try {
      final ValidatorContractController.ValidatorStats stats = stats(validator, parentHeader);

      if (stats.observedBlocks().equals(BigInteger.ZERO)) {
        return fallback.uptime(validator, parentHeader);
      }

      return stats.onlineBlocks().doubleValue() / stats.observedBlocks().doubleValue();
    } catch (final RuntimeException e) {
      return fallback.uptime(validator, parentHeader);
    }
  }

  @Override
  public double successRate(final Address validator, final BlockHeader parentHeader) {
    try {
      final ValidatorContractController.ValidatorStats stats = stats(validator, parentHeader);

      if (stats.participatedRounds().equals(BigInteger.ZERO)) {
        return fallback.successRate(validator, parentHeader);
      }

      return stats.successfulVotes().doubleValue() / stats.participatedRounds().doubleValue();
    } catch (final RuntimeException e) {
      return fallback.successRate(validator, parentHeader);
    }
  }

  @Override
  public double failureRate(final Address validator, final BlockHeader parentHeader) {
    try {
      final ValidatorContractController.ValidatorStats stats = stats(validator, parentHeader);

      if (stats.participatedRounds().equals(BigInteger.ZERO)) {
        return fallback.failureRate(validator, parentHeader);
      }

      return stats.unsuccessfulVotes().doubleValue() / stats.participatedRounds().doubleValue();
    } catch (final RuntimeException e) {
      return fallback.failureRate(validator, parentHeader);
    }
  }

  private ValidatorContractController.ValidatorStats stats(
      final Address validator, final BlockHeader parentHeader) {
    return contractController.getValidatorStats(
        parentHeader.getNumber(),
        contractAddress,
        validator);
  }
}