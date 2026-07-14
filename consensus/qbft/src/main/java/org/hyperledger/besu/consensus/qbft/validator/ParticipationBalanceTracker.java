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
import org.hyperledger.besu.ethereum.core.BlockHeader;

import java.math.BigInteger;

public class ParticipationBalanceTracker {

  private final ReputationSelectionConfig config;
  private final ValidatorContractController contractController;
  private final Address contractAddress;

  public ParticipationBalanceTracker(
      final ReputationSelectionConfig config,
      final ValidatorContractController contractController,
      final Address contractAddress) {
    this.config = config;
    this.contractController = contractController;
    this.contractAddress = contractAddress;
  }

  public double participationBalance(final Address validator, final BlockHeader parentHeader) {
    try {
      final ValidatorContractController.ValidatorStats stats =
          contractController.getValidatorStats(
              parentHeader.getNumber(), contractAddress, validator);

      final BigInteger count = stats.consecutiveParticipation();

      return Math.pow(config.getDelta(), count.doubleValue());
    } catch (final RuntimeException e) {
      return 1.0;
    }
  }
}
