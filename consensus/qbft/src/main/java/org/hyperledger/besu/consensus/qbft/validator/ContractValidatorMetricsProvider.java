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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContractValidatorMetricsProvider implements ValidatorMetricsProvider {

  private final ValidatorContractController contractController;
  private final Address contractAddress;
  private final ValidatorMetricsProvider fallback;

  public ContractValidatorMetricsProvider(
      final ValidatorContractController contractController,
      final Address contractAddress,
      final ValidatorMetricsProvider fallback) {
    this.contractController = contractController;
    this.contractAddress = contractAddress;
    this.fallback = fallback;
  }

  private static final Logger LOG = LoggerFactory.getLogger(ContractValidatorMetricsProvider.class);
  private static final BigInteger MIN_OBSERVED_BLOCKS = BigInteger.valueOf(10);
  private static final BigInteger MIN_PARTICIPATED_ROUNDS = BigInteger.valueOf(3);

  @Override
  public double uptime(final Address validator, final BlockHeader parentHeader) {
    try {
      final ValidatorContractController.ValidatorStats stats = stats(validator, parentHeader);

      LOG.info(
          "Contract stats at block {} for {}: observed={} online={} participated={} "
              + "successful={} unsuccessful={} selected={} consecutive={} active={}",
          parentHeader.getNumber(),
          validator,
          stats.observedBlocks(),
          stats.onlineBlocks(),
          stats.participatedRounds(),
          stats.successfulVotes(),
          stats.unsuccessfulVotes(),
          stats.selectedRounds(),
          stats.consecutiveParticipation(),
          stats.active());

      if (stats.observedBlocks().compareTo(MIN_OBSERVED_BLOCKS) < 0) {
        final double fallbackValue = fallback.uptime(validator, parentHeader);

        LOG.info(
            "Insufficient uptime history for {} at block {}: observed={}/{}. "
                + "Using fallback uptime={}",
            validator,
            parentHeader.getNumber(),
            stats.observedBlocks(),
            MIN_OBSERVED_BLOCKS,
            fallbackValue);

        return fallbackValue;
      }

      return clampRatio(stats.onlineBlocks(), stats.observedBlocks());
    } catch (final RuntimeException exception) {
      final double fallbackValue = fallback.uptime(validator, parentHeader);

      LOG.warn(
          "Could not read contract uptime for {} at block {}. Using fallback uptime={}",
          validator,
          parentHeader.getNumber(),
          fallbackValue,
          exception);

      return fallbackValue;
    }
  }

  @Override
  public double successRate(final Address validator, final BlockHeader parentHeader) {
    try {
      final ValidatorContractController.ValidatorStats stats = stats(validator, parentHeader);

      if (stats.participatedRounds().compareTo(MIN_PARTICIPATED_ROUNDS) < 0) {
        final double fallbackValue = fallback.successRate(validator, parentHeader);

        LOG.info(
            "Insufficient success history for {} at block {}: participated={}/{}. "
                + "Using fallback success={}",
            validator,
            parentHeader.getNumber(),
            stats.participatedRounds(),
            MIN_PARTICIPATED_ROUNDS,
            fallbackValue);

        return fallbackValue;
      }

      return clampRatio(stats.successfulVotes(), stats.participatedRounds());
    } catch (final RuntimeException exception) {
      final double fallbackValue = fallback.successRate(validator, parentHeader);

      LOG.warn(
          "Could not read contract success for {} at block {}. Using fallback success={}",
          validator,
          parentHeader.getNumber(),
          fallbackValue,
          exception);

      return fallbackValue;
    }
  }

  @Override
  public double failureRate(final Address validator, final BlockHeader parentHeader) {
    try {
      final ValidatorContractController.ValidatorStats stats = stats(validator, parentHeader);

      if (stats.participatedRounds().compareTo(MIN_PARTICIPATED_ROUNDS) < 0) {
        final double fallbackValue = fallback.failureRate(validator, parentHeader);

        LOG.info(
            "Insufficient failure history for {} at block {}: participated={}/{}. "
                + "Using fallback failure={}",
            validator,
            parentHeader.getNumber(),
            stats.participatedRounds(),
            MIN_PARTICIPATED_ROUNDS,
            fallbackValue);

        return fallbackValue;
      }

      return clampRatio(stats.unsuccessfulVotes(), stats.participatedRounds());
    } catch (final RuntimeException exception) {
      final double fallbackValue = fallback.failureRate(validator, parentHeader);

      LOG.warn(
          "Could not read contract failure for {} at block {}. Using fallback failure={}",
          validator,
          parentHeader.getNumber(),
          fallbackValue,
          exception);

      return fallbackValue;
    }
  }

  private double clampRatio(final BigInteger numerator, final BigInteger denominator) {

    if (denominator.signum() <= 0) {
      return 0.0;
    }

    final double ratio = numerator.doubleValue() / denominator.doubleValue();

    return Math.max(0.0, Math.min(1.0, ratio));
  }

  private ValidatorContractController.ValidatorStats stats(
      final Address validator, final BlockHeader parentHeader) {
    return contractController.getValidatorStats(
        parentHeader.getNumber(), contractAddress, validator);
  }
}
