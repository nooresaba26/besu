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
  private final int timeDecayWindow;
  private final double uptimeThreshold;
private final double successThreshold;
private final double failureThreshold;

  public ReputationSelectionConfig() {
    this.targetCommitteeSize = 10; // temporary
    this.minimumCommitteeSize = 4;  // temporary
    // TODO: replace with dynamically computed k* from safety condition.
    this.ticketScalingFactor = 100;
    this.alpha = 0.3;
    this.beta = 0.3;
    this.gamma = 0.4;
    this.lambda = 0.8;
    this.delta = 0.7;
    this.timeDecayWindow = 5;
//     this.uptimeThreshold = 0.586;
// this.successThreshold = 0.62;
// this.failureThreshold = 0.35;
 this.uptimeThreshold = 0.0;
this.successThreshold = 0.0;
this.failureThreshold = 1.0; //temporary
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

  public int getTimeDecayWindow() {
  return timeDecayWindow;
}
public double getUptimeThreshold() {
  return uptimeThreshold;
}

public double getSuccessThreshold() {
  return successThreshold;
}

public double getFailureThreshold() {
  return failureThreshold;
}
}
