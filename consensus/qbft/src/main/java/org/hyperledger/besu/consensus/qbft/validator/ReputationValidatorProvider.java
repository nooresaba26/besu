/*
 * Copyright contributors to Besu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
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
  private final WeightedValidatorSelector selector;
  private final ReputationCandidateProvider candidateProvider;
  private final ValidatorProvider delegate;

  private final Cache<Hash, Collection<Address>> committeeCache =
      CacheBuilder.newBuilder().maximumSize(256).build();

  public ReputationValidatorProvider(
      final Blockchain blockchain,
      final ReputationCandidateProvider candidateProvider,
      final WeightedValidatorSelector selector,
      final ValidatorProvider delegate) {
    this.blockchain = blockchain;
    this.candidateProvider = candidateProvider;
    this.selector = selector;
    this.delegate = delegate;
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
            System.out.println(
                "ReputationValidatorProvider invoked for block " + parentHeader.getNumber());
            final Collection<Address> candidates =
                candidateProvider.getCandidatesAfterBlock(parentHeader);
            System.out.println("Candidate count = " + candidates.size());
            final List<Address> selected = selector.selectValidators(candidates, parentHeader);
            System.out.println("Selected count = " + selected.size());
            System.out.println("Selected validators = " + selected);
            return List.copyOf(selected);
          });
    } catch (final ExecutionException e) {
      throw new RuntimeException("Unable to select reputation validator committee", e);
    }
  }

  @Override
  public Collection<Address> getValidatorsForBlock(final BlockHeader header) {
    if (header.getNumber() == 0) {
      return candidateProvider.getCandidatesAfterBlock(header);
    }

    return blockchain
        .getBlockHeader(header.getParentHash())
        .map(this::getValidatorsAfterBlock)
        .orElseGet(() -> candidateProvider.getCandidatesAfterBlock(header));
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
