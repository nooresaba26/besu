package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.BlockHeader;

import java.util.Collection;

public interface ReputationCandidateProvider {

  Collection<Address> getCandidatesAfterBlock(BlockHeader parentHeader);
}