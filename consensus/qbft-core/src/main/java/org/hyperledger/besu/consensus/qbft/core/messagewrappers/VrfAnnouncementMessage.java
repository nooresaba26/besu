/*
 * Copyright contributors to Besu.
 *
 * Licensed under the Apache License, Version 2.0.
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

  public VrfAnnouncementMessage(
      final SignedData<VrfAnnouncementPayload> payload) {
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

    return new VrfAnnouncementMessage(
        readPayload(rlpInput, VrfAnnouncementPayload::readFrom));
  }
}