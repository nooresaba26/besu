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

public class ReputationContractUpdateService {

  private static final Logger LOG = LoggerFactory.getLogger(ReputationContractUpdateService.class);

  private final Blockchain blockchain;
  private final ValidatorProvider validatorProvider;
  private final Address localAddress;
  private final Address contractAddress;
  private final ReputationContractTransactionSender transactionSender;

  public ReputationContractUpdateService(
      final Blockchain blockchain,
      final ValidatorProvider validatorProvider,
      final Address localAddress,
      final Address contractAddress,
      final ReputationContractTransactionSender transactionSender) {
    this.blockchain = blockchain;
    this.validatorProvider = validatorProvider;
    this.localAddress = localAddress;
    this.contractAddress = contractAddress;
    this.transactionSender = transactionSender;
  }

  public void onFinalizedBlock(final BlockHeader blockHeader) {
    

    if (blockHeader.getNumber() % 10 != 0) {
      return;
    }

    final Address proposer = proposerForBlock(blockHeader);

    LOG.info(
        "Block {} proposer={}, local={}",
        blockHeader.getNumber(),
        proposer,
        localAddress);

    if (!localAddress.equals(proposer)) {
      return;
    }

    final Collection<Address> validators = validatorProvider.getValidatorsAfterBlock(blockHeader);

    LOG.info(
        "Local node {} is proposer for finalized block {}. Preparing reputation update to contract {} for {} validators.",
        localAddress,
        blockHeader.getNumber(),
        contractAddress,
        validators.size());

    transactionSender.prepareRecordFinalizedBlockTransaction(
        contractAddress,
        blockHeader.getNumber(),
        validators,
        validators,
        validators,
        List.of());
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