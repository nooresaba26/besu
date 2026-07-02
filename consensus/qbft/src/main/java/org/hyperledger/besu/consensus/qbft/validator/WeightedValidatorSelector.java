package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.core.BlockHeader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;

public class WeightedValidatorSelector {

  private final ReputationSelectionConfig config;
  private final ReputationScoreCalculator scoreCalculator;

  public WeightedValidatorSelector(
      final ReputationSelectionConfig config, final ReputationScoreCalculator scoreCalculator) {
    this.config = config;
    this.scoreCalculator = scoreCalculator;
  }

  public List<Address> selectValidators(
      final Collection<Address> candidates, final BlockHeader parentHeader) {
    final List<Address> sortedCandidates = new ArrayList<>(candidates);
    sortedCandidates.sort(null);

    if (sortedCandidates.isEmpty()) {
      return sortedCandidates;
    }

    final int targetSize = Math.min(config.getTargetCommitteeSize(), sortedCandidates.size());

    if (sortedCandidates.size() <= targetSize) {
      return sortedCandidates;
    }

    final List<TicketedValidator> ticketedValidators =
        sortedCandidates.stream()
            .map(address -> ticket(address, parentHeader))
            .filter(ticketedValidator -> ticketedValidator.tickets() > 0)
            .toList();

    if (ticketedValidators.isEmpty()) {
      return sortedCandidates.stream().limit(config.getMinimumCommitteeSize()).toList();
    }

    final List<SelectedValidator> selectedValidators =
        ticketedValidators.stream()
            .map(ticketedValidator -> select(ticketedValidator, parentHeader))
            .filter(selectedValidator -> selectedValidator.winningTickets() > 0)
            .sorted(
                Comparator.comparingInt(SelectedValidator::winningTickets)
                    .reversed()
                    .thenComparing(SelectedValidator::address))
            .limit(targetSize)
            .sorted(Comparator.comparing(SelectedValidator::address))
            .toList();

    if (selectedValidators.size() >= config.getMinimumCommitteeSize()) {
      return selectedValidators.stream().map(SelectedValidator::address).toList();
    }

    return ticketedValidators.stream()
        .sorted(
            Comparator.comparingInt(TicketedValidator::tickets)
                .reversed()
                .thenComparing(TicketedValidator::address))
        .limit(config.getMinimumCommitteeSize())
        .map(TicketedValidator::address)
        .sorted()
        .toList();
  }

  private TicketedValidator ticket(final Address address, final BlockHeader parentHeader) {
    final double reputationScore = scoreCalculator.calculateScore(address, parentHeader);
    final int tickets =
        Math.max(0, (int) Math.round(config.getTicketScalingFactor() * reputationScore));

    return new TicketedValidator(address, tickets);
  }

  private SelectedValidator select(
      final TicketedValidator validator, final BlockHeader parentHeader) {
    int winningTickets = 0;

    for (int ticketIndex = 0; ticketIndex < validator.tickets(); ticketIndex++) {
      final Hash ticketHash =
          Hash.hash(
              Bytes.concatenate(
                  parentHeader.getHash().getBytes(),
                  validator.address().getBytes(),
                  Bytes.ofUnsignedLong(parentHeader.getNumber() + 1),
                  Bytes.ofUnsignedLong(ticketIndex)));

      final double randomValue = normalized(ticketHash);

      if (randomValue < selectionProbability()) {
        winningTickets++;
      }
    }

    return new SelectedValidator(validator.address(), winningTickets);
  }

  private double selectionProbability() {
    return Math.min(1.0, config.getTargetCommitteeSize() / (double) config.getTicketScalingFactor());
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
}