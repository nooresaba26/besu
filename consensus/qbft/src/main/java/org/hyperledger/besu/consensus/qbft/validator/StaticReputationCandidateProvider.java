package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.BlockHeader;

import java.util.Collection;
import java.util.List;

public class StaticReputationCandidateProvider implements ReputationCandidateProvider {

  private final List<Address> candidates;

  public StaticReputationCandidateProvider(final List<Address> candidates) {
    this.candidates = List.copyOf(candidates);
  }

  @Override
  public Collection<Address> getCandidatesAfterBlock(final BlockHeader parentHeader) {
    return candidates;
  }
}