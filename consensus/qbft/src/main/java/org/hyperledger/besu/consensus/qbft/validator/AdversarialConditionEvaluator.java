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

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Estimates whether the validator network is operating under adversarial conditions by examining
 * observable validator behaviour.
 */
public class AdversarialConditionEvaluator {

  private static final Logger LOG = LoggerFactory.getLogger(AdversarialConditionEvaluator.class);

  private final ReputationSelectionConfig config;
  private final ValidatorMetricsProvider metricsProvider;

  public AdversarialConditionEvaluator(
      final ReputationSelectionConfig config, final ValidatorMetricsProvider metricsProvider) {
    this.config = config;
    this.metricsProvider = metricsProvider;
  }

  /**
   * Evaluates the current candidate validator population.
   *
   * @param candidates active candidate validators
   * @param parentHeader parent block used to read the current metrics
   * @return estimated network risk assessment
   */
  public NetworkRiskAssessment evaluate(
      final Collection<Address> candidates, final BlockHeader parentHeader) {

    if (candidates.isEmpty()) {
      return new NetworkRiskAssessment(0, 0, 0.0, false);
    }

    int suspectedMaliciousCount = 0;

    for (final Address validator : candidates) {
      if (isSuspectedMalicious(validator, parentHeader)) {
        suspectedMaliciousCount++;
      }
    }

    final double suspectedMaliciousRatio = suspectedMaliciousCount / (double) candidates.size();

    final boolean adversarial = suspectedMaliciousRatio >= config.getAdversarialActivationRatio();

    final NetworkRiskAssessment assessment =
        new NetworkRiskAssessment(
            candidates.size(), suspectedMaliciousCount, suspectedMaliciousRatio, adversarial);

    LOG.info(
        "Network risk assessment at block {}: candidates={} suspectedMalicious={} "
            + "suspectedRatio={} activationRatio={} adversarial={}",
        parentHeader.getNumber() + 1,
        assessment.candidateCount(),
        assessment.suspectedMaliciousCount(),
        assessment.suspectedMaliciousRatio(),
        config.getAdversarialActivationRatio(),
        assessment.adversarial());

    return assessment;
  }

  /**
   * Classifies a validator as suspected malicious when any behavioural limit is violated.
   *
   * @param validator validator being evaluated
   * @param parentHeader block used to obtain the validator metrics
   * @return true when the validator satisfies at least one suspected-malicious condition
   */
  public boolean isSuspectedMalicious(final Address validator, final BlockHeader parentHeader) {

    final double uptime = metricsProvider.uptime(validator, parentHeader);
    final double successRate = metricsProvider.successRate(validator, parentHeader);
    final double failureRate = metricsProvider.failureRate(validator, parentHeader);

    final boolean lowUptime = uptime <= config.getSuspectedMaliciousUptimeMaximum();

    final boolean lowSuccess = successRate <= config.getSuspectedMaliciousSuccessMaximum();

    final boolean highFailure = failureRate >= config.getSuspectedMaliciousFailureMinimum();

    final boolean suspectedMalicious = lowUptime || lowSuccess || highFailure;

    LOG.debug(
        "Validator risk classification: validator={} block={} uptime={} success={} failure={} "
            + "lowUptime={} lowSuccess={} highFailure={} suspectedMalicious={}",
        validator,
        parentHeader.getNumber() + 1,
        uptime,
        successRate,
        failureRate,
        lowUptime,
        lowSuccess,
        highFailure,
        suspectedMalicious);

    return suspectedMalicious;
  }
}
