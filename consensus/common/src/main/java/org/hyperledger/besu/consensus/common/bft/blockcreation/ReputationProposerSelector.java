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
      final Blockchain blockchain, final ValidatorProvider validatorProvider) {
    this.blockchain = blockchain;
    this.validatorProvider = validatorProvider;
  }

  @Override
  public Address selectProposerForRound(final ConsensusRoundIdentifier roundIdentifier) {
    final Collection<Address> validators =
      final long parentBlockNumber = roundIdentifier.getSequenceNumber() - 1;

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

    final long blockNumber = roundIdentifier.getSequenceNumber();
    final int roundNumber = roundIdentifier.getRoundNumber();

    final int index = Math.floorMod((int) (blockNumber + roundNumber), committee.size());

    return committee.get(index);
  }
}
