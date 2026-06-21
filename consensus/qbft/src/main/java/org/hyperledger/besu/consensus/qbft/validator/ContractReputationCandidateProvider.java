package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.BlockHeader;

import java.util.Collection;
import java.util.List;

public class ContractReputationCandidateProvider implements ReputationCandidateProvider {

  private final ValidatorContractController validatorContractController;
  private final Address contractAddress;
  private final ReputationCandidateProvider fallback;

  public ContractReputationCandidateProvider(
      final ValidatorContractController validatorContractController,
      final Address contractAddress,
      final ReputationCandidateProvider fallback) {
    this.validatorContractController = validatorContractController;
    this.contractAddress = contractAddress;
    this.fallback = fallback;
  }

  @Override
  public Collection<Address> getCandidatesAfterBlock(final BlockHeader parentHeader) {
    try {
      final Collection<Address> activeValidators =
          validatorContractController.getValidators(parentHeader.getNumber(), contractAddress);

      if (activeValidators.isEmpty()) {
        return fallback.getCandidatesAfterBlock(parentHeader);
      }

      return activeValidators.stream().sorted().toList();
    } catch (final RuntimeException e) {
      return fallback.getCandidatesAfterBlock(parentHeader);
    }
  }
}