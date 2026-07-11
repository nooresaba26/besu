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

/**
 * One validator's base reputation score calculated for one block height.
 *
 * <p>This is the score before time decay and before participation balance.
 */
public record HistoricalReputationScore(Address validator, long blockNumber, long scaledScore) {

  public HistoricalReputationScore(
      final Address validator, final long blockNumber, final long scaledScore) {

    if (validator == null) {
      throw new IllegalArgumentException("Validator cannot be null");
    }

    if (blockNumber < 0) {
      throw new IllegalArgumentException("Block number cannot be negative");
    }

    if (scaledScore < 0 || scaledScore > ReputationScoreScale.SCALE) {
      throw new IllegalArgumentException(
          "Scaled score must be between 0 and " + ReputationScoreScale.SCALE);
    }

    this.validator = validator;
    this.blockNumber = blockNumber;
    this.scaledScore = scaledScore;
  }

  public double score() {
    return ReputationScoreScale.toDouble(scaledScore);
  }
}
