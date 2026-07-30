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

import org.hyperledger.besu.consensus.qbft.validator.vrf.P256TaiVrfService;
import org.hyperledger.besu.consensus.qbft.validator.vrf.VrfAnnouncement;
import org.hyperledger.besu.consensus.qbft.validator.vrf.VrfAnnouncementStore;
import org.hyperledger.besu.consensus.qbft.validator.vrf.VrfProof;
import org.hyperledger.besu.consensus.qbft.validator.vrf.VrfSeedGenerator;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.core.BlockHeader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeightedValidatorSelector {

  private static final Logger LOG = LoggerFactory.getLogger(WeightedValidatorSelector.class);

  private final ReputationSelectionConfig config;
  private final ReputationScoreCalculator scoreCalculator;
  private final AdaptiveThresholdCalculator adaptiveThresholdCalculator;
  private final FuzzyWeightCalculator fuzzyWeightCalculator;
  private final P256TaiVrfService vrfService;
  private final VrfAnnouncementStore vrfAnnouncementStore;
  private final SelectedCommitteeStore selectedCommitteeStore;
  private final AdversarialConditionEvaluator adversarialConditionEvaluator;
  private static final int PROTOCOL_MINIMUM_COMMITTEE_SIZE = 3;

  public WeightedValidatorSelector(
      final ReputationSelectionConfig config,
      final ReputationScoreCalculator scoreCalculator,
      final P256TaiVrfService vrfService,
      final VrfAnnouncementStore vrfAnnouncementStore,
      final SelectedCommitteeStore selectedCommitteeStore) {

    this.config = config;
    this.scoreCalculator = scoreCalculator;
    this.vrfService = vrfService;
    this.vrfAnnouncementStore = vrfAnnouncementStore;
    this.selectedCommitteeStore = selectedCommitteeStore;
    this.adaptiveThresholdCalculator = new AdaptiveThresholdCalculator(scoreCalculator);
    this.fuzzyWeightCalculator = new FuzzyWeightCalculator();
    this.adversarialConditionEvaluator = new AdversarialConditionEvaluator(config, scoreCalculator);
  }

  public List<Address> selectValidators(
      final Collection<Address> candidates, final BlockHeader parentHeader) {
    final List<Address> sortedCandidates = new ArrayList<>(candidates);
    sortedCandidates.sort(null);
    // Public deterministic seed for this selection round
    final Bytes vrfSeed =
        Bytes.concatenate(
            parentHeader.getHash().getBytes(), Bytes.ofUnsignedLong(parentHeader.getNumber() + 1));
    final VrfProof localProof = vrfService.generate(vrfSeed);

    LOG.info(
        "Local VRF generated: proof={} output={}",
        localProof.proof().toHexString(),
        localProof.output().toHexString());

    if (sortedCandidates.isEmpty()) {
      return sortedCandidates;
    }
    final NetworkRiskAssessment riskAssessment =
        adversarialConditionEvaluator.evaluate(sortedCandidates, parentHeader);
    generateLocalVrfAnnouncement(sortedCandidates, parentHeader);

    // LOG.debug("VRF service loaded for validator {}", vrfService.getLocalValidator());
    // final int targetSize = Math.min(config.getTargetCommitteeSize(), sortedCandidates.size());

    //    if (sortedCandidates.size() <= 4) {
    //   LOG.info("Small local QBFT network detected. Returning all validators to preserve liveness:
    // {}", sortedCandidates);
    //   return sortedCandidates;
    // }

    final NetworkMetricsSnapshot networkSnapshot =
        adaptiveThresholdCalculator.snapshot(sortedCandidates, parentHeader);

    final AdaptiveThresholds thresholds = adaptiveThresholdCalculator.calculate(networkSnapshot);

    final FuzzyWeights fuzzyWeights = fuzzyWeightCalculator.calculate(networkSnapshot);
    LOG.info(
        "Adaptive threshold filtering at block {}: enabled={} "
            + "estimatedMaliciousRatio={} activationRatio={} "
            + "uptimeThreshold={} successThreshold={} failureThreshold={}",
        parentHeader.getNumber() + 1,
        riskAssessment.adversarial(),
        riskAssessment.suspectedMaliciousRatio(),
        config.getAdversarialActivationRatio(),
        thresholds.uptimeThreshold(),
        thresholds.successThreshold(),
        thresholds.failureThreshold());

    scoreCalculator.storeCurrentBaseScores(sortedCandidates, parentHeader, fuzzyWeights);
    LOG.info(
        "Adaptive thresholds at block {}: uptime25={} success25={} failure75={}",
        parentHeader.getNumber() + 1,
        thresholds.uptimeThreshold(),
        thresholds.successThreshold(),
        thresholds.failureThreshold());

    LOG.info(
        "Adaptive fuzzy weights for score block {}: alpha={} beta={} gamma={} sum={}",
        parentHeader.getNumber(),
        fuzzyWeights.alpha(),
        fuzzyWeights.beta(),
        fuzzyWeights.gamma(),
        fuzzyWeights.alpha() + fuzzyWeights.beta() + fuzzyWeights.gamma());

    final List<TicketedValidator> ticketedValidators =
        sortedCandidates.stream()
            .map(address -> ticket(address, parentHeader, thresholds, riskAssessment.adversarial()))
            .filter(ticketedValidator -> ticketedValidator.tickets() > 0)
            .toList();

    if (ticketedValidators.isEmpty()) {
      LOG.warn("No validators received tickets. Returning all candidates to preserve liveness.");

      selectedCommitteeStore.put(parentHeader.getNumber() + 1, sortedCandidates);

      return sortedCandidates;
    }

    final int kStar = computeKStar(ticketedValidators, riskAssessment);
    LOG.info(
        "Committee adaptation: candidates={} suspectedMaliciousRatio={} adversarial={} kStar={}",
        ticketedValidators.size(),
        riskAssessment.suspectedMaliciousRatio(),
        riskAssessment.adversarial(),
        kStar);
    final double probability = selectionProbability(ticketedValidators, kStar);

    LOG.info("Computed kStar={} Pt={}", kStar, probability);

    System.out.println("Ticket selection probability Pt = " + probability);

    final List<SelectedValidator> selectedValidators =
        ticketedValidators.stream()
            .map(ticketedValidator -> select(ticketedValidator, parentHeader, probability))
            .filter(selectedValidator -> selectedValidator.winningTickets() > 0)
            .sorted(Comparator.comparing(SelectedValidator::address))
            .toList();

    final List<Address> committee =
        ensureRequiredCommitteeSize(selectedValidators, ticketedValidators, kStar, parentHeader);

    LOG.info(
        "Selected committee from probabilistic draw: size={} validators={}",
        committee.size(),
        committee);

    selectedCommitteeStore.put(parentHeader.getNumber() + 1, committee);

    return committee;
  }

  private TicketedValidator ticket(
      final Address address,
      final BlockHeader parentHeader,
      final AdaptiveThresholds thresholds,
      final boolean applyAdaptiveThresholds) {

    if (applyAdaptiveThresholds
        && !scoreCalculator.passesThresholds(address, parentHeader, thresholds)) {

      LOG.info(
          "Validator {} excluded by adaptive threshold filtering at block {}",
          address,
          parentHeader.getNumber() + 1);

      return new TicketedValidator(address, 0);
    }
    final double reputationScore = scoreCalculator.calculateScore(address, parentHeader);
    final int tickets =
        Math.max(1, (int) Math.round(config.getTicketScalingFactor() * reputationScore));

    LOG.info("Validator {} score={} tickets={}", address, reputationScore, tickets);

    return new TicketedValidator(address, tickets);
  }

  private SelectedValidator select(
      final TicketedValidator validator,
      final BlockHeader parentHeader,
      final double selectionProbability) {

    int winningTickets = 0;
    final long blockHeight = parentHeader.getNumber() + 1;

    for (int ticketIndex = 0; ticketIndex < validator.tickets(); ticketIndex++) {

      final Hash ticketHash =
          Hash.hash(
              Bytes.concatenate(
                  parentHeader.getHash().getBytes(),
                  validator.address().getBytes(),
                  Bytes.ofUnsignedLong(blockHeight),
                  Bytes.ofUnsignedLong(ticketIndex)));

      final double randomValue = normalized(ticketHash);

      if (randomValue < selectionProbability) {
        winningTickets++;
      }
    }

    return new SelectedValidator(validator.address(), winningTickets);
  }

  private double normalized(final Hash hash) {
    final byte[] bytes = hash.getBytes().toArrayUnsafe();
    long value = 0L;
    for (int i = 0; i < 8; i++) {
      value = (value << 8) | (bytes[i] & 0xffL);
    }
    return (value >>> 1) / (double) Long.MAX_VALUE;
  }

  private record TicketedValidator(Address address, int tickets) {}

  private record SelectedValidator(Address address, int winningTickets) {}

  private double selectionProbability(
      final List<TicketedValidator> ticketedValidators, final int targetCommitteeSize) {
    double low = 0.0;
    double high = 1.0;

    for (int i = 0; i < 50; i++) {
      final double mid = (low + high) / 2.0;

      final double expectedCommitteeSize =
          ticketedValidators.stream()
              .mapToDouble(v -> 1.0 - Math.pow(1.0 - mid, v.tickets()))
              .sum();

      if (expectedCommitteeSize < targetCommitteeSize) {
        low = mid;
      } else {
        high = mid;
      }
    }

    return (low + high) / 2.0;
  }

  private int computeKStar(
      final List<TicketedValidator> ticketedValidators,
      final NetworkRiskAssessment riskAssessment) {

    final int candidateCount = ticketedValidators.size();

    final double estimatedMaliciousRatio = riskAssessment.suspectedMaliciousRatio();

    final double epsilon = config.getUnsafeCommitteeProbability();

    if (candidateCount == 0) {
      return 0;
    }

    if (estimatedMaliciousRatio >= (1.0 / 3.0)) {
      LOG.warn(
          "Estimated malicious ratio {} is at or above the Byzantine boundary. "
              + "No committee size can provide the requested probabilistic guarantee. "
              + "Using all {} eligible validators.",
          estimatedMaliciousRatio,
          candidateCount);

      return candidateCount;
    }

    final int lowerBound = Math.min(PROTOCOL_MINIMUM_COMMITTEE_SIZE, candidateCount);

    for (int committeeSize = lowerBound; committeeSize <= candidateCount; committeeSize++) {

      final double unsafeProbability =
          unsafeCommitteeProbability(committeeSize, estimatedMaliciousRatio);

      LOG.debug(
          "kStar candidate: committeeSize={} estimatedMaliciousRatio={} "
              + "unsafeProbability={} epsilon={}",
          committeeSize,
          estimatedMaliciousRatio,
          unsafeProbability,
          epsilon);

      if (unsafeProbability <= epsilon) {
        LOG.info(
            "Probability-based kStar selected: candidateCount={} "
                + "estimatedMaliciousRatio={} kStar={} "
                + "unsafeProbability={} epsilon={}",
            candidateCount,
            estimatedMaliciousRatio,
            committeeSize,
            unsafeProbability,
            epsilon);

        return committeeSize;
      }
    }

    final double fullCommitteeUnsafeProbability =
        unsafeCommitteeProbability(candidateCount, estimatedMaliciousRatio);

    LOG.warn(
        "No committee size up to {} satisfies epsilon={}. "
            + "Using all eligible validators. "
            + "estimatedMaliciousRatio={} unsafeProbability={}",
        candidateCount,
        epsilon,
        estimatedMaliciousRatio,
        fullCommitteeUnsafeProbability);

    return candidateCount;
  }

  private double unsafeCommitteeProbability(
      final int committeeSize, final double estimatedMaliciousRatio) {

    if (committeeSize <= 0) {
      return 0.0;
    }

    if (estimatedMaliciousRatio <= 0.0) {
      return 0.0;
    }

    if (estimatedMaliciousRatio >= 1.0) {
      return 1.0;
    }

    final int maliciousThreshold = (int) Math.ceil(committeeSize / 3.0);

    /*
     * X follows Binomial(committeeSize, estimatedMaliciousRatio).
     *
     * Unsafe probability:
     *
     * P(X >= maliciousThreshold)
     *
     * We calculate:
     *
     * 1 - P(X < maliciousThreshold)
     */

    final double honestProbability = 1.0 - estimatedMaliciousRatio;

    double probabilityMass = Math.pow(honestProbability, committeeSize);

    double safeCumulativeProbability = probabilityMass;

    for (int maliciousCount = 1; maliciousCount < maliciousThreshold; maliciousCount++) {

      probabilityMass =
          probabilityMass
              * (committeeSize - maliciousCount + 1)
              / maliciousCount
              * estimatedMaliciousRatio
              / honestProbability;

      safeCumulativeProbability += probabilityMass;
    }

    return Math.max(0.0, Math.min(1.0, 1.0 - safeCumulativeProbability));
  }

  private void generateLocalVrfAnnouncement(
      final List<Address> candidates, final BlockHeader parentHeader) {

    final Address localValidator = vrfService.getLocalValidator();
    final long blockHeight = parentHeader.getNumber() + 1;

    if (!candidates.contains(localValidator)) {
      LOG.debug("Local validator {} is not a candidate for block {}", localValidator, blockHeight);
      return;
    }

    final VrfAnnouncement existingAnnouncement =
        vrfAnnouncementStore.getAnnouncement(blockHeight, localValidator);

    if (existingAnnouncement != null) {
      return;
    }

    final Bytes vrfInput = VrfSeedGenerator.createSeed(parentHeader).getBytes();

    final VrfProof localProof = vrfService.generate(vrfInput);

    final Bytes publicKey = vrfService.getCompressedPublicKey();

    final boolean valid = vrfService.verify(vrfInput, localProof, publicKey);

    if (!valid) {
      throw new IllegalStateException(
          "Generated VRF proof failed verification for validator " + localValidator);
    }

    LOG.info(
        "Local VRF proof verified: block={} validator={} output={}",
        blockHeight,
        localValidator,
        localProof.output());

    final VrfAnnouncement announcement =
        new VrfAnnouncement(
            blockHeight, localValidator, publicKey, localProof.output(), localProof.proof());

    vrfAnnouncementStore.put(announcement);

    final VrfAnnouncement storedAnnouncement =
        vrfAnnouncementStore.getAnnouncement(blockHeight, localValidator);

    if (storedAnnouncement == null) {
      throw new IllegalStateException(
          "VRF announcement was not stored for validator "
              + localValidator
              + " at block "
              + blockHeight);
    }

    LOG.info(
        "Stored local VRF announcement: block={} validator={} output={}",
        storedAnnouncement.blockHeight(),
        storedAnnouncement.validator(),
        storedAnnouncement.output());
  }

  private List<Address> ensureRequiredCommitteeSize(
      final List<SelectedValidator> selectedValidators,
      final List<TicketedValidator> ticketedValidators,
      final int kStar,
      final BlockHeader parentHeader) {

    final List<Address> committee =
        new ArrayList<>(selectedValidators.stream().map(SelectedValidator::address).toList());

    final int requiredSize = Math.min(kStar, ticketedValidators.size());

    if (committee.size() >= requiredSize) {
      return committee;
    }

    final long blockHeight = parentHeader.getNumber() + 1;

    final List<Address> deterministicFallback =
        ticketedValidators.stream()
            .map(TicketedValidator::address)
            .filter(address -> !committee.contains(address))
            .sorted(
                Comparator.comparing(
                    address ->
                        Hash.hash(
                            Bytes.concatenate(
                                parentHeader.getHash().getBytes(),
                                address.getBytes(),
                                Bytes.ofUnsignedLong(blockHeight)))))
            .toList();

    final int missingValidators = requiredSize - committee.size();

    committee.addAll(deterministicFallback.stream().limit(missingValidators).toList());

    committee.sort(null);

    LOG.warn(
        "Probabilistic draw selected fewer validators than kStar. "
            + "Deterministically completed committee: required={} finalSize={} validators={}",
        requiredSize,
        committee.size(),
        committee);

    return committee;
  }
}
