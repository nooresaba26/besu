/*
 * Copyright contributors to Besu.
 *
 * Licensed under the Apache License, Version 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.consensus.qbft.validator;

import org.hyperledger.besu.datatypes.Address;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Stores the locally computed reputation committee for each block height. */
public class SelectedCommitteeStore {

  private final Map<Long, List<Address>> committees =
      new ConcurrentHashMap<>();

  public void put(
      final long blockHeight,
      final Collection<Address> validators) {

    committees.put(
        blockHeight,
        validators.stream().sorted().toList());
  }

  public List<Address> get(final long blockHeight) {
    return committees.getOrDefault(blockHeight, List.of());
  }

  public void remove(final long blockHeight) {
    committees.remove(blockHeight);
  }
}