package org.hyperledger.besu.consensus.common.bft.blockcreation;

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
  public Address selectProposerForRound(final int round) {
    final Collection<Address> validators =
        validatorProvider.getValidatorsAfterBlock(blockchain.getChainHeadHeader());

    final List<Address> committee = new ArrayList<>(validators);
    committee.sort(Address::compareTo);

    if (committee.isEmpty()) {
      throw new IllegalStateException("Reputation committee cannot be empty");
    }

    final int index =
        Math.floorMod((int) blockchain.getChainHeadHeader().getNumber() + round, committee.size());

    return committee.get(index);
  }
}