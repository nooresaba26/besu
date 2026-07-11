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

/** Converts reputation scores between decimal form and deterministic fixed-point integer form. */
public final class ReputationScoreScale {

  public static final long SCALE = 1_000_000L;

  private ReputationScoreScale() {}

  public static long fromDouble(final double score) {
    if (!Double.isFinite(score)) {
      throw new IllegalArgumentException("Reputation score must be finite");
    }

    final double clampedScore = Math.max(0.0, Math.min(1.0, score));

    return Math.round(clampedScore * SCALE);
  }

  public static double toDouble(final long scaledScore) {
    if (scaledScore < 0 || scaledScore > SCALE) {
      throw new IllegalArgumentException("Scaled reputation score must be between 0 and " + SCALE);
    }

    return (double) scaledScore / SCALE;
  }
}
