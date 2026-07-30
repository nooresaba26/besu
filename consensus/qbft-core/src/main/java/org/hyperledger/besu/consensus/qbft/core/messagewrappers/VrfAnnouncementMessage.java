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
package org.hyperledger.besu.consensus.qbft.core.messagewrappers;

import org.hyperledger.besu.consensus.common.bft.messagewrappers.BftMessage;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.qbft.core.payload.VrfAnnouncementPayload;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.rlp.RLPInput;

import org.apache.tuweni.bytes.Bytes;

/** Signed QBFT VRF announcement message. */
public class VrfAnnouncementMessage extends BftMessage<VrfAnnouncementPayload> {

  public VrfAnnouncementMessage(final SignedData<VrfAnnouncementPayload> payload) {
    super(payload);
  }

  public Bytes getPublicKey() {
    return getPayload().getPublicKey();
  }

  public Bytes getOutput() {
    return getPayload().getOutput();
  }

  public Bytes getProof() {
    return getPayload().getProof();
  }

  /**
   * Decodes a signed VRF announcement.
   *
   * @param data encoded message
   * @return decoded message
   */
  public static VrfAnnouncementMessage decode(final Bytes data) {
    final RLPInput rlpInput = RLP.input(data);

    return new VrfAnnouncementMessage(readPayload(rlpInput, VrfAnnouncementPayload::readFrom));
  }
}
