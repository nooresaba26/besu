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

import java.nio.ByteBuffer;

/** Encodes and decodes fixed-point reputation scores for persistent storage. */
public final class ReputationScoreStorageValue {

  private ReputationScoreStorageValue() {}

  public static byte[] encode(final long scaledScore) {
    if (scaledScore < 0 || scaledScore > ReputationScoreScale.SCALE) {
      throw new IllegalArgumentException(
          "Scaled score must be between 0 and " + ReputationScoreScale.SCALE);
    }

    return ByteBuffer.allocate(Long.BYTES).putLong(scaledScore).array();
  }

  public static long decode(final byte[] encodedValue) {
    if (encodedValue == null || encodedValue.length != Long.BYTES) {
      throw new IllegalArgumentException(
          "Stored reputation score must contain exactly " + Long.BYTES + " bytes");
    }

    final long scaledScore = ByteBuffer.wrap(encodedValue).getLong();

    if (scaledScore < 0 || scaledScore > ReputationScoreScale.SCALE) {
      throw new IllegalStateException("Stored reputation score is outside the valid range");
    }

    return scaledScore;
  }
}
