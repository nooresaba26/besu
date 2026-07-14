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
package org.hyperledger.besu.consensus.qbft.validator.vrf;

import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.core.BlockHeader;

import java.nio.charset.StandardCharsets;

import org.apache.tuweni.bytes.Bytes;

/** Creates the common deterministic seed used for a validator-selection VRF round. */
public final class VrfSeedGenerator {

  private static final Bytes DOMAIN_SEPARATOR =
      Bytes.wrap("BESU_REPUTATION_VRF_V1".getBytes(StandardCharsets.UTF_8));

  private VrfSeedGenerator() {}

  public static Hash createSeed(final BlockHeader parentHeader) {
    if (parentHeader == null) {
      throw new IllegalArgumentException("Parent header cannot be null");
    }

    final long nextBlockHeight = parentHeader.getNumber() + 1;

    return Hash.hash(
        Bytes.concatenate(
            DOMAIN_SEPARATOR,
            parentHeader.getHash().getBytes(),
            Bytes.ofUnsignedLong(nextBlockHeight)));
  }
}
