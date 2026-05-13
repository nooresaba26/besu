package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.consensus.common.validator.ValidatorProvider;
import org.hyperledger.besu.consensus.common.validator.VoteProvider;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.BlockHeader;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class ReputationValidatorProvider implements ValidatorProvider {

  private final Blockchain blockchain;
  // private final ValidatorProvider delegate;
  private final WeightedValidatorSelector selector;
  private final ReputationCandidateProvider candidateProvider;

  private final Cache<Hash, Collection<Address>> committeeCache =
      CacheBuilder.newBuilder().maximumSize(256).build();

  public ReputationValidatorProvider(
      final Blockchain blockchain,
      final ValidatorProvider delegate,
      final WeightedValidatorSelector selector) {
    this.blockchain = blockchain;
    this.delegate = delegate;
    this.selector = selector;
  }

  @Override
  public Collection<Address> getValidatorsAtHead() {
    return getValidatorsAfterBlock(blockchain.getChainHeadHeader());
  }

  @Override
  public Collection<Address> getValidatorsAfterBlock(final BlockHeader parentHeader) {
    try {
      return committeeCache.get(
          parentHeader.getHash(),
          () -> {
            // final Collection<Address> candidates = delegate.getValidatorsAfterBlock(parentHeader);
            final Collection<Address> candidates = candidateProvider.getCandidatesAfterBlock(parentHeader);
            final List<Address> selected = selector.selectValidators(candidates, parentHeader);
            return List.copyOf(selected);
          });
    } catch (final ExecutionException e) {
      throw new RuntimeException("Unable to select reputation validator committee", e);
    }
  }

  @Override
  public Collection<Address> getValidatorsForBlock(final BlockHeader header) {
    if (header.getNumber() == 0) {
      return delegate.getValidatorsForBlock(header);
    }

    return blockchain
        .getBlockHeader(header.getParentHash())
        .map(this::getValidatorsAfterBlock)
        .orElseGet(() -> delegate.getValidatorsForBlock(header));
  }

  @Override
  public Optional<VoteProvider> getVoteProviderAtHead() {
    return delegate.getVoteProviderAtHead();
  }

  @Override
  public Optional<VoteProvider> getVoteProviderAfterBlock(final BlockHeader header) {
    return delegate.getVoteProviderAfterBlock(header);
  }
}
