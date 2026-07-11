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

/** Network-level adaptive reputation weights for one block height. */
public record FuzzyWeights(double alpha, double beta, double gamma) {

  public FuzzyWeights(final double alpha, final double beta, final double gamma) {

    if (!Double.isFinite(alpha) || !Double.isFinite(beta) || !Double.isFinite(gamma)) {
      throw new IllegalArgumentException("Fuzzy weights must be finite");
    }

    if (alpha < 0.0 || beta < 0.0 || gamma < 0.0) {
      throw new IllegalArgumentException("Fuzzy weights cannot be negative");
    }

    final double total = alpha + beta + gamma;

    if (total <= 0.0) {
      throw new IllegalArgumentException("Fuzzy weight total must be greater than zero");
    }

    this.alpha = alpha;
    this.beta = beta;
    this.gamma = gamma;
  }
}
