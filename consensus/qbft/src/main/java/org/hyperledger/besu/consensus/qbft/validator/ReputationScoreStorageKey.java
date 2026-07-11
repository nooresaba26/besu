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

import java.nio.ByteBuffer;

import org.apache.tuweni.bytes.Bytes;

/** Creates deterministic database keys for validator reputation history. */
public final class ReputationScoreStorageKey {

  private static final byte VERSION = 1;

  private ReputationScoreStorageKey() {}

  public static byte[] create(final Address validator, final long blockNumber) {
    if (validator == null) {
      throw new IllegalArgumentException("Validator cannot be null");
    }

    if (blockNumber < 0) {
      throw new IllegalArgumentException("Block number cannot be negative");
    }

    final byte[] addressBytes = Bytes.fromHexString(validator.toHexString()).toArray();

    return ByteBuffer.allocate(1 + addressBytes.length + Long.BYTES)
        .put(VERSION)
        .put(addressBytes)
        .putLong(blockNumber)
        .array();
  }
}
