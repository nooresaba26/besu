package org.hyperledger.besu.consensus.common.bft.blockcreation;

import static com.google.common.base.Preconditions.checkArgument;

import org.hyperledger.besu.consensus.common.BlockInterface;
import org.hyperledger.besu.consensus.common.bft.ConsensusRoundIdentifier;
import org.hyperledger.besu.consensus.common.validator.ValidatorProvider;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.BlockHeader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReputationProposerSelector implements ProposerSelector {

  private static final Logger LOG = LoggerFactory.getLogger(ReputationProposerSelector.class);

  private final Blockchain blockchain;
  private final BlockInterface blockInterface;
  private final ValidatorProvider validatorProvider;

  public ReputationProposerSelector(
      final Blockchain blockchain,
      final BlockInterface blockInterface,
      final ValidatorProvider validatorProvider) {
    this.blockchain = blockchain;
    this.blockInterface = blockInterface;
    this.validatorProvider = validatorProvider;
  }

  @Override
  public Address selectProposerForRound(final ConsensusRoundIdentifier roundIdentifier) {
    checkArgument(roundIdentifier.getRoundNumber() >= 0);
    checkArgument(roundIdentifier.getSequenceNumber() > 0);

    final long parentBlockNumber = roundIdentifier.getSequenceNumber() - 1;
    final Optional<BlockHeader> maybeParentHeader = blockchain.getBlockHeader(parentBlockNumber);

    if (maybeParentHeader.isEmpty()) {
      LOG.trace("Unable to determine proposer for requested block {}", parentBlockNumber);
      throw new RuntimeException("Unable to determine parent block for reputation proposer");
    }

    final BlockHeader parentHeader = maybeParentHeader.get();
    final Collection<Address> validatorsForRound =
        validatorProvider.getValidatorsAfterBlock(parentHeader);

    if (validatorsForRound.isEmpty()) {
      throw new RuntimeException("No validators available for reputation proposer selection");
    }

    final List<Address> sortedValidators = new ArrayList<>(validatorsForRound);
    sortedValidators.sort(null);

    final long rotation =
        roundIdentifier.getSequenceNumber() + roundIdentifier.getRoundNumber();

    final int proposerIndex =
        (int) Math.floorMod(rotation, (long) sortedValidators.size());

    return sortedValidators.get(proposerIndex);
  }
}
