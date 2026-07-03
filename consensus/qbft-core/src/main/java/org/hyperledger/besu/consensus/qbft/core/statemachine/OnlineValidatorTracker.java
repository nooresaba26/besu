package org.hyperledger.besu.consensus.qbft.core.statemachine;

import org.hyperledger.besu.datatypes.Address;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineValidatorTracker {

  private final Map<Long, Set<Address>> onlineValidators =
      new ConcurrentHashMap<>();

  public void recordOnline(final long blockHeight, final Address validator) {
    onlineValidators
        .computeIfAbsent(blockHeight, k -> ConcurrentHashMap.newKeySet())
        .add(validator);
  }

  public Collection<Address> getOnlineValidators(final long blockHeight) {
    return onlineValidators.getOrDefault(
        blockHeight,
        Collections.emptySet());
  }

  public void clear(final long blockHeight) {
    onlineValidators.remove(blockHeight);
  }
}