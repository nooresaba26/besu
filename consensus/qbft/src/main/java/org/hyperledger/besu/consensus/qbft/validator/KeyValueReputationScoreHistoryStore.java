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
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorageTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Stores historical validator base reputation scores in Besu's persistent key-value database. */
public class KeyValueReputationScoreHistoryStore implements ReputationScoreHistoryStore {

  private final KeyValueStorage storage;

  public KeyValueReputationScoreHistoryStore(final KeyValueStorage storage) {
    if (storage == null) {
      throw new IllegalArgumentException("Key-value storage cannot be null");
    }

    this.storage = storage;
  }

  @Override
  public void save(final Address validator, final long blockNumber, final long scaledScore) {

    final byte[] key = ReputationScoreStorageKey.create(validator, blockNumber);
    final byte[] value = ReputationScoreStorageValue.encode(scaledScore);

    final KeyValueStorageTransaction transaction = storage.startTransaction();

    try {
      transaction.put(key, value);
      transaction.commit();
    } catch (final RuntimeException exception) {
      transaction.rollback();
      throw exception;
    }
  }

  @Override
  public Optional<HistoricalReputationScore> get(final Address validator, final long blockNumber) {

    final byte[] key = ReputationScoreStorageKey.create(validator, blockNumber);

    return storage
        .get(key)
        .map(ReputationScoreStorageValue::decode)
        .map(scaledScore -> new HistoricalReputationScore(validator, blockNumber, scaledScore));
  }

  @Override
  public List<HistoricalReputationScore> getRecent(
      final Address validator, final long currentBlockNumber, final int maximumEntries) {

    if (maximumEntries < 0) {
      throw new IllegalArgumentException("Maximum entries cannot be negative");
    }

    final List<HistoricalReputationScore> scores = new ArrayList<>();

    for (int offset = 0; offset < maximumEntries; offset++) {
      final long blockNumber = currentBlockNumber - offset;

      if (blockNumber < 0) {
        break;
      }

      get(validator, blockNumber).ifPresent(scores::add);
    }

    return List.copyOf(scores);
  }

  @Override
  public void delete(final Address validator, final long blockNumber) {
    final byte[] key = ReputationScoreStorageKey.create(validator, blockNumber);
    final KeyValueStorageTransaction transaction = storage.startTransaction();

    try {
      transaction.remove(key);
      transaction.commit();
    } catch (final RuntimeException exception) {
      transaction.rollback();
      throw exception;
    }
  }
}
