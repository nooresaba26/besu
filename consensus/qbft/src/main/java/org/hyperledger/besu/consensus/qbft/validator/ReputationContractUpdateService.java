package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.consensus.common.validator.ValidatorProvider;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.BlockHeader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hyperledger.besu.consensus.common.bft.BftBlockInterface;
import org.hyperledger.besu.consensus.qbft.core.statemachine.OnlineValidatorTracker;

public class ReputationContractUpdateService {

  private static final Logger LOG = LoggerFactory.getLogger(ReputationContractUpdateService.class);

  private final Blockchain blockchain;
  private final ValidatorProvider validatorProvider;
  private final Address localAddress;
  private final Address contractAddress;
  private final ReputationContractTransactionSender transactionSender;
  private final BftBlockInterface bftBlockInterface;
  private final OnlineValidatorTracker onlineValidatorTracker;

  public ReputationContractUpdateService(
      final Blockchain blockchain,
      final ValidatorProvider validatorProvider,
      final Address localAddress,
      final Address contractAddress,
      final ReputationContractTransactionSender transactionSender,
      final BftBlockInterface bftBlockInterface,
      final OnlineValidatorTracker onlineValidatorTracker) {
    this.blockchain = blockchain;
    this.validatorProvider = validatorProvider;
    this.localAddress = localAddress;
    this.contractAddress = contractAddress;
    this.transactionSender = transactionSender;
    this.bftBlockInterface = bftBlockInterface;
    this.onlineValidatorTracker = onlineValidatorTracker;
  }

  public void onFinalizedBlock(final BlockHeader blockHeader) {
    

    // if (blockHeader.getNumber() % 10 != 0) {
    //   return;
    // }

    final Address proposer = proposerForBlock(blockHeader);

    LOG.info(
        "Block {} proposer={}, local={}",
        blockHeader.getNumber(),
        proposer,
        localAddress);

    if (!localAddress.equals(proposer)) {
      return;
    }

    final Collection<Address> observedValidators =
    bftBlockInterface.validatorsInBlock(blockHeader);

final Collection<Address> successfulValidators =
    bftBlockInterface.getCommitters(blockHeader);

final Collection<Address> onlineValidators =
    onlineValidatorTracker.getOnlineValidators(blockHeader.getNumber());

final Collection<Address> selectedValidators = observedValidators;

final Collection<Address> failedValidators =
    selectedValidators.stream()
        .filter(validator -> !successfulValidators.contains(validator))
        .toList();

        LOG.info(
    "Block {} online validators from tracker: {}",
    blockHeader.getNumber(),
    onlineValidators);
    LOG.info(
        "Local node {} is proposer for finalized block {}. Preparing reputation update to contract {} for {} validators.",
        localAddress,
        blockHeader.getNumber(),
        contractAddress,
        observedValidators.size());

        LOG.info("Observed validators: {}", observedValidators);
LOG.info("Committers: {}", successfulValidators);
LOG.info("Failed validators: {}", failedValidators);

 transactionSender.prepareRecordFinalizedBlockTransaction(
    contractAddress,
    blockHeader.getNumber(),
    onlineValidators,
    observedValidators,
    successfulValidators,
    failedValidators);
    onlineValidatorTracker.clear(blockHeader.getNumber());
  }

  private Address proposerForBlock(final BlockHeader blockHeader) {
    final long sequenceNumber = blockHeader.getNumber();
    final long parentBlockNumber = sequenceNumber - 1;

    final Collection<Address> validators =
        blockchain
            .getBlockHeader(parentBlockNumber)
            .map(validatorProvider::getValidatorsAfterBlock)
            .orElseGet(() -> validatorProvider.getValidatorsAtHead());

    final List<Address> committee = new ArrayList<>(validators);
    committee.sort(Address::compareTo);

    if (committee.isEmpty()) {
      throw new IllegalStateException("Reputation committee cannot be empty");
    }

    final int index = Math.floorMod((int) sequenceNumber, committee.size());
    return committee.get(index);
  }
}