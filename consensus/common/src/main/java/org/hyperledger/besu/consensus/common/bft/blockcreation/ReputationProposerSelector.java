package org.hyperledger.besu.consensus.common.bft.blockcreation;

import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.validator.ValidatorProvider;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.chain.Blockchain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ReputationProposerSelector implements ProposerSelector {

  private final Blockchain blockchain;
  private final ValidatorProvider validatorProvider;

  public ReputationProposerSelector(
      final Blockchain blockchain,
      final ValidatorProvider validatorProvider) {
    this.blockchain = blockchain;
    this.validatorProvider = validatorProvider;
  }

  @Override
  public Address selectProposerForRound(final ConsensusRoundIdentifier roundIdentifier) {
    final Collection<Address> validators =
        validatorProvider.getValidatorsAfterBlock(blockchain.getChainHeadHeader());

    final List<Address> committee = new ArrayList<>(validators);
    committee.sort(Address::compareTo);

    if (committee.isEmpty()) {
      throw new IllegalStateException("Reputation committee cannot be empty");
    }

    final long blockNumber = roundIdentifier.getSequenceNumber();
    final int roundNumber = roundIdentifier.getRoundNumber();

    final int index =
        Math.floorMod((int) (blockNumber + roundNumber), committee.size());

    return committee.get(index);
  }
}