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

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.core.BlockHeader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;

public class WeightedValidatorSelector {

  private final ReputationSelectionConfig config;
  private final ReputationScoreCalculator scoreCalculator;

  public WeightedValidatorSelector(
      final ReputationSelectionConfig config, final ReputationScoreCalculator scoreCalculator) {
    this.config = config;
    this.scoreCalculator = scoreCalculator;
  }

  public List<Address> selectValidators(
      final Collection<Address> candidates, final BlockHeader parentHeader) {
    final List<Address> sortedCandidates = new ArrayList<>(candidates);
    sortedCandidates.sort(null);

    if (sortedCandidates.isEmpty()) {
      return sortedCandidates;
    }

    final int targetSize = Math.min(config.getTargetCommitteeSize(), sortedCandidates.size());

    if (sortedCandidates.size() <= targetSize) {
      return sortedCandidates;
    }

    final List<ScoredValidator> scoredValidators =
        sortedCandidates.stream()
            .map(address -> score(address, parentHeader))
            .sorted(
                Comparator.comparingDouble(ScoredValidator::selectionKey)
                    .reversed()
                    .thenComparing(ScoredValidator::address))
            .limit(targetSize)
            .sorted(Comparator.comparing(ScoredValidator::address))
            .toList();

    return scoredValidators.stream().map(ScoredValidator::address).toList();
  }

  private ScoredValidator score(final Address address, final BlockHeader parentHeader) {
    final double reputationScore = scoreCalculator.calculateScore(address, parentHeader);

    final Hash randomHash =
        Hash.hash(
            Bytes.concatenate(
                parentHeader.getHash().getBytes(),
                address.getBytes(),
                Bytes.ofUnsignedLong(parentHeader.getNumber() + 1)));

    final double randomValue = normalized(randomHash);

    final double selectionKey = reputationScore * randomValue;

    return new ScoredValidator(address, selectionKey);
  }

  private double normalized(final Hash hash) {
    final byte[] bytes = hash.getBytes().toArrayUnsafe();
    long value = 0L;
    for (int i = 0; i < 8; i++) {
      value = (value << 8) | (bytes[i] & 0xffL);
    }
    return (value >>> 1) / (double) Long.MAX_VALUE;
  }

  private record ScoredValidator(Address address, double selectionKey) {}
}
