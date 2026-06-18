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

public class ReputationSelectionConfig {

  private final int targetCommitteeSize;
  private final int minimumCommitteeSize;
  private final int ticketScalingFactor;
  private final double alpha;
  private final double beta;
  private final double gamma;
  private final double lambda;
  private final double delta;

  public ReputationSelectionConfig() {
    this.targetCommitteeSize = 10;
    this.minimumCommitteeSize = 4;
    this.ticketScalingFactor = 100;
    this.alpha = 0.3;
    this.beta = 0.3;
    this.gamma = 0.4;
    this.lambda = 0.8;
    this.delta = 0.7;
  }

  public int getTargetCommitteeSize() {
    return targetCommitteeSize;
  }

  public int getMinimumCommitteeSize() {
    return minimumCommitteeSize;
  }

  public int getTicketScalingFactor() {
    return ticketScalingFactor;
  }

  public double getAlpha() {
    return alpha;
  }

  public double getBeta() {
    return beta;
  }

  public double getGamma() {
    return gamma;
  }

  public double getLambda() {
    return lambda;
  }

  public double getDelta() {
    return delta;
  }
}
